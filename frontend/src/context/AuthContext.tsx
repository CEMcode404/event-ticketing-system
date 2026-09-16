import { createContext, useContext, useState, type ReactNode } from "react";
import { apiFetch } from "../lib/api";

interface LoginResponse {
  token: string;
}

interface AuthContextValue {
  token: string | null;
  isAuthenticated: boolean;
  login: (username: string, password: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  // Deliberately React state, not localStorage — lost on page refresh.
  // See project notes: in-memory-only was chosen over the full
  // access+refresh-cookie pattern as a deliberate scope reduction for a
  // single-admin test tool.
  const [token, setToken] = useState<string | null>(null);

  async function login(username: string, password: string) {
    const response = await apiFetch<LoginResponse>("/api/admin/login", {
      method: "POST",
      body: { username, password },
    });
    setToken(response.token);
  }

  function logout() {
    setToken(null);
  }

  return (
    <AuthContext.Provider value={{ token, isAuthenticated: token !== null, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
}