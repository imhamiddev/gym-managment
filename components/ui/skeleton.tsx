import { cn } from "@/lib/utils";

/** Shimmering placeholder. Size it with className. */
export function Skeleton({ className }: { className?: string }) {
  return <div aria-hidden className={cn("skeleton rounded-xl", className)} />;
}
