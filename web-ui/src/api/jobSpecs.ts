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

export function deleteJobSpec(user: User, accountId: string, jobSpecId: string): Promise<void> {
  return apiRequest<void>(`/job-spec/${jobSpecId}`, user, { method: 'DELETE', accountId });
}
