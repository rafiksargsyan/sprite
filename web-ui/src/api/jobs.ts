import type { User } from 'firebase/auth';
import { apiRequest } from './client';
import type {
  ThumbnailsGenerationJobCreationRequest,
  ThumbnailsGenerationJobDTO,
  JobLimitsDTO,
} from '../types/api.types';

export function listJobs(
  user: User,
  accountId: string,
): Promise<ThumbnailsGenerationJobDTO[]> {
  return apiRequest<ThumbnailsGenerationJobDTO[]>('/thumbnails-generation-job', user, { accountId });
}

export function createJob(
  user: User,
  accountId: string,
  body: ThumbnailsGenerationJobCreationRequest,
): Promise<ThumbnailsGenerationJobDTO> {
  return apiRequest<ThumbnailsGenerationJobDTO>('/thumbnails-generation-job', user, {
    method: 'POST',
    accountId,
    body: JSON.stringify(body),
  });
}

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';

export async function getJobLimits(): Promise<JobLimitsDTO> {
  const response = await fetch(`${BASE_URL}/thumbnails-generation-job/limits`);
  return response.json();
}
