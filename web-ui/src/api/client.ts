import type { User } from 'firebase/auth';

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';

export async function apiRequest<T>(
  path: string,
  user: User,
  options: RequestInit & { accountId?: string } = {},
): Promise<T> {
  const token = await user.getIdToken();
  const { accountId, headers: extraHeaders, ...fetchOptions } = options;

  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    Authorization: `Bearer ${token}`,
    ...(accountId ? { 'X-ACCOUNT-ID': accountId } : {}),
    ...(extraHeaders as Record<string, string> | undefined),
  };

  const response = await fetch(`${BASE_URL}${path}`, { ...fetchOptions, headers });

  if (!response.ok) {
    const body = await response.json().catch(() => null);
    throw new Error(body?.message ?? `Request failed: ${response.status}`);
  }

  return response.json() as Promise<T>;
}
