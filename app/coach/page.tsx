import Link from "next/link";
import { CodeCopy, CreateGymForm, RequestsList } from "@/components/GymSetup";
import { StatCard } from "@/components/StatCard";
import { Badge, TONE, type Tone } from "@/components/ui/badge";
import { Card } from "@/components/ui/card";
import { Icon, type IconName } from "@/components/ui/icon";
import { PageHeader } from "@/components/ui/page-header";
import { requireUser } from "@/lib/auth";
import { isBirthday, toFa, ymdTehran } from "@/lib/date";
import { findCoachGym } from "@/lib/coach";
import { loadMembers, loadPending } from "@/lib/queries";
import { cn } from "@/lib/utils";

export const metadata = { title: "نمای کلی" };

const QUICK: { href: string; icon: IconName; tone: Tone; label: string; hint: string }[] = [
  { href: "/coach/attendance", icon: "attendance", tone: "attendance", label: "حضور و غیاب", hint: "ثبت جلسه امروز" },
  { href: "/coach/tuition", icon: "wallet", tone: "tuition", label: "شهریه", hint: "ثبت پرداخت جدید" },
  { href: "/coach/members", icon: "members", tone: "members", label: "اعضا", hint: "مشخصات و وضعیت" },
];

export default async function CoachHome() {
  const user = await requireUser("COACH");
  const gym = (await findCoachGym(user.id))?.gym ?? null;
  if (!gym) {
    return (
      <>
        <PageHeader title={`خوش آمدید، ${user.firstName}`} description="برای شروع، باشگاه خود را بسازید." icon="overview" />
        <CreateGymForm />
      </>
    );
  }

  const [members, pending] = await Promise.all([loadMembers(gym.id), loadPending(gym.id)]);
  const active = members.filter((m) => (m.remaining ?? 0) > 7).length;
  const expiring = members.filter((m) => m.remaining !== null && m.remaining > 0 && m.remaining <= 7).length;
  const expired = members.filter((m) => !m.remaining).length;
  const birthdays = members.filter((m) => isBirthday(m.birthYmd, ymdTehran(new Date())));

  return (
    <>
      <PageHeader title={gym.name} description="نمای کلی باشگاه" icon="overview" />

      <section className="flex flex-col gap-4 rounded-[28px] border border-primary/20 bg-primary/8 p-5 sm:flex-row sm:items-center sm:justify-between sm:p-6">
        <div className="flex items-center gap-4">
          <span className="grid size-12 shrink-0 place-items-center rounded-2xl bg-primary text-primary-foreground">
            <Icon name="ticket" size={24} set="bold" />
          </span>
          <div>
            <p className="text-sm text-muted-foreground">کد باشگاه برای ورزشکاران</p>
            <p className="text-3xl font-extrabold leading-10 tracking-[0.3em] ltr">{gym.code}</p>
          </div>
        </div>
        <CodeCopy code={gym.code} />
      </section>

      {birthdays.length ? (
        <section className="flex items-center gap-3 rounded-3xl border border-attendance/25 bg-attendance/10 p-4" role="status">
          <span className="grid size-11 shrink-0 place-items-center rounded-2xl bg-attendance/15 text-attendance"><Icon name="star" size={22} set="bulk" /></span>
          <p className="leading-7"><span className="block font-semibold">تولد امروز</span><span className="block text-sm text-muted-foreground">{birthdays.map((b) => `${b.firstName} ${b.lastName}`).join("، ")}</span></p>
        </section>
      ) : null}

      <section className="grid grid-cols-2 gap-3 lg:grid-cols-4" aria-label="آمار">
        <StatCard icon="members" tone="members" label="عضو" value={members.length} />
        <StatCard icon="shield" tone="success" label="شهریه فعال" value={active} />
        <StatCard icon="time" tone="warning" label="رو به پایان (تا ۷ روز)" value={expiring} />
        <StatCard icon="danger" tone="danger" label="منقضی یا بدون شهریه" value={expired} />
      </section>

      <section className="grid gap-3 sm:grid-cols-3" aria-label="دسترسی سریع">
        {QUICK.map((q) => (
          <Link key={q.href} href={q.href} className="group flex items-center gap-3 rounded-3xl border border-border bg-background p-4 shadow-card transition-[transform,border-color] hover:border-border-strong active:scale-[0.98]">
            <span className={cn("grid size-11 place-items-center rounded-2xl", TONE[q.tone].soft)}>
              <Icon name={q.icon} size={22} set="bulk" />
            </span>
            <span className="flex-1 leading-6">
              <span className="block font-semibold">{q.label}</span>
              <span className="block text-xs text-muted-foreground">{q.hint}</span>
            </span>
            <Icon name="chevronLeft" size={18} className="text-muted-foreground transition-transform group-hover:-translate-x-1" />
          </Link>
        ))}
      </section>

      <section className="flex flex-col gap-3">
        <h2 className="flex items-center gap-2 text-base font-semibold">
          درخواست‌های عضویت
          {pending.length ? <Badge tone="members">{toFa(pending.length)}</Badge> : null}
        </h2>
        <Card className="gap-0 overflow-hidden py-0 shadow-card">
          <RequestsList requests={pending} />
        </Card>
      </section>
    </>
  );
}
