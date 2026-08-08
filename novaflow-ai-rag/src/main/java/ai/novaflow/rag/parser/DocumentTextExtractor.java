package ai.novaflow.rag.parser;

import ai.novaflow.common.exception.BusinessException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hslf.usermodel.HSLFSlideShow;
import org.apache.poi.hslf.usermodel.HSLFTextShape;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class DocumentTextExtractor {

    public String extract(byte[] content, String docType) {
        if (content == null || content.length == 0) {
            throw new BusinessException("文件内容为空");
        }
        return switch (docType) {
            case "txt", "md", "html", "htm" -> new String(content, StandardCharsets.UTF_8);
            case "pdf" -> extractPdf(content);
            case "docx" -> extractDocx(content);
            case "doc" -> extractDoc(content);
            case "xlsx" -> extractXlsx(content);
            case "xls" -> extractXls(content);
            case "pptx" -> extractPptx(content);
            case "ppt" -> extractPpt(content);
            default -> throw new BusinessException("暂不支持的文档类型: " + docType);
        };
    }

    private String extractPdf(byte[] content) {
        try (PDDocument document = Loader.loadPDF(content)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = normalize(stripper.getText(document));
            if (StringUtils.hasText(text)) {
                return text;
            }
            int pages = document.getNumberOfPages();
            throw new BusinessException(
                    "该 PDF 为图片型文档（共 " + pages + " 页），暂不支持 OCR。"
                            + "请上传 PPTX 源文件，或从 PowerPoint 重新导出为可搜索文字的 PDF");
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception e) {
            throw new BusinessException("PDF 解析失败: " + e.getMessage());
        }
    }

    private String extractDocx(byte[] content) {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(content));
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return normalize(extractor.getText());
        } catch (Exception e) {
            throw new BusinessException("Word 解析失败: " + e.getMessage());
        }
    }

    private String extractDoc(byte[] content) {
        try (HWPFDocument document = new HWPFDocument(new ByteArrayInputStream(content));
             WordExtractor extractor = new WordExtractor(document)) {
            return normalize(extractor.getText());
        } catch (Exception e) {
            throw new BusinessException("Word 解析失败: " + e.getMessage());
        }
    }

    private String extractXlsx(byte[] content) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(content))) {
            return normalize(extractWorkbook(workbook));
        } catch (Exception e) {
            throw new BusinessException("Excel 解析失败: " + e.getMessage());
        }
    }

    private String extractXls(byte[] content) {
        try (Workbook workbook = org.apache.poi.ss.usermodel.WorkbookFactory.create(new ByteArrayInputStream(content))) {
            return normalize(extractWorkbook(workbook));
        } catch (Exception e) {
            throw new BusinessException("Excel 解析失败: " + e.getMessage());
        }
    }

    private String extractWorkbook(Workbook workbook) {
        DataFormatter formatter = new DataFormatter();
        List<String> lines = new ArrayList<>();
        for (Sheet sheet : workbook) {
            lines.add("# " + sheet.getSheetName());
            for (Row row : sheet) {
                List<String> cells = new ArrayList<>();
                for (Cell cell : row) {
                    String value = formatter.formatCellValue(cell).trim();
                    if (!value.isEmpty()) {
                        cells.add(value);
                    }
                }
                if (!cells.isEmpty()) {
                    lines.add(String.join("\t", cells));
                }
            }
        }
        return String.join("\n", lines);
    }

    private String extractPptx(byte[] content) {
        try (XMLSlideShow slideShow = new XMLSlideShow(new ByteArrayInputStream(content))) {
            List<String> lines = new ArrayList<>();
            slideShow.getSlides().forEach(slide -> {
                lines.add("# Slide");
                for (XSLFShape shape : slide.getShapes()) {
                    if (shape instanceof XSLFTextShape textShape) {
                        String text = textShape.getText();
                        if (text != null && !text.isBlank()) {
                            lines.add(text.trim());
                        }
                    }
                }
            });
            return normalize(String.join("\n", lines));
        } catch (Exception e) {
            throw new BusinessException("PPT 解析失败: " + e.getMessage());
        }
    }

    private String extractPpt(byte[] content) {
        try (HSLFSlideShow slideShow = new HSLFSlideShow(new ByteArrayInputStream(content))) {
            List<String> lines = new ArrayList<>();
            slideShow.getSlides().forEach(slide -> {
                lines.add("# Slide");
                slide.getShapes().forEach(shape -> {
                    if (shape instanceof HSLFTextShape textShape) {
                        String text = textShape.getText();
                        if (text != null && !text.isBlank()) {
                            lines.add(text.trim());
                        }
                    }
                });
            });
            return normalize(String.join("\n", lines));
        } catch (Exception e) {
            throw new BusinessException("PPT 解析失败: " + e.getMessage());
        }
    }

    private String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\r\n", "\n").replaceAll("\n{3,}", "\n\n").trim();
    }
}
