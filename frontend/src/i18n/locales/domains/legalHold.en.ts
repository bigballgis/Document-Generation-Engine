export const legalHoldEn = {
    list: {
      title: 'Legal holds',
      description:
        'Create and release legal holds that exempt matching invocation and audit records from retention deletion.',
      help: 'Only global administrators can manage legal holds. Active holds block cleanup until released.',
      empty: 'No legal holds yet',
      emptyDescription:
        'Create a legal hold to protect invocation or audit records from retention cleanup.',
      emptyDescriptionReadOnly:
        'No legal holds are in scope right now. Holds appear here when an administrator creates them.',
      columns: {
        holdId: 'Hold ID',
        scope: 'Scope',
        summary: 'Protected scope',
        status: 'Status',
        reason: 'Reason',
        createdBy: 'Created by',
        createdAt: 'Created at',
      },
    },
    filters: {
      status: 'Status',
      statusAll: 'All statuses',
    },
    status: {
      ACTIVE: 'Active',
      RELEASED: 'Released',
    },
    scope: {
      TEMPLATE_WINDOW: 'Template window',
      INVOCATION_SET: 'Invocation set',
      openEnded: 'Open-ended',
      templateWindowSummary: 'Template {template} · {from} → {to}',
      invocationSetSummary: '{count} invocation ID(s)',
    },
    create: {
      open: 'Create legal hold',
      title: 'Create legal hold',
      submit: 'Create hold',
      fields: {
        scopeType: 'Scope type',
        reason: 'Reason (optional)',
        template: 'Template',
        effectiveFrom: 'Effective from (UTC)',
        effectiveTo: 'Effective to (UTC, optional)',
        invocationIds: 'Invocation external IDs',
      },
      placeholders: {
        reason: 'Short reason for audit (no sensitive payload)',
        template: 'Search by name or external ID',
        effectiveFrom: 'Select start time',
        effectiveTo: 'Leave empty for open-ended',
        invocationIds: 'One ID per line, or comma-separated (max 500)',
      },
      validation: {
        scopeTypeRequired: 'Select a scope type.',
        templateRequired: 'Select a template.',
        effectiveFromRequired: 'Select an effective-from time.',
        invocationIdsRequired: 'Enter at least one invocation external ID.',
      },
    },
    release: {
      action: 'Release',
      confirmTitle: 'Release legal hold',
      confirmMessage:
        'Release hold {holdExternalId}? Protected records may become eligible for retention cleanup.',
      confirmButton: 'Release hold',
    },
    error: {
      loadList: 'Unable to load legal holds.',
      create: 'Unable to create the legal hold.',
      release: 'Unable to release the legal hold.',
    },
  }
