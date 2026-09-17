import { useAuth0 } from "@auth0/auth0-react";

export function Navbar() {
  const { isAuthenticated, isLoading, logout } = useAuth0();

  return (
    <header className="sticky top-0 z-10 border-b border-border bg-bg">
      <div className="mx-auto flex max-w-6xl items-center gap-8 px-6 py-5">
        <img src="/logo.svg" alt="" className="h-6 w-6" />
        <span className="font-display text-2xl font-bold tracking-wide">Lineup</span>
        <nav className="flex flex-1 gap-6">
          <a href="/" className="text-[15px] text-muted hover:text-ink">
            Events
          </a>
          <a href="/about" className="text-[15px] text-muted hover:text-ink">
            About
          </a>
        </nav>
                {isLoading ? null : isAuthenticated ? (
          <button
            type="button"
            onClick={() => logout({ logoutParams: { returnTo: window.location.origin } })}
            className="text-[15px] text-muted hover:text-ink"
          >
            Log out
          </button>
        ) : (
          <a href="/admin/login" className="text-[15px] text-muted hover:text-ink">
            Sign in
          </a>
        )}
      </div>
    </header>
  );
}