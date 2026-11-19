"use client";

import Link from "next/link";
import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Icon } from "@/components/ui/icon";
import { cn } from "@/lib/utils";

export type QrMode = "bw" | "color";

const OPTIONS: { value: QrMode; label: string; desc: string }[] = [
  { value: "bw", label: "سیاه و سفید", desc: "کم‌مصرف‌ترین حالت؛ مناسب هر پرینتری" },
  { value: "color", label: "رنگی", desc: "جذاب‌تر برای نصب روی دیوار باشگاه" },
];

const STEPS = [
  { n: "۱", text: "دوربین گوشی را روی QR بگیرید" },
  { n: "۲", text: "وارد حساب خود شوید" },
  { n: "۳", text: <><span className="whitespace-nowrap">«ثبت حضور من»</span> را بزنید</> },
];

/** Same dumbbell mark as the app logo, sized in poster units so it scales with the sheet. */
function Mark({ color }: { color: boolean }) {
  return (
    <span
      aria-hidden
      style={{
        display: "grid",
        placeItems: "center",
        width: "5.6cqw",
        height: "5.6cqw",
        borderRadius: "1.6cqw",
        background: color ? "#0a7cf0" : "#000",
        color: "#fff",
      }}
    >
      <svg viewBox="0 0 24 24" fill="currentColor" style={{ width: "62%", height: "62%" }}>
        <rect x="2" y="7" width="3.2" height="10" rx="1.4" />
        <rect x="5.6" y="9.4" width="2.4" height="5.2" rx="1" />
        <rect x="8.4" y="11" width="7.2" height="2" rx="1" />
        <rect x="16" y="9.4" width="2.4" height="5.2" rx="1" />
        <rect x="18.8" y="7" width="3.2" height="10" rx="1.4" />
      </svg>
    </span>
  );
}

/**
 * Print screen for the gym's check-in QR: pick the black-and-white or the
 * colour poster, preview it as an A4 sheet, and print. Everything inside the
 * sheet is sized in container units (cqw), so the preview and the printed
 * page are the same layout at different scales.
 */
export function QrPoster({
  gymName,
  svgBw,
  svgColor,
  initialMode = "color",
}: {
  gymName: string;
  svgBw: string;
  svgColor: string;
  initialMode?: QrMode;
}) {
  const [mode, setMode] = useState<QrMode>(initialMode);
  const color = mode === "color";
  const svg = color ? svgColor : svgBw;

  // Poster palette. B&W uses pure black on white only, so nothing prints as a tint.
  const ink = color ? "#0b1b33" : "#000000";
  const muted = color ? "#4b5b73" : "#262626";
  const accent = color ? "#0a7cf0" : "#000000";

  const choose = (m: QrMode) => {
    setMode(m);
    try {
      window.history.replaceState(null, "", `?v=${m}`);
    } catch {
      /* the URL is only a convenience */
    }
  };

  const nameSize = gymName.length > 28 ? "5.4cqw" : gymName.length > 18 ? "6.4cqw" : "7.6cqw";

  return (
    <main className="fixed inset-0 z-50 overflow-auto bg-neutral-100 text-neutral-900 print:overflow-hidden print:bg-white">
      <style>{
        "@page { size: A4; margin: 8mm; } @media print { html, body { background: #fff !important; } .qr-sheet, .qr-sheet * { -webkit-print-color-adjust: exact; print-color-adjust: exact; } }"
      }</style>

      <div className="mx-auto flex w-full max-w-5xl flex-col gap-6 p-4 sm:p-6 lg:flex-row lg:items-start print:block print:max-w-none print:p-0">
        {/* Controls — never printed */}
        <aside className="flex w-full flex-col gap-5 lg:sticky lg:top-6 lg:w-80 lg:shrink-0 print:hidden">
          <div className="flex flex-col gap-1">
            <h1 className="text-2xl font-extrabold leading-10">چاپ QR حضور</h1>
            <p className="text-sm leading-7 text-neutral-600">نسخه‌ی مورد نظر را انتخاب کنید و روی «چاپ» بزنید.</p>
          </div>

          <div role="radiogroup" aria-label="نسخه‌ی چاپ" className="grid grid-cols-2 gap-2">
            {OPTIONS.map((o) => {
              const active = o.value === mode;
              return (
                <button
                  key={o.value}
                  type="button"
                  role="radio"
                  aria-checked={active}
                  onClick={() => choose(o.value)}
                  className={cn(
                    "flex flex-col items-start gap-1.5 rounded-2xl border-2 p-3 text-start outline-none transition-colors focus-visible:ring-2 focus-visible:ring-blue-500/50",
                    active ? "border-blue-500 bg-blue-50" : "border-neutral-200 bg-white hover:border-neutral-300",
                  )}
                >
                  <span className="flex items-center gap-2 font-bold leading-7">
                    <span
                      aria-hidden
                      className="size-4 rounded-full border border-neutral-300"
                      style={{ background: o.value === "bw" ? "linear-gradient(90deg,#000 50%,#fff 50%)" : "linear-gradient(135deg,#0a7cf0,#0a3f8f)" }}
                    />
                    {o.label}
                  </span>
                  <span className="text-xs leading-6 text-neutral-600">{o.desc}</span>
                </button>
              );
            })}
          </div>

          <Button size="lg" onClick={() => window.print()}>
            <Icon name="document" size={18} />
            چاپ
          </Button>

          <ul className="flex list-disc flex-col gap-1.5 ps-5 text-sm leading-7 text-neutral-600 marker:text-neutral-400">
            <li>روی کاغذ A4 چاپ می‌شود؛ QR حدود ۱۰ سانتی‌متر است و از چند متری اسکن می‌شود.</li>
            <li>در پنجره‌ی چاپ، Scale را روی ۱۰۰٪ (Default) بگذارید.</li>
            <li>QR را هم‌سطح چشم و بدون بازتاب نور نصب کنید.</li>
          </ul>

          <Link href="/coach/attendance/qr" className="text-sm font-medium text-blue-600 hover:underline">
            بازگشت به صفحه‌ی QR
          </Link>
        </aside>

        {/* A4 sheet — preview on screen, the page itself in print */}
        <div className="flex min-w-0 flex-1 justify-center print:block">
          <div
            className="qr-sheet @container relative aspect-[194/279] w-full max-w-[560px] overflow-hidden rounded-2xl bg-white text-center shadow-lg ring-1 ring-black/5 print:aspect-auto print:h-[279mm] print:w-[194mm] print:max-w-none print:rounded-none print:shadow-none print:ring-0"
            style={{ color: ink }}
          >
            <div
              aria-hidden
              className="absolute"
              style={{ inset: "3cqw", border: `${color ? 1 : 0.7}cqw solid ${accent}`, borderRadius: "4cqw" }}
            />

            <div className="relative flex h-full flex-col items-center justify-between" style={{ padding: "9cqw 8cqw 7cqw", gap: "3cqw" }}>
              <div className="flex flex-col items-center" style={{ gap: "2.5cqw" }}>
                <span
                  style={{
                    fontSize: "3.2cqw",
                    lineHeight: 1.6,
                    fontWeight: 700,
                    padding: "0.4cqw 3.6cqw",
                    borderRadius: "999px",
                    border: `0.4cqw solid ${accent}`,
                    background: color ? accent : "#fff",
                    color: color ? "#fff" : "#000",
                  }}
                >
                  ثبت حضور
                </span>
                <h2 className="text-balance" style={{ margin: 0, fontSize: nameSize, lineHeight: 1.5, fontWeight: 800 }}>
                  {gymName}
                </h2>
              </div>

              <div
                role="img"
                aria-label="QR code حضور"
                className="[&>svg]:block [&>svg]:h-auto [&>svg]:w-full"
                style={{
                  width: "54cqw",
                  padding: "2cqw",
                  background: "#fff",
                  borderRadius: "5cqw",
                  border: `${color ? 1.1 : 0.8}cqw solid ${accent}`,
                }}
                dangerouslySetInnerHTML={{ __html: svg }}
              />

              <div className="flex w-full flex-col items-center" style={{ gap: "4.5cqw" }}>
                <p className="text-balance" style={{ margin: 0, fontSize: "5cqw", lineHeight: 1.6, fontWeight: 800 }}>
                  برای ثبت حضور، QR را با دوربین گوشی اسکن کنید
                </p>
                <ol className="grid w-full grid-cols-3" style={{ margin: 0, padding: 0, listStyle: "none", gap: "3cqw" }}>
                  {STEPS.map((s) => (
                    <li key={s.n} className="flex flex-col items-center" style={{ gap: "1.6cqw" }}>
                      <span
                        style={{
                          display: "grid",
                          placeItems: "center",
                          width: "7.4cqw",
                          height: "7.4cqw",
                          borderRadius: "50%",
                          border: `0.5cqw solid ${accent}`,
                          background: color ? accent : "#fff",
                          color: color ? "#fff" : "#000",
                          fontSize: "3.6cqw",
                          fontWeight: 800,
                          lineHeight: 1,
                        }}
                      >
                        {s.n}
                      </span>
                      <span className="text-balance" style={{ fontSize: "2.9cqw", lineHeight: 1.7, fontWeight: 600, color: muted }}>
                        {s.text}
                      </span>
                    </li>
                  ))}
                </ol>
              </div>

              <div className="flex items-center" style={{ gap: "1.8cqw", fontSize: "2.6cqw", color: muted }}>
                <Mark color={color} />
                <span style={{ fontWeight: 600 }}>ثبت حضور با باشگاه‌یار</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </main>
  );
}
