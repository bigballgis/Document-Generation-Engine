import { unwrapEnvelope } from '@/api/envelope'
import { http } from '@/api/http'
import type { ApiEnvelope } from '@/types/session'
import type { CallerContract } from '@/types/contract'

export async function getCallerContract(
  templateId: string,
  environment = 'dev',
): Promise<CallerContract> {
  const response = await http.get<ApiEnvelope<CallerContract>>(
    `/templates/${templateId}/api/contract`,
    { params: { environment } },
  )
  return unwrapEnvelope(response.data)
}
