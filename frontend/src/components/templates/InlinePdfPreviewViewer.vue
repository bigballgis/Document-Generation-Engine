<script setup lang="ts">
import { nextTick, onBeforeUnmount, ref, shallowRef, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { configurePdfJsWorker, pdfjs } from '@/utils/configurePdfJsWorker'
import { blobToArrayBuffer } from '@/utils/blobToArrayBuffer'

const props = defineProps<{
  blob: Blob | null
  loading?: boolean
  errorMessage?: string | null
}>()

const { t } = useI18n()

const canvasRef = ref<HTMLCanvasElement | null>(null)
const pageNumber = ref(1)
const pageCount = ref(0)
const renderError = ref<string | null>(null)
const rendering = ref(false)
const hasRenderedPage = ref(false)

const pdfDocument = shallowRef<pdfjs.PDFDocumentProxy | null>(null)
let renderTask: pdfjs.RenderTask | null = null
let loadGeneration = 0

configurePdfJsWorker()

async function destroyDocument() {
  if (renderTask) {
    try {
      await renderTask.promise
    } catch {
      // cancelled render
    }
    renderTask.cancel()
    renderTask = null
  }
  if (pdfDocument.value) {
    await pdfDocument.value.destroy()
    pdfDocument.value = null
  }
  pageCount.value = 0
  pageNumber.value = 1
  hasRenderedPage.value = false
}

async function renderCurrentPage() {
  const canvas = canvasRef.value
  const doc = pdfDocument.value
  if (!canvas || !doc || pageNumber.value < 1) {
    return
  }

  if (renderTask) {
    renderTask.cancel()
    renderTask = null
  }

  rendering.value = true
  renderError.value = null

  try {
    const page = await doc.getPage(pageNumber.value)
    const viewport = page.getViewport({ scale: 1 })
    const context = canvas.getContext('2d')
    if (!context) {
      throw new Error('Canvas 2D context unavailable')
    }

    canvas.width = viewport.width
    canvas.height = viewport.height

    renderTask = page.render({
      canvasContext: context,
      viewport,
    })
    await renderTask.promise
    hasRenderedPage.value = true
  } catch (error) {
    if ((error as { name?: string }).name !== 'RenderingCancelledException') {
      renderError.value = t('templates.preview.inlinePdf.renderFailed')
    }
  } finally {
    rendering.value = false
    renderTask = null
  }
}

async function loadBlob(blob: Blob) {
  const generation = ++loadGeneration
  await destroyDocument()
  renderError.value = null

  try {
    const data = await blobToArrayBuffer(blob)
    if (generation !== loadGeneration) {
      return
    }
    const doc = await pdfjs.getDocument({ data }).promise
    if (generation !== loadGeneration) {
      await doc.destroy()
      return
    }
    pdfDocument.value = doc
    pageCount.value = doc.numPages
    pageNumber.value = 1
    await nextTick()
    await renderCurrentPage()
  } catch {
    if (generation !== loadGeneration) {
      return
    }
    renderError.value = t('templates.preview.inlinePdf.renderFailed')
  }
}

watch(
  () => props.blob,
  (blob) => {
    if (!blob) {
      void destroyDocument()
      renderError.value = null
      return
    }
    void loadBlob(blob)
  },
  { immediate: true },
)

watch(pageNumber, () => {
  void renderCurrentPage()
})

onBeforeUnmount(() => {
  loadGeneration++
  void destroyDocument()
})

function goToPreviousPage() {
  if (pageNumber.value > 1) {
    pageNumber.value -= 1
  }
}

function goToNextPage() {
  if (pageNumber.value < pageCount.value) {
    pageNumber.value += 1
  }
}
</script>

<template>
  <section
    class="inline-pdf-preview"
    data-testid="inline-pdf-preview-viewer"
    :aria-label="t('templates.preview.inlinePdf.ariaLabel')"
  >
    <div v-if="loading" class="inline-pdf-preview__status" data-testid="inline-pdf-preview-loading">
      <span>{{ t('templates.preview.inlinePdf.loading') }}</span>
    </div>

    <el-alert
      v-else-if="errorMessage || renderError"
      type="error"
      :closable="false"
      :title="errorMessage ?? renderError ?? ''"
      data-testid="inline-pdf-preview-error"
    />

    <template v-else-if="blob && pageCount > 0">
      <div class="inline-pdf-preview__toolbar">
        <el-button
          size="small"
          :disabled="pageNumber <= 1 || rendering"
          data-testid="inline-pdf-preview-prev"
          @click="goToPreviousPage"
        >
          {{ t('templates.preview.inlinePdf.previousPage') }}
        </el-button>
        <span class="inline-pdf-preview__page-label" data-testid="inline-pdf-preview-page-label">
          {{ t('templates.preview.inlinePdf.pageLabel', { current: pageNumber, total: pageCount }) }}
        </span>
        <el-button
          size="small"
          :disabled="pageNumber >= pageCount || rendering"
          data-testid="inline-pdf-preview-next"
          @click="goToNextPage"
        >
          {{ t('templates.preview.inlinePdf.nextPage') }}
        </el-button>
      </div>

      <div class="inline-pdf-preview__canvas-wrap" :class="{ 'inline-pdf-preview__canvas-wrap--rendering': rendering }">
        <canvas
          ref="canvasRef"
          class="inline-pdf-preview__canvas"
          data-testid="inline-pdf-preview-canvas"
        />
      </div>
    </template>
  </section>
</template>

<style scoped lang="scss">
.inline-pdf-preview {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);

  &__status {
    display: inline-flex;
    align-items: center;
    gap: var(--space-2);
    color: var(--text-muted);
    font-size: var(--font-size-sm);
  }

  &__toolbar {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: var(--space-2);
  }

  &__page-label {
    font-size: var(--font-size-sm);
    color: var(--text-secondary);
    min-width: 6rem;
    text-align: center;
  }

  &__canvas-wrap {
    overflow: auto;
    max-height: 28rem;
    border: 1px solid var(--border-color);
    border-radius: var(--radius-sm);
    background: var(--surface-card);
    padding: var(--space-2);

    &--rendering {
      opacity: 0.65;
    }
  }

  &__canvas {
    display: block;
    max-width: 100%;
    height: auto;
  }
}
</style>
