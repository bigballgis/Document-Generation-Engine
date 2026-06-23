export default {

  app: {

    title: '文档生成系统',

  },

  nav: {

    login: '登录',

    logout: '退出登录',

    managementNavigation: '管理导航',

    groups: {

      overview: '概览',

      entitlement: '访问与身份',

      versionCatalog: '版本目录',

      content: '文档内容',

      apiAccess: 'API 访问',

      security: '安全与审计',

    },

    items: {

      dashboard: '我的任务',

      users: '用户管理',

      groups: '组管理',

      masters: '主文档',

      templates: '模板',

      masterVersions: '母版版本',

      templateVersions: '模板版本',

      apiPolicies: 'API 策略',

      audit: '审计日志',

    },

    routes: {

      globalGovernance: '全局治理',

      groupGovernance: '分组治理',

      templateAuthoring: '模板编排',

      apiPolicy: 'API 策略',

      audit: '审计控制台',

      masters: '主文档',

      templates: '模板',

      testerWorkbench: '测试工作台',

      approverWorkbench: '审批工作台',

      identityAdministration: '身份与分组',

    },

  },

  table: {

    filterPlaceholder: '筛选…',

    filterAll: '全部',

    clearFilters: '清除列筛选',

  },

  common: {

    yes: '是',

    no: '否',

    confirm: '确定',

    cancel: '取消',

    save: '保存',

    edit: '编辑',

    delete: '删除',

    actions: '操作',

    retry: '重试',

    loadError: '页面加载失败。',

    language: '语言',

    locales: {

      en: 'English',

      zhCN: '简体中文',

    },

    environments: {

      dev: '开发',

      uat: '测试',

      prod: '生产',

    },

  },

  dashboard: {

    title: '我的任务',

    description: '版本变更相关的工作流待办，以及版本目录概览。',

    loadError: '无法加载任务列表。',

    stats: {

      sectionTitle: '版本与工作流概览',

      sectionDescription: '目录数量为已登记母版/模板；工作流数量为进行中的版本变更。',

      pendingActions: {

        title: '待我处理',

        description: '等待您测试、审批、发布或审核的事项。',

        action: '查看任务列表',

      },

      masterPendingReview: {

        title: '待审核母版版本',

        description: '已上传、等待批准或驳回的母版。',

        action: '打开母版版本',

      },

      masterVersionsInProgress: {

        title: '进行中的母版版本',

        description: '草稿或被驳回、仍需修改或重新提交的母版。',

        action: '打开母版版本',

      },

      templateVersionsInWorkflow: {

        title: '工作流中的模板版本',

        description: '处于草稿、测试、审批或待发布阶段的模板。',

        action: '打开模板版本',

      },

      publishedVersions: {

        title: '已发布版本',

        description: '已有可调用发布版本的模板。',

        action: '打开模板版本',

      },

      stoppedVersions: {

        title: '已停用的发布版本',

        description: '已发布但被暂停运行时调用的模板。',

        action: '打开模板版本',

      },

      catalogMasters: {

        title: '母版目录条目',

        description: '授权范围内已登记的母版文档。',

        action: '浏览母版目录',

      },

      catalogTemplates: {

        title: '模板目录条目',

        description: '已登记的模板定义（各自管理发布版本）。',

        action: '浏览模板目录',

      },

    },

    tasks: {

      title: '待办事项',

      description: '在对应母版或模板详情页完成以下版本工作流步骤。',

      empty: '当前没有待办事项。',

    },

    quickLinks: {

      title: '版本目录',

      templates: '模板版本目录',

      masters: '母版版本目录',

      apiPolicies: 'API 策略',

    },

  },

  versionCatalog: {

    master: {

      noticeTitle: '这是版本目录，不是待办队列',

      noticeDescription:

        '每一行代表一条母版版本线。上传新 DOCX 或修改元数据会启动审核工作流；审核通过后才可用于模板编排。',

    },

    template: {

      noticeTitle: '这是版本目录，不是待办队列',

      noticeDescription:

        '每一行代表一个模板定义及其发布版本。新建或修改模板会触发测试→审批→发布流程，待办显示在「我的任务」中。',

    },

  },

  identity: {

    usersPageTitle: '用户管理',

    usersPageDescription: '在授权范围内创建和维护管理用户及角色分配。',

    groupsPageTitle: '组管理',

    groupsPageDescription: '在授权范围内创建业务组并管理成员。',

  },

  templates: {

    detail: {
      tabs: {
        overview: '概览与工作流',
        authoring: '版本编排',
        releaseVersions: '发布版本历史',
        apiAccess: 'API 访问',
      },
    },

    versions: {
      workflowHintTitle: '版本工作流进行中',
      workflowHintDescription:
        '完成「概览与工作流」中的测试、审批和发布后，新版本会出现在此列表。',
      loadError: '无法加载发布版本历史。',
      devVersionNumber: '开发版本号',
      defaultRoute: '默认路由',
      defaultRouteYes: '默认',
      defaultRouteNo: '—',
      updatedAt: '更新时间',
    },

    contract: {

      environment: '环境',

    },

    policy: {

      policyVersion: '策略版本',

      outputFormats: '输出格式',

      outputModes: '输出模式',

      pdfEncryptionEnabled: 'PDF 加密',

      impact: {

        title: '策略影响预览',

        confirmPrompt: '请确认策略影响后再保存。',

      },

    },

    deleteAction: {

      button: '删除模板',

      title: '删除模板',

      reasonPrompt: '请填写删除原因（审计必填）。',

      reasonRequired: '删除原因不能为空。',

      confirmTitle: '确认删除模板',

      confirmMessage: '删除后模板将不再出现在编排与运行时列表中，是否继续？',

      success: '模板已删除。',

    },

  },

} as const

