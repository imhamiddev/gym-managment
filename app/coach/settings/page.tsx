import { GymSettingsForm } from "@/components/GymSettingsForm";
import { PageHeader } from "@/components/ui/page-header";
import { CoachesCard } from "@/components/CoachesCard";
import { PlansCard } from "@/components/PlansCard";
import { requireOwnerGym } from "@/lib/coach";
import { db } from "@/lib/db";

export const metadata = { title: "تنظیمات باشگاه" };

export default async function GymSettingsPage() {
  const gym = await requireOwnerGym();
  const [plans, assistants, owner] = await Promise.all([
    db.plan.findMany({ where: { gymId: gym.id }, orderBy: { createdAt: "asc" } }),
    db.gymCoach.findMany({ where: { gymId: gym.id }, include: { user: true }, orderBy: { createdAt: "asc" } }),
    db.user.findUnique({ where: { id: gym.coachId } }),
  ]);
  return (
    <>
      <PageHeader title="تنظیمات باشگاه" description="نام، کد دعوت، روزها و ساعت کاری، شماره کارت" icon="setting" />
      <GymSettingsForm
        initial={{ name: gym.name, code: gym.code, workDays: gym.workDays, openTime: gym.openTime, closeTime: gym.closeTime, cardNumber: gym.cardNumber }}
      />
      <PlansCard plans={plans.map((p) => ({ id: p.id, name: p.name, amount: p.amount, durationDays: p.durationDays }))} />
      <CoachesCard
        owner={owner ? `${owner.firstName} ${owner.lastName}` : ""}
        coaches={assistants.map((c) => ({ userId: c.userId, name: `${c.user.firstName} ${c.user.lastName}`, phone: c.user.phone }))}
      />
    </>
  );
}
