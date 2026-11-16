import { db } from "@/lib/db";
import { requireGym } from "@/lib/coach";
import { loadMembers } from "@/lib/queries";
import { isYmd, ymdTehran } from "@/lib/date";
import { AttendanceBoard } from "@/components/AttendanceBoard";

export default async function AttendancePage({ searchParams }: { searchParams: Promise<{ d?: string }> }) {
  const gym = await requireGym();
  const { d } = await searchParams;
  const ymd = d && isYmd(d) ? d : ymdTehran(new Date());

  const [members, records] = await Promise.all([
    loadMembers(gym.id),
    db.attendance.findMany({ where: { date: ymd, membership: { gymId: gym.id } } }),
  ]);
  const initial = Object.fromEntries(records.map((r) => [r.membershipId, r.present]));

  return (
    <>
      <h1 className="page-title">حضور و غیاب</h1>
      <AttendanceBoard
        key={ymd}
        ymd={ymd}
        members={members.map((m) => ({ id: m.id, name: `${m.firstName} ${m.lastName}` }))}
        initial={initial}
      />
    </>
  );
}
