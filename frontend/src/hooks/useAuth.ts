import { useState, useEffect, useCallback } from "react";
import { getAuthState, subscribeToAuth, setUser, setLoading as setAuthLoading, logout as authLogout, storeTokens, getStoredTokens } from "../stores/authStore";
import { api } from "../lib/api";

export function useAuth() {
  const [authState, setAuthState] = useState(getAuthState());

  useEffect(() => {
    return subscribeToAuth(() => setAuthState(getAuthState()));
  }, []);

  const login = useCallback(async (email: string, password: string) => {
    const res = await api.login({ email, password });
    storeTokens(res.accessToken, res.refreshToken);
    setUser({
      id: res.userId,
      username: res.username,
      email: res.email,
      firstName: "",
      lastName: "",
      role: res.roles[0] || "USER",
    });
    return res;
  }, []);

  const register = useCallback(async (data: { username: string; email: string; password: string; firstName: string; lastName: string; role?: string }) => {
    await api.register(data);
  }, []);

  const logout = useCallback(async () => {
    try { await api.logout(); } catch {}
    authLogout();
  }, []);

  const checkAuth = useCallback(async () => {
    const { accessToken } = getStoredTokens();
    if (!accessToken) {
      setAuthLoading(false);
      return;
    }
    try {
      await api.validateToken();
    } catch {
      authLogout();
    } finally {
      setAuthLoading(false);
    }
  }, []);

  return { ...authState, login, register, logout, checkAuth };
}
