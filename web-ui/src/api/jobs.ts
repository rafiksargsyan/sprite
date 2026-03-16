import type { User } from 'firebase/auth';
import { apiRequest } from './client';
import type {
  ThumbnailsGenerationJobCreationRequest,
  ThumbnailsGenerationJobDTO,
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
