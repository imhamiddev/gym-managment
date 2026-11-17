import { cn } from "@/lib/utils";
import { TONE, type Tone } from "./badge";

const PALETTE: Tone[] = ["primary", "members", "tuition", "attendance", "warning", "success"];

function pick(name: string): Tone {
  let h = 0;
  for (const ch of name) h = (h * 31 + ch.codePointAt(0)!) >>> 0;
  return PALETTE[h % PALETTE.length];
}

export function initials(firstName: string, lastName: string) {
  const a = [...firstName.trim()][0] ?? "";
  const b = [...lastName.trim()][0] ?? "";
  return `${a}${b}`.trim() || "؟";
}

const SIZES = { sm: "size-9 text-xs", md: "size-11 text-sm", lg: "size-16 text-xl", xl: "size-24 text-3xl" } as const;

export function Avatar({
  name,
  firstName,
  lastName,
  size = "md",
  tone,
  className,
}: {
  /** Full name — used for the color. */
  name: string;
  firstName: string;
  lastName: string;
  size?: keyof typeof SIZES;
  tone?: Tone;
  className?: string;
}) {
  return (
    <span
      aria-hidden
      className={cn(
        "inline-grid shrink-0 place-items-center rounded-full font-semibold tracking-normal",
        TONE[tone ?? pick(name)].soft,
        SIZES[size],
        className,
      )}
    >
      {initials(firstName, lastName)}
    </span>
  );
}
