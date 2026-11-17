import type { ReactNode } from "react";
import { cn } from "@/lib/utils";

export type Tone = "primary" | "members" | "tuition" | "attendance" | "success" | "warning" | "danger" | "neutral";

/** Static class names so Tailwind can see every combination. */
export const TONE: Record<Tone, { soft: string; text: string; solid: string; dot: string; ring: string }> = {
  primary: { soft: "bg-primary/12 text-primary", text: "text-primary", solid: "bg-primary text-primary-foreground", dot: "bg-primary", ring: "ring-primary/25" },
  members: { soft: "bg-members/12 text-members", text: "text-members", solid: "bg-members text-white dark:text-[#160a2b]", dot: "bg-members", ring: "ring-members/25" },
  tuition: { soft: "bg-tuition/12 text-tuition", text: "text-tuition", solid: "bg-tuition text-white dark:text-[#04201e]", dot: "bg-tuition", ring: "ring-tuition/25" },
  attendance: { soft: "bg-attendance/12 text-attendance", text: "text-attendance", solid: "bg-attendance text-white dark:text-[#2a0716]", dot: "bg-attendance", ring: "ring-attendance/25" },
  success: { soft: "bg-success/12 text-success", text: "text-success", solid: "bg-success text-white dark:text-[#052313]", dot: "bg-success", ring: "ring-success/25" },
  warning: { soft: "bg-warning/14 text-warning", text: "text-warning", solid: "bg-warning text-white dark:text-[#2b1c00]", dot: "bg-warning", ring: "ring-warning/25" },
  danger: { soft: "bg-danger/12 text-danger", text: "text-danger", solid: "bg-danger text-white dark:text-[#2a0a0a]", dot: "bg-danger", ring: "ring-danger/25" },
  neutral: { soft: "bg-card text-muted-foreground", text: "text-muted-foreground", solid: "bg-foreground text-background", dot: "bg-muted-foreground", ring: "ring-border-strong" },
};

export function Badge({
  tone = "neutral",
  dot = false,
  className,
  children,
}: {
  tone?: Tone;
  dot?: boolean;
  className?: string;
  children: ReactNode;
}) {
  return (
    <span className={cn("inline-flex h-7 items-center gap-1.5 whitespace-nowrap rounded-full px-3 text-xs font-medium", TONE[tone].soft, className)}>
      {dot ? <span aria-hidden className={cn("size-1.5 rounded-full", TONE[tone].dot)} /> : null}
      {children}
    </span>
  );
}
