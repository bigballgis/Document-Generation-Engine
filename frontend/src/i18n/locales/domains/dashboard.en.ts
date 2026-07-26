export const dashboardEn = {
    title: 'My tasks',
    description:
      'Workflow to-dos for in-flight letter templates, plus a snapshot of your letterheads and templates.',
    loadError: 'Unable to load your task list.',
    summary: {
      error: {
        load: 'Unable to load dashboard summary statistics.',
      },
    },
    tabs: {
      overview: 'Overview',
      workflow: 'Workflow',
    },
    workflowTab: {
      description: 'Role-based workflow guidance for your current responsibilities.',
    },
    stats: {
      sectionTitle: 'Catalog & workflow snapshot',
      sectionDescription:
        'Package counts list registered letterheads and templates; workflow counts reflect in-flight review or approval steps.',
      pendingActions: {
        title: 'To-dos assigned to you',
        description:
          'Open items waiting for your test, approval, go-live confirmation, or review decision.',
        action: 'Review task list',
      },
      masterPendingReview: {
        title: 'Letterheads awaiting review',
        description: 'Letterhead packages with a revision line pending approve/reject.',
        action: 'Open letterheads',
      },
      masterVersionsInProgress: {
        title: 'Letterheads in progress',
        description: 'Draft letterheads that still need file or metadata fixes before resubmission.',
        action: 'Open letterheads',
      },
      templateVersionsInWorkflow: {
        title: 'Templates in workflow',
        description: 'Template packages in draft, testing, approval, or pending release.',
        action: 'Open templates',
      },
      publishedVersions: {
        title: 'Published templates',
        description: 'Template packages with a live release version callable at runtime.',
        action: 'Open templates',
      },
      stoppedVersions: {
        title: 'Stopped templates',
        description: 'Published templates temporarily blocked from runtime use.',
        action: 'Open templates',
      },
      catalogMasters: {
        title: 'Letterheads in catalog',
        description: 'Registered letterhead packages across your authorized groups.',
        action: 'Browse letterheads',
      },
      catalogTemplates: {
        title: 'Templates in catalog',
        description: 'Registered template packages (each owns its release version lines).',
        action: 'Browse templates',
      },
      externalServicesAlerts: {
        title: 'External services attention',
        description: 'Cross-package API access alerts that may need review.',
        action: 'Open external services overview',
      },
    },
    tasks: {
      title: 'My to-dos',
      description:
        'Open each item from its letterhead or template detail page to complete the next step.',
      empty: 'You have no to-dos right now.',
      columns: {
        action: 'Action',
        item: 'Item',
        group: 'Group',
        hint: 'What to do',
      },
      actions: {
        open: 'Open',
      },
      masterReview: {
        title: 'Review letterhead',
        description: 'Approve or reject the uploaded letterhead before templates can use it.',
      },
      templateTest: {
        title: 'Record test result',
        description: 'Run test generation and pass or fail the template in testing.',
      },
      templateApproval: {
        title: 'Record approval decision',
        description: 'Approve or reject the template after testing.',
      },
      templateLegalApproval: {
        title: 'Record legal review decision',
        description: 'Complete the legal review stage before compliance approval.',
      },
      templatePublish: {
        title: 'Confirm go-live',
        description: 'Release the approved template to the selected environment.',
      },
      templateDraft: {
        title: 'Continue template design',
        description: 'Finish bindings, rules, and submit the draft for testing.',
      },
      masterRework: {
        title: 'Letterheads to fix',
        itemTitle: 'Fix letterhead and resubmit',
        description:
          'Update the file or placeholders, then submit again for review.',
      },
      templateRework: {
        title: 'Fix template and resubmit',
        description:
          'Address test or approval feedback, then submit for testing again.',
      },
      clauseOutdatedBump: {
        title: 'Clause references to update',
        itemTitle: 'Update pinned clause versions',
        description:
          'One or more clause references are pinned to an older approved version. Bump pins on the template clause panel.',
      },
      annualReviewDue: {
        title: 'Annual review due',
        itemTitle: 'Complete annual review',
        description:
          'This template’s next review date has been reached. Open the package overview to complete the annual review.',
      },
      contentModuleReview: {
        title: 'Standard clauses to review',
        itemTitle: 'Review standard clause',
        description: 'Approve or reject the submitted standard clause version.',
      },
      contentModuleRework: {
        title: 'Standard clauses to fix',
        itemTitle: 'Fix standard clause and resubmit',
        description: 'Update the draft using the rejection reason, then submit again for review.',
      },
    },
    journey: {
      openWorkspace: 'Open workspace',
    },
    quickLinks: {
      title: 'Document catalogs',
      templates: 'Templates',
      masters: 'Letterhead templates',
      apiPolicies: 'External services overview',
    },
  }
