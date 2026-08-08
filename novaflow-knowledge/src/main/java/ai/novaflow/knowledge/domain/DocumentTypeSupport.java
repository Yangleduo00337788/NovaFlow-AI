package ai.novaflow.knowledge.domain;

import java.util.Locale;
import java.util.Set;

public final class DocumentTypeSupport {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "md", "html", "htm"
    );

    private DocumentTypeSupport() {
    }

    public static String resolveType(String filename) {
        if (filename == null || !filename.contains(".")) {
            return null;
        }
        String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        return switch (ext) {
            case "doc" -> "doc";
            case "docx" -> "docx";
            case "xls" -> "xls";
            case "xlsx" -> "xlsx";
            case "ppt" -> "ppt";
            case "pptx" -> "pptx";
            case "pdf" -> "pdf";
            case "txt" -> "txt";
            case "md" -> "md";
            case "html", "htm" -> "html";
            default -> null;
        };
    }

    public static boolean isSupported(String filename) {
        if (filename == null || !filename.contains(".")) {
            return false;
        }
        String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        return ALLOWED_EXTENSIONS.contains(ext);
    }

    public static String processStatusLabel(Integer status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case 0 -> "待处理";
            case 1 -> "处理中";
            case 2 -> "已完成";
            case 3 -> "失败";
            default -> "未知";
        };
    }
}
