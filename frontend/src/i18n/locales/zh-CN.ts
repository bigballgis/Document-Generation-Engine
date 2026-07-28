import { appZhCn } from './domains/app.zh-CN'
import { commandPaletteZhCn } from './domains/commandPalette.zh-CN'
import { onboardingTourZhCn } from './domains/onboardingTour.zh-CN'
import { loginZhCn } from './domains/login.zh-CN'
import { sessionZhCn } from './domains/session.zh-CN'
import { apiMessagesZhCn } from './domains/apiMessages.zh-CN'
import { apiErrorZhCn } from '@/i18n/catalogs/apiErrorZhCn'
import { forbiddenZhCn } from './domains/forbidden.zh-CN'
import { retiredSurfaceZhCn } from './domains/retiredSurface.zh-CN'
import { navZhCn } from './domains/nav.zh-CN'
import { brandZhCn } from './domains/brand.zh-CN'
import { homeZhCn } from './domains/home.zh-CN'
import { dashboardZhCn } from './domains/dashboard.zh-CN'
import { packageCatalogZhCn } from './domains/packageCatalog.zh-CN'
import { collaborationZhCn } from './domains/collaboration.zh-CN'
import { mastersZhCn } from './domains/masters.zh-CN'
import { auditZhCn } from './domains/audit.zh-CN'
import { documentLocaleZhCn } from './domains/documentLocale.zh-CN'
import { localeVariantFamilyZhCn } from './domains/localeVariantFamily.zh-CN'
import { templatesZhCn } from './domains/templates/index.zh-CN'
import { tableZhCn } from './domains/table.zh-CN'
import { pasteZhCn } from './domains/paste.zh-CN'
import { commonZhCn } from './domains/common.zh-CN'
import { apiPolicyZhCn } from './domains/apiPolicy.zh-CN'
import { contentModulesZhCn } from './domains/contentModules.zh-CN'
import { journeyZhCn } from './domains/journey.zh-CN'
import { identityZhCn } from './domains/identity.zh-CN'
import { legalHoldZhCn } from './domains/legalHold.zh-CN'
import { assetLibraryZhCn } from './domains/assetLibrary.zh-CN'

export default {
  app: appZhCn,
  commandPalette: commandPaletteZhCn,
  onboardingTour: onboardingTourZhCn,
  login: loginZhCn,
  session: sessionZhCn,
  api: {
    error: apiErrorZhCn,
    ...apiMessagesZhCn,
  },
  forbidden: forbiddenZhCn,
  retiredSurface: retiredSurfaceZhCn,
  nav: navZhCn,
  brand: brandZhCn,
  home: homeZhCn,
  dashboard: dashboardZhCn,
  packageCatalog: packageCatalogZhCn,
  collaboration: collaborationZhCn,
  masters: mastersZhCn,
  audit: auditZhCn,
  documentLocale: documentLocaleZhCn,
  localeVariantFamily: localeVariantFamilyZhCn,
  templates: templatesZhCn,
  table: tableZhCn,
  paste: pasteZhCn,
  common: commonZhCn,
  apiPolicy: apiPolicyZhCn,
  contentModules: contentModulesZhCn,
  journey: journeyZhCn,
  identity: identityZhCn,
  legalHold: legalHoldZhCn,
  assetLibrary: assetLibraryZhCn,
} as const
