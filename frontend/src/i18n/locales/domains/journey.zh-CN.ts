export const journeyZhCn = {
    timeline: {
      ariaLabel: '角色工作流程进度',
      emptyTitle: '暂无工作流步骤',
      empty: {
        guidance:
          '当前视图尚无可用的工作流旅程。请从待办列表打开任务，或使用下方快捷入口继续。',
      },
    },
    custom: {
      demo: '自定义演示引导',
    },
    roles: {
      DOCUMENT_AUTHOR: {
        title: '文档作者工作流',
        empty: {
          guidance: '创建或打开模板开始编写，然后按下方步骤推进。',
        },
        waitingTesting: {
          guidance: '模板已提交测试，请等待测试结果。',
        },
        waitingApproval: {
          guidance: '模板待审批，请等待审批结果。',
        },
        complete: {
          guidance: '该模板已上线，可随时查看已发布版本。',
          cta: '查看模板',
        },
        remediation: {
          guidance: '根据测试或审批反馈修改模板后重新提交。',
        },
        awaitGoLive: {
          teamLeadGuidance: '待组长确认上线。',
        },
        steps: {
          create: {
            label: '创建模板',
            guidance: '创建新模板或打开现有草稿。',
            cta: '创建模板',
          },
          design: {
            label: '设计模板内容',
            guidance: '设计模板内容并按需绑定标准条款。',
            cta: '设计模板内容',
          },
          trialGenerate: {
            label: '试生成预览',
            guidance: '运行试生成以预览输出文档。',
            cta: '运行试生成',
          },
          submitTest: {
            label: '提交测试',
            guidance: '内容就绪后提交测试。',
            cta: '提交测试',
          },
          submitApproval: {
            label: '提交审批',
            guidance: '测试通过后提交审批。',
            cta: '提交审批',
          },
          awaitGoLive: {
            label: '等待确认上线',
            guidance: '审批通过后等待确认上线。',
          },
        },
        letterhead: {
          empty: {
          guidance: '上传母版文档开始设计流程，然后按下方步骤推进。',
        },
        waitingReview: {
          guidance: '母版已提交审核，请等待审核结果。',
        },
        complete: {
          guidance: '该母版已审核通过，可随时查看或下载。',
          cta: '查看母版',
        },
        steps: {
          upload: {
            label: '上传母版文档',
            guidance: '上传母版文档以开始设计。',
            cta: '上传母版',
          },
          placeholders: {
            label: '设置版式占位符',
            guidance: '为动态内容区域设置版式占位符。',
            cta: '检查版式占位符',
          },
          submitReview: {
            label: '提交审核',
            guidance: '设计完成后提交审核。',
            cta: '提交审核',
          },
          rework: {
            label: '修改后重新提交',
            guidance: '根据审核意见修改并重新提交母版。',
            cta: '更新并重新提交',
          },
        },
        },
      },
      
      TEMPLATE_TESTER: {
        title: '模板测试流程',
        empty: {
          guidance: '从任务列表打开测试任务，然后按下方步骤推进。',
        },
        steps: {
          reviewRequest: {
            label: '查看测试任务',
            guidance: '打开测试任务并查看提交摘要。',
            cta: '查看测试任务',
          },
          checkEvidence: {
            label: '核对输出效果',
            guidance: '核对输出样例与预期效果。',
            cta: '核对输出效果',
          },
          recordResult: {
            label: '记录测试结果',
            guidance: '记录测试结果以便作者继续推进。',
            cta: '记录测试结果',
          },
        },
      },
      
      LEGAL_REVIEWER: {
        title: '法务审阅流程',
        empty: {
          guidance: '从任务列表打开法务审阅任务，然后按下方步骤推进。',
        },
        steps: {
          reviewRequest: {
            label: '查看法务任务',
            guidance: '打开法务审阅任务并确认提交背景。',
            cta: '查看法务任务',
          },
          reviewSubmission: {
            label: '审阅提交材料',
            guidance: '在记录法务决定前审阅证据与保真摘要。',
            cta: '审阅提交材料',
          },
          recordDecision: {
            label: '记录法务决定',
            guidance: '通过或驳回法务阶段，以便进入合规审批。',
            cta: '记录法务决定',
          },
        },
      },
      GROUP_ADMIN: {
        title: '组长上线确认流程',
        empty: {
          guidance: '从任务列表打开母版审核或上线确认任务，然后按下方步骤推进。',
        },
        steps: {
          reviewLetterhead: {
            label: '审核母版',
            guidance: '审阅等待您确认的已上传母版。',
            cta: '审核母版',
          },
          reviewGoLiveRequest: {
            label: '查看上线申请',
            guidance: '打开已审批模板并查看为何可以上线。',
            cta: '查看上线申请',
          },
          runPreReleaseChecks: {
            label: '上线前检查',
            guidance: '查看上线前检查项并在上线前处理待办问题。',
            cta: '查看上线前检查',
          },
          confirmGoLive: {
            label: '确认上线',
            guidance: '确认发布摘要并将模板上架。',
            cta: '确认上线',
          },
        },
        compliance: {
          empty: {
          guidance: '从任务列表打开审批任务，然后按下方步骤推进。',
        },
        steps: {
          reviewRequest: {
            label: '查看审批任务',
            guidance: '打开审批任务并查看提交原因。',
            cta: '查看审批任务',
          },
          reviewSubmission: {
            label: '审阅提交材料',
            guidance: '审阅提交摘要与支撑证据。',
            cta: '审阅提交材料',
          },
          recordDecision: {
            label: '记录审批决定',
            guidance: '记录审批决定以便作者继续推进。',
            cta: '记录审批决定',
          },
        },
        },
      },
      GLOBAL_ADMIN: {
        title: '全行管理工作流程',
        empty: {
          guidance: '先查看下方全行概览，再通过任务中心与快捷入口管理用户、模板与提醒时限。',
        },
        steps: {
          reviewOverview: {
            label: '查看全行概览',
            guidance: '浏览汇总卡片与待办，了解全行需要跟进的事项。',
            cta: '查看概览',
          },
          manageUsersGroups: {
            label: '管理用户与分组',
            guidance: '在授权范围内创建和维护用户账户与业务分组。',
            cta: '管理用户与分组',
          },
          removeTemplates: {
            label: '移除模板',
            guidance: '在模板列表中移除不再需要的模板。',
            cta: '打开模板列表',
          },
          setReminderDefaults: {
            label: '设置全行提醒时限',
            guidance: '为测试、审批与上线队列设置全行默认提醒时限。',
            cta: '设置提醒时限',
          },
          monitorOverdue: {
            label: '跟进逾期提醒',
            guidance: '在任务中心跟进逾期提醒，避免影响交付进度。',
            cta: '查看待跟进逾期',
          },
          reviewAllTodos: {
            label: '查看全部待办',
            guidance: '在全行任务中心处理各队列中的待办事项。',
            cta: '查看任务中心',
          },
        },
      },
      AUDIT_ADMIN: {
        title: '操作记录工作流程',
        empty: {
          guidance:
            '在下方打开操作记录，使用筛选查找条目，需要时可导出。本页仅可查看 — 您无法在此修改模板或用户。',
        },
        steps: {
          openActivityLog: {
            label: '打开操作记录',
            guidance: '在授权范围内查看管理与模板工作流活动。',
            cta: '打开操作记录',
          },
          searchAndFilter: {
            label: '搜索并筛选记录',
            guidance: '按事件类型、时间范围或模板缩小结果范围。',
            cta: '应用筛选',
          },
          reviewEntries: {
            label: '查看谁做了什么',
            guidance: '阅读每条记录的操作人、发生了什么、涉及哪个模板以及时间。',
            cta: '查看条目',
          },
          exportRecords: {
            label: '导出记录',
            guidance: '下载匹配的活动记录，便于离线查阅或合规存档。',
            cta: '导出记录',
          },
          viewOnlyMode: {
            label: '仅查看 — 不可操作',
            guidance: '您可在此搜索并导出，但无法修改模板、用户或决策。',
            cta: '查看操作记录',
          },
        },
      },
    },
  }
