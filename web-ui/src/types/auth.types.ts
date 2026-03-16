import type { User } from 'firebase/auth';

export interface AuthContextValue {
  user: User | null;
  accountId: string | null;
  loading: boolean;
  pendingEmailConfirmation: boolean;
  signInWithGoogle: () => Promise<void>;
  signInWithGithub: () => Promise<void>;
  sendMagicLink: (email: string) => Promise<void>;
  confirmEmailForLink: (email: string) => Promise<void>;
  signOut: () => Promise<void>;
}
