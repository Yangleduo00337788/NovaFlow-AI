package ai.novaflow.knowledge.service;

import ai.novaflow.common.audit.AuditRecorder;
import ai.novaflow.common.context.TenantContext;
import ai.novaflow.common.domain.PageResult;
import ai.novaflow.common.exception.BusinessException;
import ai.novaflow.common.util.PageQueryUtils;
import ai.novaflow.knowledge.domain.DocumentTypeSupport;
import ai.novaflow.knowledge.domain.vo.DocumentVO;
import ai.novaflow.knowledge.entity.DocumentEntity;
import ai.novaflow.knowledge.entity.KnowledgeBaseEntity;
import ai.novaflow.knowledge.mapper.DocumentMapper;
import ai.novaflow.knowledge.mapper.KnowledgeBaseMapper;
import ai.novaflow.knowledge.storage.DocumentStorageService;
import ai.novaflow.tenant.entity.TenantEntity;
import ai.novaflow.tenant.mapper.TenantMapper;
import ai.novaflow.tenant.support.TenantQuotas;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import ai.novaflow.knowledge.event.DocumentProcessEvent;
import ai.novaflow.knowledge.event.DocumentVectorDeleteEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private static final long MAX_FILE_SIZE = 50L * 1024 * 1024;

    private final DocumentMapper documentMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final KnowledgeBaseService knowledgeBaseService;
    private final DocumentStorageService documentStorageService;
    private final TenantMapper tenantMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final AuditRecorder auditRecorder;

    public PageResult<DocumentVO> page(Long knowledgeBaseId, int page, int pageSize, String keyword) {
        page = PageQueryUtils.normalizePage(page);
        pageSize = PageQueryUtils.normalizePageSize(pageSize);
        knowledgeBaseService.getKnowledgeBaseOrThrow(knowledgeBaseId);
        QueryWrapper query = QueryWrapper.create()
                .eq("knowledge_base_id", knowledgeBaseId)
                .eq("tenant_id", requireTenantId())
                .eq("is_deleted", 0);
        if (StringUtils.hasText(keyword)) {
            query.like("doc_name", keyword);
        }
        query.orderBy("created_at", false);

        Page<DocumentEntity> result = documentMapper.paginate(Page.of(page, pageSize), query);
        List<DocumentVO> list = result.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(list, result.getTotalRow(), page, pageSize);
    }

    @Transactional
    public DocumentVO upload(Long knowledgeBaseId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的文件");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException("文件大小不能超过 50MB");
        }

        String originalFilename = file.getOriginalFilename();
        if (!StringUtils.hasText(originalFilename)) {
            throw new BusinessException("文件名无效");
        }
        String docType = DocumentTypeSupport.resolveType(originalFilename);
        if (docType == null) {
            throw new BusinessException("不支持的文件类型，支持 PDF/Word/Excel/PPT/TXT/Markdown/HTML");
        }

        KnowledgeBaseEntity knowledgeBase = knowledgeBaseService.getKnowledgeBaseOrThrow(knowledgeBaseId);
        Long tenantId = knowledgeBase.getTenantId();
        Long userId = StpUtil.getLoginIdAsLong();
        assertStorageQuota(tenantId, file.getSize());

        String objectPath = documentStorageService.store(tenantId, knowledgeBaseId, originalFilename, file);
        String fileHash = computeHash(file);

        DocumentEntity entity = new DocumentEntity();
        entity.setTenantId(tenantId);
        entity.setKnowledgeBaseId(knowledgeBaseId);
        entity.setDocName(originalFilename);
        entity.setDocType(docType);
        entity.setFilePath(objectPath);
        entity.setFileSize(file.getSize());
        entity.setFileHash(fileHash);
        entity.setSourceType("upload");
        entity.setProcessStatus(0);
        entity.setChunkCount(0);
        entity.setCharCount(0);
        entity.setCreatedBy(userId);
        entity.setIsDeleted(0);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        documentMapper.insert(entity);

        knowledgeBase.setDocumentCount(safeInt(knowledgeBase.getDocumentCount()) + 1);
        knowledgeBase.setTotalSizeBytes(safeLong(knowledgeBase.getTotalSizeBytes()) + file.getSize());
        knowledgeBase.setUpdatedAt(LocalDateTime.now());
        knowledgeBaseMapper.update(knowledgeBase);

        eventPublisher.publishEvent(new DocumentProcessEvent(this, entity.getId(), knowledgeBaseId, tenantId));
        return toVO(entity);
    }

    @Transactional
    public void delete(Long knowledgeBaseId, Long documentId) {
        knowledgeBaseService.getKnowledgeBaseOrThrow(knowledgeBaseId);
        DocumentEntity entity = documentMapper.selectOneByQuery(
                QueryWrapper.create()
                        .eq("id", documentId)
                        .eq("knowledge_base_id", knowledgeBaseId)
                        .eq("tenant_id", requireTenantId())
                        .eq("is_deleted", 0)
        );
        if (entity == null) {
            throw new BusinessException("文档不存在");
        }

        documentStorageService.delete(entity.getFilePath());
        eventPublisher.publishEvent(new DocumentVectorDeleteEvent(this, entity.getId(), knowledgeBaseId, entity.getTenantId()));
        entity.setIsDeleted(1);
        entity.setUpdatedAt(LocalDateTime.now());
        documentMapper.update(entity);

        KnowledgeBaseEntity knowledgeBase = knowledgeBaseService.getKnowledgeBaseOrThrow(knowledgeBaseId);
        knowledgeBase.setDocumentCount(Math.max(0, safeInt(knowledgeBase.getDocumentCount()) - 1));
        knowledgeBase.setTotalSizeBytes(Math.max(0L, safeLong(knowledgeBase.getTotalSizeBytes()) - safeLong(entity.getFileSize())));
        knowledgeBase.setChunkCount(Math.max(0, safeInt(knowledgeBase.getChunkCount()) - safeInt(entity.getChunkCount())));
        knowledgeBase.setUpdatedAt(LocalDateTime.now());
        knowledgeBaseMapper.update(knowledgeBase);
        auditRecorder.record("document.delete", "document", entity.getId(),
                "删除文档: " + entity.getDocName() + "（知识库 ID " + knowledgeBaseId + "）");
    }

    public void triggerReprocess(Long knowledgeBaseId, Long documentId) {
        knowledgeBaseService.getKnowledgeBaseOrThrow(knowledgeBaseId);
        DocumentEntity entity = documentMapper.selectOneByQuery(
                QueryWrapper.create()
                        .eq("id", documentId)
                        .eq("knowledge_base_id", knowledgeBaseId)
                        .eq("tenant_id", requireTenantId())
                        .eq("is_deleted", 0)
        );
        if (entity == null) {
            throw new BusinessException("文档不存在");
        }
        entity.setProcessStatus(0);
        entity.setProcessError(null);
        entity.setUpdatedAt(LocalDateTime.now());
        documentMapper.update(entity);
        eventPublisher.publishEvent(new DocumentProcessEvent(this, documentId, knowledgeBaseId, entity.getTenantId()));
    }

    private String computeHash(MultipartFile file) {
        try {
            return DigestUtil.md5Hex(file.getInputStream());
        } catch (IOException e) {
            return null;
        }
    }

    private Long requireTenantId() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BusinessException("租户上下文缺失");
        }
        return tenantId;
    }

    private void assertStorageQuota(Long tenantId, long incomingBytes) {
        TenantEntity tenant = tenantMapper.selectOneById(tenantId);
        if (tenant == null) {
            return;
        }
        long limitMb = tenant.getMaxStorageMb() != null && tenant.getMaxStorageMb() > 0
                ? tenant.getMaxStorageMb()
                : 0;
        if (limitMb <= 0) {
            return;
        }
        Long used = documentMapper.sumFileSizeByTenant(tenantId);
        TenantQuotas.assertStorageWithinLimit(
                used != null ? used : 0L,
                incomingBytes,
                limitMb * 1024L * 1024L);
    }

    private int safeInt(Integer value) {
        return value != null ? value : 0;
    }

    private long safeLong(Long value) {
        return value != null ? value : 0L;
    }

    private DocumentVO toVO(DocumentEntity entity) {
        return DocumentVO.builder()
                .id(entity.getId())
                .knowledgeBaseId(entity.getKnowledgeBaseId())
                .docName(entity.getDocName())
                .docType(entity.getDocType())
                .fileSize(entity.getFileSize())
                .fileHash(entity.getFileHash())
                .sourceType(entity.getSourceType())
                .processStatus(entity.getProcessStatus())
                .processStatusLabel(DocumentTypeSupport.processStatusLabel(entity.getProcessStatus()))
                .processError(entity.getProcessError())
                .chunkCount(entity.getChunkCount())
                .charCount(entity.getCharCount())
                .processedAt(entity.getProcessedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
