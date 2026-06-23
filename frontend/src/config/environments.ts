export const ALLOWED_ENVIRONMENTS = ['dev', 'uat', 'prod'] as const

export type RuntimeEnvironment = (typeof ALLOWED_ENVIRONMENTS)[number]

export const DEFAULT_ENVIRONMENT: RuntimeEnvironment = 'dev'

export const ENVIRONMENT_LABEL_KEY: Record<RuntimeEnvironment, string> = {
  dev: 'common.environments.dev',
  uat: 'common.environments.uat',
  prod: 'common.environments.prod',
}

const ENVIRONMENTS = new Set<string>(ALLOWED_ENVIRONMENTS)

export function isRuntimeEnvironment(value: string): value is RuntimeEnvironment {
  return ENVIRONMENTS.has(value)
}

export function resolveRuntimeEnvironment(
  value: string | undefined | null,
): RuntimeEnvironment {
  if (value && isRuntimeEnvironment(value)) {
    return value
  }
  return DEFAULT_ENVIRONMENT
}
