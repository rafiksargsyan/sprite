import { createContext, useContext, useEffect, useState } from "react";
import type { ReactNode } from 'react';
import { auth } from "../../lib/firebase";
import { onAuthStateChanged } from "firebase/auth";
import type { User } from "firebase/auth";

interface AuthContextType {
  user: User | null;
  token: string | null;
}

const AuthContext = createContext<AuthContextType>({
  user: null,
  token: null
});

export function AuthProvider({ children }: { children: ReactNode }) {

  const [user, setUser] = useState<User | null>(null);
  const [token, setToken] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {

    const unsubscribe = onAuthStateChanged(auth, async (firebaseUser) => {

      if (firebaseUser) {
        const idToken = await firebaseUser.getIdToken();
        setUser(firebaseUser);
        setToken(idToken);
      } else {
        setUser(null);
        setToken(null);
      }

      setLoading(false);

    });

    return unsubscribe;

  }, []);

  return (
    <AuthContext.Provider value={{ user, token }}>
      {!loading && children}
    </AuthContext.Provider>
  );
}

export const useAuth = () => useContext(AuthContext);
