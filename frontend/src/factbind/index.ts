import contractRaw from '../../../contracts/api.yaml?raw'
import { parse as parseYaml } from 'yaml'
import { buildQuery, request as httpRequest } from '../api/http'

/**
 * FactBind 的前端半边：读同一份契约（仓库根的 contracts/api.yaml），按契约拼 URL、发请求。
 *
 * 契约是 YAML 文本，用 `?raw` 原样读进来再解析——这样不需要任何 Vite 插件或构建期改造。
 *
 * 调用方只说"这条 operation 叫什么、有哪些参数值"；HTTP 方法、路径、以及每个参数该放 path 还是
 * query，全部由契约决定。契约是**运行期加载的数据**，没有代码生成。
 */

interface ContractParameter {
  name: string
  in: string
  required?: boolean
  schema?: { default?: unknown }
}

interface ContractOperation {
  operationId?: string
  parameters?: ContractParameter[]
}

export interface ResolvedParameter {
  name: string
  in: string
  required: boolean
  defaultValue?: string
}

export interface ResolvedOperation {
  symbol: string
  method: string
  path: string
  parameters: ResolvedParameter[]
}

const HTTP_METHODS = ['get', 'post', 'put', 'patch', 'delete'] as const

/** 启动时把契约读成"符号 → 方法 / 路径 / 参数"的表。 */
const registry = new Map<string, ResolvedOperation>()
const document = parseYaml(contractRaw) as { paths?: Record<string, Record<string, ContractOperation>> }

if (!document?.paths) {
  throw new Error('FactBind contract error: missing "paths"')
}

for (const [path, pathItem] of Object.entries(document.paths)) {
  for (const method of HTTP_METHODS) {
    const raw = pathItem[method]
    if (!raw) {
      continue
    }
    const symbol = raw.operationId
    if (!symbol) {
      throw new Error(`FactBind contract error: ${method.toUpperCase()} ${path} has no operationId`)
    }
    if (registry.has(symbol)) {
      throw new Error(`FactBind contract error: duplicate operationId "${symbol}"`)
    }
    registry.set(symbol, {
      symbol,
      method: method.toUpperCase(),
      path,
      parameters: (raw.parameters ?? []).map((parameter) => ({
        name: parameter.name,
        in: parameter.in,
        required: parameter.required === true,
        defaultValue:
          parameter.schema?.default === undefined ? undefined : String(parameter.schema.default),
      })),
    })
  }
}

/** 拿到一条事实本身。 */
export function operation(symbol: string): ResolvedOperation {
  const op = registry.get(symbol)
  if (!op) {
    throw new Error(`FactBind: no operation with symbol "${symbol}" in the contract`)
  }
  return op
}

/** 按契约拼出路径（只填 path 参数）。 */
export function path(symbol: string, params: Record<string, string | number> = {}): string {
  const op = operation(symbol)
  let result = op.path
  for (const parameter of op.parameters) {
    if (parameter.in !== 'path') {
      continue
    }
    const value = params[parameter.name]
    if (value === undefined) {
      throw new Error(`FactBind: operation "${symbol}" requires parameter "${parameter.name}"`)
    }
    result = result.replace(`{${parameter.name}}`, String(value))
  }
  return result
}

export interface FactBindCall {
  /** 应用侧只关心"有哪些参数"，它在 HTTP 上的名字与位置由契约决定。 */
  params?: Record<string, string | number | boolean | undefined | null>
  body?: unknown
  signal?: AbortSignal
}

/** 按契约发请求（方法、路径、参数位置全部来自契约）。 */
export function request<T>(symbol: string, call: FactBindCall = {}): Promise<T> {
  const op = operation(symbol)
  const values = call.params ?? {}
  const pathParams: Record<string, string | number> = {}
  const query: Record<string, string | number | boolean | undefined> = {}

  for (const parameter of op.parameters) {
    const value = values[parameter.name] ?? parameter.defaultValue
    if (value === undefined || value === null) {
      if (parameter.required) {
        throw new Error(`FactBind: operation "${symbol}" requires parameter "${parameter.name}"`)
      }
      continue
    }
    if (parameter.in === 'path') {
      pathParams[parameter.name] = value as string | number
    } else if (parameter.in === 'query') {
      query[parameter.name] = value
    }
  }

  return httpRequest<T>(`${path(symbol, pathParams)}${buildQuery(query)}`, {
    method: op.method as 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE',
    body: call.body,
    signal: call.signal,
  })
}
