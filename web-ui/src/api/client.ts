import type { User } from 'firebase/auth';
import { env } from '../lib/env';

const BASE_URL = env.VITE_API_BASE_URL;

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

  if (response.status === 204) return undefined as T;
  return response.json() as Promise<T>;
}

export async function apiRequestText(
  path: string,
  user: User,
  options: RequestInit & { accountId?: string } = {},
): Promise<string> {
  const token = await user.getIdToken();
  const { accountId, headers: extraHeaders, ...fetchOptions } = options;

  const headers: Record<string, string> = {
    Authorization: `Bearer ${token}`,
    ...(accountId ? { 'X-ACCOUNT-ID': accountId } : {}),
    ...(extraHeaders as Record<string, string> | undefined),
  };

  const response = await fetch(`${BASE_URL}${path}`, { ...fetchOptions, headers });

  if (!response.ok) {
    const body = await response.json().catch(() => null);
    throw new Error(body?.message ?? `Request failed: ${response.status}`);
  }

  return response.text();
}
