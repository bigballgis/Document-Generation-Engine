export const apiErrorEn = {
  authentication: {
    invalidCredentials: "Invalid credentials.",
    sessionExpired: "Your session has expired. Please sign in again.",
    sessionAbsoluteLimitReached: "Your sign-in session has reached its maximum duration. Please sign in again.",
    sessionRevoked: "Your session is no longer valid. Please sign in again.",
    sessionValidationUnavailable: "We are unable to verify your session right now. Please try again later."
  },
  authorization: {
    accessDenied: "You do not have permission to perform this action.",
    groupScopeOutOfRange: "The requested group scope is outside your authorized scope.",
    roleAssignmentNotAllowed: "You are not allowed to assign one or more of the requested roles.",
    roleNotAssignable: "The requested management role is not assignable.",
    userDeleteNotAllowed: "You are not allowed to delete users.",
    groupManagementNotAllowed: "You are not allowed to manage business groups."
  },
  validation: {
    requestBodyInvalid: "The request body is invalid.",
    fieldRequired: "This field is required.",
    fieldInvalid: "This field is invalid.",
    fieldSizeInvalid: "This field length is invalid.",
    fieldPatternInvalid: "This field format is invalid.",
    fieldUnknown: "Unknown field.",
    contentModuleStructureMissing: "The referenced content module has no pinned structure.",
    contentModuleNestingCycle: "Content module nesting cycle detected during expansion.",
    variableValidationFailed:
      "One or more template variables failed VariableSchema validation.",
  },
  storage: {
    operationFailed: "Object storage operation failed."
  },
  generation: {
    internalError: "An internal error occurred.",
    serviceUnavailable: "The generation service is temporarily unavailable.",
    generationServiceUnavailable: "Document generation service is temporarily unavailable.",
    generationTimeout: "Document generation timed out.",
    pdfConversionFailed: "PDF conversion failed.",
    pdfConversionCapacityExceeded: "PDF conversion capacity is temporarily exceeded; retry later.",
    pdfArchivalEncryptionMutex: "PDF/A archival output cannot be combined with encryption.",
    artifactTooLarge: "Generated artifact exceeds the configured size limit.",
    docxNormalizationFailed: "DOCX normalization failed.",
    idempotencyDigestFailed: "The request could not be processed safely. Please retry."
  },
  idempotency: {
    hashFailed: "Unable to compute the idempotency fingerprint."
  },
  master: {
    notFound: "The letterhead was not found.",
    accessDenied: "You do not have permission to access this letterhead.",
    invalidState: "The letterhead is not in a valid state for this operation.",
    anchorIntegrityFailed: "Anchor integrity validation failed for the letterhead.",
    invalidFile: "The uploaded file is not a valid DOCX letterhead.",
    emptyAnchors: "The letterhead must contain at least one anchor.",
    docxRequired: "A valid DOCX file is required.",
    docxTooLarge: "The uploaded DOCX exceeds the maximum allowed size.",
    docxCorrupt: "The uploaded DOCX package is invalid or corrupt.",
    anchorExtractionFailed: "Unable to extract anchors from the uploaded DOCX.",
    styleCatalogParseFailed: "Unable to parse the letterhead style catalog from styles.xml.",
    storageFailed: "Unable to store the uploaded letterhead.",
    downloadFailed: "Unable to download the letterhead file.",
    invalidReviewTransition: "The letterhead is not in a valid state for this review action.",
    currentRevisionUnavailable: "The current letterhead revision could not be resolved for pinning.",
    revisionDiffBaselineUnavailable: "A previous letterhead revision is required for comparison.",
    revisionInUseByPublishedRelease:
      "The letterhead revision is pinned by one or more published releases and cannot be deleted.",
    revisionDeleteFailed: "Unable to delete the letterhead revision.",
    cannotDeleteCurrentRevision: "The current letterhead revision cannot be deleted.",
  },
  template: {
    notFound: "The template was not found.",
    testDataSetNotFound: "The test data set was not found.",
    testDataSetLocked: "The test data set is locked by test evidence and cannot be modified.",
    testDataSetSchemaInvalid: "The test data set variables do not match the template variable schema.",
    testDataSetPiiHandlingRequired:
      "PII-tagged test data values require piiHandling of SYNTHETIC or EXPLICIT_SENSITIVE.",
    piiConfirmReasonRequired: "An explicit sensitive test-data confirmation requires a non-blank reason.",
    piiSecondaryConfirmRequired:
      "An explicit sensitive test-data confirmation requires secondaryConfirmed=true.",
    piiCategoryInvalid: "The variable piiCategory value is not supported.",
    accessDenied: "You do not have permission to access this template.",
    changeDiffReleaseVersionsRequired:
      "Both releaseVersionA and releaseVersionB are required for release change-diff comparison.",
    confirmationRequired: "Secondary confirmation is required for this operation.",
    invalidState: "The template is not in a valid state for this operation (current status: {0}).",
    optimisticLockConflict: "This template version was updated elsewhere. Reload, then try again.",
    invalidRulesJson: "The composition or binding rules JSON is invalid and could not be parsed.",
    devLineInFlight: "A development version line is already in progress. Finish or abandon it before cloning a published release.",
    versionImmutable: "Published template version content cannot be modified.",
    defaultRouteTargetCannotDeactivate: "The release version configured as the default API route cannot be deactivated.",
    alreadyDeleted: "The template has already been deleted.",
    masterNotApproved: "Templates can only be created from approved letterheads.",
    masterGroupMismatch: "The letterhead group does not match the template group.",
    externalIdExists: "A template with this external identifier already exists.",
    localeRequired: "A valid BCP-47 locale is required for the template body language.",
    localeVariantConflict:
      "A template with the same locale already exists in this locale variant family for the group.",
    approvalMatrixModeLocked:
      "Approval matrix mode cannot be changed after approval has started.",
    approvalStageRoleForbidden:
      "You do not have permission to decide this approval stage.",
    approvalStageMismatch:
      "The requested approval stage does not match the template's current approval stage.",
    compositionInclusionRuleInvalid: "The composition inclusion rule set is invalid.",
    compositionInclusionUnsatisfied:
      "A required composition inclusion rule was not satisfied by the request context.",
    bulkRepin: {
      dryRunRequired: "dryRun is required for bulk re-pin.",
      groupCodeRequired: "groupCode is required when the authorized group context is ambiguous.",
      targetXor: "Provide exactly one of toSemanticVersion or useLatestApproved=true.",
    },
    contentModuleJurisdictionMismatch:
      "An included content module jurisdiction does not match the request jurisdiction.",
    variableTypeUnsupported: "The variable type is not supported.",
    enumValuesRequired: "Enum variables require enum values.",
    structuredContentInvalid: "The structured content definition is invalid.",
    structuredContentUnknownNodeType: "The structured content contains an unsupported node type.",
    structuredContentForbiddenConstruct: "The structured content contains a forbidden construct.",
    invalidPasteCleaningEvidence: "Paste-cleaning evidence could not be serialized.",
    bindingVersionConflict:
      "This binding was updated elsewhere. Reload the binding, then save again.",
    bindingExpectedUpdatedAtRequired:
      "expectedUpdatedAt is required when updating an existing anchor binding.",
    publishGateBlocked: "Publish is blocked by gate check {0}.",
    submitForApprovalGateBlocked: "Submit for approval is blocked by gate check {0}.",
    decisionReasonCategoryRequired: "A reason category is required for failed or rejected decisions.",
    decisionImpactSummaryRequired: "An impact summary is required for failed or rejected decisions.",
    decisionFidelityConfirmationRequired: "Fidelity evidence must be confirmed before recording a passing test decision.",
    decisionCoverageConfirmationRequired: "Coverage summary must be confirmed before recording a passing test decision.",
    decisionPreviewConfirmationRequired: "Preview evidence must be confirmed before recording a passing test decision.",
    decisionRationaleRequired: "Approval rationale is required.",
    decisionKeyEvidenceConfirmationRequired: "Key evidence must be confirmed before approving.",
    decisionRemediationLinkRequired: "A remediation link to test record, change diff, or checklist is required for rejection.",
    exceptionInterventionNotAllowed: "Exception intervention is only allowed for group administrators.",
    exceptionReasonRequired: "An exception reason is required for intervention decisions.",
    exceptionSecondaryConfirmRequired: "Secondary confirmation is required for exception intervention.",
    contentModuleReferenceMissing: "The referenced content module version was not found.",
    contentModuleReferenceInvalid: "The content module reference is not approved, active, or accessible.",
    contentModuleReferenceLocked: "Published content module references cannot be changed.",
    exportNotEligible: "Only approved or published templates can be exported.",
    exportFormatUnsupported: "The requested export format is not supported.",
    exportFailed: "The template export could not be completed.",
    exportAssetBinaryMissing: "Promotion export failed because a referenced asset binary is unavailable.",
    importBundleInvalid: "The template import bundle is invalid.",
    importBundleUnsupportedFormat: "The template import bundle format is not supported.",
    importBundleContainsSecrets: "The template import bundle must not contain secrets or credentials.",
    importConflict: "The template import conflicts with an existing template.",
    importFailed: "The template import could not be completed.",
    importDependenciesUnsatisfied: "Template import dependencies are not satisfied.",
    dep: {
      bundleFormatOk: "The import bundle format is supported.",
      masterPinAbsent: "The import bundle is missing a master pin fingerprint.",
      masterDocxAbsent: "The import ZIP is missing artifacts/master.docx.",
      masterDocxHashMismatch: "The embedded master DOCX hash does not match the bundle master pin.",
      masterFingerprintMismatch: "The target master fingerprint does not match the bundle master pin.",
      masterFingerprintOk: "The target master fingerprint matches the bundle master pin.",
      masterWillMaterialize: "The embedded master DOCX will be materialized as a DRAFT letterhead (must re-approve on PROD).",
      clausePresent: "The content module clause is already present in the target environment.",
      clauseWillMaterialize: "The content module clause will be materialized from the bundle snapshot.",
      clauseMissing: "The content module clause is missing and no snapshot is available to materialize.",
      assetKeyPresent: "The asset key exists in object storage.",
      assetKeyMissing: "The asset key does not exist in object storage.",
      assetWillMaterialize: "The asset binary in the pack will be materialized into the asset library.",
      assetBinaryAbsent: "The asset key is missing on the target and the pack does not embed its binary.",
      clauseNestingOk: "The clause nesting edge is already satisfied on the target.",
      clauseNestingWillMaterialize: "The clause nesting edge will be covered by materialized clause snapshots.",
      clauseNestingMissing: "The clause nesting closure is incomplete (missing snapshots or target modules).",
      renderProfileAbsent: "The export bundle does not include a render profile snapshot.",
      renderProfilePresent: "The export bundle includes a render profile snapshot."
    },
    computeExpressionInvalid: "The compute expression is invalid.",
    nextReviewDueInvalid: "The nextReviewDue value must be a valid calendar date."
  },
  library: {
    exportEmpty: "No exportable templates were found for the requested library export scope.",
    exportLimitExceeded: "The library export request exceeds the maximum of 500 templates.",
    exportFailed: "The library export could not be completed."
  },
  variable: {
    computeFailed: "Variable compute evaluation failed."
  },
  rendering: {
    previewNotFound: "The preview record was not found.",
    fidelityWarningIndexOutOfRange: "The fidelity warning index is out of range for this preview.",
    previewArtifactNotAvailable: "The preview artifact is not available for download.",
    previewArtifactExpired: "The preview artifact has expired. Please regenerate the preview.",
    generationFailed: "Document generation failed.",
    renderProfileInvalid: "The render profile configuration is invalid.",
    previewConcurrencyLimitExceeded: "The preview generation concurrency limit has been reached. Please retry later.",
    batchTestRunNotFound: "The batch test run was not found.",
    unsupportedNodeType: "The template contains a structured content node type that cannot be rendered. Remove or replace the unsupported node before publishing.",
    imageAssetNotFound: "The referenced image asset could not be found.",
    sealAssetNotFound: "The referenced seal asset could not be found.",
    ooxmlValidationFailed:
      "The generated document failed OOXML validation and was not saved. Regenerate after correcting structured content.",
    pinnedMasterUnavailable: "The pinned letterhead revision for this release is unavailable.",
    qrBarcodePayloadMissing:
      "The QR or barcode payload variable is missing or blank. Provide a non-empty value for the referenced key.",
    qrBarcodeConfigInvalid:
      "The QR or barcode node configuration is invalid. Check sizePx, errorCorrection, and format.",
    qrBarcodeEncodeFailed:
      "The QR or barcode payload could not be encoded. Check the payload characters and format.",
    attachmentListPayloadMissing:
      "The attachment list payload variable is missing or null. Provide a string array for the referenced key.",
    attachmentListPayloadInvalid:
      "The attachment list payload is invalid. Provide a string array for the referenced key."
  },
  lifecycle: {
    selfApprovalForbidden:
      "Self-approval is not permitted; the decision actor must differ from the most recent submitter.",
    exceptionInterventionNotAllowed:
      "Exception intervention is only allowed for group or global administrators.",
    exceptionReasonRequired: "An exception reason is required for intervention decisions.",
    exceptionSecondaryConfirmRequired: "Secondary confirmation is required for exception intervention."
  },
  apimgmt: {
    policyNotFound: "The API policy was not found.",
    accessDenied: "You do not have permission to manage API settings for this template.",
    templateNotPublished: "API policy can only be configured for published templates.",
    credentialNotActive: "The API credential is not active.",
    credentialExpiryDaysInvalid: "Credential expiry days must be between 1 and 365.",
    credentialAlreadyRevoked: "The API credential is already revoked.",
    defaultRouteTargetNotCallable: "The default route target release version is not callable.",
    policyVersionNotFound: "The requested API policy version was not found.",
    policyImpactBlocked: "The candidate API policy has blocking impacts and cannot be applied.",
    policyImpactConfirmationRequired: "Impact preview warnings must be confirmed before applying the API policy change.",
    invocationRetentionPresetInvalid: "Invocation record retention must use a supported preset value.",
    documentRetentionPresetInvalid: "Document retention must use a supported preset value.",
    documentRetentionExceedsRecordRetention: "Document retention cannot exceed invocation record retention."
  },
  runtime: {
    invalidCredentials: "Invalid API credentials.",
    accessAccountRequired: "The access account header is required.",
    adGroupDenied: "The caller is not authorized by AD Group policy.",
    policyNotConfigured: "API policy is not configured for this template.",
    templateCredentialMismatch: "The credential is not valid for this template.",
    apiCredentialExpired: "The API credential has expired.",
    apiCredentialRevoked: "The API credential has been revoked.",
    releaseVersionRequired: "A release version is required for generation.",
    versionNotCallable: "The requested release version is not callable.",
    idempotencyConflict: "The idempotency key was already used with a different request.",
    documentNotFound: "The generated document was not found.",
    downloadUrlExpired: "The download URL has expired.",
    outputFormatUnsupported: "The requested output format is not supported.",
    outputModeUnsupported: "The requested output mode is not supported.",
    batchNotEnabled: "Batch generation is not enabled for this template.",
    batchLimitExceeded: "The batch item count exceeds the configured limit.",
    itemIdDuplicated: "Duplicate item identifiers are not allowed in a batch request.",
    asyncTaskNotFound: "The async task was not found.",
    asyncTaskExpired: "The async task has expired.",
    batchProcessingFailed: "Batch processing failed for one or more items.",
    asyncTaskCancellationNotAllowed: "The async task cannot be cancelled in its current state.",
    idempotencyKeyRequired: "The idempotency key is required.",
    rateLimitExceeded: "Too many requests. Please retry later.",
    rateLimitBackendUnavailable:
      "Rate-limit service is temporarily unavailable. Please retry later.",
    invocationNotFound: "The invocation record was not found.",
    invocationRecordExpired: "The invocation record has expired.",
    invocationViewInvalid: "The invocation view parameter is invalid.",
    templateLocaleMismatch:
      "The request locale is not language-compatible with the pinned template locale.",
  },
  encryption: {
    encryptionParameterInvalid: "Encryption parameters are invalid for the current request.",
    encryptionNotAllowed: "Dynamic encryption is not allowed for this API.",
    openPasswordRequired: "An open password is required when encryption is enabled.",
    ownerPasswordRequired: "An owner password is required when encryption permissions are provided.",
    passwordsMustDiffer: "Open and owner passwords must be different.",
    passwordLengthInvalid: "Encryption passwords must be between 12 and 128 characters.",
    permissionUnsupported: "The requested encryption permission is not supported.",
    encryptionFailed: "Document encryption failed."
  },
  audit: {
    invalidTimeWindow: "The audit time window is invalid.",
    scopeRequired: "Group scope and template identifier are required for group-scoped audit queries.",
    releaseBundleSnapshotUnavailable: "Release-bundle snapshot is not available for this invocation.",
    releaseBundleHashMismatch: "Release-bundle hash does not match the pinned master object.",
    invocationKindNotRegenerable:
      "This invocation kind cannot be regenerated; use a SINGLE, BATCH_ITEM, or ASYNC_TASK record.",
    invocationRecordExpired: "Invocation record has expired.",
    specimenWatermarkFailed: "SPECIMEN watermark could not be applied.",
    productionReissueReasonRequired: "A non-blank reason is required for production re-issue.",
  },
  batch: {
    originalBatchNotFound: "Original batch was not found."
  },
  notFound: {
    userNotFound: "The requested user does not exist.",
    groupNotFound: "The requested business group does not exist.",
    legalHoldNotFound: "The legal hold was not found."
  },
  conflict: {
    usernameAlreadyExists: "A user with this username already exists.",
    groupCodeAlreadyExists: "A business group with this code already exists.",
    legalHoldAlreadyReleased: "The legal hold has already been released.",
    dataIntegrity: "The change conflicts with an existing unique record. Reload and retry.",
  },
  contentModule: {
    notFound: "The content module was not found.",
    accessDenied: "You do not have permission to access this content module.",
    groupCodeRequired: "A group code is required to list content modules.",
    moduleCodeExists: "A content module with this module code already exists.",
    localeRequired: "A valid BCP-47 locale is required for the content module body language.",
    localeVariantConflict:
      "A content module with the same locale already exists in this locale variant family for the group.",
    versionExists: "A content module version with this semantic version already exists.",
    versionRequired: "The content module has no version to project catalog status from.",
    draftOnlyEditable: "Only draft versions can be edited.",
    contentStructureRequired: "Content structure is required.",
    nestingCycle: "Content module nesting forms a cycle.",
    nestingDepthExceeded: "Content module nesting depth exceeds the maximum of 8.",
    nestingTargetUnresolved:
      "A nested content module reference could not be resolved to a visible module.",
    nestingStructureInvalid:
      "Content module structure JSON is malformed and cannot be used for nesting governance.",
    moduleIdRequired: "The content module identifier is required.",
    changeDescriptionRequired: "A change description is required to submit for review.",
    rejectionReasonRequired: "A rejection reason is required to reject review.",
    reviewRoleDenied: "You do not have permission to perform this content module review action.",
    reviewStateTransitionDenied: "The content module version is not in a valid state for this review transition.",
    reviewRequestInvalid: "The content module review request is invalid.",
    lifecycleRoleDenied: "You do not have permission to perform this content module lifecycle operation.",
    lifecycleStateTransitionDenied: "The content module version is not in a valid state for this lifecycle operation.",
    lifecycleRequestInvalid: "The content module lifecycle request is invalid.",
    versionTargetRequired: "A content module versionId or semanticVersion is required for this operation.",
    versionTargetNotFound: "The specified content module version was not found.",
    impactConfirmationRequired: "Impact summary review and secondary confirmation are required for this lifecycle operation.",
    invalidEffectiveRange: "effectiveFrom must be less than or equal to effectiveTo.",
    searchTooLong: "The search string must be at most 200 characters.",
    searchModeInvalid: "The searchMode value is not supported.",
  },
  collaboration: {
    accessDenied: "You do not have permission to view collaboration work items.",
    queueDenied: "You do not have permission to view this collaboration work item queue.",
    workItemNotFound: "The collaboration work item was not found."
  },
  assetLibrary: {
    accessDenied: "You do not have permission to access the asset library.",
    assetKeyInvalid: "The asset key is invalid.",
    assetKeyConflict: "An active asset with this key already exists.",
    groupCodeRequired: "A group code is required to upload or disable an asset library entry.",
    contentTypeUnsupported: "The uploaded content type is not supported.",
    contentTypeMismatch: "The uploaded content type does not match the file contents.",
    payloadTooLarge: "The uploaded file exceeds the maximum allowed size of 5 MiB.",
    payloadEmpty: "The uploaded file is empty.",
    assetNotFound: "The asset library entry was not found."
  },
  documentBrand: {
    legalEntityUnknown: "The legal entity code is unknown for this group.",
    legalEntityInactive: "The legal entity is inactive.",
    documentBrandInactive: "The document brand is inactive or unavailable.",
    documentBrandNotAllowed: "The resolved document brand is not allowed for this template.",
    documentBrandUnknown: "The document brand code is unknown for this group.",
    codeInvalid: "The document brand or legal entity code format is invalid.",
    codeConflict: "A catalog entry with this code already exists in the group.",
    logoRequired: "A logo object reference is required for the document brand.",
    documentBrandRequired: "A document brand code is required when creating a legal entity.",
    surfaceRetired:
      "The DocumentBrand management surface has been retired. Manage letterhead, logo, and seal via Letterhead (master).",
  },
  legalEntity: {
    surfaceRetired:
      "The LegalEntity management surface has been retired. Legal holds remain available as a separate surface.",
  },
} as const
