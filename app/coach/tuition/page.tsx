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
      <PageHeader title="مدیریت شهریه" description="روی هر عضو بزنید تا شهریه‌اش را ثبت یا ویرایش کنید" icon="wallet" tone="tuition" />
      <TuitionManager members={members} payments={payments} />
    </>
  );
}
