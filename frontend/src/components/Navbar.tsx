export function Navbar() {
  return (
    <header className="sticky top-0 z-10 border-b border-border bg-bg">
      <div className="mx-auto flex max-w-6xl items-center gap-8 px-6 py-5">
        <span className="font-display text-2xl font-bold tracking-wide">Lineup</span>
        <nav className="flex flex-1 gap-6">
          <a href="/" className="text-[15px] text-muted hover:text-ink">
            Events
          </a>
          <a href="/about" className="text-[15px] text-muted hover:text-ink">
            About
          </a>
        </nav>
        <a href="/admin/login" className="text-[15px] text-muted hover:text-ink">
          Admin sign in
        </a>
      </div>
    </header>
  );
}