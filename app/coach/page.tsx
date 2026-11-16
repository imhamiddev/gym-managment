import { db } from "@/lib/db";
import { requireUser } from "@/lib/auth";
import { loadMembers, loadPending } from "@/lib/queries";
import { toFa } from "@/lib/date";
import { CodeCopy, CreateGymForm, RequestsList } from "@/components/GymSetup";

export default async function CoachHome() {
  const user = await requireUser("COACH");
  const gym = await db.gym.findUnique({ where: { coachId: user.id } });
  if (!gym) {
    return (
      <>
        <h1 className="page-title">خوش آمدید، {user.firstName}</h1>
        <CreateGymForm />
      </>
    );
  }

  const [members, pending] = await Promise.all([loadMembers(gym.id), loadPending(gym.id)]);
  const active = members.filter((m) => (m.remaining ?? 0) > 7).length;
  const expiring = members.filter((m) => m.remaining !== null && m.remaining > 0 && m.remaining <= 7).length;
  const expired = members.filter((m) => !m.remaining).length;

  return (
    <>
      <div className="row-between">
        <h1 className="page-title">{gym.name}</h1>
      </div>

      <section className="panel row-between">
        <div>
          <p className="muted">کد باشگاه برای ورزشکاران</p>
          <div className="code-box">{gym.code}</div>
        </div>
        <CodeCopy code={gym.code} />
      </section>

      <section className="stats">
        <div className="stat"><b>{toFa(members.length)}</b><span className="muted">عضو</span></div>
        <div className="stat"><b>{toFa(active)}</b><span className="muted">شهریه فعال</span></div>
        <div className="stat"><b>{toFa(expiring)}</b><span className="muted">رو به پایان (تا ۷ روز)</span></div>
        <div className="stat"><b>{toFa(expired)}</b><span className="muted">منقضی یا بدون شهریه</span></div>
      </section>

      <section className="stack">
        <h2>درخواست‌های عضویت{pending.length ? ` (${toFa(pending.length)})` : ""}</h2>
        <div className="panel panel-flush">
          <RequestsList requests={pending} />
        </div>
      </section>
    </>
  );
}
