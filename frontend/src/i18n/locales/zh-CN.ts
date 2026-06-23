export default {
  app: {
    title: '文档生成平台',
  },
  nav: {
    login: '登录',
    logout: '退出登录',
    managementNavigation: '管理导航',
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
  templates: {
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
