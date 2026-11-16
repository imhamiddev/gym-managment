import { requireUser } from "@/lib/auth";
import { PanelShell } from "@/components/PanelShell";

export default async function CoachLayout({ children }: { children: React.ReactNode }) {
  const user = await requireUser("COACH");
  return (
    <PanelShell
      name={`مربی ${user.firstName} ${user.lastName}`}
      links={[
        { href: "/coach", label: "نمای کلی" },
        { href: "/coach/members", label: "مدیریت اعضا" },
        { href: "/coach/tuition", label: "مدیریت شهریه" },
        { href: "/coach/attendance", label: "حضور و غیاب" },
      ]}
    >
      {children}
    </PanelShell>
  );
}
