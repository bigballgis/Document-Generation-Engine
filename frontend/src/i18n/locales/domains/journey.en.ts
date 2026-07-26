export const journeyEn = {
    timeline: {
      ariaLabel: 'Role workflow progress',
      emptyTitle: 'No workflow steps available',
      empty: {
        guidance:
          'There is no active workflow journey for this view yet. Open a task from your to-do list or use the quick links below to continue.',
      },
    },
    custom: {
      demo: 'Custom demo guidance',
    },
    roles: {
      DOCUMENT_AUTHOR: {
        title: 'Document authoring workflow',
        empty: {
          guidance:
            'Create or open a template to begin authoring, then follow each step below.',
        },
        waitingTesting: {
          guidance: 'Your template is with the testing team. You will be notified when a result is recorded.',
        },
        waitingApproval: {
          guidance: 'Your template is awaiting approval. You will be notified when a decision is recorded.',
        },
        complete: {
          guidance: 'This template is live. You can view published versions anytime.',
          cta: 'View template',
        },
        remediation: {
          guidance: 'Address test or approval feedback, update the template, then resubmit.',
        },
        awaitGoLive: {
          teamLeadGuidance: 'Awaiting team-lead go-live.',
        },
        steps: {
          create: {
            label: 'Create template',
            guidance: 'Create a new template or open an existing draft.',
            cta: 'Create template',
          },
          design: {
            label: 'Design content',
            guidance: 'Design template content and bind standard clauses as needed.',
            cta: 'Design content',
          },
          trialGenerate: {
            label: 'Run trial output',
            guidance: 'Run a trial output to preview the generated document.',
            cta: 'Run trial output',
          },
          submitTest: {
            label: 'Submit for testing',
            guidance: 'Submit the template for testing when content is ready.',
            cta: 'Submit for testing',
          },
          submitApproval: {
            label: 'Submit for approval',
            guidance: 'Submit the template for approval after testing passes.',
            cta: 'Submit for approval',
          },
          awaitGoLive: {
            label: 'Awaiting go-live',
            guidance: 'Wait for go-live confirmation after approval.',
          },
        },
        letterhead: {
          empty: {
          guidance:
            'Upload a letterhead document to begin your design workflow, then follow each step below.',
        },
        waitingReview: {
          guidance:
            'Your letterhead is with the review team. You will be notified when a decision is recorded.',
        },
        complete: {
          guidance: 'This letterhead is approved. You can view or download it anytime.',
          cta: 'View letterhead',
        },
        steps: {
          upload: {
            label: 'Upload letterhead',
            guidance: 'Upload your letterhead document to get started.',
            cta: 'Upload letterhead',
          },
          placeholders: {
            label: 'Set layout placeholders',
            guidance: 'Define layout placeholders for dynamic content areas.',
            cta: 'Check layout placeholders',
          },
          submitReview: {
            label: 'Submit for review',
            guidance: 'Submit your letterhead design for review when ready.',
            cta: 'Submit for review',
          },
          rework: {
            label: 'Fix and resubmit',
            guidance: 'Address review feedback and resubmit your letterhead.',
            cta: 'Update and resubmit',
          },
        },
        },
      },
      
      TEMPLATE_TESTER: {
        title: 'Template testing workflow',
        empty: {
          guidance:
            'Open a test request from your task list, then follow each step below.',
        },
        steps: {
          reviewRequest: {
            label: 'Review test request',
            guidance: 'Open the test request and review the submission summary.',
            cta: 'Review test request',
          },
          checkEvidence: {
            label: 'Check output evidence',
            guidance: 'Check output samples and evidence against expectations.',
            cta: 'Check output evidence',
          },
          recordResult: {
            label: 'Record test result',
            guidance: 'Record your test result so the author can proceed.',
            cta: 'Record test result',
          },
        },
      },
      
      LEGAL_REVIEWER: {
        title: 'Legal review workflow',
        empty: {
          guidance:
            'Open a legal review request from your task list, then follow each step below.',
        },
        steps: {
          reviewRequest: {
            label: 'Review legal request',
            guidance: 'Open the legal review request and confirm the submission context.',
            cta: 'Review legal request',
          },
          reviewSubmission: {
            label: 'Review submission package',
            guidance: 'Review evidence and fidelity summaries before recording a legal decision.',
            cta: 'Review submission package',
          },
          recordDecision: {
            label: 'Record legal decision',
            guidance: 'Approve or reject the legal stage so compliance can proceed.',
            cta: 'Record legal decision',
          },
        },
      },
      GROUP_ADMIN: {
        title: 'Team-lead go-live workflow',
        empty: {
          guidance:
            'Open a letterhead review or go-live request from your task list, then follow each step below.',
        },
        steps: {
          reviewLetterhead: {
            label: 'Review letterhead',
            guidance: 'Review uploaded letterheads waiting for your approval.',
            cta: 'Review letterhead',
          },
          reviewGoLiveRequest: {
            label: 'Review go-live request',
            guidance: 'Open the approved template and review why it is ready for go-live.',
            cta: 'Review go-live request',
          },
          runPreReleaseChecks: {
            label: 'Pre-release checks',
            guidance: 'Review pre-release checks and resolve any issues before go-live.',
            cta: 'View pre-release checks',
          },
          confirmGoLive: {
            label: 'Confirm go-live',
            guidance: 'Confirm the release summary and put the template live.',
            cta: 'Confirm go-live',
          },
        },
        compliance: {
          empty: {
          guidance:
            'Open an approval request from your task list, then follow each step below.',
        },
        steps: {
          reviewRequest: {
            label: 'Review approval request',
            guidance: 'Open the approval request and review why it was submitted.',
            cta: 'Review approval request',
          },
          reviewSubmission: {
            label: 'Review submission package',
            guidance: 'Review the submission summary and supporting evidence.',
            cta: 'Review submission package',
          },
          recordDecision: {
            label: 'Record approval decision',
            guidance: 'Record your approval decision so the author can proceed.',
            cta: 'Record approval decision',
          },
        },
        },
      },
      GLOBAL_ADMIN: {
        title: 'Bank-wide administration workflow',
        empty: {
          guidance:
            'Review the bank-wide overview below, then use the task hub and quick links to manage users, templates, and reminder timing.',
        },
        steps: {
          reviewOverview: {
            label: 'Review bank-wide overview',
            guidance: 'Scan summary cards and open tasks to see what needs attention across the bank.',
            cta: 'Review overview',
          },
          manageUsersGroups: {
            label: 'Manage users and groups',
            guidance: 'Create and maintain user accounts and business groups within your scope.',
            cta: 'Manage users and groups',
          },
          removeTemplates: {
            label: 'Remove templates',
            guidance: 'Remove templates that are no longer needed from the template list.',
            cta: 'Open template list',
          },
          setReminderDefaults: {
            label: 'Set bank-wide reminder timing',
            guidance: 'Set default reminder timing for testing, approval, and go-live queues bank-wide.',
            cta: 'Set reminder timing',
          },
          monitorOverdue: {
            label: 'Monitor overdue reminders',
            guidance: 'Follow up on overdue reminders from the task hub before they affect delivery.',
            cta: 'View overdue to follow up',
          },
          reviewAllTodos: {
            label: 'Review all to-dos',
            guidance: 'Work through open items across queues from the bank-wide task hub.',
            cta: 'Review task hub',
          },
        },
      },
      AUDIT_ADMIN: {
        title: 'Activity log workflow',
        empty: {
          guidance:
            'Open the activity log below, apply filters to find records, and export when needed. This page is view-only — you cannot change templates or users here.',
        },
        steps: {
          openActivityLog: {
            label: 'Open activity log',
            guidance: 'Review management and template workflow activity within your authorized scope.',
            cta: 'Open activity log',
          },
          searchAndFilter: {
            label: 'Search and filter records',
            guidance: 'Narrow results by event type, date range, or template.',
            cta: 'Apply filters',
          },
          reviewEntries: {
            label: 'Review who did what',
            guidance: 'Read who performed each action, what happened, on which template, and when.',
            cta: 'Review entries',
          },
          exportRecords: {
            label: 'Export records',
            guidance: 'Download matching activity records for offline review or compliance.',
            cta: 'Export records',
          },
          viewOnlyMode: {
            label: 'View only — no actions',
            guidance: 'You can search and export from this page but cannot modify templates, users, or decisions.',
            cta: 'View activity log',
          },
        },
      },
    },
  }
