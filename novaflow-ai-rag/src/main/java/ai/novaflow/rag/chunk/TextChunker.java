package ai.novaflow.rag.chunk;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Component
public class TextChunker {

    public List<String> chunk(String text, String strategy, int chunkSize, int chunkOverlap) {
        if (!StringUtils.hasText(text)) {
            return List.of();
        }
        int size = Math.max(chunkSize, 128);
        int overlap = Math.max(0, Math.min(chunkOverlap, size / 2));
        if ("paragraph".equalsIgnoreCase(strategy)) {
            return chunkByParagraph(text, size, overlap);
        }
        return chunkFixed(text, size, overlap);
    }

    private List<String> chunkByParagraph(String text, int chunkSize, int overlap) {
        String[] paragraphs = text.split("\\n{2,}");
        List<String> chunks = new ArrayList<>();
        StringBuilder buffer = new StringBuilder();
        for (String paragraph : paragraphs) {
            String part = paragraph.trim();
            if (part.isEmpty()) {
                continue;
            }
            if (buffer.length() + part.length() + 2 > chunkSize && buffer.length() > 0) {
                chunks.add(buffer.toString().trim());
                buffer = new StringBuilder(tail(buffer.toString(), overlap));
            }
            if (!buffer.isEmpty()) {
                buffer.append("\n\n");
            }
            buffer.append(part);
        }
        if (!buffer.isEmpty()) {
            chunks.add(buffer.toString().trim());
        }
        if (chunks.isEmpty()) {
            return chunkFixed(text, chunkSize, overlap);
        }
        return chunks;
    }

    private List<String> chunkFixed(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(text.length(), start + chunkSize);
            String piece = text.substring(start, end).trim();
            if (!piece.isEmpty()) {
                chunks.add(piece);
            }
            if (end >= text.length()) {
                break;
            }
            start = Math.max(end - overlap, start + 1);
        }
        return chunks;
    }

    private String tail(String text, int overlap) {
        if (overlap <= 0 || text.length() <= overlap) {
            return text;
        }
        return text.substring(text.length() - overlap);
    }
}
