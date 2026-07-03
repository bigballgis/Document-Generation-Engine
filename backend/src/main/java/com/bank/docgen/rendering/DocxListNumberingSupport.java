package com.bank.docgen.rendering;

import java.math.BigInteger;
import org.apache.poi.xwpf.usermodel.XWPFAbstractNum;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFNumbering;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTAbstractNum;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTLvl;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STNumberFormat;

/**
 * Creates and reuses Word list definitions for ordered and unordered structured lists.
 */
public final class DocxListNumberingSupport {

    private final XWPFDocument document;
    private BigInteger orderedNumId;
    private BigInteger bulletNumId;

    public DocxListNumberingSupport(XWPFDocument document) {
        this.document = document;
    }

    public void applyListFormatting(XWPFParagraph paragraph, boolean ordered) {
        BigInteger numId = ordered ? orderedNumId() : bulletNumId();
        paragraph.setNumID(numId);
        if (paragraph.getCTP().getPPr() == null) {
            paragraph.getCTP().addNewPPr();
        }
        if (paragraph.getCTP().getPPr().getNumPr() == null) {
            paragraph.getCTP().getPPr().addNewNumPr();
        }
        if (paragraph.getCTP().getPPr().getNumPr().getIlvl() == null) {
            paragraph.getCTP().getPPr().getNumPr().addNewIlvl();
        }
        paragraph.getCTP().getPPr().getNumPr().getIlvl().setVal(BigInteger.ZERO);
    }

    private BigInteger orderedNumId() {
        if (orderedNumId == null) {
            orderedNumId = createListDefinition(STNumberFormat.DECIMAL);
        }
        return orderedNumId;
    }

    private BigInteger bulletNumId() {
        if (bulletNumId == null) {
            bulletNumId = createListDefinition(STNumberFormat.BULLET);
        }
        return bulletNumId;
    }

    private BigInteger createListDefinition(STNumberFormat.Enum format) {
        XWPFNumbering numbering = document.createNumbering();
        CTAbstractNum abstractNum = CTAbstractNum.Factory.newInstance();
        abstractNum.setAbstractNumId(BigInteger.valueOf(numbering.getAbstractNums().size() + 1L));
        CTLvl level = abstractNum.addNewLvl();
        level.setIlvl(BigInteger.ZERO);
        level.addNewNumFmt().setVal(format);
        level.addNewStart().setVal(BigInteger.ONE);
        level.addNewLvlText().setVal(format == STNumberFormat.BULLET ? "\u2022" : "%1.");
        level.addNewLvlJc().setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STJc.LEFT);
        XWPFAbstractNum xwpfAbstractNum = new XWPFAbstractNum(abstractNum);
        BigInteger abstractNumId = numbering.addAbstractNum(xwpfAbstractNum);
        return numbering.addNum(abstractNumId);
    }
}
