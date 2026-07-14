/** Read blob bytes in browser and jsdom test environments. */
export async function blobToArrayBuffer(blob: Blob): Promise<ArrayBuffer> {
  if (typeof blob.arrayBuffer === 'function') {
    return blob.arrayBuffer()
  }
  return new Response(blob).arrayBuffer()
}
