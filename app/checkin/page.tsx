import Link from "next/link";
import { redirect } from "next/navigation";
import { CheckInButton } from "@/components/CheckInButton";
import { Card } from "@/components/ui/card";
import { Brand } from "@/components/ui/logo";
import { getUser } from "@/lib/auth";
import { tokensMatch } from "@/lib/checkin";
import { db } from "@/lib/db";

export const metadata = { title: "ثبت حضور" };
export const dynamic = "force-dynamic";

function Shell({ children }: { children: React.ReactNode }) {
  return (
    <main className="mx-auto flex min-h-dvh w-full max-w-md flex-col items-center justify-center gap-6 px-5 py-10 pt-safe">
      <Brand />
      <Card className="w-full items-center gap-5 px-6 py-8 text-center shadow-card">{children}</Card>
    </main>
  );
}

export default async function CheckinPage({ searchParams }: { searchParams: Promise<{ g?: string; t?: string }> }) {
  const { g = "", t = "" } = await searchParams;
  const user = await getUser();
  if (!user) redirect(`/login?next=${encodeURIComponent(`/checkin?g=${g}&t=${t}`)}`);

  if (user.role !== "ATHLETE") {
    return (
      <Shell>
        <p className="leading-8">ثبت حضور با QR فقط برای ورزشکاران است.</p>
        <Link href="/coach" className="text-sm font-medium text-primary hover:underline">بازگشت</Link>
      </Shell>
    );
  }

  const m = g ? await db.membership.findUnique({ where: { athleteId: user.id }, include: { gym: true } }) : null;
  if (!m || m.status !== "APPROVED" || m.gymId !== g) {
    return (
      <Shell>
        <p className="leading-8">شما عضو تاییدشده‌ی این باشگاه نیستید.</p>
        <Link href="/athlete" className="text-sm font-medium text-primary hover:underline">رفتن به پنل من</Link>
      </Shell>
    );
  }
  if (!tokensMatch(m.gym.checkinToken, t)) {
    return (
      <Shell>
        <p className="leading-8">این QR دیگر معتبر نیست. QR جدید را از مربی بگیرید و دوباره اسکن کنید.</p>
        <Link href="/athlete" className="text-sm font-medium text-primary hover:underline">رفتن به پنل من</Link>
      </Shell>
    );
  }

  return (
    <Shell>
      <div>
        <p className="text-sm text-muted-foreground">{m.gym.name}</p>
        <h1 className="text-xl font-bold leading-9">سلام {user.firstName}</h1>
      </div>
      <CheckInButton gymId={g} token={t} />
    </Shell>
  );
}
