package com.bank.docgen.demo;

import com.bank.docgen.demo.support.DemoMasterDocxLayoutSupport;
import com.bank.docgen.demo.support.DemoMasterDocxPageNumberSupport;
import com.bank.docgen.demo.support.DemoMasterDocxStyleSupport;
import com.bank.docgen.rendering.DocxMasterStyleRegistry;
import com.bank.docgen.rendering.DocxWordCompatibilitySupport;
import java.io.ByteArrayOutputStream;
import org.apache.poi.wp.usermodel.HeaderFooterType;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFHeader;
import org.apache.poi.xwpf.usermodel.XWPFFooter;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFStyles;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;

/**
 * Bank-grade retail letterhead masters for demo catalog seeding and full-flow E2E fixtures (P23-T11).
 */
public final class DemoRetailLetterheadDocxBuilder {

  /** Bump when page layout / header / footer / style catalog changes. */
  public static final String MASTER_LAYOUT_VERSION = "full-flow-layout-v2-bank-style-manifest";

  private DemoRetailLetterheadDocxBuilder() {
  }

  public static byte[] buildFullFlowMaster(String anchorId) {
    try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      DemoMasterDocxLayoutSupport.configureA4PageLayout(document);
      DemoMasterDocxStyleSupport.applySharedBankStyles(document);
      applyRetailProductStyles(document);

      XWPFHeader header = document.createHeader(HeaderFooterType.DEFAULT);
      XWPFParagraph brandLine = header.createParagraph();
      brandLine.setAlignment(ParagraphAlignment.LEFT);
      XWPFRun brandRun = brandLine.createRun();
      brandRun.setBold(true);
      brandRun.setFontSize(9);
      brandRun.setColor("006633");
      brandRun.setFontFamily("Calibri");
      brandRun.setText("Meridian Retail Banking — Customer Correspondence");

      XWPFFooter footer = document.createFooter(HeaderFooterType.DEFAULT);
      XWPFParagraph disclaimerLine = footer.createParagraph();
      disclaimerLine.setAlignment(ParagraphAlignment.LEFT);
      XWPFRun disclaimerRun = disclaimerLine.createRun();
      disclaimerRun.setFontSize(7);
      disclaimerRun.setColor("666666");
      disclaimerRun.setFontFamily("Calibri");
      disclaimerRun.setText(
          "Customer Service: 0800 123 4567  |  42 High Street, Manchester M1 1AA  |  FSCS protected deposits"
      );
      XWPFParagraph pageLine = footer.createParagraph();
      pageLine.setAlignment(ParagraphAlignment.CENTER);
      DemoMasterDocxPageNumberSupport.addGlobalPageNumberFields(pageLine);

      addCenteredTitle(document, "Customer Correspondence");
      XWPFParagraph anchorParagraph = document.createParagraph();
      XWPFRun anchorRun = anchorParagraph.createRun();
      anchorRun.setFontFamily("Calibri");
      anchorRun.setText("Dear {{anchor:" + anchorId + "}} customer");

      DocxWordCompatibilitySupport.ensureWordCompatiblePackage(document);
      document.write(output);
      return output.toByteArray();
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to build full-flow retail letterhead master DOCX", ex);
    }
  }

  public static byte[] buildLetterheadReplacement(String anchorId) {
    try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      DemoMasterDocxLayoutSupport.configureA4PageLayout(document);
      DemoMasterDocxStyleSupport.applySharedBankStyles(document);
      applyRetailProductStyles(document);

      addCenteredTitle(document, "Meridian Retail Banking");
      XWPFParagraph addressParagraph = document.createParagraph();
      addressParagraph.setAlignment(ParagraphAlignment.CENTER);
      XWPFRun addressRun = addressParagraph.createRun();
      addressRun.setFontSize(9);
      addressRun.setColor("666666");
      addressRun.setFontFamily("Calibri");
      addressRun.setText("100 Commerce Street, Retail District · www.demo-retail.example");

      document.createParagraph();

      XWPFTable accountTable = document.createTable(2, 2);
      styleTableHeaderRow(accountTable.getRow(0), "Account reference", "Letter date");
      setCellText(accountTable.getRow(1).getCell(0), "Customer: {{anchor:" + anchorId + "}}");
      setCellText(accountTable.getRow(1).getCell(1), "{{letterDate}}");

      XWPFParagraph bodyParagraph = document.createParagraph();
      XWPFRun bodyRun = bodyParagraph.createRun();
      bodyRun.setFontFamily("Calibri");
      bodyRun.setText(
          "We are writing to confirm the details of your recent correspondence regarding your retail account."
      );

      XWPFParagraph footerParagraph = document.createParagraph();
      footerParagraph.setAlignment(ParagraphAlignment.CENTER);
      XWPFRun footerRun = footerParagraph.createRun();
      footerRun.setFontSize(8);
      footerRun.setItalic(true);
      footerRun.setColor("888888");
      footerRun.setFontFamily("Calibri");
      footerRun.setText(
          "Demo Retail Bank is a fictitious entity for automated testing only. This is not legal or financial advice."
      );

      DocxWordCompatibilitySupport.ensureWordCompatiblePackage(document);
      document.write(output);
      return output.toByteArray();
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to build retail letterhead replacement DOCX", ex);
    }
  }

  private static void applyRetailProductStyles(XWPFDocument document) {
    XWPFStyles styles = document.getStyles();
    if (styles == null) {
      return;
    }
    DocxMasterStyleRegistry.registerBankParagraphStyle(styles, "DisclaimerBody", 16, "Calibri", false);
  }

  private static void addCenteredTitle(XWPFDocument document, String text) {
    XWPFParagraph paragraph = document.createParagraph();
    paragraph.setAlignment(ParagraphAlignment.CENTER);
    XWPFRun run = paragraph.createRun();
    run.setBold(true);
    run.setFontSize(14);
    run.setColor("006633");
    run.setFontFamily("Calibri");
    run.setText(text);
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
    run.setFontFamily("Calibri");
    run.setText(text);
  }
}
