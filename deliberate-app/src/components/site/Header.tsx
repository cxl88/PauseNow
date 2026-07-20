import { Link } from "@tanstack/react-router";
import { site } from "@/config/site";
import { useState } from "react";
import { Menu, X } from "lucide-react";

export function Header() {
  const [open, setOpen] = useState(false);
  return (
    <header className="sticky top-0 z-40 border-b border-border/70 bg-background/85 backdrop-blur">
      <div className="mx-auto flex h-16 w-full max-w-[1216px] items-center justify-between px-5 sm:px-8">
        <Link to="/" className="flex items-center gap-2 font-semibold text-ink">
          <span
            aria-hidden
            className="grid h-8 w-8 place-items-center rounded-full bg-brand text-brand-foreground text-sm font-bold"
          >
            停
          </span>
          <span className="text-base">{site.brand}</span>
        </Link>

        <nav aria-label="主导航" className="hidden md:block">
          <ul className="flex items-center gap-7 text-sm text-ink/80">
            {site.nav.map((n) => (
              <li key={n.href}>
                <a href={n.href} className="hover:text-brand transition-colors">
                  {n.label}
                </a>
              </li>
            ))}
          </ul>
        </nav>

        <button
          type="button"
          aria-label={open ? "关闭菜单" : "打开菜单"}
          aria-expanded={open}
          className="md:hidden grid h-10 w-10 place-items-center rounded-full hover:bg-brand-soft/50"
          onClick={() => setOpen((v) => !v)}
        >
          {open ? <X className="size-5" /> : <Menu className="size-5" />}
        </button>
      </div>

      {open && (
        <div className="md:hidden border-t border-border/70 bg-background">
          <ul className="mx-auto flex w-full max-w-[1216px] flex-col gap-1 px-5 py-3 text-sm">
            {site.nav.map((n) => (
              <li key={n.href}>
                <a
                  href={n.href}
                  onClick={() => setOpen(false)}
                  className="block rounded-lg px-3 py-2 hover:bg-brand-soft/50"
                >
                  {n.label}
                </a>
              </li>
            ))}
          </ul>
        </div>
      )}
    </header>
  );
}
