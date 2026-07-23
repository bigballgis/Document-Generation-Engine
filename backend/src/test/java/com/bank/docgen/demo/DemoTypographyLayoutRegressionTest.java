package com.bank.docgen.demo;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.docgen.demo.support.DemoTypographyLayoutAssertions;
import com.bank.docgen.demo.support.DemoTypographyLayoutAssertions.PageNumberingProfile;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Keep-set POI typography/layout regression for bank-letter Live masters (TM #164).
 */
class DemoTypographyLayoutRegressionTest {

  @ParameterizedTest(name = "bddDemoTyp_master_{0}_meetsBankTypographyBaseline")
  @MethodSource("demoFamilyMasters")
  void bddDemoTyp_masterTypographyLayoutMeetsBankBaseline(
      String familyKey,
      byte[] masterDocx,
      PageNumberingProfile profile
  ) throws Exception {
    int assertionCount = DemoTypographyLayoutAssertions.assertBankGradeTypography(masterDocx, profile);
    assertThat(assertionCount)
        .as("Assertion count for demo family %s", familyKey)
        .isEqualTo(DemoTypographyLayoutAssertions.BANK_GRADE_ASSERTION_COUNT);
  }

  @Test
  void bddDemoKeep_regressionSuiteCoversKeepSetFamilies() throws Exception {
    assertThat(demoFamilyMasters().map(args -> args.get()[0].toString()))
        .containsExactlyInAnyOrder(
            "fol",
            "credit-limit",
            "annual-review",
            "facility-renewal",
            "facility-amendment",
            "commitment",
            "formal-demand",
            "covenant-waiver");
  }

  static Stream<Arguments> demoFamilyMasters() throws Exception {
    return Stream.of(
        Arguments.of(
            "fol",
            FolMasterDocxAssetGeneratorTest.buildWholesaleFolMasterDocx(),
            PageNumberingProfile.SECTION_AND_GLOBAL),
        Arguments.of(
            "credit-limit",
            CreditLimitMasterDocxAssetGeneratorTest.buildMaster(),
            PageNumberingProfile.SECTION_AND_GLOBAL),
        Arguments.of(
            "annual-review",
            AnnualReviewMasterDocxAssetGeneratorTest.buildAnnualReviewMaster(),
            PageNumberingProfile.SECTION_AND_GLOBAL),
        Arguments.of(
            "facility-renewal",
            AnnualReviewMasterDocxAssetGeneratorTest.buildFacilityRenewalMaster(),
            PageNumberingProfile.SECTION_AND_GLOBAL),
        Arguments.of(
            "facility-amendment",
            FacilityAmendmentMasterDocxAssetGeneratorTest.buildMaster(),
            PageNumberingProfile.SECTION_AND_GLOBAL),
        Arguments.of(
            "commitment",
            CommitmentMasterDocxAssetGeneratorTest.buildMaster(),
            PageNumberingProfile.SECTION_AND_GLOBAL),
        Arguments.of(
            "formal-demand",
            FormalDemandMasterDocxAssetGeneratorTest.buildMaster(),
            PageNumberingProfile.SECTION_AND_GLOBAL),
        Arguments.of(
            "covenant-waiver",
            CovenantWaiverMasterDocxAssetGeneratorTest.buildMaster(),
            PageNumberingProfile.SECTION_AND_GLOBAL)
    );
  }
}
