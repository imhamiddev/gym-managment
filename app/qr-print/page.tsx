import { headers } from "next/headers";
import QRCode from "qrcode";
import { PrintButton } from "@/components/PrintButton";
import { requireUser } from "@/lib/auth";
import { ensureCheckinToken, findCoachGym } from "@/lib/coach";
import { redirect } from "next/navigation";

export const metadata = { title: "چاپ QR حضور" };
export const dynamic = "force-dynamic";

/** Clean, shell-free poster for printing the gym's check-in QR. */
export default async function QrPrintPage() {
  const user = await requireUser("COACH");
  const found = await findCoachGym(user.id);
  if (!found) redirect("/coach");
  const { gym } = found;
  const token = await ensureCheckinToken(gym);
  const h = await headers();
  const host = h.get("x-forwarded-host") ?? h.get("host") ?? "localhost:3000";
  const proto = h.get("x-forwarded-proto") ?? (host.startsWith("localhost") ? "http" : "https");
  const svg = await QRCode.toString(`${proto}://${host}/checkin?g=${gym.id}&t=${token}`, { type: "svg", margin: 1, errorCorrectionLevel: "M" });

  return (
    <main className="fixed inset-0 z-50 flex flex-col items-center justify-center gap-8 overflow-auto bg-white p-8 text-center text-black">
      <style>{"@page { size: A4; margin: 12mm; } @media print { body { background: #fff !important; } }"}</style>
      <h1 className="text-4xl font-extrabold leading-[1.6] sm:text-5xl">{gym.name}</h1>
      <div className="w-full max-w-[520px] [&>svg]:h-auto [&>svg]:w-full" role="img" aria-label="QR code حضور" dangerouslySetInnerHTML={{ __html: svg }} />
      <p className="text-3xl font-bold leading-[1.8]">برای ثبت حضور، QR را با دوربین گوشی اسکن کنید</p>
      <p className="text-lg leading-8 text-neutral-600">ابتدا وارد حساب خود شوید، سپس دکمه‌ی «ثبت حضور من» را بزنید.</p>
      <PrintButton />
    </main>
  );
}
