import { headers } from "next/headers";
import QRCode from "qrcode";
import { QrPoster } from "@/components/QrPoster";
import { requireUser } from "@/lib/auth";
import { ensureCheckinToken, findCoachGym } from "@/lib/coach";
import { redirect } from "next/navigation";

export const metadata = { title: "چاپ QR حضور" };
export const dynamic = "force-dynamic";

/** Shell-free print page for the gym's check-in QR, with a black-and-white and a colour poster. */
export default async function QrPrintPage({ searchParams }: { searchParams: Promise<{ v?: string; s?: string }> }) {
  const user = await requireUser("COACH");
  const found = await findCoachGym(user.id);
  if (!found) redirect("/coach");
  const { gym } = found;
  const token = await ensureCheckinToken(gym);
  const h = await headers();
  const host = h.get("x-forwarded-host") ?? h.get("host") ?? "localhost:3000";
  const proto = h.get("x-forwarded-proto") ?? (host.startsWith("localhost") ? "http" : "https");
  const url = `${proto}://${host}/checkin?g=${gym.id}&t=${token}`;

  // Quiet zone (margin 4) is baked into the SVG, and level Q tolerates dirt/scuffs on a wall poster.
  // The colour version keeps a very dark blue (≈10:1 on white) so every scanner still reads it.
  const base = { type: "svg" as const, margin: 4, errorCorrectionLevel: "Q" as const };
  const [svgBw, svgColor] = await Promise.all([
    QRCode.toString(url, { ...base, color: { dark: "#000000", light: "#ffffff" } }),
    QRCode.toString(url, { ...base, color: { dark: "#0a3f8f", light: "#ffffff" } }),
  ]);

  const { v, s } = await searchParams;
  return (
    <QrPoster
      gymName={gym.name}
      svgBw={svgBw}
      svgColor={svgColor}
      initialMode={v === "bw" ? "bw" : "color"}
      initialSize={s === "a5" ? "a5" : "a4"}
    />
  );
}
