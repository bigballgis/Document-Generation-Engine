package com.bank.docgen.demo;

final class DemoDocxFactory {

    private DemoDocxFactory() {}

    static byte[] buildHeaderAnchorDocx(String anchorId) {
        return DemoRetailLetterheadDocxBuilder.buildFullFlowMaster(anchorId);
    }

    static byte[] buildRetailLetterheadReplacementDocx(String anchorId) {
        return DemoRetailLetterheadDocxBuilder.buildLetterheadReplacement(anchorId);
    }
}
