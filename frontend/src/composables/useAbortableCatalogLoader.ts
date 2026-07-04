import { useRouteScopedAbortController } from '@/composables/useRouteScopedAbortController'

/** Combines route-scoped abort with catalog list reload (SOR-F06). */
export function useAbortableCatalogLoader(load: (signal: AbortSignal) => Promise<void>) {
  const { signal, renewController } = useRouteScopedAbortController()

  async function reload() {
    renewController()
    await load(signal.value).catch(() => undefined)
  }

  return { reload, signal, renewController }
}
