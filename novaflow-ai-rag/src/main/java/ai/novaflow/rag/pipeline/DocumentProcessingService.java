package ai.novaflow.rag.pipeline;

import ai.novaflow.aiengine.llm.EmbeddingAdapterFactory;
import ai.novaflow.common.exception.BusinessException;
import ai.novaflow.knowledge.entity.DocumentEntity;
import ai.novaflow.knowledge.entity.KnowledgeBaseEntity;
import ai.novaflow.knowledge.mapper.DocumentMapper;
import ai.novaflow.knowledge.mapper.KnowledgeBaseMapper;
import ai.novaflow.knowledge.storage.DocumentStorageService;
import ai.novaflow.model.domain.ResolvedModelConfig;
import ai.novaflow.model.service.ModelResolutionService;
import ai.novaflow.rag.chunk.TextChunker;
import ai.novaflow.rag.parser.DocumentTextExtractor;
import ai.novaflow.rag.vector.QdrantVectorService;
import com.mybatisflex.core.query.QueryWrapper;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentProcessingService {

    private static final int EMBED_BATCH_SIZE = 16;

    private final DocumentMapper documentMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final DocumentStorageService documentStorageService;
    private final DocumentTextExtractor documentTextExtractor;
    private final TextChunker textChunker;
    private final ModelResolutionService modelResolutionService;
    private final EmbeddingAdapterFactory embeddingAdapterFactory;
    private final QdrantVectorService qdrantVectorService;

    @Transactional
    public void process(Long documentId, Long knowledgeBaseId, Long tenantId) {
        DocumentEntity document = getDocument(documentId, knowledgeBaseId, tenantId);
        KnowledgeBaseEntity knowledgeBase = getKnowledgeBase(knowledgeBaseId, tenantId);
        int previousChunkCount = safeInt(document.getChunkCount());

        markProcessing(document);
        try {
            byte[] content = documentStorageService.load(document.getFilePath());
            String text = documentTextExtractor.extract(content, document.getDocType());
            if (!StringUtils.hasText(text)) {
                throw new BusinessException("未能从文档中提取到文本内容");
            }

            List<String> chunks = textChunker.chunk(
                    text,
                    knowledgeBase.getChunkStrategy(),
                    safeInt(knowledgeBase.getChunkSize(), 512),
                    safeInt(knowledgeBase.getChunkOverlap(), 50));
            if (chunks.isEmpty()) {
                throw new BusinessException("文档分块结果为空");
            }

            ResolvedModelConfig embeddingConfig = modelResolutionService.resolveEmbeddingModel(
                    knowledgeBase.getEmbeddingModel(), tenantId);
            EmbeddingModel embeddingModel = embeddingAdapterFactory.createEmbeddingModel(embeddingConfig);
            List<List<Float>> embeddings = embedInBatches(embeddingModel, chunks);

            String collectionName = ensureCollectionName(knowledgeBase);
            qdrantVectorService.deleteByDocument(collectionName, documentId);
            qdrantVectorService.ensureCollection(collectionName, embeddings.get(0).size());
            qdrantVectorService.upsertChunks(
                    collectionName,
                    tenantId,
                    knowledgeBaseId,
                    documentId,
                    document.getDocName(),
                    chunks,
                    embeddings);

            markCompleted(document, knowledgeBase, previousChunkCount, chunks.size(), text.length());
            log.info("Document processed: kbId={}, docId={}, chunks={}", knowledgeBaseId, documentId, chunks.size());
        } catch (Exception ex) {
            markFailed(document, ex);
            log.error("Document processing failed: kbId={}, docId={}", knowledgeBaseId, documentId, ex);
        }
    }

    public void deleteVectors(Long documentId, Long knowledgeBaseId, Long tenantId) {
        KnowledgeBaseEntity knowledgeBase = getKnowledgeBase(knowledgeBaseId, tenantId);
        String collectionName = ensureCollectionName(knowledgeBase);
        qdrantVectorService.deleteByDocument(collectionName, documentId);
    }

    private List<List<Float>> embedInBatches(EmbeddingModel embeddingModel, List<String> chunks) {
        List<List<Float>> all = new ArrayList<>(chunks.size());
        for (int start = 0; start < chunks.size(); start += EMBED_BATCH_SIZE) {
            int end = Math.min(chunks.size(), start + EMBED_BATCH_SIZE);
            List<TextSegment> batch = chunks.subList(start, end).stream().map(TextSegment::from).toList();
            Response<List<Embedding>> response = embeddingModel.embedAll(batch);
            for (Embedding embedding : response.content()) {
                all.add(embedding.vectorAsList());
            }
        }
        return all;
    }

    private String ensureCollectionName(KnowledgeBaseEntity knowledgeBase) {
        if (StringUtils.hasText(knowledgeBase.getQdrantCollection())) {
            return knowledgeBase.getQdrantCollection();
        }
        String collectionName = "kb_" + knowledgeBase.getTenantId() + "_" + knowledgeBase.getId();
        knowledgeBase.setQdrantCollection(collectionName);
        knowledgeBase.setUpdatedAt(LocalDateTime.now());
        knowledgeBaseMapper.update(knowledgeBase);
        return collectionName;
    }

    private void markProcessing(DocumentEntity document) {
        document.setProcessStatus(1);
        document.setProcessError(null);
        document.setUpdatedAt(LocalDateTime.now());
        documentMapper.update(document);
    }

    private void markCompleted(
            DocumentEntity document,
            KnowledgeBaseEntity knowledgeBase,
            int previousChunkCount,
            int newChunkCount,
            int charCount) {
        document.setProcessStatus(2);
        document.setProcessError(null);
        document.setChunkCount(newChunkCount);
        document.setCharCount(charCount);
        document.setProcessedAt(LocalDateTime.now());
        document.setUpdatedAt(LocalDateTime.now());
        documentMapper.update(document);

        knowledgeBase.setChunkCount(Math.max(0, safeInt(knowledgeBase.getChunkCount()) - previousChunkCount + newChunkCount));
        knowledgeBase.setUpdatedAt(LocalDateTime.now());
        knowledgeBaseMapper.update(knowledgeBase);
    }

    private void markFailed(DocumentEntity document, Exception ex) {
        document.setProcessStatus(3);
        document.setProcessError(trimError(ex));
        document.setUpdatedAt(LocalDateTime.now());
        documentMapper.update(document);
    }

    private DocumentEntity getDocument(Long documentId, Long knowledgeBaseId, Long tenantId) {
        DocumentEntity entity = documentMapper.selectOneByQuery(
                QueryWrapper.create()
                        .eq("id", documentId)
                        .eq("knowledge_base_id", knowledgeBaseId)
                        .eq("tenant_id", tenantId)
                        .eq("is_deleted", 0)
        );
        if (entity == null) {
            throw new BusinessException("文档不存在");
        }
        return entity;
    }

    private KnowledgeBaseEntity getKnowledgeBase(Long knowledgeBaseId, Long tenantId) {
        KnowledgeBaseEntity entity = knowledgeBaseMapper.selectOneByQuery(
                QueryWrapper.create()
                        .eq("id", knowledgeBaseId)
                        .eq("tenant_id", tenantId)
                        .eq("is_deleted", 0)
        );
        if (entity == null) {
            throw new BusinessException("知识库不存在");
        }
        return entity;
    }

    private int safeInt(Integer value) {
        return value != null ? value : 0;
    }

    private int safeInt(Integer value, int defaultValue) {
        return value != null ? value : defaultValue;
    }

    private String trimError(Exception ex) {
        String message = ex.getMessage();
        if (!StringUtils.hasText(message)) {
            message = ex.getClass().getSimpleName();
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }
}
