/**
 * Vite dev-server proxy bypass for Management UI SPA routes under `/api/*`
 * that must return `index.html` on hard refresh (parallel to nginx SPA exceptions
 * in `nginx.conf` / Helm `frontend-nginx-configmap.yaml`).
 *
 * Real backend JSON APIs live under `/api/management/v1/**` and must keep proxying.
 */
export function bypassSpaApiRoutes(url: string | undefined): string | undefined {
  if (!url) {
    return undefined
  }
  const path = url.split('?')[0] ?? ''
  if (path === '/api/policies' || path.startsWith('/api/policies/')) {
    return '/index.html'
  }
  if (path === '/api/packages' || path.startsWith('/api/packages/')) {
    return '/index.html'
  }
  if (path === '/api/invocations' || path.startsWith('/api/invocations/')) {
    return '/index.html'
  }
  return undefined
}
