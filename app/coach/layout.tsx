import { AppShell } from "@/components/shell/AppShell";
import { requireUser } from "@/lib/auth";
import { COACH_LINKS } from "@/lib/nav";

export default async function CoachLayout({ children }: { children: React.ReactNode }) {
  const user = await requireUser("COACH");
  return (
    <AppShell links={COACH_LINKS} user={{ firstName: user.firstName, lastName: user.lastName, roleLabel: "مربی", profileHref: "/coach/profile" }}>
      {children}
    </AppShell>
  );
}
