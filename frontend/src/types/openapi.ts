import type { components } from '@/types/generated/openapi-v1'

export type { components, operations, paths } from '@/types/generated/openapi-v1'

export type OpenApiSchemaName = keyof components['schemas']

/** Resolve a named schema from the generated OpenAPI v1 contract. */
export type Schema<T extends OpenApiSchemaName> = components['schemas'][T]
