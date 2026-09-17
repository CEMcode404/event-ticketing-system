import { useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth0 } from "@auth0/auth0-react";

export function LoginPage() {
  const { loginWithRedirect, isAuthenticated, isLoading } = useAuth0();
  const navigate = useNavigate();

  useEffect(() => {
    if (isLoading) return;

    if (isAuthenticated) {
      // TODO: send to /admin once that page exists — home is a
      // placeholder destination for now.
      navigate("/");
    } else {
      loginWithRedirect();
    }
  }, [isLoading, isAuthenticated, loginWithRedirect, navigate]);

  return (
    <div className="flex min-h-svh items-center justify-center bg-bg">
      <p className="text-muted">Redirecting…</p>
    </div>
  );
}