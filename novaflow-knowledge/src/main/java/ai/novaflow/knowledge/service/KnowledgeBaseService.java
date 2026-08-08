package ai.novaflow.knowledge.service;

import ai.novaflow.common.context.TenantContext;
import ai.novaflow.common.domain.PageResult;
import ai.novaflow.common.exception.BusinessException;
import ai.novaflow.knowledge.domain.dto.KnowledgeBaseSaveRequest;
import ai.novaflow.knowledge.domain.vo.KnowledgeBaseVO;
import ai.novaflow.knowledge.entity.DocumentEntity;
import ai.novaflow.knowledge.entity.KnowledgeBaseEntity;
import ai.novaflow.knowledge.mapper.DocumentMapper;
import ai.novaflow.knowledge.mapper.KnowledgeBaseMapper;
import ai.novaflow.knowledge.storage.DocumentStorageService;
import ai.novaflow.user.service.RecentAccessService;
import cn.dev33.satoken.stp.StpUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class KnowledgeBaseService {

    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final DocumentMapper documentMapper;
    private final DocumentStorageService documentStorageService;
    private final RecentAccessService recentAccessService;

    public PageResult<KnowledgeBaseVO> page(int page, int pageSize, String keyword) {
        Long tenantId = requireTenantId();
        QueryWrapper query = QueryWrapper.create()
                .eq("tenant_id", tenantId)
                .eq("is_deleted", 0);
        if (StringUtils.hasText(keyword)) {
            query.like("kb_name", keyword);
        }
        query.orderBy("updated_at", false);

        Page<KnowledgeBaseEntity> result = knowledgeBaseMapper.paginate(Page.of(page, pageSize), query);
        List<KnowledgeBaseVO> list = result.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(list, result.getTotalRow(), page, pageSize);
    }

    public KnowledgeBaseVO detail(Long id) {
        KnowledgeBaseEntity entity = getKnowledgeBaseOrThrow(id);
        recordRecentAccess(entity);
        return toVO(entity);
    }

    @Transactional
    public KnowledgeBaseVO create(KnowledgeBaseSaveRequest request) {
        Long tenantId = requireTenantId();
        Long userId = StpUtil.getLoginIdAsLong();
        ensureNameUnique(tenantId, request.getKbName(), null);

        KnowledgeBaseEntity entity = new KnowledgeBaseEntity();
        entity.setTenantId(tenantId);
        entity.setApplicationId(request.getApplicationId() != null ? request.getApplicationId() : 1L);
        applyRequest(entity, request);
        entity.setDocumentCount(0);
        entity.setChunkCount(0);
        entity.setTotalSizeBytes(0L);
        entity.setStatus(1);
        entity.setCreatedBy(userId);
        entity.setIsDeleted(0);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        knowledgeBaseMapper.insert(entity);
        entity.setQdrantCollection(buildQdrantCollection(tenantId, entity.getId()));
        knowledgeBaseMapper.update(entity);
        return toVO(entity);
    }

    private String buildQdrantCollection(Long tenantId, Long knowledgeBaseId) {
        return "kb_" + tenantId + "_" + knowledgeBaseId;
    }

    @Transactional
    public KnowledgeBaseVO update(Long id, KnowledgeBaseSaveRequest request) {
        KnowledgeBaseEntity entity = getKnowledgeBaseOrThrow(id);
        ensureNameUnique(entity.getTenantId(), request.getKbName(), id);
        applyRequest(entity, request);
        entity.setUpdatedAt(LocalDateTime.now());
        knowledgeBaseMapper.update(entity);
        return toVO(entity);
    }

    @Transactional
    public void delete(Long id) {
        KnowledgeBaseEntity entity = getKnowledgeBaseOrThrow(id);
        List<DocumentEntity> documents = documentMapper.selectListByQuery(
                QueryWrapper.create()
                        .eq("knowledge_base_id", id)
                        .eq("tenant_id", entity.getTenantId())
                        .eq("is_deleted", 0)
        );
        for (DocumentEntity document : documents) {
            documentStorageService.delete(document.getFilePath());
            document.setIsDeleted(1);
            document.setUpdatedAt(LocalDateTime.now());
            documentMapper.update(document);
        }
        entity.setIsDeleted(1);
        entity.setUpdatedAt(LocalDateTime.now());
        knowledgeBaseMapper.update(entity);
    }

    public KnowledgeBaseEntity getKnowledgeBaseOrThrow(Long id) {
        KnowledgeBaseEntity entity = knowledgeBaseMapper.selectOneByQuery(
                QueryWrapper.create()
                        .eq("id", id)
                        .eq("tenant_id", requireTenantId())
                        .eq("is_deleted", 0)
        );
        if (entity == null) {
            throw new BusinessException("知识库不存在");
        }
        return entity;
    }

    private void ensureNameUnique(Long tenantId, String kbName, Long excludeId) {
        QueryWrapper query = QueryWrapper.create()
                .eq("tenant_id", tenantId)
                .eq("kb_name", kbName)
                .eq("is_deleted", 0);
        if (excludeId != null) {
            query.ne("id", excludeId);
        }
        if (knowledgeBaseMapper.selectCountByQuery(query) > 0) {
            throw new BusinessException("知识库名称已存在");
        }
    }

    private void applyRequest(KnowledgeBaseEntity entity, KnowledgeBaseSaveRequest request) {
        entity.setKbName(request.getKbName().trim());
        entity.setDescription(request.getDescription());
        entity.setEmbeddingModel(request.getEmbeddingModel());
        entity.setChunkStrategy(StringUtils.hasText(request.getChunkStrategy()) ? request.getChunkStrategy() : "fixed");
        entity.setChunkSize(request.getChunkSize() != null ? request.getChunkSize() : 512);
        entity.setChunkOverlap(request.getChunkOverlap() != null ? request.getChunkOverlap() : 50);
        entity.setRetrievalTopK(request.getRetrievalTopK() != null ? request.getRetrievalTopK() : 5);
        entity.setRetrievalScoreThreshold(toBigDecimal(request.getRetrievalScoreThreshold()));
        entity.setVisibility(StringUtils.hasText(request.getVisibility()) ? request.getVisibility() : "private");
    }

    private java.math.BigDecimal toBigDecimal(Float value) {
        return value != null ? java.math.BigDecimal.valueOf(value) : null;
    }

    private Float toFloat(java.math.BigDecimal value) {
        return value != null ? value.floatValue() : null;
    }

    private void recordRecentAccess(KnowledgeBaseEntity entity) {
        if (!StpUtil.isLogin()) {
            return;
        }
        recentAccessService.record(
                entity.getTenantId(),
                StpUtil.getLoginIdAsLong(),
                "knowledge",
                entity.getId(),
                entity.getKbName()
        );
    }

    private Long requireTenantId() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BusinessException("租户上下文缺失");
        }
        return tenantId;
    }

    private KnowledgeBaseVO toVO(KnowledgeBaseEntity entity) {
        return KnowledgeBaseVO.builder()
                .id(entity.getId())
                .applicationId(entity.getApplicationId())
                .kbName(entity.getKbName())
                .description(entity.getDescription())
                .embeddingModel(entity.getEmbeddingModel())
                .chunkStrategy(entity.getChunkStrategy())
                .chunkSize(entity.getChunkSize())
                .chunkOverlap(entity.getChunkOverlap())
                .retrievalTopK(entity.getRetrievalTopK())
                .retrievalScoreThreshold(toFloat(entity.getRetrievalScoreThreshold()))
                .documentCount(entity.getDocumentCount())
                .chunkCount(entity.getChunkCount())
                .totalSizeBytes(entity.getTotalSizeBytes())
                .visibility(entity.getVisibility())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
