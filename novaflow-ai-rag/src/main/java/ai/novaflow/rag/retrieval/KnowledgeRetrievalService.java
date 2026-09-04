package ai.novaflow.rag.retrieval;

import ai.novaflow.aiengine.llm.RerankClient;
import ai.novaflow.aiengine.llm.EmbeddingAdapterFactory;
import ai.novaflow.common.domain.RetrievalConfig;
import ai.novaflow.common.exception.BusinessException;
import ai.novaflow.knowledge.entity.KnowledgeBaseEntity;
import ai.novaflow.knowledge.mapper.KnowledgeBaseMapper;
import ai.novaflow.knowledge.service.KnowledgeBaseService;
import ai.novaflow.model.domain.ResolvedModelConfig;
import ai.novaflow.model.service.ModelResolutionService;
import ai.novaflow.rag.domain.RetrievedChunk;
import ai.novaflow.rag.domain.dto.RetrievalTestRequest;
import ai.novaflow.rag.domain.vo.RetrievalTestResultVO;
import ai.novaflow.rag.domain.vo.RetrievedChunkVO;
import ai.novaflow.rag.vector.QdrantVectorService;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class KnowledgeRetrievalService {

    private final KnowledgeBaseService knowledgeBaseService;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final ModelResolutionService modelResolutionService;
    private final EmbeddingAdapterFactory embeddingAdapterFactory;
    private final QdrantVectorService qdrantVectorService;
    private final RerankClient rerankClient;

    public RetrievalTestResultVO testRetrieve(Long knowledgeBaseId, Long tenantId, RetrievalTestRequest request) {
        knowledgeBaseService.requireSearchAccess(knowledgeBaseId);
        long start = System.currentTimeMillis();
        KnowledgeBaseEntity knowledgeBase = knowledgeBaseService.getKnowledgeBaseOrThrow(knowledgeBaseId);
        int topK = request.getTopK() != null ? request.getTopK() : effectiveTopK(knowledgeBase);
        Float scoreThreshold = request.getScoreThreshold() != null
                ? request.getScoreThreshold()
                : toFloat(knowledgeBase.getRetrievalScoreThreshold());
        List<RetrievedChunk> chunks = retrieve(
                knowledgeBaseId,
                tenantId,
                request.getQuery().trim(),
                topK,
                scoreThreshold,
                RetrievalConfig.builder()
                        .topK(topK)
                        .scoreThreshold(scoreThreshold)
                        .rerankEnabled(request.getRerankEnabled())
                        .rerankModel(request.getRerankModel())
                        .rerankCandidateK(request.getRerankCandidateK())
                        .hybridEnabled(request.getHybridEnabled())
                        .hybridAlpha(request.getHybridAlpha())
                        .build());

        return RetrievalTestResultVO.builder()
                .query(request.getQuery().trim())
                .topK(topK)
                .latencyMs(System.currentTimeMillis() - start)
                .chunks(chunks.stream().map(this::toVO).toList())
                .build();
    }

    public List<RetrievedChunk> retrieve(
            Long knowledgeBaseId,
            Long tenantId,
            String query,
            int topK,
            Float scoreThreshold) {
        return retrieve(knowledgeBaseId, tenantId, query, topK, scoreThreshold, null);
    }

    public List<RetrievedChunk> retrieve(
            Long knowledgeBaseId,
            Long tenantId,
            String query,
            int topK,
            Float scoreThreshold,
            RetrievalConfig retrievalConfig) {
        if (!StringUtils.hasText(query)) {
            throw new BusinessException("检索问题不能为空");
        }

        KnowledgeBaseEntity knowledgeBase = knowledgeBaseService.getKnowledgeBaseOrThrow(knowledgeBaseId);
        if (safeInt(knowledgeBase.getChunkCount()) <= 0) {
            return List.of();
        }

        RetrievalConfig config = retrievalConfig != null ? retrievalConfig : RetrievalConfig.builder().topK(topK).scoreThreshold(scoreThreshold).build();
        int candidateK = resolveCandidateK(topK, config);

        List<Float> queryVector = embedQuery(knowledgeBase, tenantId, query.trim());
        String collectionName = ensureCollectionName(knowledgeBase);
        List<RetrievedChunk> candidates = qdrantVectorService.search(
                collectionName,
                tenantId,
                knowledgeBaseId,
                knowledgeBase.getKbName(),
                queryVector,
                candidateK,
                shouldDeferThreshold(config) ? null : scoreThreshold);

        List<RetrievedChunk> hybridRanked = applyHybridRanking(query, config, candidates);
        List<RetrievedChunk> ranked = applyRerank(tenantId, query, topK, scoreThreshold, config, hybridRanked);
        if (ranked.size() <= topK) {
            return ranked;
        }
        return ranked.subList(0, topK);
    }

    public List<RetrievedChunk> retrieveAcrossKnowledgeBases(
            List<Long> knowledgeBaseIds,
            Long tenantId,
            String query,
            int topK,
            Float scoreThreshold) {
        return retrieveAcrossKnowledgeBases(knowledgeBaseIds, tenantId, query, topK, scoreThreshold, null);
    }

    public List<RetrievedChunk> retrieveAcrossKnowledgeBases(
            List<Long> knowledgeBaseIds,
            Long tenantId,
            String query,
            int topK,
            Float scoreThreshold,
            RetrievalConfig retrievalConfig) {
        if (knowledgeBaseIds == null || knowledgeBaseIds.isEmpty()) {
            return List.of();
        }

        List<RetrievedChunk> merged = new ArrayList<>();
        for (Long knowledgeBaseId : knowledgeBaseIds) {
            merged.addAll(retrieve(knowledgeBaseId, tenantId, query, topK, scoreThreshold, retrievalConfig));
        }
        merged.sort(Comparator.comparing(RetrievedChunk::getScore, Comparator.nullsLast(Comparator.reverseOrder())));
        if (merged.size() <= topK) {
            return merged;
        }
        return merged.subList(0, topK);
    }

    private int resolveCandidateK(int topK, RetrievalConfig config) {
        if (config.effectiveHybridEnabled()) {
            return config.effectiveHybridCandidateK(topK);
        }
        if (config.effectiveRerankEnabled()) {
            return config.effectiveRerankCandidateK(topK);
        }
        return topK;
    }

    private boolean shouldDeferThreshold(RetrievalConfig config) {
        return config.effectiveRerankEnabled() || config.effectiveHybridEnabled();
    }

    private List<RetrievedChunk> applyHybridRanking(String query, RetrievalConfig config, List<RetrievedChunk> candidates) {
        if (!config.effectiveHybridEnabled() || candidates == null || candidates.isEmpty()) {
            return candidates;
        }
        float alpha = config.effectiveHybridAlpha();
        List<RetrievedChunk> ranked = new ArrayList<>(candidates);
        for (RetrievedChunk chunk : ranked) {
            float vectorScore = chunk.getScore() != null ? chunk.getScore() : 0F;
            float keywordScore = KeywordMatchScorer.score(query, chunk.getText());
            chunk.setScore(alpha * vectorScore + (1F - alpha) * keywordScore);
        }
        ranked.sort(Comparator.comparing(RetrievedChunk::getScore, Comparator.nullsLast(Comparator.reverseOrder())));
        return ranked;
    }

    private List<RetrievedChunk> applyRerank(
            Long tenantId,
            String query,
            int topK,
            Float scoreThreshold,
            RetrievalConfig config,
            List<RetrievedChunk> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }
        if (!config.effectiveRerankEnabled() || !StringUtils.hasText(config.getRerankModel())) {
            return filterByThreshold(candidates, scoreThreshold);
        }
        try {
            ResolvedModelConfig rerankConfig = modelResolutionService.resolveRerankModel(config.getRerankModel(), tenantId);
            List<String> documents = candidates.stream().map(RetrievedChunk::getText).toList();
            List<RerankClient.ScoredIndex> scored = rerankClient.rerank(rerankConfig, query, documents, topK);
            List<RetrievedChunk> ranked = new ArrayList<>();
            for (RerankClient.ScoredIndex item : scored) {
                if (item.getIndex() < 0 || item.getIndex() >= candidates.size()) {
                    continue;
                }
                RetrievedChunk chunk = candidates.get(item.getIndex());
                chunk.setScore(item.getScore());
                ranked.add(chunk);
            }
            return filterByThreshold(ranked, scoreThreshold);
        } catch (Exception e) {
            return filterByThreshold(candidates, scoreThreshold);
        }
    }

    private List<RetrievedChunk> filterByThreshold(List<RetrievedChunk> chunks, Float scoreThreshold) {
        if (scoreThreshold == null) {
            return chunks;
        }
        return chunks.stream()
                .filter(chunk -> chunk.getScore() == null || chunk.getScore() >= scoreThreshold)
                .toList();
    }

    public String buildContextPrompt(List<RetrievedChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < chunks.size(); i++) {
            RetrievedChunk chunk = chunks.get(i);
            builder.append('[').append(i + 1).append("] 来源：")
                    .append(StringUtils.hasText(chunk.getDocName()) ? chunk.getDocName() : "未知文档");
            if (chunk.getScore() != null) {
                builder.append("（相关度 ").append(String.format("%.2f", chunk.getScore())).append('）');
            }
            builder.append('\n').append(chunk.getText()).append("\n\n");
        }
        return builder.toString().trim();
    }

    private List<Float> embedQuery(KnowledgeBaseEntity knowledgeBase, Long tenantId, String query) {
        ResolvedModelConfig embeddingConfig = modelResolutionService.resolveEmbeddingModel(
                knowledgeBase.getEmbeddingModel(), tenantId);
        EmbeddingModel embeddingModel = embeddingAdapterFactory.createEmbeddingModel(embeddingConfig);
        Response<Embedding> response = embeddingModel.embed(TextSegment.from(query));
        return response.content().vectorAsList();
    }

    private String ensureCollectionName(KnowledgeBaseEntity knowledgeBase) {
        if (StringUtils.hasText(knowledgeBase.getQdrantCollection())) {
            return knowledgeBase.getQdrantCollection();
        }
        String collectionName = "kb_" + knowledgeBase.getTenantId() + "_" + knowledgeBase.getId();
        knowledgeBase.setQdrantCollection(collectionName);
        knowledgeBaseMapper.update(knowledgeBase);
        return collectionName;
    }

    private RetrievedChunkVO toVO(RetrievedChunk chunk) {
        return RetrievedChunkVO.builder()
                .knowledgeBaseId(chunk.getKnowledgeBaseId())
                .knowledgeBaseName(chunk.getKnowledgeBaseName())
                .documentId(chunk.getDocumentId())
                .docName(chunk.getDocName())
                .chunkIndex(chunk.getChunkIndex())
                .text(chunk.getText())
                .score(chunk.getScore())
                .build();
    }

    private int safeInt(Integer value) {
        return value != null ? value : 0;
    }

    private int effectiveTopK(KnowledgeBaseEntity knowledgeBase) {
        Integer topK = knowledgeBase.getRetrievalTopK();
        return topK != null && topK > 0 ? topK : 5;
    }

    private Float toFloat(java.math.BigDecimal value) {
        return value != null ? value.floatValue() : null;
    }
}
