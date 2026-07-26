/**
 * FOS-W2-8 — optional leave interceptor for in-page workspace tab switches.
 * Bindings (or other dirty editors) register `requestLeave`; workspace tab shell calls it.
 */

export type WorkspaceTabLeaveHandler = (leave: () => void | Promise<void>) => void | Promise<void>

let leaveHandler: WorkspaceTabLeaveHandler | null = null

export function registerWorkspaceTabDirtyLeave(handler: WorkspaceTabLeaveHandler | null): void {
  leaveHandler = handler
}

export async function requestWorkspaceTabLeave(
  leave: () => void | Promise<void>,
): Promise<void> {
  if (!leaveHandler) {
    await leave()
    return
  }
  await leaveHandler(leave)
}
