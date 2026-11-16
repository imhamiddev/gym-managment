import Link from "next/link";
import { Button } from "@/components/arc/button/button";
import { logout } from "@/lib/actions";
import { NavLinks } from "./NavLinks";

export function PanelShell({
  name,
  links,
  children,
}: {
  name: string;
  links: { href: string; label: string }[];
  children: React.ReactNode;
}) {
  return (
    <>
      <header className="topbar">
        <div className="container topbar-inner">
          <Link href="/" className="brand">باشگاه‌یار</Link>
          <NavLinks links={links} />
          <span className="muted">{name}</span>
          <form action={logout}>
            <Button variant="ghost" size="sm" type="submit">خروج</Button>
          </form>
        </div>
      </header>
      <main className="container page">{children}</main>
    </>
  );
}
