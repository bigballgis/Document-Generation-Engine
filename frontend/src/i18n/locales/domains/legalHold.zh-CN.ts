export const legalHoldZhCn = {
    list: {
      title: '法律冻结',
      description: '创建并释放法律冻结，使匹配的调用记录与审计记录免于留存清理删除。',
      help: '仅全局管理员可管理法律冻结。处于活动状态的冻结在释放前会阻止清理。',
      empty: '暂无法律冻结',
      emptyDescription: '创建法律冻结以保护调用或审计记录免于留存清理。',
      emptyDescriptionReadOnly:
        '当前范围内暂无法律冻结。管理员创建后将显示在此。',
      columns: {
        holdId: '冻结 ID',
        scope: '范围类型',
        summary: '保护范围',
        status: '状态',
        reason: '原因',
        createdBy: '创建人',
        createdAt: '创建时间',
      },
    },
    filters: {
      status: '状态',
      statusAll: '全部状态',
    },
    status: {
      ACTIVE: '活动',
      RELEASED: '已释放',
    },
    scope: {
      TEMPLATE_WINDOW: '模板时间窗',
      INVOCATION_SET: '调用集合',
      openEnded: '开放结束',
      templateWindowSummary: '模板 {template} · {from} → {to}',
      invocationSetSummary: '{count} 个调用 ID',
    },
    create: {
      open: '创建法律冻结',
      title: '创建法律冻结',
      submit: '创建冻结',
      fields: {
        scopeType: '范围类型',
        reason: '原因（可选）',
        template: '模板',
        effectiveFrom: '生效起（UTC）',
        effectiveTo: '生效止（UTC，可选）',
        invocationIds: '调用外部 ID',
      },
      placeholders: {
        reason: '简短原因（用于审计，勿填敏感内容）',
        template: '按名称或外部 ID 搜索',
        effectiveFrom: '选择开始时间',
        effectiveTo: '留空表示开放结束',
        invocationIds: '每行一个 ID，或逗号分隔（最多 500）',
      },
      validation: {
        scopeTypeRequired: '请选择范围类型。',
        templateRequired: '请选择模板。',
        effectiveFromRequired: '请选择生效开始时间。',
        invocationIdsRequired: '请至少输入一个调用外部 ID。',
      },
    },
    release: {
      action: '释放',
      confirmTitle: '释放法律冻结',
      confirmMessage: '释放冻结 {holdExternalId}？受保护记录可能重新符合留存清理条件。',
      confirmButton: '释放冻结',
    },
    error: {
      loadList: '无法加载法律冻结列表。',
      create: '无法创建法律冻结。',
      release: '无法释放法律冻结。',
    },
  }
