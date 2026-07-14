/**
 * Deep link from a fidelity warning to the binding editor for the anchor artifact.
 */
export function buildFidelityBindingEditLink(params: {
  templateId: string
  devVersionId: string
  anchorId: string | null | undefined
}): string | null {
  const anchorId = params.anchorId?.trim()
  if (!anchorId) {
    return null
  }
  const query = new URLSearchParams({
    workspaceTab: 'design',
    designTab: 'bindings',
    anchorId,
  })
  return `/templates/${params.templateId}/dev/${params.devVersionId}?${query.toString()}`
}
