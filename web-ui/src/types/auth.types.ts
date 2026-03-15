import type { User } from 'firebase/auth';

export interface AuthContextType {
  user: User | null;
  loading: boolean;
}

export interface EmailLinkState {
  emailSent: boolean;
  email: string;
}

export interface LoginPageProps {
  onLoginSuccess?: (user: User) => void;
}