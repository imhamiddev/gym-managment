import { cn } from "@/lib/utils";

/** Dumbbell mark for باشگاه‌یار. */
export function LogoMark({ className }: { className?: string }) {
  return (
    <span aria-hidden className={cn("grid size-10 place-items-center rounded-2xl bg-primary text-primary-foreground", className)}>
      <svg width="22" height="22" viewBox="0 0 24 24" fill="currentColor">
        <rect x="2" y="7" width="3.2" height="10" rx="1.4" />
        <rect x="5.6" y="9.4" width="2.4" height="5.2" rx="1" />
        <rect x="8.4" y="11" width="7.2" height="2" rx="1" />
        <rect x="16" y="9.4" width="2.4" height="5.2" rx="1" />
        <rect x="18.8" y="7" width="3.2" height="10" rx="1.4" />
      </svg>
    </span>
  );
}

export function Brand({ className }: { className?: string }) {
  return (
    <span className={cn("inline-flex items-center gap-2.5", className)}>
      <LogoMark />
      <span className="text-lg font-bold tracking-normal">باشگاه‌یار</span>
    </span>
  );
}
