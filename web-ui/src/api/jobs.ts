import type { User } from 'firebase/auth';
import { apiRequest, apiRequestText } from './client';
import type {
  ThumbnailsGenerationJobCreationRequest,
  ThumbnailsGenerationJobDTO,
  JobLimitsDTO,
  PageResponse,
  PreviewFilesResponse,
} from '../types/api.types';

export function listJobs(
  user: User,
  accountId: string,
  page: number,
  size: number,
): Promise<PageResponse<ThumbnailsGenerationJobDTO>> {
  return apiRequest<PageResponse<ThumbnailsGenerationJobDTO>>(
    `/thumbnails-generation-job?page=${page}&size=${size}`,
    user,
    { accountId },
  );
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

export function getJobPreviewFiles(
  user: User,
  accountId: string,
  jobId: string,
  configFolderName: string,
): Promise<PreviewFilesResponse> {
  return apiRequest<PreviewFilesResponse>(
    `/thumbnails-generation-job/${jobId}/preview/${configFolderName}`,
    user,
    { accountId },
  );
}

export function getJobPreviewVtt(
  user: User,
  accountId: string,
  jobId: string,
  configFolderName: string,
): Promise<string> {
  return apiRequestText(
    `/thumbnails-generation-job/${jobId}/preview/${configFolderName}/vtt`,
    user,
    { accountId },
  );
}

export function cancelJob(
  user: User,
  accountId: string,
  jobId: string,
): Promise<void> {
  return apiRequest<void>(`/thumbnails-generation-job/${jobId}`, user, {
    method: 'DELETE',
    accountId,
  });
}

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';

export async function getJobLimits(): Promise<JobLimitsDTO> {
  const response = await fetch(`${BASE_URL}/thumbnails-generation-job/limits`);
  return response.json();
}
