import { appEn } from './domains/app.en'
import { commandPaletteEn } from './domains/commandPalette.en'
import { onboardingTourEn } from './domains/onboardingTour.en'
import { loginEn } from './domains/login.en'
import { sessionEn } from './domains/session.en'
import { apiMessagesEn } from './domains/apiMessages.en'
import { apiErrorEn } from '@/i18n/catalogs/apiErrorEn'
import { forbiddenEn } from './domains/forbidden.en'
import { retiredSurfaceEn } from './domains/retiredSurface.en'
import { navEn } from './domains/nav.en'
import { brandEn } from './domains/brand.en'
import { homeEn } from './domains/home.en'
import { dashboardEn } from './domains/dashboard.en'
import { packageCatalogEn } from './domains/packageCatalog.en'
import { collaborationEn } from './domains/collaboration.en'
import { mastersEn } from './domains/masters.en'
import { auditEn } from './domains/audit.en'
import { documentLocaleEn } from './domains/documentLocale.en'
import { localeVariantFamilyEn } from './domains/localeVariantFamily.en'
import { templatesEn } from './domains/templates/index.en'
import { tableEn } from './domains/table.en'
import { pasteEn } from './domains/paste.en'
import { commonEn } from './domains/common.en'
import { apiPolicyEn } from './domains/apiPolicy.en'
import { contentModulesEn } from './domains/contentModules.en'
import { journeyEn } from './domains/journey.en'
import { identityEn } from './domains/identity.en'
import { legalHoldEn } from './domains/legalHold.en'
import { assetLibraryEn } from './domains/assetLibrary.en'

export default {
  app: appEn,
  commandPalette: commandPaletteEn,
  onboardingTour: onboardingTourEn,
  login: loginEn,
  session: sessionEn,
  api: {
    error: apiErrorEn,
    ...apiMessagesEn,
  },
  forbidden: forbiddenEn,
  retiredSurface: retiredSurfaceEn,
  nav: navEn,
  brand: brandEn,
  home: homeEn,
  dashboard: dashboardEn,
  packageCatalog: packageCatalogEn,
  collaboration: collaborationEn,
  masters: mastersEn,
  audit: auditEn,
  documentLocale: documentLocaleEn,
  localeVariantFamily: localeVariantFamilyEn,
  templates: templatesEn,
  table: tableEn,
  paste: pasteEn,
  common: commonEn,
  apiPolicy: apiPolicyEn,
  contentModules: contentModulesEn,
  journey: journeyEn,
  identity: identityEn,
  legalHold: legalHoldEn,
  assetLibrary: assetLibraryEn,
}
