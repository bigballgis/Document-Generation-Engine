export const apiMessagesEn = {
  publishGate: {
      anchorIntegrity: {
        ready: 'Layout placeholder bindings are valid.',
        blocked: 'Layout placeholder binding validation has blocking issues.',
      },
      variableSchema: {
        ready: 'Variable schema is configured.',
        missing: 'Variable schema is not configured.',
      },
      ruleBounds: {
        ready: 'Composition rules are within bounds.',
        blocked: 'Composition rules have blocking validation issues.',
      },
      testResults: {
        ready: 'Batch test results are available.',
        missing: 'No batch test run recorded.',
      },
      previewPresent: {
        ready: 'Successful preview artifacts exist.',
        missing: 'No successful preview artifacts exist.',
      },
      changeDiff: {
        ready: 'Change diff summary is available.',
      },
      approvalSummary: {
        ready: 'Approval decision recorded.',
        missing: 'Approval decision is missing.',
      },
      coverageThresholds: {
        ready: 'Coverage meets configured thresholds.',
        blocked: 'Coverage is below configured thresholds.',
      },
      apiPolicy: {
        ready: 'API access is configured.',
        blocked: 'API access is not callable (default route and AD groups are missing).',
      },
      blockerStatus: {
        ready: 'No unresolved blockers detected.',
        blocked: 'Unresolved blockers remain.',
      },
      contentModuleReferences: {
        ready: 'Content module references are valid.',
        blocked: 'Content module references are missing or invalid.',
      },
      unsupportedStructuredNodes: {
        ready: 'Structured content nodes are supported by the DOCX writer.',
        blocked:
          'Structured content contains unsupported or writer-missing node types that would cause silent content loss.',
      },
      pasteCleaningBlockers: {
        ready: 'No unresolved paste-cleaning blockers on bindings.',
        blocked:
          'One or more bindings have unresolved paste-cleaning blockers that must be cleared before publish.',
      },
      contentModuleEffectiveExpired: {
        ready: 'Referenced content modules are within their effective period.',
        blocked: 'One or more referenced content modules are past their effective end date. Update or re-pin modules.',
      },
      contentModuleEffectiveNotStarted: {
        ready: 'Referenced content modules have started their effective period.',
        blocked: 'One or more referenced content modules have not reached their effective start date yet.',
      },
      contentModuleLocaleMismatch: {
        ready: 'Content module locales match the template locale.',
        blocked: 'A referenced content module locale does not match this template. Align locales or choose another module version.',
      },
      contentModuleNestingCycle: {
        ready: 'Content module nesting has no cycles.',
        blocked: 'Content module nesting contains a cycle. Break the cycle in clause references before go-live.',
      },
      contentModuleNestingDepthExceeded: {
        ready: 'Content module nesting depth is within limits.',
        blocked: 'Content module nesting is too deep. Flatten nested clause references.',
      },
      contentModuleNestingUnpinned: {
        ready: 'Nested content module references are pinned to approved versions.',
        blocked: 'A nested content module reference is unpinned. Pin each nested clause to an approved version.',
      },
      compositionInclusionReferenceInvalid: {
        ready: 'Composition inclusion references are valid.',
        blocked: 'A composition inclusion rule references an invalid clause key. Fix the inclusion rules.',
      },
      paginationDeltaBudget: {
        ready: 'Word–PDF pagination delta is within the allowed budget.',
        blocked: 'Word and PDF page counts differ beyond the allowed budget. Adjust layout or review preview pages.',
      },
      fidelityWarningsViewed: {
        ready: 'Fidelity warnings have been reviewed.',
        blocked: 'Review all fidelity warnings in preview before go-live.',
      },

    },
  apimgmt: {
      policyImpact: {
        blocking: 'These access settings have blocking impacts.',
        warning: 'These access settings have non-blocking warnings.',
        safe: 'These access settings are safe to apply.',
        defaultRouteChanged: 'Default route target will change.',
        defaultRouteNotCallable: 'The candidate default route is not callable.',
        idempotencyDefaultRouteGuard:
          'Existing idempotency keys may conflict after default route changes.',
      },
    },
}
