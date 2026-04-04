/** 从 axios / 业务错误中取出后端 message */
export function getApiErrorMessage(err: unknown, fallback: string): string {
  const e = err as {
    response?: { data?: { message?: string } };
    message?: string;
  };
  return e?.response?.data?.message || e?.message || fallback;
}
