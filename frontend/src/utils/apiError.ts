/** 从 axios / 业务错误中取出后端 message */
export function getApiErrorMessage(err: unknown, fallback: string): string {
  const e = err as {
    code?: string;
    response?: { data?: { message?: string } };
    message?: string;
  };
  const raw = e?.response?.data?.message || e?.message || fallback;
  const msg = typeof raw === "string" ? raw : String(raw);
  if (
    e?.code === "ECONNABORTED" ||
    /timeout of \d+ms exceeded/i.test(msg) ||
    msg.includes("Network Error")
  ) {
    return "请求超时或网络异常。若刚重启后端，请稍等再刷新；并确认本机 MySQL 与 application-dev 中的 Redis 可访问。";
  }
  return msg;
}
