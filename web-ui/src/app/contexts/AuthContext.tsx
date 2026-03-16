import { createContext, useEffect, useState, type ReactNode } from 'react';
import {
  GoogleAuthProvider,
  GithubAuthProvider,
  signInWithPopup,
  sendSignInLinkToEmail,
  signInWithEmailLink,
  isSignInWithEmailLink,
  signOut as firebaseSignOut,
  onAuthStateChanged,
  type User,
} from 'firebase/auth';
import { auth } from '../../lib/firebase';
import {
  saveEmailForSignIn,
  getEmailForSignIn,
  clearEmailForSignIn,
} from '../../utils/emailStorage';
import { signUpExternal } from '../../api/users';
import type { AuthContextValue } from '../../types/auth.types';

export const AuthContext = createContext<AuthContextValue | null>(null);

const ACTION_CODE_SETTINGS = {
  url: window.location.origin,
  handleCodeInApp: true,
};

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [accountId, setAccountId] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [pendingEmailConfirmation, setPendingEmailConfirmation] = useState(false);
  const [pendingSignInLink, setPendingSignInLink] = useState<string | null>(null);

  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, async (firebaseUser) => {
      setUser(firebaseUser);
      if (firebaseUser) {
        try {
          const dto = await signUpExternal(firebaseUser);
          setAccountId(dto.accountId);
        } catch {
          setAccountId(null);
        }
      } else {
        setAccountId(null);
      }
      setLoading(false);
    });

    if (isSignInWithEmailLink(auth, window.location.href)) {
      const signInLink = window.location.href;
      const email = getEmailForSignIn();
      if (email) {
        signInWithEmailLink(auth, email, signInLink)
          .then(() => {
            clearEmailForSignIn();
            window.history.replaceState({}, '', window.location.pathname);
          })
          .catch(console.error);
      } else {
        setPendingSignInLink(signInLink);
        setPendingEmailConfirmation(true);
      }
    }

    return unsubscribe;
  }, []);

  const signInWithGoogle = async () => {
    const provider = new GoogleAuthProvider();
    await signInWithPopup(auth, provider);
  };

  const signInWithGithub = async () => {
    const provider = new GithubAuthProvider();
    await signInWithPopup(auth, provider);
  };

  const sendMagicLink = async (email: string) => {
    await sendSignInLinkToEmail(auth, email, ACTION_CODE_SETTINGS);
    saveEmailForSignIn(email);
  };

  const confirmEmailForLink = async (email: string) => {
    if (!pendingSignInLink) throw new Error('No sign-in link available');
    await signInWithEmailLink(auth, email, pendingSignInLink);
    clearEmailForSignIn();
    setPendingEmailConfirmation(false);
    setPendingSignInLink(null);
    window.history.replaceState({}, '', window.location.pathname);
  };

  const signOut = async () => {
    await firebaseSignOut(auth);
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        accountId,
        loading,
        pendingEmailConfirmation,
        signInWithGoogle,
        signInWithGithub,
        sendMagicLink,
        confirmEmailForLink,
        signOut,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}
