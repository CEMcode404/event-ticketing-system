import { useEffect } from "react";
import { useAuth0 } from "@auth0/auth0-react";

export function LoginPage() {
  const { loginWithRedirect, isAuthenticated, isLoading } = useAuth0();

  useEffect(() => {
    if (!isLoading && !isAuthenticated) {
      loginWithRedirect();
    }
  }, [isLoading, isAuthenticated, loginWithRedirect]);

  return (
    <div className="flex min-h-svh items-center justify-center bg-bg">
      <p className="text-muted">Redirecting to sign in…</p>
    </div>
  );
}