import { headers } from "next/headers";
import QRCode from "qrcode";
import { QrActions } from "@/components/QrActions";
import { ButtonLink } from "@/components/ui/button";
import { Icon } from "@/components/ui/icon";
import { PageHeader } from "@/components/ui/page-header";
import { ensureCheckinToken, requireGym } from "@/lib/coach";
import { describeDays, formatHours } from "@/lib/gym";

export const metadata = { title: "QR حضور" };
export const dynamic = "force-dynamic";

export default async function CheckinQrPage() {
  const gym = await requireGym();
  const token = await ensureCheckinToken(gym);
  const h = await headers();
  const host = h.get("x-forwarded-host") ?? h.get("host") ?? "localhost:3000";
  const proto = h.get("x-forwarded-proto") ?? (host.startsWith("localhost") ? "http" : "https");
  const svg = await QRCode.toString(`${proto}://${host}/checkin?g=${gym.id}&t=${token}`, { type: "svg", margin: 1, errorCorrectionLevel: "M" });

  return (
    <>
      <PageHeader
        title="QR حضور"
        description={gym.name}
        icon="attendance"
        tone="attendance"
        actions={
          <ButtonLink href="/coach/attendance" variant="secondary" size="sm">
            <Icon name="chevronRight" size={16} />
            بازگشت
          </ButtonLink>
        }
      />
      <section className="mx-auto flex w-full max-w-sm flex-col items-center gap-4 rounded-[32px] border border-border bg-background p-6 text-center shadow-card">
        {/* QR codes need dark-on-light to scan reliably, so the code keeps a white card in dark mode too. */}
        <div role="img" aria-label="QR code حضور" className="w-full max-w-[280px] rounded-3xl bg-white p-3 [&>svg]:h-auto [&>svg]:w-full" dangerouslySetInnerHTML={{ __html: svg }} />
        <p className="font-semibold leading-7">این QR ثابت است. یک‌بار چاپ کنید و در باشگاه نصب کنید.</p>
        <p className="text-sm leading-7 text-muted-foreground">
          هر عضو تاییدشده با اسکن آن حاضر ثبت می‌شود، فقط در روزهای {describeDays(gym.workDays)} و ساعت {formatHours(gym.openTime, gym.closeTime)} (با ۳۰ دقیقه مهلت). تا وقتی «تعویض QR» نزنید، همین کد معتبر می‌ماند.
        </p>
        <QrActions canRegenerate={gym.isOwner} />
      </section>
    </>
  );
}
