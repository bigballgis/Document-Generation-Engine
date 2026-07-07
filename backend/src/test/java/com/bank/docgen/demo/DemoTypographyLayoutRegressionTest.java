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
 * P23-T15 — centralized POI typography/layout regression suite for all demo master DOCX families.
 *
 * <p>BDD: TYP-001, 002, 003, 004, 013, 014, 016, 018 (master build-time surface).
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
  void bddDemoTyp016_regressionSuiteCoversEightFamiliesPlusFol() throws Exception {
    long familyCount = demoFamilyMasters().count();
    assertThat(familyCount).isGreaterThanOrEqualTo(9);
    assertThat(demoFamilyMasters().map(args -> args.get()[0].toString()))
        .contains("credit-limit", "mortgage", "trade-lc", "collection-rate", "annual-review", "wealth",
            "retail-account-open", "fol");
  }

  static Stream<Arguments> demoFamilyMasters() throws Exception {
    return Stream.of(
        Arguments.of(
            "credit-limit",
            CreditLimitMasterDocxAssetGeneratorTest.buildMaster(),
            PageNumberingProfile.SECTION_AND_GLOBAL),
        Arguments.of(
            "mortgage",
            MortgageMasterDocxAssetGeneratorTest.buildMaster(),
            PageNumberingProfile.SECTION_AND_GLOBAL),
        Arguments.of(
            "trade-lc",
            TradeLcMasterDocxAssetGeneratorTest.buildLcMaster(),
            PageNumberingProfile.GLOBAL_ONLY),
        Arguments.of(
            "trade-guarantee",
            TradeLcMasterDocxAssetGeneratorTest.buildGuaranteeMaster(),
            PageNumberingProfile.GLOBAL_ONLY),
        Arguments.of(
            "collection-rate",
            CollectionMasterDocxAssetGeneratorTest.buildRateChangeMaster(),
            PageNumberingProfile.GLOBAL_ONLY),
        Arguments.of(
            "collection-overdue",
            CollectionMasterDocxAssetGeneratorTest.buildOverdueMaster(),
            PageNumberingProfile.GLOBAL_ONLY),
        Arguments.of(
            "annual-review",
            AnnualReviewMasterDocxAssetGeneratorTest.buildAnnualReviewMaster(),
            PageNumberingProfile.SECTION_AND_GLOBAL),
        Arguments.of(
            "facility-renewal",
            AnnualReviewMasterDocxAssetGeneratorTest.buildFacilityRenewalMaster(),
            PageNumberingProfile.SECTION_AND_GLOBAL),
        Arguments.of(
            "wealth",
            WealthMasterDocxAssetGeneratorTest.buildMaster(),
            PageNumberingProfile.GLOBAL_ONLY),
        Arguments.of(
            "retail-account-open",
            RetailAccountMasterDocxAssetGeneratorTest.buildAccountOpeningMaster(),
            PageNumberingProfile.GLOBAL_ONLY),
        Arguments.of(
            "retail-account-balance",
            RetailAccountMasterDocxAssetGeneratorTest.buildBalanceConfirmationMaster(),
            PageNumberingProfile.GLOBAL_ONLY),
        Arguments.of(
            "fol",
            FolMasterDocxAssetGeneratorTest.buildWholesaleFolMasterDocx(),
            PageNumberingProfile.SECTION_AND_GLOBAL),
        Arguments.of(
            "full-flow",
            DemoRetailLetterheadDocxBuilder.buildFullFlowMaster(DemoCatalogSeeder.DEMO_ANCHOR_ID),
            PageNumberingProfile.GLOBAL_ONLY)
    );
  }
}
