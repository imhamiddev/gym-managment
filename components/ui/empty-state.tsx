import type { ReactNode } from "react";
import { cn } from "@/lib/utils";
import { Icon, type IconName } from "./icon";
import { TONE, type Tone } from "./badge";

export function EmptyState({
  icon,
  tone = "neutral",
  title,
  description,
  action,
  className,
}: {
  icon: IconName;
  tone?: Tone;
  title: string;
  description?: ReactNode;
  action?: ReactNode;
  className?: string;
}) {
  return (
    <div className={cn("flex flex-col items-center gap-3 px-6 py-12 text-center", className)}>
      <span className={cn("grid size-16 place-items-center rounded-3xl", TONE[tone].soft)}>
        <Icon name={icon} size={30} set="bulk" />
      </span>
      <div className="max-w-sm">
        <p className="font-semibold">{title}</p>
        {description ? <p className="mt-1 text-sm leading-7 text-muted-foreground">{description}</p> : null}
      </div>
      {action ? <div className="mt-1">{action}</div> : null}
    </div>
  );
}
