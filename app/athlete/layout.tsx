import { AppShell } from "@/components/shell/AppShell";
import { requireUser } from "@/lib/auth";
import { ATHLETE_LINKS } from "@/lib/nav";

export default async function AthleteLayout({ children }: { children: React.ReactNode }) {
  const user = await requireUser("ATHLETE");
  return (
    <AppShell links={ATHLETE_LINKS} user={{ firstName: user.firstName, lastName: user.lastName, roleLabel: "ورزشکار", profileHref: "/athlete/profile" }}>
      {children}
    </AppShell>
  );
}
