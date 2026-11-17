"use client";

import { AnimatePresence, motion, useReducedMotion } from "motion/react";
import Link, { useLinkStatus } from "next/link";
import { usePathname } from "next/navigation";
import { type ReactNode, useEffect, useId, useState } from "react";
import { Avatar } from "@/components/ui/avatar";
import { TONE } from "@/components/ui/badge";
import { Drawer } from "@/components/ui/drawer";
import { Icon } from "@/components/ui/icon";
import { Brand } from "@/components/ui/logo";
import { ThemeToggle } from "@/components/ui/theme-toggle";
import { Loader } from "@/components/ui/loader";
import { logout } from "@/lib/actions";
import type { NavLink } from "@/lib/nav";
import { cn } from "@/lib/utils";

export type ShellUser = { firstName: string; lastName: string; roleLabel: string; profileHref: string };

function isActive(path: string, l: NavLink) {
  return l.exact ? path === l.href : path === l.href || path.startsWith(`${l.href}/`);
}

/** Spinner that appears on a nav item while its page is being fetched. */
function Pending() {
  const { pending } = useLinkStatus();
  return (
    <AnimatePresence initial={false}>
      {pending ? (
        <motion.span
          initial={{ opacity: 0, scale: 0.6 }}
          animate={{ opacity: 1, scale: 1 }}
          exit={{ opacity: 0, scale: 0.6 }}
          className="ms-auto text-muted-foreground"
        >
          <Loader variant="spinner" size={14} />
        </motion.span>
      ) : null}
    </AnimatePresence>
  );
}

function NavList({ links, onNavigate }: { links: NavLink[]; onNavigate?: () => void }) {
  const path = usePathname();
  const pillId = useId();
  const reduce = useReducedMotion();
  return (
    <nav aria-label="منوی اصلی" className="flex flex-col gap-1">
      {links.map((l) => {
        const active = isActive(path, l);
        return (
          <Link
            key={l.href}
            href={l.href}
            onClick={onNavigate}
            aria-current={active ? "page" : undefined}
            className={cn(
              "group relative flex h-12 items-center gap-3 rounded-2xl px-3 text-[15px] font-medium transition-colors active:scale-[0.98]",
              active ? "text-foreground" : "text-muted-foreground hover:bg-card hover:text-foreground",
            )}
          >
            {active ? (
              <motion.span
                layoutId={pillId}
                aria-hidden
                transition={reduce ? { duration: 0 } : { type: "spring", stiffness: 380, damping: 34, mass: 0.6 }}
                className="absolute inset-0 rounded-2xl bg-card"
              />
            ) : null}
            <span
              className={cn(
                "relative grid size-9 shrink-0 place-items-center rounded-xl transition-colors duration-200",
                active ? TONE[l.tone].solid : cn("bg-card group-hover:bg-background", l.tone === "neutral" ? "" : TONE[l.tone].text),
              )}
            >
              <Icon name={l.icon} size={20} set={active ? "bold" : "light"} />
            </span>
            <span className="relative">{l.label}</span>
            <Pending />
          </Link>
        );
      })}
    </nav>
  );
}

function UserCard({ user, onNavigate }: { user: ShellUser; onNavigate?: () => void }) {
  const path = usePathname();
  const name = `${user.firstName} ${user.lastName}`;
  const active = path === user.profileHref;
  return (
    <Link
      href={user.profileHref}
      onClick={onNavigate}
      aria-label="پروفایل من"
      className={cn(
        "flex items-center gap-3 rounded-2xl p-2 transition-colors hover:bg-card active:scale-[0.98]",
        active && "bg-card",
      )}
    >
      <Avatar name={name} firstName={user.firstName} lastName={user.lastName} />
      <span className="min-w-0 flex-1 leading-6">
        <span className="block truncate text-sm font-semibold">{name}</span>
        <span className="block truncate text-xs text-muted-foreground">{user.roleLabel}</span>
      </span>
    </Link>
  );
}

function SidebarBody({ links, user, onNavigate }: { links: NavLink[]; user: ShellUser; onNavigate?: () => void }) {
  return (
    <>
      <div className="flex-1 overflow-y-auto px-3 py-2">
        <NavList links={links} onNavigate={onNavigate} />
      </div>
      <div className="border-t border-border p-3 pb-[max(0.75rem,env(safe-area-inset-bottom))]">
        <UserCard user={user} onNavigate={onNavigate} />
        <div className="mt-2 flex items-center gap-2">
          <ThemeToggle
            variant="circle"
            start="top-right"
            className="size-11 shrink-0 rounded-full border border-border-strong text-muted-foreground transition-[color,background-color,transform] hover:bg-card hover:text-foreground active:scale-90"
          />
          <form action={logout} className="flex-1">
            <button
              type="submit"
              className="flex h-11 w-full items-center justify-center gap-2 rounded-full border border-border-strong text-sm font-medium text-muted-foreground transition-[color,background-color,transform] hover:bg-danger/10 hover:text-danger active:scale-[0.97]"
            >
              <Icon name="logout" size={18} />
              خروج از حساب
            </button>
          </form>
        </div>
      </div>
    </>
  );
}

export function AppShell({ links, user, children }: { links: NavLink[]; user: ShellUser; children: ReactNode }) {
  const [open, setOpen] = useState(false);
  const path = usePathname();
  const reduce = useReducedMotion();

  useEffect(() => setOpen(false), [path]);

  const current = links.find((l) => isActive(path, l));

  return (
    <div className="min-h-dvh">
      {/* Desktop: fixed sidebar on the right (start side in RTL) */}
      <aside className="fixed inset-y-0 start-0 z-30 hidden w-72 flex-col border-e border-border bg-background lg:flex">
        <div className="flex h-20 items-center px-5">
          <Link href="/" aria-label="صفحه اصلی باشگاه‌یار">
            <Brand />
          </Link>
        </div>
        <SidebarBody links={links} user={user} />
      </aside>

      {/* Phones and tablets: top bar + drawer from the right */}
      <header className="glass sticky top-0 z-30 flex items-center gap-1 border-b border-border px-2 pt-safe lg:hidden">
        <div className="flex h-14 w-full items-center gap-1">
          <button
            type="button"
            onClick={() => setOpen(true)}
            aria-label="باز کردن منو"
            aria-expanded={open}
            className="grid size-11 place-items-center rounded-full text-foreground transition-colors hover:bg-card active:scale-90"
          >
            <Icon name="menu" size={24} />
          </button>
          <span className="min-w-0 flex-1 truncate text-[15px] font-semibold">{current?.label ?? "باشگاه‌یار"}</span>
          <ThemeToggle
            variant="circle"
            start="top-left"
            className="size-11 rounded-full text-muted-foreground transition-colors hover:bg-card hover:text-foreground active:scale-90"
          />
        </div>
      </header>

      <Drawer open={open} onOpenChange={setOpen} side="right" ariaLabel="منو" className="w-[19rem] bg-background">
        <div className="flex h-20 shrink-0 items-center justify-between ps-5 pe-3 pt-safe">
          <Brand />
          <button
            type="button"
            onClick={() => setOpen(false)}
            aria-label="بستن منو"
            className="grid size-11 place-items-center rounded-full text-muted-foreground transition-colors hover:bg-card active:scale-90"
          >
            <Icon name="close" size={22} />
          </button>
        </div>
        <SidebarBody links={links} user={user} onNavigate={() => setOpen(false)} />
      </Drawer>

      <main className="lg:ps-72">
        <motion.div
          key={path}
          initial={reduce ? false : { opacity: 0, y: 6 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.28, ease: [0.16, 1, 0.3, 1] }}
          className="mx-auto flex w-full max-w-5xl flex-col gap-6 px-4 pb-[max(2rem,env(safe-area-inset-bottom))] pt-5 sm:px-6 sm:pt-8 lg:px-10 lg:pt-10"
        >
          {children}
        </motion.div>
      </main>
    </div>
  );
}
