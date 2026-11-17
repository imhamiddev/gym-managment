import { ProfileForm } from "@/components/ProfileForm";
import { PageHeader } from "@/components/ui/page-header";
import { requireUser } from "@/lib/auth";
import { db } from "@/lib/db";
import { formatJalali, ymdTehran } from "@/lib/date";

export async function ProfilePage({ role }: { role: "COACH" | "ATHLETE" }) {
  const user = await requireUser(role);
  let gymLabel: string | null = null;
  let gymValue: string | null = null;
  if (role === "COACH") {
    const gym = await db.gym.findUnique({ where: { coachId: user.id } });
    if (gym) { gymLabel = "باشگاه"; gymValue = gym.name; }
  } else {
    const m = await db.membership.findUnique({ where: { athleteId: user.id }, include: { gym: true } });
    if (m) { gymLabel = "باشگاه"; gymValue = m.gym.name; }
  }
  return (
    <>
      <PageHeader title="پروفایل" description="اطلاعات حساب و تنظیمات نمایش" icon="user" />
      <ProfileForm
        info={{
          firstName: user.firstName,
          lastName: user.lastName,
          phone: user.phone,
          roleLabel: role === "COACH" ? "مربی" : "ورزشکار",
          joined: formatJalali(ymdTehran(user.createdAt)),
          gymLabel,
          gymValue,
        }}
      />
    </>
  );
}
