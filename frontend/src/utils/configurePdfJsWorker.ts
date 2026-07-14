import * as pdfjs from 'pdfjs-dist'
import pdfWorkerUrl from 'pdfjs-dist/build/pdf.worker.min.mjs?url'

let configured = false

/** One-time pdf.js worker wiring for Vite (CE-U04 inline preview). */
export function configurePdfJsWorker(): void {
  if (configured) {
    return
  }
  pdfjs.GlobalWorkerOptions.workerSrc = pdfWorkerUrl
  configured = true
}

export { pdfjs }
