import { requireGym } from "@/lib/coach";
import { loadMembers } from "@/lib/queries";
import { MembersTable } from "@/components/MembersTable";

export default async function MembersPage() {
  const gym = await requireGym();
  const members = await loadMembers(gym.id);
  return (
    <>
      <h1 className="page-title">مدیریت اعضا</h1>
      <MembersTable members={members} />
    </>
  );
}
