import { db } from "@/lib/db";
import { requireUser } from "@/lib/auth";
import { Badge } from "@/components/arc/badge/badge";
import { PanelShell } from "@/components/PanelShell";
import { CancelRequestButton, JoinGymForm } from "@/components/GymSetup";
import { RemainingBadge } from "@/components/RemainingBadge";
import { formatDays, formatJalali, formatToman, remainingDays, toFa, ymdAdd, ymdTehran } from "@/lib/date";

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

  return (
    <PanelShell name={`${user.firstName} ${user.lastName}`} links={[{ href: "/athlete", label: "پنل من" }]}>
      {!m && (
        <>
          <h1 className="page-title">خوش آمدید، {user.firstName}</h1>
          <JoinGymForm />
        </>
      )}

      {m?.status === "PENDING" && (
        <>
          <h1 className="page-title">در انتظار تایید مربی</h1>
          <div className="panel stack" style={{ maxWidth: 460 }}>
            <p>درخواست عضویت شما برای «{m.gym.name}» ارسال شد. بعد از تایید مربی‌تان اینجا اطلاعات شهریه و حضور را می‌بینید.</p>
            <div><CancelRequestButton /></div>
          </div>
        </>
      )}

      {m?.status === "APPROVED" && (
        <>
          <div>
            <h1 className="page-title">{m.gym.name}</h1>
            <p className="muted">مربی: {m.gym.coach.firstName} {m.gym.coach.lastName}</p>
          </div>

          <section className="stats">
            <div className="stat">
              <span className="muted">شهریه باقی‌مانده</span>
              <div style={{ marginTop: 8 }}><RemainingBadge days={remaining} /></div>
            </div>
            <div className="stat"><b>{toFa(present)}</b><span className="muted">حضور در ۳۰ روز اخیر</span></div>
            <div className="stat"><b>{toFa(payments.length)}</b><span className="muted">پرداخت ثبت‌شده</span></div>
          </section>

          <section className="stack">
            <h2>تاریخچه شهریه</h2>
            <div className="panel panel-flush">
              {payments.length === 0 ? (
                <div className="empty">هنوز شهریه‌ای برای شما ثبت نشده است.</div>
              ) : (
                <div className="table-wrap">
                  <table className="table">
                    <thead><tr><th>تاریخ پرداخت</th><th>مبلغ</th><th>مدت</th><th>پایان اشتراک</th></tr></thead>
                    <tbody>
                      {payments.map((p) => (
                        <tr key={p.id}>
                          <td>{formatJalali(p.paidYmd)}</td>
                          <td>{formatToman(p.amount)}</td>
                          <td>{formatDays(p.durationDays)}</td>
                          <td>{formatJalali(ymdAdd(p.paidYmd, p.durationDays))}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          </section>

          <section className="stack">
            <h2>حضور و غیاب اخیر</h2>
            <div className="panel panel-flush">
              {recent.length === 0 ? (
                <div className="empty">هنوز حضور و غیابی ثبت نشده است.</div>
              ) : (
                recent.map((a) => (
                  <div className="att-row" key={a.id}>
                    <span>{formatJalali(a.date)}</span>
                    <Badge tone={a.present ? "success" : "danger"}>{a.present ? "حاضر" : "غایب"}</Badge>
                  </div>
                ))
              )}
            </div>
          </section>
        </>
      )}
    </PanelShell>
  );
}
