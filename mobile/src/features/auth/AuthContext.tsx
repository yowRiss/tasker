import React, { createContext, useContext, useState, useEffect } from 'react';
import { AuthService } from '../../services/authService';
import { apiRequest } from '../../services/apiClient';

export type AuthState = 'checking' | 'authenticated' | 'unauthenticated' | 'reauth_required';

interface AuthUser {
  id: string;
  username: string;
}

interface AuthContextType {
  authState: AuthState;
  user: AuthUser | null;
  login: (username: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  setReauthRequired: () => void;
  checkSession: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [authState, setAuthState] = useState<AuthState>('checking');
  const [user, setUser] = useState<AuthUser | null>(null);

  const checkSession = async () => {
    const token = await AuthService.getToken();
    if (!token) {
      setAuthState('unauthenticated');
      setUser(null);
      return;
    }

    try {
      const res = await apiRequest<{ id: string; username: string }>('/v1/me');
      setUser(res);
      setAuthState('authenticated');
    } catch (err: any) {
      if (err.status === 401) {
        setAuthState('unauthenticated');
      } else {
        // If offline or network error, assume token is valid for offline operations
        setAuthState('authenticated');
      }
    }
  };

  useEffect(() => {
    checkSession();
  }, []);

  const login = async (username: string, password: string) => {
    const res = await apiRequest<{ token: string }>('/v1/auth/login', {
      method: 'POST',
      body: JSON.stringify({ username, password }),
    });

    await AuthService.setToken(res.token);
    await checkSession();
  };

  const logout = async () => {
    await AuthService.clearToken();
    setUser(null);
    setAuthState('unauthenticated');
  };

  const setReauthRequired = () => {
    setAuthState('reauth_required');
  };

  return (
    <AuthContext.Provider value={{ authState, user, login, logout, setReauthRequired, checkSession }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
