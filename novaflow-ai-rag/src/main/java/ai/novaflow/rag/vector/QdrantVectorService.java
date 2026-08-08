package ai.novaflow.rag.vector;

import ai.novaflow.common.exception.BusinessException;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Collections.Distance;
import io.qdrant.client.grpc.Collections.VectorParams;
import io.qdrant.client.grpc.Points.Filter;
import io.qdrant.client.grpc.Points.PointStruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static io.qdrant.client.ConditionFactory.matchKeyword;
import static io.qdrant.client.PointIdFactory.id;
import static io.qdrant.client.ValueFactory.value;
import static io.qdrant.client.VectorsFactory.vectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class QdrantVectorService {

    private final QdrantClient qdrantClient;

    public void ensureCollection(String collectionName, int vectorSize) {
        try {
            Boolean exists = qdrantClient.collectionExistsAsync(collectionName).get();
            if (Boolean.TRUE.equals(exists)) {
                return;
            }
            qdrantClient.createCollectionAsync(collectionName,
                    VectorParams.newBuilder()
                            .setSize(vectorSize)
                            .setDistance(Distance.Cosine)
                            .build()).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("Qdrant 连接被中断");
        } catch (ExecutionException e) {
            throw new BusinessException("Qdrant 创建集合失败: " + rootMessage(e));
        }
    }

    public void upsertChunks(
            String collectionName,
            Long tenantId,
            Long knowledgeBaseId,
            Long documentId,
            String docName,
            List<String> chunks,
            List<List<Float>> embeddings) {
        if (chunks.size() != embeddings.size()) {
            throw new BusinessException("向量数量与分块数量不一致");
        }
        List<PointStruct> points = new ArrayList<>(chunks.size());
        for (int i = 0; i < chunks.size(); i++) {
            points.add(PointStruct.newBuilder()
                    .setId(id(UUID.randomUUID()))
                    .setVectors(vectors(embeddings.get(i)))
                    .putAllPayload(Map.of(
                            "tenant_id", value(tenantId),
                            "knowledge_base_id", value(knowledgeBaseId),
                            "document_id", value(String.valueOf(documentId)),
                            "chunk_index", value(i),
                            "doc_name", value(docName),
                            "text", value(chunks.get(i))
                    ))
                    .build());
        }
        try {
            qdrantClient.upsertAsync(collectionName, points).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("Qdrant 写入被中断");
        } catch (ExecutionException e) {
            throw new BusinessException("Qdrant 写入失败: " + rootMessage(e));
        }
    }

    public void deleteByDocument(String collectionName, Long documentId) {
        try {
            Boolean exists = qdrantClient.collectionExistsAsync(collectionName).get();
            if (!Boolean.TRUE.equals(exists)) {
                return;
            }
            Filter filter = Filter.newBuilder()
                    .addMust(matchKeyword("document_id", String.valueOf(documentId)))
                    .build();
            qdrantClient.deleteAsync(collectionName, filter).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("Qdrant 删除被中断");
        } catch (ExecutionException e) {
            log.warn("Qdrant delete skipped for document {}: {}", documentId, rootMessage(e));
        }
    }

    public void deleteCollection(String collectionName) {
        try {
            Boolean exists = qdrantClient.collectionExistsAsync(collectionName).get();
            if (!Boolean.TRUE.equals(exists)) {
                return;
            }
            qdrantClient.deleteCollectionAsync(collectionName).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            log.warn("Qdrant collection delete skipped {}: {}", collectionName, rootMessage(e));
        }
    }

    private String rootMessage(ExecutionException e) {
        Throwable cause = e.getCause() != null ? e.getCause() : e;
        return cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName();
    }
}
