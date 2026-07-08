/// <reference types="vite/client" />

declare module 'adm-zip' {
  export default class AdmZip {
    constructor(buffer?: Buffer)
    getEntry(entryName: string): { getData(): Buffer } | null
  }
}

declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<object, object, unknown>
  export default component
}
