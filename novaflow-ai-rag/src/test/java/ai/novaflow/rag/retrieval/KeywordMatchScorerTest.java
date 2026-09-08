package ai.novaflow.rag.retrieval;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KeywordMatchScorerTest {

    @Test
    void emptyQueryOrTextReturnsZero() {
        assertEquals(0F, KeywordMatchScorer.score(null, "hello"));
        assertEquals(0F, KeywordMatchScorer.score("hello", ""));
    }

    @Test
    void partialTokenMatchReturnsRatio() {
        float score = KeywordMatchScorer.score("alpha beta", "This text mentions alpha only.");
        assertEquals(0.5F, score);
    }

    @Test
    void caseInsensitiveMatch() {
        float score = KeywordMatchScorer.score("NOVAFLOW", "welcome to novaflow platform");
        assertEquals(1F, score);
    }

    @Test
    void cjkQueryUsesWholePhraseWhenNoTokens() {
        float score = KeywordMatchScorer.score("知识库", "这是知识库检索结果");
        assertEquals(1F, score);
    }

    @Test
    void ignoresSingleCharacterTokens() {
        float score = KeywordMatchScorer.score("a b", "bbbb");
        assertEquals(0F, score);
    }
}
