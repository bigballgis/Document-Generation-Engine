let activeDragPathKey: string | null = null

export function setStructuredBlockDragPathKey(pathKey: string | null): void {
  activeDragPathKey = pathKey
}

export function getStructuredBlockDragPathKey(): string | null {
  return activeDragPathKey
}
