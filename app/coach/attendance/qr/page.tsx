import { headers } from "next/headers";
import QRCode from "qrcode";
import { ButtonLink } from "@/components/ui/button";
import { Icon } from "@/components/ui/icon";
import { PageHeader } from "@/components/ui/page-header";
import { checkinToken } from "@/lib/checkin";
import { requireGym } from "@/lib/coach";
import { formatJalali, ymdTehran } from "@/lib/date";
import { describeDays, formatHours, isGymDay } from "@/lib/gym";

export const metadata = { title: "QR حضور" };
export const dynamic = "force-dynamic";

export default async function CheckinQrPage() {
  const gym = await requireGym();
  const today = ymdTehran(new Date());
  const h = await headers();
  const host = h.get("x-forwarded-host") ?? h.get("host") ?? "localhost:3000";
  const proto = h.get("x-forwarded-proto") ?? (host.startsWith("localhost") ? "http" : "https");
  const url = `${proto}://${host}/checkin?g=${gym.id}&t=${checkinToken(gym.id, today)}`;
  const svg = await QRCode.toString(url, { type: "svg", margin: 1, errorCorrectionLevel: "M" });
  const open = isGymDay(gym.workDays, today);

  return (
    <>
      <PageHeader
        title="QR حضور امروز"
        description={`${gym.name} · ${formatJalali(today)}`}
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
        <div
          role="img"
          aria-label="QR code حضور امروز"
          className="w-full max-w-[280px] rounded-3xl bg-white p-3 [&>svg]:h-auto [&>svg]:w-full"
          dangerouslySetInnerHTML={{ __html: svg }}
        />
        <p className="font-semibold leading-7">ورزشکاران با دوربین گوشی این کد را اسکن کنند و حضورشان ثبت می‌شود.</p>
        <p className="text-sm leading-7 text-muted-foreground">
          این کد فقط برای امروز معتبر است و فردا خودکار عوض می‌شود. ثبت حضور فقط در روزهای {describeDays(gym.workDays)} و ساعت {formatHours(gym.openTime, gym.closeTime)} (با ۳۰ دقیقه مهلت) انجام می‌شود.
        </p>
        {!open ? <p className="rounded-2xl bg-warning/10 px-4 py-3 text-sm text-warning">امروز باشگاه فعال نیست؛ ثبت حضور کار نمی‌کند.</p> : null}
      </section>
    </>
  );
}
