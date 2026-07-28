package com.pdfconverter.service;

import com.pdfconverter.dto.CoverPageRequest;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

/**
 * Builds a cover / front page PDF from user-supplied field values.
 * Layout mirrors the live preview exactly: centered logo (defaults to
 * the TU logo, overridable), university header, colored subject line,
 * campus block, fulfillment statement, submission details and the
 * internal/external examiner signature row — all on A4.
 */
@Service
public class CoverPageService {

    private static final PDFont BOLD = new PDType1Font(Standard14Fonts.FontName.TIMES_BOLD);
    private static final PDFont REGULAR = new PDType1Font(Standard14Fonts.FontName.TIMES_ROMAN);
    private static final PDFont ITALIC_BOLD = new PDType1Font(Standard14Fonts.FontName.TIMES_BOLD_ITALIC);

    private static final String DEFAULT_LOGO_CLASSPATH = "static/tu-logo.png";

    public byte[] generate(CoverPageRequest req) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            float pageWidth = page.getMediaBox().getWidth();
            float y = page.getMediaBox().getHeight() - 90; // top margin

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {

                // ---- Logo: always drawn — user upload if present, otherwise the permanent TU logo ----
                try {
                    byte[] imgBytes = resolveLogoBytes(req.getLogoBase64());
                    PDImageXObject image = PDImageXObject.createFromByteArray(document, imgBytes, "logo");
                    float logoSize = 90;
                    cs.drawImage(image, (pageWidth - logoSize) / 2, y - logoSize, logoSize, logoSize);
                    y -= (logoSize + 25);
                } catch (Exception ignored) {
                    // if even the default logo fails to load, just skip drawing and keep spacing consistent
                    y -= 15;
                }

                // ---- University / Faculty header ----
                y = centerText(cs, req.getUniversityName(), BOLD, 16, y, Color.BLACK, pageWidth);
                y -= 20;
                y = centerText(cs, req.getFacultyName(), BOLD, 13, y, Color.BLACK, pageWidth);
                y -= 45;

                // ---- Report label ----
                y = centerText(cs, req.getReportLabel(), BOLD, 13, y, Color.BLACK, pageWidth);
                y -= 40;

                // ---- Subject (colored) ----
                y = centerText(cs, req.getSubjectName(), BOLD, 13, y, hexToColor(req.getSubjectColor()), pageWidth);
                y -= 40;

                // ---- Submitted to block ----
                y = centerText(cs, req.getSubmittedToLabel(), BOLD, 12, y, Color.BLACK, pageWidth);
                y -= 22;
                y = centerText(cs, req.getDepartmentName(), BOLD, 12, y, Color.BLACK, pageWidth);
                y -= 22;
                y = centerText(cs, req.getCampusName(), BOLD, 12, y, hexToColor(req.getCampusNameColor()), pageWidth);
                y -= 22;
                if (req.getCampusAddress() != null && !req.getCampusAddress().isBlank()) {
                    y = centerText(cs, req.getCampusAddress(), BOLD, 12, y, Color.BLACK, pageWidth);
                    y -= 22;
                }
                y -= 20;

                // ---- Fulfillment statement ----
                y = wrapCenterText(cs, req.getFulfillmentLine(), ITALIC_BOLD, 11, y, Color.BLACK, pageWidth, 460);
                y -= 60;

                // ---- Submitted by block ----
                y = centerText(cs, "Submitted by", REGULAR, 12, y, Color.BLACK, pageWidth);
                y -= 24;
                y = centerText(cs, "Name: " + safe(req.getStudentName()), REGULAR, 12, y, Color.BLACK, pageWidth);
                y -= 22;
                y = centerText(cs, "Registration No: " + safe(req.getRegistrationNo()), REGULAR, 12, y, Color.BLACK, pageWidth);
                y -= 22;
                y = centerText(cs, "Semester: ", REGULAR, 12, y, Color.BLACK, pageWidth,
                        req.getSemester(), BOLD);

                // Extra blank gap reserved for a handwritten signature, drawn just above
                // the internal/external examiner signature lines below.
                y -= 90;

                // ---- Examiner signature row ----
                float leftX = 70;
                float rightX = pageWidth - 260;
                float lineWidth = 190;

                cs.setLineWidth(1);
                cs.moveTo(leftX, y);
                cs.lineTo(leftX + lineWidth, y);
                cs.stroke();

                cs.moveTo(rightX, y);
                cs.lineTo(rightX + lineWidth, y);
                cs.stroke();

                y -= 20;
                drawText(cs, safe(req.getInternalExaminerName()), BOLD, 11, leftX, y, Color.BLACK);
                drawText(cs, safe(req.getExternalExaminerName()), BOLD, 11, rightX, y, Color.BLACK);

                y -= 18;
                drawText(cs, safe(req.getInternalExaminerLabel()), BOLD, 11, leftX, y, Color.BLACK);
                drawText(cs, safe(req.getExternalExaminerLabel()), BOLD, 11, rightX, y, Color.BLACK);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }

    // ---------- helpers ----------

    /** Uses the user-uploaded logo if provided; otherwise falls back to the bundled TU logo. */
    private byte[] resolveLogoBytes(String logoBase64) throws IOException {
        if (logoBase64 != null && !logoBase64.isBlank()) {
            return decodeBase64Image(logoBase64);
        }
        try (InputStream in = new ClassPathResource(DEFAULT_LOGO_CLASSPATH).getInputStream()) {
            return in.readAllBytes();
        }
    }

    private byte[] decodeBase64Image(String base64) {
        String data = base64.contains(",") ? base64.substring(base64.indexOf(',') + 1) : base64;
        return Base64.getDecoder().decode(data);
    }

    private Color hexToColor(String hex) {
        try {
            return Color.decode(hex);
        } catch (Exception e) {
            return Color.BLACK;
        }
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private float centerText(PDPageContentStream cs, String text, PDFont font, float size,
                             float y, Color color, float pageWidth) throws IOException {
        if (text == null) text = "";
        float textWidth = font.getStringWidth(text) / 1000 * size;
        float x = (pageWidth - textWidth) / 2;
        drawText(cs, text, font, size, x, y, color);
        return y;
    }

    private float centerText(PDPageContentStream cs, String label, PDFont labelFont, float size,
                             float y, Color color, float pageWidth, String value, PDFont valueFont) throws IOException {
        float labelWidth = labelFont.getStringWidth(label) / 1000 * size;
        float valueWidth = valueFont.getStringWidth(safe(value)) / 1000 * size;
        float totalWidth = labelWidth + valueWidth;
        float startX = (pageWidth - totalWidth) / 2;

        drawText(cs, label, labelFont, size, startX, y, color);
        drawText(cs, safe(value), valueFont, size, startX + labelWidth, y, color);
        return y;
    }

    private void drawText(PDPageContentStream cs, String text, PDFont font, float size,
                          float x, float y, Color color) throws IOException {
        cs.beginText();
        cs.setFont(font, size);
        cs.setNonStrokingColor(color);
        cs.newLineAtOffset(x, y);
        cs.showText(text == null ? "" : text);
        cs.endText();
    }

    private float wrapCenterText(PDPageContentStream cs, String text, PDFont font, float size,
                                 float y, Color color, float pageWidth, float maxWidth) throws IOException {
        if (text == null || text.isBlank()) return y;
        String[] words = text.split("\\s+");
        StringBuilder line = new StringBuilder();

        for (String word : words) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            float width = font.getStringWidth(candidate) / 1000 * size;
            if (width > maxWidth && !line.isEmpty()) {
                y = centerText(cs, line.toString(), font, size, y, color, pageWidth);
                y -= 18;
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(candidate);
            }
        }
        if (!line.isEmpty()) {
            y = centerText(cs, line.toString(), font, size, y, color, pageWidth);
        }
        return y;
    }
}