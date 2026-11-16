import { requireGym } from "@/lib/coach";
import { loadMembers, loadPayments } from "@/lib/queries";
import { TuitionManager } from "@/components/TuitionManager";

export default async function TuitionPage() {
  const gym = await requireGym();
  const [members, payments] = await Promise.all([loadMembers(gym.id), loadPayments(gym.id)]);
  return (
    <>
      <h1 className="page-title">مدیریت شهریه</h1>
      <TuitionManager
        members={members.map((m) => ({ id: m.id, name: `${m.firstName} ${m.lastName}` }))}
        payments={payments}
      />
    </>
  );
}
