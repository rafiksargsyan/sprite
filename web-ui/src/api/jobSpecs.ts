import type { User } from 'firebase/auth';
import { apiRequest } from './client';
import type { JobSpecCreationRequest, JobSpecDTO } from '../types/api.types';

export function listJobSpecs(user: User, accountId: string): Promise<JobSpecDTO[]> {
  return apiRequest<JobSpecDTO[]>('/job-spec', user, { accountId });
}

export function createJobSpec(
  user: User,
  accountId: string,
  body: JobSpecCreationRequest,
): Promise<JobSpecDTO> {
  return apiRequest<JobSpecDTO>('/job-spec', user, {
    method: 'POST',
    accountId,
    body: JSON.stringify(body),
  });
}
