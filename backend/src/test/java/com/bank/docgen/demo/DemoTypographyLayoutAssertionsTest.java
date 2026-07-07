package com.bank.docgen.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bank.docgen.demo.support.DemoMasterDocxAssertions;
import com.bank.docgen.demo.support.DemoTypographyLayoutAssertions;
import com.bank.docgen.demo.support.DemoTypographyLayoutAssertions.PageNumberingProfile;
import org.junit.jupiter.api.Test;

class DemoTypographyLayoutAssertionsTest {

  @Test
  void extractStyleAsciiFont_readsHeadingAndBodyFontsFromStylesXml() throws Exception {
    byte[] docx = CreditLimitMasterDocxAssetGeneratorTest.buildMaster();
    String stylesXml = DemoMasterDocxAssertions.readStylesXml(docx);

    assertThat(DemoTypographyLayoutAssertions.extractStyleAsciiFont(stylesXml, "Heading1"))
        .isEqualToIgnoringCase("Cambria");
    assertThat(DemoTypographyLayoutAssertions.extractStyleAsciiFont(stylesXml, "ClauseBody"))
        .isEqualToIgnoringCase("Calibri");
  }

  @Test
  void assertBankGradeTypography_returnsExpectedAssertionCount() throws Exception {
    byte[] docx = CreditLimitMasterDocxAssetGeneratorTest.buildMaster();
    int count = DemoTypographyLayoutAssertions.assertBankGradeTypography(docx, PageNumberingProfile.SECTION_AND_GLOBAL);
    assertThat(count).isEqualTo(DemoTypographyLayoutAssertions.BANK_GRADE_ASSERTION_COUNT);
  }

  @Test
  void assertBankGradeTypography_rejectsGlobalOnlyWhenSectionPagesPresent() throws Exception {
    byte[] docx = CreditLimitMasterDocxAssetGeneratorTest.buildMaster();
    assertThatThrownBy(() ->
        DemoTypographyLayoutAssertions.assertBankGradeTypography(docx, PageNumberingProfile.GLOBAL_ONLY))
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining("SECTIONPAGES");
  }
}
