export function Footer() {
  return (
    <footer className="border-t border-border bg-bg-panel">
      <div className="mx-auto flex max-w-6xl flex-wrap items-center gap-6 px-6 py-8">
        <span className="font-display text-lg font-bold">Lineup</span>
        <nav className="flex flex-1 gap-5">
          <a href="/about" className="text-sm text-muted hover:text-ink">
            About
          </a>
          <a href="/contact" className="text-sm text-muted hover:text-ink">
            Contact
          </a>
          <a href="/terms" className="text-sm text-muted hover:text-ink">
            Terms
          </a>
        </nav>
        <span className="text-[13px] text-muted">© 2026 Lineup. All rights reserved.</span>
      </div>
    </footer>
  );
}