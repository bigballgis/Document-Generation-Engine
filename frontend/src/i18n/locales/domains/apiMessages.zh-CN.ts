export const apiMessagesZhCn = {
  publishGate: {
      anchorIntegrity: {
        ready: '版式占位符绑定有效。',
        blocked: '版式占位符绑定校验存在阻塞项。',
      },
      variableSchema: {
        ready: '变量架构已配置。',
        missing: '变量架构未配置。',
      },
      ruleBounds: {
        ready: '组合规则在允许范围内。',
        blocked: '组合规则校验存在阻塞项。',
      },
      testResults: {
        ready: '批量测试结果可用。',
        missing: '尚未记录批量测试运行。',
      },
      previewPresent: {
        ready: '存在成功的预览产物。',
        missing: '不存在成功的预览产物。',
      },
      changeDiff: {
        ready: '变更差异摘要可用。',
      },
      approvalSummary: {
        ready: '审批决策已记录。',
        missing: '缺少审批决策。',
      },
      coverageThresholds: {
        ready: '覆盖率满足配置阈值。',
        blocked: '覆盖率低于配置阈值。',
      },
      apiPolicy: {
        ready: 'API 接入已配置。',
        blocked: 'API 访问策略不可调用（缺少默认路由与 AD 组）。',
      },
      blockerStatus: {
        ready: '未检测到未解决的阻塞项。',
        blocked: '仍存在未解决的阻塞项。',
      },
      contentModuleReferences: {
        ready: '内容模块引用有效。',
        blocked: '内容模块引用缺失或无效。',
      },
      unsupportedStructuredNodes: {
        ready: '结构化内容节点均可由 DOCX writer 渲染。',
        blocked: '结构化内容包含不支持或缺少 writer 的节点类型，会导致静默丢内容。',
      },
      pasteCleaningBlockers: {
        ready: '绑定上无未解除的粘贴清洗阻断。',
        blocked: '一个或多个绑定存在未解除的粘贴清洗阻断，发布前必须清除。',
      },
      contentModuleEffectiveExpired: {
        ready: '引用的条款模块均在有效期内。',
        blocked: '一个或多个引用的条款模块已过有效期。请更新或重新固定模块版本。',
      },
      contentModuleEffectiveNotStarted: {
        ready: '引用的条款模块均已到达生效日期。',
        blocked: '一个或多个引用的条款模块尚未到达生效日期。',
      },
      contentModuleLocaleMismatch: {
        ready: '条款模块语言区域与模板一致。',
        blocked: '引用的条款模块语言区域与本模板不一致。请对齐语言或更换模块版本。',
      },
      contentModuleNestingCycle: {
        ready: '条款模块嵌套无环。',
        blocked: '条款模块嵌套存在环路。请在上线前打断循环引用。',
      },
      contentModuleNestingDepthExceeded: {
        ready: '条款模块嵌套深度在限制内。',
        blocked: '条款模块嵌套过深。请扁平化嵌套引用。',
      },
      contentModuleNestingUnpinned: {
        ready: '嵌套条款引用均已固定到已批准版本。',
        blocked: '存在未固定版本的嵌套条款引用。请为每个嵌套条款固定已批准版本。',
      },
      compositionInclusionReferenceInvalid: {
        ready: '组合纳入引用有效。',
        blocked: '组合纳入规则引用了无效的条款键。请修正纳入规则。',
      },
      paginationDeltaBudget: {
        ready: 'Word–PDF 页数差异在允许预算内。',
        blocked: 'Word 与 PDF 页数差异超出允许预算。请调整版式或核对预览页。',
      },
      fidelityWarningsViewed: {
        ready: '保真警告已审阅。',
        blocked: '上线前请在预览中审阅全部保真警告。',
      },

    },
  apimgmt: {
      policyImpact: {
        blocking: '这些接入设置存在阻塞性影响。',
        warning: '这些接入设置存在非阻塞警告。',
        safe: '这些接入设置可安全应用。',
        defaultRouteChanged: '默认路由目标将变更。',
        defaultRouteNotCallable: '候选默认路由不可调用。',
        idempotencyDefaultRouteGuard: '默认路由变更后，现有幂等键可能冲突。',
      },
    },
}
