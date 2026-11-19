"use client";

import Link from "next/link";
import { type ReactNode, useState } from "react";
import { Button } from "@/components/ui/button";
import { Icon } from "@/components/ui/icon";
import { cn } from "@/lib/utils";

export type QrMode = "bw" | "color";
export type QrSize = "a4" | "a5";

/** `sheet` is the printable area (paper minus @page margins), in mm. */
const SIZES: Record<QrSize, { label: string; desc: string; page: string; margin: number; sheet: [number, number] }> = {
  a4: { label: "A4", desc: "نصب روی دیوار یا در ورودی", page: "A4", margin: 8, sheet: [194, 279] },
  a5: { label: "A5", desc: "کوچک‌تر؛ مناسب میز پذیرش", page: "A5", margin: 6, sheet: [136, 198] },
};

const STEPS: { n: string; text: ReactNode }[] = [
  { n: "۱", text: "دوربین گوشی را روی QR بگیرید" },
  { n: "۲", text: "وارد حساب خود شوید" },
  { n: "۳", text: "حضور شما خودکار ثبت می‌شود" },
];

function Segmented<T extends string>({
  label,
  value,
  onChange,
  options,
}: {
  label: string;
  value: T;
  onChange: (v: T) => void;
  options: { value: T; title: string; desc: string; swatch?: string }[];
}) {
  return (
    <div className="flex flex-col gap-2">
      <span className="text-sm font-semibold leading-7 text-neutral-700">{label}</span>
      <div role="radiogroup" aria-label={label} className="grid grid-cols-2 gap-2">
        {options.map((o) => {
          const active = o.value === value;
          return (
            <button
              key={o.value}
              type="button"
              role="radio"
              aria-checked={active}
              onClick={() => onChange(o.value)}
              className={cn(
                "flex flex-col items-start gap-1.5 rounded-2xl border-2 p-3 text-start outline-none transition-colors focus-visible:ring-2 focus-visible:ring-blue-500/50",
                active ? "border-blue-500 bg-blue-50" : "border-neutral-200 bg-white hover:border-neutral-300",
              )}
            >
              <span className="flex items-center gap-2 font-bold leading-7">
                {o.swatch ? <span aria-hidden className="size-4 rounded-full border border-neutral-300" style={{ background: o.swatch }} /> : null}
                {o.title}
              </span>
              <span className="text-xs leading-6 text-neutral-600">{o.desc}</span>
            </button>
          );
        })}
      </div>
    </div>
  );
}

/**
 * Print screen for the gym's check-in QR: pick black-and-white or colour and
 * A4 or A5, preview the sheet, and print. Everything inside the sheet is sized
 * in container units (cqw), so the preview, the A4 page and the A5 page are the
 * same layout at different scales.
 */
export function QrPoster({
  gymName,
  svgBw,
  svgColor,
  initialMode = "color",
  initialSize = "a4",
}: {
  gymName: string;
  svgBw: string;
  svgColor: string;
  initialMode?: QrMode;
  initialSize?: QrSize;
}) {
  const [mode, setMode] = useState<QrMode>(initialMode);
  const [size, setSize] = useState<QrSize>(initialSize);
  const color = mode === "color";
  const svg = color ? svgColor : svgBw;
  const s = SIZES[size];

  // Poster palette. B&W uses pure black on white only, so nothing prints as a tint.
  const ink = color ? "#0b1b33" : "#000000";
  const muted = color ? "#4b5b73" : "#262626";
  const accent = color ? "#0a7cf0" : "#000000";

  const sync = (m: QrMode, z: QrSize) => {
    try {
      window.history.replaceState(null, "", `?v=${m}&s=${z}`);
    } catch {
      /* the URL is only a convenience */
    }
  };
  const chooseMode = (m: QrMode) => {
    setMode(m);
    sync(m, size);
  };
  const chooseSize = (z: QrSize) => {
    setSize(z);
    sync(mode, z);
  };

  const titleLength = gymName.length + 9; // "ثبت حضور " + name
  const titleSize = titleLength > 40 ? "5.6cqw" : titleLength > 28 ? "6.8cqw" : "8cqw";

  // The @page size and the sheet's printed dimensions follow the chosen paper.
  const printCss = `@page { size: ${s.page}; margin: ${s.margin}mm; } @media print { html, body { background: #fff !important; } .qr-sheet { width: ${s.sheet[0]}mm !important; height: ${s.sheet[1]}mm !important; max-width: none !important; aspect-ratio: auto !important; } .qr-sheet, .qr-sheet * { -webkit-print-color-adjust: exact; print-color-adjust: exact; } }`;

  return (
    <main className="fixed inset-0 z-50 overflow-auto bg-neutral-100 text-neutral-900 print:overflow-hidden print:bg-white">
      <style>{printCss}</style>

      <div className="mx-auto flex w-full max-w-5xl flex-col gap-6 p-4 sm:p-6 lg:flex-row lg:items-start print:block print:max-w-none print:p-0">
        {/* Controls — never printed */}
        <aside className="flex w-full flex-col gap-5 lg:sticky lg:top-6 lg:w-80 lg:shrink-0 print:hidden">
          <div className="flex flex-col gap-1">
            <h1 className="text-2xl font-extrabold leading-10">چاپ QR حضور</h1>
            <p className="text-sm leading-7 text-neutral-600">نسخه و اندازه‌ی کاغذ را انتخاب کنید و روی «چاپ» بزنید.</p>
          </div>

          <Segmented
            label="نسخه‌ی چاپ"
            value={mode}
            onChange={chooseMode}
            options={[
              { value: "bw", title: "سیاه و سفید", desc: "کم‌مصرف‌ترین حالت؛ مناسب هر پرینتری", swatch: "linear-gradient(90deg,#000 50%,#fff 50%)" },
              { value: "color", title: "رنگی", desc: "جذاب‌تر برای نصب روی دیوار باشگاه", swatch: "linear-gradient(135deg,#0a7cf0,#0a3f8f)" },
            ]}
          />

          <Segmented
            label="اندازه‌ی کاغذ"
            value={size}
            onChange={chooseSize}
            options={(Object.keys(SIZES) as QrSize[]).map((k) => ({ value: k, title: SIZES[k].label, desc: SIZES[k].desc }))}
          />

          <Button size="lg" onClick={() => window.print()}>
            <Icon name="document" size={18} />
            چاپ {s.label}
          </Button>

          <ul className="flex list-disc flex-col gap-1.5 ps-5 text-sm leading-7 text-neutral-600 marker:text-neutral-400">
            <li>در پنجره‌ی چاپ، اندازه‌ی کاغذ خودکار روی {s.label} تنظیم می‌شود؛ Scale را روی ۱۰۰٪ (Default) بگذارید.</li>
            <li>QR روی A4 حدود ۱۲ و روی A5 حدود ۸ سانتی‌متر است و از چند متری اسکن می‌شود.</li>
            <li>QR را هم‌سطح چشم و بدون بازتاب نور نصب کنید.</li>
          </ul>

          <Link href="/coach/attendance/qr" className="text-sm font-medium text-blue-600 hover:underline">
            بازگشت به صفحه‌ی QR
          </Link>
        </aside>

        {/* Paper sheet — preview on screen, the page itself in print */}
        <div className="flex min-w-0 flex-1 justify-center print:block">
          <div
            className="qr-sheet @container relative w-full max-w-[560px] overflow-hidden rounded-2xl bg-white text-center shadow-lg ring-1 ring-black/5 print:rounded-none print:shadow-none print:ring-0"
            style={{ color: ink, aspectRatio: `${s.sheet[0]} / ${s.sheet[1]}` }}
          >
            <div
              aria-hidden
              className="absolute"
              style={{ inset: "3cqw", border: `${color ? 1 : 0.7}cqw solid ${accent}`, borderRadius: "4cqw" }}
            />

            <div className="relative flex h-full flex-col items-center justify-between" style={{ padding: "10cqw 8cqw 9cqw", gap: "3cqw" }}>
              <h2 className="text-balance" style={{ margin: 0, fontSize: titleSize, lineHeight: 1.55, fontWeight: 800 }}>
                <span style={{ color: accent }}>ثبت حضور</span> {gymName}
              </h2>

              <div
                role="img"
                aria-label="QR code حضور"
                className="[&>svg]:block [&>svg]:h-auto [&>svg]:w-full"
                style={{
                  width: "64cqw",
                  padding: "2cqw",
                  background: "#fff",
                  borderRadius: "5cqw",
                  border: `${color ? 1.1 : 0.8}cqw solid ${accent}`,
                }}
                dangerouslySetInnerHTML={{ __html: svg }}
              />

              <ol className="grid w-full grid-cols-3" style={{ margin: 0, padding: 0, listStyle: "none", gap: "3cqw" }}>
                {STEPS.map((st) => (
                  <li key={st.n} className="flex flex-col items-center" style={{ gap: "2cqw" }}>
                    <span
                      style={{
                        display: "grid",
                        placeItems: "center",
                        width: "8.4cqw",
                        height: "8.4cqw",
                        borderRadius: "50%",
                        border: `0.5cqw solid ${accent}`,
                        background: color ? accent : "#fff",
                        color: color ? "#fff" : "#000",
                        fontSize: "4.2cqw",
                        fontWeight: 800,
                        lineHeight: 1,
                      }}
                    >
                      {st.n}
                    </span>
                    <span className="text-balance" style={{ fontSize: "3.3cqw", lineHeight: 1.7, fontWeight: 600, color: muted }}>
                      {st.text}
                    </span>
                  </li>
                ))}
              </ol>
            </div>
          </div>
        </div>
      </div>
    </main>
  );
}
