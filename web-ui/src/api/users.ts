import type { User } from 'firebase/auth';
import { apiRequest } from './client';
import type { UserDTO } from '../types/api.types';

export function signUpExternal(user: User): Promise<UserDTO> {
  return apiRequest<UserDTO>('/user/signup-external', user, { method: 'POST' });
}
