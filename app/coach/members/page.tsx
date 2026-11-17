import { MembersTable } from "@/components/MembersTable";
import { PageHeader } from "@/components/ui/page-header";
import { requireGym } from "@/lib/coach";
import { toFa } from "@/lib/date";
import { loadMembers } from "@/lib/queries";

export const metadata = { title: "مدیریت اعضا" };

export default async function MembersPage() {
  const gym = await requireGym();
  const members = await loadMembers(gym.id);
  return (
    <>
      <PageHeader title="مدیریت اعضا" description={`${toFa(members.length)} عضو تاییدشده`} icon="members" tone="members" />
      <MembersTable members={members} />
    </>
  );
}
