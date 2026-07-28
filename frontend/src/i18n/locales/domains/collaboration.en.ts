export const collaborationEn = {
    notifications: {
      title: 'Notifications',
      bellAriaLabel: 'Notifications',
      bellAriaLabelWithCount: 'Notifications, {count} unread',
      markAll: 'Mark all as read',
      empty: 'No notifications',
      loading: 'Loading notifications…',
      error: {
        loadUnread: 'Unable to refresh notification count.',
        loadList: 'Unable to load notifications.',
        markRead: 'Unable to mark notification as read.',
        markAll: 'Unable to mark all notifications as read.',
      },
    },
    workItems: {
      empty: 'You have no to-do items right now.',
      ageValue: '{value}',
      badge: {
        overdue: 'Overdue reminder',
      },
      error: {
        load: 'Unable to load collaboration to-do items.',
      },
      columns: {
        template: 'Template',
        group: 'Group',
        submitter: 'Submitter',
        age: 'Waiting',
        queue: 'Queue',
        summary: 'Summary',
        trigger: 'Stage',
      },
    },
    workItem: {
      queue: {
        TEST: {
          label: 'In testing',
          title: 'Waiting on my testing',
        },
        APPROVAL: {
          label: 'Awaiting approval',
          title: 'Waiting on my approval',
        },
        LEGAL: {
          label: 'Awaiting legal review',
          title: 'Waiting on my legal review',
        },
        REMEDIATION: {
          label: 'Needs fixes',
          title: 'Waiting on my fixes',
        },
        PENDING_RELEASE: {
          label: 'Awaiting go-live',
          title: 'Waiting to confirm go-live',
        },
        ESCALATION: {
          label: 'Overdue follow-up',
          title: 'Overdue to follow up',
        },
      },
      trigger: {
        SUBMIT_FOR_TEST: {
          description: 'Template submitted for testing — run test generation and record a decision.',
        },
        TEST_FAILURE_OR_RETURN_TO_DRAFT: {
          description: 'Template returned from testing — address feedback and resubmit.',
        },
        SUBMIT_FOR_APPROVAL: {
          description: 'Template submitted for approval after testing.',
        },
        APPROVAL_FAILURE_OR_RETURN_TO_DRAFT: {
          description: 'Template returned from approval — address feedback and resubmit.',
        },
        APPROVAL_PENDING_RELEASE: {
          description: 'Template approved and awaiting release to the target environment.',
        },
        TIMEOUT_ESCALATION: {
          description: 'Item is overdue — follow up on this reminder.',
        },
      },
    },
    timeoutConfig: {
      title: 'Reminder timing',
      description:
        'Set when overdue reminders appear for testing, approval, go-live confirmation, and fix tasks. Reminders are notifications only — they do not change workflow status.',
      refresh: 'Refresh',
      save: 'Save reminder timing',
      saveSuccess: 'Reminder timing saved.',
      scopeType: 'Configuration scope',
      scopeGlobal: 'Global default',
      scopeGroup: 'Group override',
      groupCode: 'Group code',
      groupCodePlaceholder: 'Enter group code',
      testThresholdHours: 'Testing reminder after (hours)',
      approvalThresholdHours: 'Approval reminder after (hours)',
      pendingReleaseThresholdHours: 'Go-live reminder after (hours)',
      remediationThresholdHours: 'Fix reminder after (hours)',
      lastUpdated: 'Last updated: {updatedAt}',
      error: {
        load: 'Unable to load reminder timing.',
        save: 'Unable to save reminder timing.',
      },
    },
  }
