import type { FullResult, Reporter, TestCase, TestResult } from '@playwright/test/reporter'

/**
 * FOS-W12-3 — a smoke run with zero executed functional tests cannot be green.
 */
class MinExecutedReporter implements Reporter {
  private readonly minExecuted: number
  private readonly seen = new Set<string>()
  private executed = 0
  private skipped = 0

  constructor(options?: { minExecuted?: number }) {
    this.minExecuted = options?.minExecuted ?? 1
  }

  onTestEnd(test: TestCase, result: TestResult): void {
    const key = test.id
    if (result.status === 'skipped') {
      if (!this.seen.has(key)) {
        this.seen.add(key)
        this.skipped += 1
      }
      return
    }
    if (this.seen.has(key)) {
      return
    }
    this.seen.add(key)
    if (result.status === 'passed' || result.status === 'failed' || result.status === 'timedOut') {
      this.executed += 1
    }
  }

  async onEnd(result: FullResult): Promise<void> {
    if (this.executed < this.minExecuted) {
      result.status = 'failed'
      throw new Error(
        `FOS-W12-3: executed functional tests=${this.executed} (min=${this.minExecuted}); `
          + `skipped=${this.skipped}. Vacuous skip-all cannot be green.`,
      )
    }
  }
}

export default MinExecutedReporter
