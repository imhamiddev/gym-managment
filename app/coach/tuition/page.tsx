import { PageHeader } from "@/components/ui/page-header";
import { TuitionManager } from "@/components/TuitionManager";
import { requireGym } from "@/lib/coach";
import { loadMembers, loadPayments } from "@/lib/queries";

export const metadata = { title: "مدیریت شهریه" };

export default async function TuitionPage() {
  const gym = await requireGym();
  const [members, payments] = await Promise.all([loadMembers(gym.id), loadPayments(gym.id)]);
  return (
    <>
      <PageHeader title="مدیریت شهریه" description="ثبت و ویرایش پرداخت‌های اعضا" icon="wallet" tone="tuition" />
      <TuitionManager members={members.map((m) => ({ id: m.id, name: `${m.firstName} ${m.lastName}` }))} payments={payments} />
    </>
  );
}
