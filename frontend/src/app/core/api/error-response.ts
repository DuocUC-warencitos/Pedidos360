// ErrorResponse del backend roony-error-spring (MVC, no ProblemDetail)
export interface ErrorResponse {
  code: string;
  message: string;
  timestamp: string;
  path: string;
  traceId?: string | null;
  details?: string[] | null;
}

export function isErrorResponse(payload: unknown): payload is ErrorResponse {
  if (typeof payload !== 'object' || payload === null) return false;
  if (!('code' in payload) || !('message' in payload)) return false;
  const p = payload as { code: unknown; message: unknown };
  return typeof p.code === 'string' && typeof p.message === 'string';
}
