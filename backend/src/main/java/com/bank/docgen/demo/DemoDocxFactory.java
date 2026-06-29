package com.bank.docgen.demo;

import java.io.ByteArrayOutputStream;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;

final class DemoDocxFactory {

    private DemoDocxFactory() {}

    static byte[] buildHeaderAnchorDocx(String anchorId) {
        try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            XWPFParagraph paragraph = document.createParagraph();
            XWPFRun run = paragraph.createRun();
            run.setText("Dear {{anchor:" + anchorId + "}} customer");
            document.write(output);
            return output.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to build demo DOCX", ex);
        }
    }

    static byte[] buildRetailLetterheadReplacementDocx(String anchorId) {
        try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            XWPFParagraph companyParagraph = document.createParagraph();
            companyParagraph.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun companyRun = companyParagraph.createRun();
            companyRun.setBold(true);
            companyRun.setFontSize(16);
            companyRun.setText("Demo Retail Bank");

            XWPFParagraph addressParagraph = document.createParagraph();
            addressParagraph.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun addressRun = addressParagraph.createRun();
            addressRun.setFontSize(9);
            addressRun.setColor("666666");
            addressRun.setText("100 Commerce Street, Retail District · www.demo-retail.example");

            document.createParagraph();

            XWPFTable accountTable = document.createTable(2, 2);
            styleTableHeaderRow(accountTable.getRow(0), "Account reference", "Letter date");
            setCellText(accountTable.getRow(1).getCell(0), "Customer: {{anchor:" + anchorId + "}}");
            setCellText(accountTable.getRow(1).getCell(1), "{{letterDate}}");

            XWPFParagraph bodyParagraph = document.createParagraph();
            XWPFRun bodyRun = bodyParagraph.createRun();
            bodyRun.setText(
                    "We are writing to confirm the details of your recent correspondence regarding your retail account."
            );

            XWPFParagraph footerParagraph = document.createParagraph();
            footerParagraph.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun footerRun = footerParagraph.createRun();
            footerRun.setFontSize(8);
            footerRun.setItalic(true);
            footerRun.setColor("888888");
            footerRun.setText(
                    "Demo Retail Bank is a fictitious entity for automated testing only. This is not legal or financial advice."
            );

            document.write(output);
            return output.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to build retail letterhead replacement DOCX", ex);
        }
    }

    private static void styleTableHeaderRow(XWPFTableRow row, String leftHeader, String rightHeader) {
        setCellText(row.getCell(0), leftHeader, true);
        setCellText(row.getCell(1), rightHeader, true);
    }

    private static void setCellText(XWPFTableCell cell, String text) {
        setCellText(cell, text, false);
    }

    private static void setCellText(XWPFTableCell cell, String text, boolean bold) {
        if (!cell.getParagraphs().isEmpty()) {
            cell.removeParagraph(0);
        }
        XWPFParagraph paragraph = cell.addParagraph();
        XWPFRun run = paragraph.createRun();
        run.setBold(bold);
        run.setText(text);
    }
}
