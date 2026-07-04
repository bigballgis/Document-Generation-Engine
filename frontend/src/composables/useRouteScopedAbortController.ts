import { computed, onBeforeUnmount, ref } from 'vue'

/**
 * Abort in-flight HTTP requests when the owning component unmounts (SOR-F06).
 */
export function useRouteScopedAbortController() {
  const controller = ref(new AbortController())

  function renewController() {
    controller.value.abort()
    controller.value = new AbortController()
  }

  onBeforeUnmount(() => {
    controller.value.abort()
  })

  return {
    signal: computed(() => controller.value.signal),
    renewController,
    abort: () => controller.value.abort(),
  }
}
