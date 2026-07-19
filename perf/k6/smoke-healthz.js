/**
 * IBL-D3 / F22 — safe smoke load against acceptance stack.
 *
 * Hits GET /healthz only (no auth, no mutations, no generation traffic).
 * Numbers from this script are measured-input for NFR §待确认 (LR-D5) —
 * never confirmed SLOs.
 *
 * Env:
 *   BASE_URL  default http://localhost:8080
 *
 * Example:
 *   k6 run -e BASE_URL=http://localhost:8080 perf/k6/smoke-healthz.js
 */
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  // Tiny smoke — not a capacity soak. Do not treat thresholds as SLOs.
  vus: 2,
  duration: '15s',
  thresholds: {
    // Soft gates for smoke usability only — NOT proposed/confirmed NFR SLOs.
    http_req_failed: ['rate<0.05'],
    http_req_duration: ['p(95)<2000'],
  },
};

const baseUrl = (__ENV.BASE_URL || 'http://localhost:8080').replace(/\/$/, '');

export default function () {
  const res = http.get(`${baseUrl}/healthz`, {
    tags: { name: 'healthz', suite: 'ibl-d3-smoke' },
  });

  check(res, {
    'healthz status is 200': (r) => r.status === 200,
    'healthz body mentions UP': (r) =>
      typeof r.body === 'string' && r.body.includes('UP'),
  });

  sleep(0.5);
}
