export const dashboardZhCn = {
    title: '我的任务',
    description: '进行中的信函模板待办，以及母版与模板目录快照。',
    loadError: '无法加载任务列表。',
    summary: {
      error: {
        load: '无法加载仪表盘汇总统计。',
      },
    },
    tabs: {
      overview: '概况',
      workflow: '工作流程',
    },
    workflowTab: {
      description: '按当前角色展示管理工作流程与各步骤指引。',
    },
    stats: {
      sectionTitle: '目录与工作流快照',
      sectionDescription:
        '目录数量统计已登记的母版与模板；工作流数量反映进行中的审核或审批步骤。',
      pendingActions: {
        title: '分配给我的待办',
        description: '等待您完成测试、审批、确认上线或审核的待办事项。',
        action: '查看任务列表',
      },
      masterPendingReview: {
        title: '待审核母版',
        description: '修订线已上传、等待批准或驳回的母版。',
        action: '打开母版',
      },
      masterVersionsInProgress: {
        title: '进行中的母版',
        description: '草稿状态、仍需补充文件或元数据后重新提交的母版。',
        action: '打开母版',
      },
      templateVersionsInWorkflow: {
        title: '工作流中的模板',
        description: '处于草稿、测试、审批或待发布阶段的模板。',
        action: '打开模板',
      },
      publishedVersions: {
        title: '已发布模板',
        description: '已有可调用发布版本的模板包。',
        action: '打开模板',
      },
      stoppedVersions: {
        title: '已停用模板',
        description: '已发布但被暂停运行时调用的模板。',
        action: '打开模板',
      },
      catalogMasters: {
        title: '目录中的母版',
        description: '授权范围内已登记的母版包。',
        action: '浏览母版',
      },
      catalogTemplates: {
        title: '目录中的模板',
        description: '已登记的模板包（各自管理发布版本线）。',
        action: '浏览模板',
      },
      externalServicesAlerts: {
        title: '对外服务需关注',
        description: '跨模板包的 API 接入告警，可能需要您处理。',
        action: '打开对外服务总览',
      },
    },
    tasks: {
      title: '我的待办',
      description: '请从对应的母版或模板详情页打开并完成下一步。',
      empty: '当前没有待办事项。',
      columns: {
        action: '操作',
        item: '对象',
        group: '分组',
        hint: '说明',
      },
      actions: {
        open: '打开',
      },
      masterReview: {
        title: '审核母版',
        description: '在模板引用前批准或驳回已上传的母版。',
      },
      templateTest: {
        title: '记录测试结果',
        description: '执行测试生成并对测试中的模板做出通过或不通过决定。',
      },
      templateApproval: {
        title: '记录审批结果',
        description: '测试完成后批准或驳回模板。',
      },
      templateLegalApproval: {
        title: '记录法务审阅结果',
        description: '在进入合规审批前完成法务阶段判定。',
      },
      templatePublish: {
        title: '确认上线',
        description: '将已审批模板发布到指定环境。',
      },
      templateDraft: {
        title: '继续模板设计',
        description: '完善绑定与规则后提交草稿进入测试。',
      },
      masterRework: {
        title: '待修改母版',
        itemTitle: '修改母版并重新提交',
        description: '更新文件或版式占位符后，重新提交审核。',
      },
      templateRework: {
        title: '修改模板并重新提交',
        description: '根据测试或审批反馈修改后，重新提交测试。',
      },
      clauseOutdatedBump: {
        title: '引用条款有新版',
        itemTitle: '升级引用的条款版本',
        description: '模板中存在固定于旧版已批准条款的引用，请在条款面板一键升 pin。',
      },
      annualReviewDue: {
        title: '年检到期',
        itemTitle: '完成年检',
        description: '该模板的下一复查日期已到期。打开模板概览完成年检。',
      },
      contentModuleReview: {
        title: '待审标准条款',
        itemTitle: '审核标准条款',
        description: '批准或驳回已提交的标准条款版本。',
      },
      contentModuleRework: {
        title: '待返工标准条款',
        itemTitle: '修改标准条款并重新提交',
        description: '根据驳回原因修改草稿后再次提交审核。',
      },
    },
    journey: {
      openWorkspace: '打开工作区',
    },
    quickLinks: {
      title: '文档目录',
      templates: '模板',
      masters: '母版文档',
      apiPolicies: '对外服务概览',
    },
  }
