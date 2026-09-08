package ai.novaflow.rag.retrieval;

import ai.novaflow.aiengine.llm.EmbeddingAdapterFactory;
import ai.novaflow.aiengine.llm.RerankClient;
import ai.novaflow.knowledge.mapper.KnowledgeBaseMapper;
import ai.novaflow.knowledge.service.KnowledgeBaseService;
import ai.novaflow.model.service.ModelResolutionService;
import ai.novaflow.rag.domain.RetrievedChunk;
import ai.novaflow.rag.vector.QdrantVectorService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class KnowledgeRetrievalServiceTest {

    @Mock
    private KnowledgeBaseService knowledgeBaseService;
    @Mock
    private KnowledgeBaseMapper knowledgeBaseMapper;
    @Mock
    private ModelResolutionService modelResolutionService;
    @Mock
    private EmbeddingAdapterFactory embeddingAdapterFactory;
    @Mock
    private QdrantVectorService qdrantVectorService;
    @Mock
    private RerankClient rerankClient;

    @InjectMocks
    private KnowledgeRetrievalService knowledgeRetrievalService;

    @Test
    void buildContextPromptReturnsEmptyForNullOrEmptyChunks() {
        assertEquals("", knowledgeRetrievalService.buildContextPrompt(null));
        assertEquals("", knowledgeRetrievalService.buildContextPrompt(List.of()));
    }

    @Test
    void buildContextPromptFormatsChunkWithDocNameAndScore() {
        RetrievedChunk chunk = RetrievedChunk.builder()
                .docName("手册.pdf")
                .text("安装步骤一")
                .score(0.87F)
                .build();
        String prompt = knowledgeRetrievalService.buildContextPrompt(List.of(chunk));
        assertTrue(prompt.contains("[1] 来源：手册.pdf"));
        assertTrue(prompt.contains("相关度 0.87"));
        assertTrue(prompt.contains("安装步骤一"));
    }

    @Test
    void buildContextPromptUsesUnknownDocNameWhenMissing() {
        RetrievedChunk chunk = RetrievedChunk.builder()
                .text("匿名片段")
                .build();
        String prompt = knowledgeRetrievalService.buildContextPrompt(List.of(chunk));
        assertTrue(prompt.contains("未知文档"));
    }

    @Test
    void buildContextPromptJoinsMultipleChunks() {
        List<RetrievedChunk> chunks = List.of(
                RetrievedChunk.builder().docName("A").text("one").build(),
                RetrievedChunk.builder().docName("B").text("two").build()
        );
        String prompt = knowledgeRetrievalService.buildContextPrompt(chunks);
        assertTrue(prompt.contains("[1] 来源：A"));
        assertTrue(prompt.contains("[2] 来源：B"));
    }
}
