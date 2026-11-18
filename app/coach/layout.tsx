import { AppShell } from "@/components/shell/AppShell";
import { requireUser } from "@/lib/auth";
import { findCoachGym } from "@/lib/coach";
import { COACH_LINKS } from "@/lib/nav";

export default async function CoachLayout({ children }: { children: React.ReactNode }) {
  const user = await requireUser("COACH");
  const found = await findCoachGym(user.id);
  // Assistant coaches manage members, tuition and attendance; only the owner edits gym settings.
  const links = found && !found.isOwner ? COACH_LINKS.filter((l) => l.href !== "/coach/settings") : COACH_LINKS;
  return (
    <AppShell links={links} user={{ firstName: user.firstName, lastName: user.lastName, roleLabel: found && !found.isOwner ? "مربی (دستیار)" : "مربی", profileHref: "/coach/profile" }}>
      {children}
    </AppShell>
  );
}
