import { create } from "zustand";

// Simple store without zustand dependency - we use React context
// This is a lightweight auth store using React built-ins

interface AuthState {
  user: {
    id: string;
    username: string;
    email: string;
    firstName: string;
    lastName: string;
    role: string;
    avatarUrl?: string;
  } | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  setUser: (user: AuthState["user"]) => void;
  setLoading: (loading: boolean) => void;
  logout: () => void;
}

// We'll use a simple module-level state pattern
let state: AuthState = {
  user: null,
  isAuthenticated: false,
  isLoading: true,
  setUser: () => {},
  setLoading: () => {},
  logout: () => {},
};

const listeners = new Set<() => void>();

function notify() {
  listeners.forEach((l) => l());
}

export function getAuthState(): AuthState {
  return state;
}

export function subscribeToAuth(callback: () => void): () => void {
  listeners.add(callback);
  return () => listeners.delete(callback);
}

export function setUser(user: AuthState["user"]) {
  state = { ...state, user, isAuthenticated: !!user };
  notify();
}

export function setLoading(isLoading: boolean) {
  state = { ...state, isLoading };
  notify();
}

export function logout() {
  localStorage.removeItem("accessToken");
  localStorage.removeItem("refreshToken");
  state = { ...state, user: null, isAuthenticated: false };
  notify();
}

export function getStoredTokens(): { accessToken: string | null; refreshToken: string | null } {
  return {
    accessToken: localStorage.getItem("accessToken"),
    refreshToken: localStorage.getItem("refreshToken"),
  };
}

export function storeTokens(accessToken: string, refreshToken: string) {
  localStorage.setItem("accessToken", accessToken);
  localStorage.setItem("refreshToken", refreshToken);
}
