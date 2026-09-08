package ai.novaflow.rag.chunk;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextChunkerTest {

    private final TextChunker chunker = new TextChunker();

    @Test
    void blankTextReturnsEmptyList() {
        assertTrue(chunker.chunk(null, "fixed", 512, 64).isEmpty());
        assertTrue(chunker.chunk("   ", "fixed", 512, 64).isEmpty());
    }

    @Test
    void fixedStrategyClampsChunkSizeToMinimum() {
        String text = "a".repeat(100);
        List<String> chunks = chunker.chunk(text, "fixed", 64, 0);
        assertEquals(1, chunks.size());
        assertEquals(100, chunks.get(0).length());
    }

    @Test
    void fixedStrategySplitsWithOverlap() {
        String text = "a".repeat(300);
        List<String> chunks = chunker.chunk(text, "fixed", 128, 32);
        assertTrue(chunks.size() >= 2);
        String tail = chunks.get(0).substring(chunks.get(0).length() - 32);
        assertTrue(chunks.get(1).startsWith(tail));
    }

    @Test
    void paragraphStrategyMergesShortParagraphs() {
        String text = "First paragraph.\n\nSecond paragraph.\n\nThird paragraph.";
        List<String> chunks = chunker.chunk(text, "paragraph", 200, 20);
        assertEquals(1, chunks.size());
        assertTrue(chunks.get(0).contains("First paragraph."));
        assertTrue(chunks.get(0).contains("Third paragraph."));
    }

    @Test
    void paragraphStrategyKeepsSingleLongParagraphAsOneChunk() {
        String text = "a".repeat(300);
        List<String> chunks = chunker.chunk(text, "paragraph", 400, 16);
        assertEquals(1, chunks.size());
        assertEquals(300, chunks.get(0).length());
    }
}
