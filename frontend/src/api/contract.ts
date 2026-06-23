import { http } from '@/api/http'
import type { ApiEnvelope } from '@/types/session'
import type { CallerContract } from '@/types/contract'

function unwrap<T>(envelope: ApiEnvelope<T>): T {
  if (!envelope.result) {
    throw new Error('API response missing result')
  }
  return envelope.result
}

export async function getCallerContract(
  templateId: string,
  environment = 'dev',
): Promise<CallerContract> {
  const response = await http.get<ApiEnvelope<CallerContract>>(
    `/templates/${templateId}/api/contract`,
    { params: { environment } },
  )
  return unwrap(response.data)
}
