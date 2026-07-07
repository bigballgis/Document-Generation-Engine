package com.bank.docgen.demo.support;

import java.math.BigInteger;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageMar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageSz;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTP;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STSectionMark;

/**
 * Shared layout helpers for build-time demo master DOCX assets.
 */
public final class DemoMasterDocxLayoutSupport {

    private DemoMasterDocxLayoutSupport() {
    }

    public static void configureA4PageLayout(XWPFDocument document) {
        CTSectPr sectPr = document.getDocument().getBody().isSetSectPr()
                ? document.getDocument().getBody().getSectPr()
                : document.getDocument().getBody().addNewSectPr();

        CTPageSz pageSize = sectPr.isSetPgSz() ? sectPr.getPgSz() : sectPr.addNewPgSz();
        pageSize.setW(BigInteger.valueOf(11906));
        pageSize.setH(BigInteger.valueOf(16838));

        CTPageMar margins = sectPr.isSetPgMar() ? sectPr.getPgMar() : sectPr.addNewPgMar();
        long baseline = DemoMasterDocxStyleSupport.MARGIN_BASELINE_TWIPS;
        margins.setTop(BigInteger.valueOf(baseline));
        margins.setBottom(BigInteger.valueOf(baseline));
        margins.setLeft(BigInteger.valueOf(baseline));
        margins.setRight(BigInteger.valueOf(baseline));
        margins.setHeader(BigInteger.valueOf(708));
        margins.setFooter(BigInteger.valueOf(708));
        margins.setGutter(BigInteger.valueOf(0));
    }

    public static void insertSectionBreakNextPage(XWPFParagraph paragraph, boolean restartPageNumbering) {
        CTP ctp = paragraph.getCTP();
        CTPPr pPr = ctp.isSetPPr() ? ctp.getPPr() : ctp.addNewPPr();
        CTSectPr sectPr = pPr.isSetSectPr() ? pPr.getSectPr() : pPr.addNewSectPr();
        sectPr.addNewType().setVal(STSectionMark.NEXT_PAGE);
        if (restartPageNumbering) {
            sectPr.addNewPgNumType().setStart(BigInteger.ONE);
        }
    }
}
