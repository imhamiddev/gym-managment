import { db } from "@/lib/db";
import { requireGym } from "@/lib/coach";
import { loadMembers } from "@/lib/queries";
import { isYmd, ymdTehran } from "@/lib/date";
import { AttendanceBoard } from "@/components/AttendanceBoard";
import { PageHeader } from "@/components/ui/page-header";

export const metadata = { title: "حضور و غیاب" };

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
      <PageHeader title="حضور و غیاب" description="فقط در روزهای فعال باشگاه قابل ثبت است" icon="attendance" tone="attendance" />
      <AttendanceBoard
        key={ymd}
        ymd={ymd}
        workDays={gym.workDays}
        members={members.map((m) => ({ id: m.id, name: `${m.firstName} ${m.lastName}`, firstName: m.firstName, lastName: m.lastName }))}
        initial={initial}
      />
    </>
  );
}
