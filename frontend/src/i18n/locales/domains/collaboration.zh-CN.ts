export const collaborationZhCn = {
    notifications: {
      title: '通知',
      bellAriaLabel: '通知',
      bellAriaLabelWithCount: '通知，{count} 条未读',
      markAll: '全部标为已读',
      empty: '暂无通知',
      loading: '正在加载通知…',
      error: {
        loadUnread: '无法刷新未读通知数。',
        loadList: '无法加载通知。',
        markRead: '无法将通知标为已读。',
        markAll: '无法将全部通知标为已读。',
      },
    },
    workItems: {
      empty: '当前没有待办事项。',
      ageValue: '{value}',
      badge: {
        overdue: '超时提醒',
      },
      error: {
        load: '无法加载协作待办事项。',
      },
      columns: {
        template: '模板',
        group: '分组',
        submitter: '提交人',
        age: '等待时长',
        queue: '队列',
        summary: '摘要',
        trigger: '阶段',
      },
    },
    workItem: {
      queue: {
        TEST: {
          label: '测试中',
          title: '待我测试',
        },
        APPROVAL: {
          label: '待审批',
          title: '待我审批',
        },
        LEGAL: {
          label: '待法务审阅',
          title: '待我法务审阅',
        },
        REMEDIATION: {
          label: '待修改',
          title: '待我修改',
        },
        PENDING_RELEASE: {
          label: '待上线',
          title: '待确认上线',
        },
        ESCALATION: {
          label: '超时待跟进',
          title: '超时待跟进',
        },
      },
      trigger: {
        SUBMIT_FOR_TEST: {
          description: '模板已提交测试，请执行测试生成并记录决策。',
        },
        TEST_FAILURE_OR_RETURN_TO_DRAFT: {
          description: '模板从测试退回，请处理反馈后重新提交。',
        },
        SUBMIT_FOR_APPROVAL: {
          description: '模板测试通过后已提交审批。',
        },
        APPROVAL_FAILURE_OR_RETURN_TO_DRAFT: {
          description: '模板从审批退回，请处理反馈后重新提交。',
        },
        APPROVAL_PENDING_RELEASE: {
          description: '模板已审批通过，等待发布到目标环境。',
        },
        TIMEOUT_ESCALATION: {
          description: '该事项已逾期，请按提醒跟进。',
        },
      },
    },
    timeoutConfig: {
      title: '催办时限设置',
      description:
        '设置测试、审批、待上线确认和修改任务的超时提醒时间。提醒仅发送通知，不会自动变更流程状态。',
      refresh: '刷新',
      save: '保存催办时限',
      saveSuccess: '催办时限已保存。',
      scopeType: '配置范围',
      scopeGlobal: '全局默认',
      scopeGroup: '分组覆盖',
      groupCode: '分组代码',
      groupCodePlaceholder: '输入分组代码',
      testThresholdHours: '测试催办时限（小时）',
      approvalThresholdHours: '审批催办时限（小时）',
      pendingReleaseThresholdHours: '上线催办时限（小时）',
      remediationThresholdHours: '修改催办时限（小时）',
      lastUpdated: '最后更新：{updatedAt}',
      error: {
        load: '无法加载催办时限设置。',
        save: '无法保存催办时限设置。',
      },
    },
  }
