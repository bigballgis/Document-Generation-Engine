export const apiErrorEn = {
  "authentication": {
    "invalidCredentials": "Invalid credentials.",
    "sessionExpired": "Your session has expired. Please sign in again."
  },
  "authorization": {
    "accessDenied": "You do not have permission to perform this action.",
    "groupScopeOutOfRange": "The requested group scope is outside your authorized scope.",
    "roleAssignmentNotAllowed": "You are not allowed to assign one or more of the requested roles.",
    "userDeleteNotAllowed": "You are not allowed to delete users.",
    "groupManagementNotAllowed": "You are not allowed to manage business groups."
  },
  "validation": {
    "requestBodyInvalid": "The request body is invalid.",
    "fieldRequired": "This field is required.",
    "fieldInvalid": "This field is invalid.",
    "fieldSizeInvalid": "This field length is invalid.",
    "fieldPatternInvalid": "This field format is invalid."
  },
  "storage": {
    "operationFailed": "Object storage operation failed."
  },
  "generation": {
    "internalError": "An internal error occurred.",
    "serviceUnavailable": "The generation service is temporarily unavailable.",
    "pdfConversionFailed": "PDF conversion failed."
  },
  "master": {
    "notFound": "The master document was not found.",
    "accessDenied": "You do not have permission to access this master document.",
    "invalidState": "The master document is not in a valid state for this operation.",
    "anchorIntegrityFailed": "Anchor integrity validation failed for the master document.",
    "invalidFile": "The uploaded file is not a valid DOCX master document.",
    "emptyAnchors": "The master document must contain at least one anchor.",
    "docxRequired": "A valid DOCX file is required.",
    "anchorExtractionFailed": "Unable to extract anchors from the uploaded DOCX.",
    "storageFailed": "Unable to store the uploaded master document.",
    "downloadFailed": "Unable to download the master document file.",
    "invalidReviewTransition": "The master document is not in a valid state for this review action."
  },
  "template": {
    "notFound": "The template was not found.",
    "testDataSetNotFound": "The test data set was not found.",
    "accessDenied": "You do not have permission to access this template.",
    "confirmationRequired": "Secondary confirmation is required for this operation.",
    "invalidState": "The template is not in a valid state for this operation.",
    "alreadyDeleted": "The template has already been deleted.",
    "masterNotApproved": "Templates can only be created from approved master documents.",
    "masterGroupMismatch": "The master document group does not match the template group.",
    "externalIdExists": "A template with this external identifier already exists.",
    "variableTypeUnsupported": "The variable type is not supported.",
    "enumValuesRequired": "Enum variables require enum values.",
    "structuredContentInvalid": "The structured content definition is invalid.",
    "publishGateBlocked": "Publish is blocked until binding validation passes.",
    "decisionReasonCategoryRequired": "A reason category is required for failed or rejected decisions.",
    "decisionImpactSummaryRequired": "An impact summary is required for failed or rejected decisions."
  },
  "rendering": {
    "previewNotFound": "The preview record was not found.",
    "generationFailed": "Document generation failed."
  },
  "apimgmt": {
    "policyNotFound": "The API policy was not found.",
    "accessDenied": "You do not have permission to manage API settings for this template.",
    "templateNotPublished": "API policy can only be configured for published templates.",
    "credentialNotActive": "The API credential is not active."
  },
  "runtime": {
    "invalidCredentials": "Invalid API credentials.",
    "accessAccountRequired": "The access account header is required.",
    "adGroupDenied": "The caller is not authorized by AD Group policy.",
    "policyNotConfigured": "API policy is not configured for this template.",
    "templateCredentialMismatch": "The credential is not valid for this template.",
    "releaseVersionRequired": "A release version is required for generation.",
    "versionNotCallable": "The requested release version is not callable.",
    "idempotencyConflict": "The idempotency key was already used with a different request.",
    "documentNotFound": "The generated document was not found.",
    "downloadUrlExpired": "The download URL has expired.",
    "outputFormatUnsupported": "The requested output format is not supported.",
    "outputModeUnsupported": "The requested output mode is not supported.",
    "batchNotEnabled": "Batch generation is not enabled for this template.",
    "batchLimitExceeded": "The batch item count exceeds the configured limit.",
    "itemIdDuplicated": "Duplicate item identifiers are not allowed in a batch request.",
    "asyncTaskNotFound": "The async task was not found.",
    "asyncTaskExpired": "The async task has expired.",
    "batchProcessingFailed": "Batch processing failed for one or more items.",
    "asyncTaskCancellationNotAllowed": "The async task cannot be cancelled in its current state.",
    "idempotencyKeyRequired": "The idempotency key is required.",
    "rateLimitExceeded": "Too many requests. Please retry later."
  },
  "encryption": {
    "encryptionParameterInvalid": "Encryption parameters are invalid for the current request.",
    "encryptionNotAllowed": "Dynamic encryption is not allowed for this API.",
    "openPasswordRequired": "An open password is required when encryption is enabled.",
    "ownerPasswordRequired": "An owner password is required when encryption permissions are provided.",
    "passwordsMustDiffer": "Open and owner passwords must be different.",
    "passwordLengthInvalid": "Encryption passwords must be between 12 and 128 characters.",
    "permissionUnsupported": "The requested encryption permission is not supported.",
    "encryptionFailed": "Document encryption failed."
  },
  "audit": {
    "invalidTimeWindow": "The audit time window is invalid.",
    "scopeRequired": "Group scope and template identifier are required for group-scoped audit queries."
  },
  "notFound": {
    "userNotFound": "The requested user does not exist.",
    "groupNotFound": "The requested business group does not exist."
  },
  "conflict": {
    "usernameAlreadyExists": "A user with this username already exists.",
    "groupCodeAlreadyExists": "A business group with this code already exists."
  }
} as const
