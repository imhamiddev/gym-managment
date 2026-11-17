import { CancelRequestButton, JoinGymForm } from "@/components/GymSetup";
import { RemainingBadge } from "@/components/RemainingBadge";
import { StatCard } from "@/components/StatCard";
import { Badge } from "@/components/ui/badge";
import { Card } from "@/components/ui/card";
import { EmptyState } from "@/components/ui/empty-state";
import { Icon } from "@/components/ui/icon";
import { PageHeader } from "@/components/ui/page-header";
import { requireUser } from "@/lib/auth";
import { formatDays, formatJalali, formatToman, remainingDays, ymdAdd, ymdTehran } from "@/lib/date";
import { db } from "@/lib/db";
import { cn } from "@/lib/utils";

export const metadata = { title: "پنل من" };

export default async function AthletePage() {
  const user = await requireUser("ATHLETE");
  const m = await db.membership.findUnique({
    where: { athleteId: user.id },
    include: {
      gym: { include: { coach: true } },
      payments: { orderBy: { paidAt: "desc" } },
      attendances: { orderBy: { date: "desc" }, take: 30 },
    },
  });

  const today = ymdTehran(new Date());
  const payments = (m?.payments ?? []).map((p) => ({ id: p.id, amount: p.amount, paidYmd: p.paidAt.toISOString().slice(0, 10), durationDays: p.durationDays }));
  const remaining = remainingDays(payments, today);
  const since = ymdAdd(today, -29);
  const recent = (m?.attendances ?? []).filter((a) => a.date >= since);
  const present = recent.filter((a) => a.present).length;

  if (!m) {
    return (
      <>
        <PageHeader title={`خوش آمدید، ${user.firstName}`} description="برای شروع به یک باشگاه بپیوندید." icon="overview" />
        <JoinGymForm />
      </>
    );
  }

  if (m.status === "PENDING") {
    return (
      <>
        <PageHeader title="در انتظار تایید مربی" icon="time" tone="warning" />
        <Card className="mx-auto w-full max-w-lg items-center gap-4 px-6 text-center shadow-card">
          <span className="relative grid size-16 place-items-center rounded-3xl bg-warning/14 text-warning">
            <span aria-hidden className="absolute inset-0 animate-ping rounded-3xl bg-warning/20 [animation-duration:2.4s] motion-reduce:hidden" />
            <Icon name="time" size={30} set="bulk" className="relative" />
          </span>
          <p className="leading-8">
            درخواست عضویت شما برای «{m.gym.name}» ارسال شد. بعد از تایید مربی‌تان اینجا اطلاعات شهریه و حضور را می‌بینید.
          </p>
          <CancelRequestButton />
        </Card>
      </>
    );
  }

  const lastDuration = payments[0]?.durationDays ?? 30;
  const ratio = remaining === null ? 0 : Math.min(1, remaining / lastDuration);
  const tone = remaining === null ? "bg-muted-foreground" : remaining === 0 ? "bg-danger" : remaining <= 7 ? "bg-warning" : "bg-success";

  return (
    <>
      <PageHeader title={m.gym.name} description={`مربی: ${m.gym.coach.firstName} ${m.gym.coach.lastName}`} icon="home" />

      <section className="rounded-[28px] border border-border bg-background p-5 shadow-card sm:p-6">
        <div className="flex items-center justify-between gap-3">
          <p className="text-sm text-muted-foreground">شهریه باقی‌مانده</p>
          <RemainingBadge days={remaining} />
        </div>
        <p className="mb-4 mt-1 text-4xl font-extrabold leading-[3rem]">
          {remaining === null ? "—" : formatDays(remaining)}
        </p>
        <div className="h-2.5 overflow-hidden rounded-full bg-card" role="progressbar" aria-valuemin={0} aria-valuemax={lastDuration} aria-valuenow={remaining ?? 0}>
          <div className={cn("h-full origin-right rounded-full transition-transform duration-700", tone)} style={{ transform: `scaleX(${ratio})` }} />
        </div>
      </section>

      <section className="grid grid-cols-2 gap-3" aria-label="آمار">
        <StatCard icon="attendance" tone="attendance" label="حضور در ۳۰ روز اخیر" value={present} />
        <StatCard icon="wallet" tone="tuition" label="پرداخت ثبت‌شده" value={payments.length} />
      </section>

      <section className="flex flex-col gap-3">
        <h2 className="text-base font-semibold">تاریخچه شهریه</h2>
        <Card className="gap-0 overflow-hidden py-0 shadow-card">
          {payments.length === 0 ? (
            <EmptyState icon="wallet" tone="tuition" title="هنوز شهریه‌ای ثبت نشده" description="بعد از ثبت اولین شهریه توسط مربی، اینجا نمایش داده می‌شود." />
          ) : (
            <ul className="divide-y divide-border">
              {payments.map((p) => (
                <li key={p.id} className="flex items-center gap-3 px-4 py-4 sm:px-5">
                  <span className="grid size-10 shrink-0 place-items-center rounded-2xl bg-tuition/12 text-tuition">
                    <Icon name="wallet" size={20} set="bulk" />
                  </span>
                  <div className="min-w-0 flex-1">
                    <p className="font-semibold">{formatToman(p.amount)}</p>
                    <p className="text-xs text-muted-foreground">
                      {formatJalali(p.paidYmd)} · تا {formatJalali(ymdAdd(p.paidYmd, p.durationDays))}
                    </p>
                  </div>
                  <Badge>{formatDays(p.durationDays)}</Badge>
                </li>
              ))}
            </ul>
          )}
        </Card>
      </section>

      <section className="flex flex-col gap-3">
        <h2 className="text-base font-semibold">حضور و غیاب اخیر</h2>
        {recent.length === 0 ? (
          <Card className="shadow-card"><EmptyState icon="attendance" tone="attendance" title="هنوز حضور و غیابی ثبت نشده" /></Card>
        ) : (
          <ul className="grid grid-cols-2 gap-2 sm:grid-cols-3 lg:grid-cols-4">
            {recent.map((a) => (
              <li key={a.id} className="flex items-center justify-between gap-2 rounded-2xl border border-border bg-background px-3.5 py-3">
                <span className="text-sm">{formatJalali(a.date)}</span>
                <span className={cn("grid size-7 place-items-center rounded-full", a.present ? "bg-success/12 text-success" : "bg-danger/12 text-danger")} aria-label={a.present ? "حاضر" : "غایب"}>
                  <Icon name={a.present ? "check" : "close"} size={14} />
                </span>
              </li>
            ))}
          </ul>
        )}
      </section>
    </>
  );
}
