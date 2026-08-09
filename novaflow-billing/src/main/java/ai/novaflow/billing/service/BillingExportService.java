package ai.novaflow.billing.service;

import ai.novaflow.billing.domain.vo.BillingOverviewVO;
import ai.novaflow.model.domain.BillingCurrency;
import ai.novaflow.model.domain.TokenUsageLogRow;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class BillingExportService {

    private static final int EXPORT_LIMIT = 10_000;
    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final BillingService billingService;

    public byte[] exportExcel(Long tenantId, String month) throws IOException {
        BillingOverviewVO overview = billingService.getOverview(month);
        List<TokenUsageLogRow> records = billingService.listExportRecords(tenantId, month);

        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet summarySheet = workbook.createSheet("概览");
            int rowIdx = 0;
            rowIdx = writeSummaryRow(summarySheet, rowIdx, "账期", overview.getPeriodLabel());
            rowIdx = writeSummaryRow(summarySheet, rowIdx, "本月调用", String.valueOf(overview.getTotalCalls()));
            rowIdx = writeSummaryRow(summarySheet, rowIdx, "本月 Token", String.valueOf(overview.getTotalTokens()));
            rowIdx = writeSummaryRow(summarySheet, rowIdx, "本月预估费用", overview.getTotalCostLabel());
            rowIdx = writeSummaryRow(summarySheet, rowIdx, "Token 环比", overview.getTokenChangePercent());
            rowIdx = writeSummaryRow(summarySheet, rowIdx, "调用环比", overview.getCallChangePercent());
            if (overview.getQuota() != null) {
                rowIdx = writeSummaryRow(summarySheet, rowIdx, "套餐", overview.getQuota().getPlanTypeLabel());
                if (overview.getQuota().getMonthlyTokenQuota() != null) {
                    rowIdx = writeSummaryRow(
                            summarySheet,
                            rowIdx,
                            "Token 配额",
                            overview.getQuota().getUsedTokens() + " / " + overview.getQuota().getMonthlyTokenQuota());
                }
            }
            summarySheet.autoSizeColumn(0);
            summarySheet.autoSizeColumn(1);

            Sheet detailSheet = workbook.createSheet("明细");
            Row header = detailSheet.createRow(0);
            String[] headers = {"时间", "Agent", "模型", "类型", "输入Token", "输出Token", "总Token", "成本", "币种"};
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }
            int detailRow = 1;
            for (TokenUsageLogRow record : records) {
                Row row = detailSheet.createRow(detailRow++);
                row.createCell(0).setCellValue(record.getCreatedAt() != null ? DATE_TIME_FMT.format(record.getCreatedAt()) : "");
                row.createCell(1).setCellValue(record.getAgentName() != null ? record.getAgentName() : "系统调用");
                row.createCell(2).setCellValue(
                        record.getDisplayName() != null ? record.getDisplayName() : record.getModelName());
                row.createCell(3).setCellValue(record.getUsageType() != null ? record.getUsageType() : "chat");
                row.createCell(4).setCellValue(safeInt(record.getInputTokens()));
                row.createCell(5).setCellValue(safeInt(record.getOutputTokens()));
                row.createCell(6).setCellValue(safeInt(record.getTotalTokens()));
                row.createCell(7).setCellValue(record.getCost() != null ? record.getCost().doubleValue() : 0D);
                row.createCell(8).setCellValue(record.getCurrency() != null ? record.getCurrency() : BillingCurrency.CNY.getCode());
            }
            for (int i = 0; i < headers.length; i++) {
                detailSheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    public byte[] exportPdf(Long tenantId, String month) throws IOException {
        BillingOverviewVO overview = billingService.getOverview(month);
        List<TokenUsageLogRow> records = billingService.listExportRecords(tenantId, month);

        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

            float margin = 48;
            float y = page.getMediaBox().getHeight() - margin;
            float lineHeight = 16;
            float pageBottom = margin;

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(fontBold, 16);
                content.newLineAtOffset(margin, y);
                content.showText("NovaFlow AI Billing Report");
                content.endText();
                y -= lineHeight * 2;

                y = writePdfLine(content, font, margin, y, lineHeight, "Period: " + safeAscii(overview.getPeriodLabel()));
                y = writePdfLine(content, font, margin, y, lineHeight, "Calls: " + overview.getTotalCalls());
                y = writePdfLine(content, font, margin, y, lineHeight, "Tokens: " + overview.getTotalTokens());
                y = writePdfLine(content, font, margin, y, lineHeight, "Estimated Cost: " + safeAscii(overview.getTotalCostLabel()));
                y = writePdfLine(content, font, margin, y, lineHeight, "Token Change: " + safeAscii(overview.getTokenChangePercent()));
                y -= lineHeight;

                y = writePdfLine(content, fontBold, margin, y, lineHeight, "Usage Records (top " + records.size() + ")");
                y -= 4;
                for (TokenUsageLogRow record : records) {
                    if (y < pageBottom + lineHeight * 2) {
                        break;
                    }
                    String line = String.format(
                            Locale.ROOT,
                            "%s | %s | %s | %d tokens | %s",
                            record.getCreatedAt() != null ? DATE_TIME_FMT.format(record.getCreatedAt()) : "-",
                            safeAscii(record.getAgentName() != null ? record.getAgentName() : "System"),
                            safeAscii(record.getDisplayName() != null ? record.getDisplayName() : record.getModelName()),
                            safeInt(record.getTotalTokens()),
                            safeAscii(formatCost(record))
                    );
                    y = writePdfLine(content, font, margin, y, lineHeight, line);
                }
            }

            document.save(out);
            return out.toByteArray();
        }
    }

    private int writeSummaryRow(Sheet sheet, int rowIdx, String label, String value) {
        Row row = sheet.createRow(rowIdx);
        row.createCell(0).setCellValue(label);
        row.createCell(1).setCellValue(value);
        return rowIdx + 1;
    }

    private float writePdfLine(
            PDPageContentStream content,
            PDType1Font font,
            float x,
            float y,
            float lineHeight,
            String text) throws IOException {
        content.beginText();
        content.setFont(font, 10);
        content.newLineAtOffset(x, y);
        content.showText(trimPdfLine(text));
        content.endText();
        return y - lineHeight;
    }

    private String formatCost(TokenUsageLogRow record) {
        if (record.getCost() == null) {
            return "0";
        }
        BillingCurrency currency = BillingCurrency.fromCode(record.getCurrency());
        return currency.getSymbol() + record.getCost().toPlainString();
    }

    private int safeInt(Integer value) {
        return value != null ? value : 0;
    }

    private String safeAscii(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("[^\\x20-\\x7E]", "?");
    }

    private String trimPdfLine(String text) {
        String trimmed = text == null ? "" : text;
        return trimmed.length() > 110 ? trimmed.substring(0, 110) + "..." : trimmed;
    }
}
