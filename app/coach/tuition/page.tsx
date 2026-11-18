import { PageHeader } from "@/components/ui/page-header";
import { TuitionManager } from "@/components/TuitionManager";
import { requireGym } from "@/lib/coach";
import { db } from "@/lib/db";
import { loadMembers, loadPayments } from "@/lib/queries";

export const metadata = { title: "مدیریت شهریه" };

export default async function TuitionPage() {
  const gym = await requireGym();
  const [members, payments, plans] = await Promise.all([
    loadMembers(gym.id),
    loadPayments(gym.id),
    db.plan.findMany({ where: { gymId: gym.id }, orderBy: { createdAt: "asc" } }),
  ]);
  return (
    <>
      <PageHeader title="مدیریت شهریه" description="روی هر عضو بزنید تا شهریه‌اش را ثبت یا ویرایش کنید" icon="wallet" tone="tuition" />
      <TuitionManager members={members} payments={payments} plans={plans.map((p) => ({ id: p.id, name: p.name, amount: p.amount, durationDays: p.durationDays }))} />
    </>
  );
}
