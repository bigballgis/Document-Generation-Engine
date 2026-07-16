/** Split newline- or comma-separated invocation external IDs. */
export function parseLegalHoldInvocationIds(text: string): string[] {
  return text
    .split(/[\n,]+/)
    .map((id) => id.trim())
    .filter((id) => id.length > 0)
}
