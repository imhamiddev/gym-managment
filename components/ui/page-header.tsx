import type { ReactNode } from "react";
import { cn } from "@/lib/utils";
import { Icon, type IconName } from "./icon";
import { TONE, type Tone } from "./badge";

export function PageHeader({
  title,
  description,
  icon,
  tone = "primary",
  actions,
  className,
}: {
  title: string;
  description?: ReactNode;
  icon?: IconName;
  tone?: Tone;
  actions?: ReactNode;
  className?: string;
}) {
  return (
    <header className={cn("flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between", className)}>
      <div className="flex min-w-0 items-center gap-3.5">
        {icon ? (
          <span className={cn("grid size-12 shrink-0 place-items-center rounded-2xl", TONE[tone].soft)}>
            <Icon name={icon} size={24} set="bulk" />
          </span>
        ) : null}
        <div className="min-w-0">
          <h1 className="truncate text-xl font-bold leading-9 sm:text-2xl">{title}</h1>
          {description ? <p className="text-sm leading-6 text-muted-foreground">{description}</p> : null}
        </div>
      </div>
      {actions ? <div className="flex shrink-0 flex-wrap items-center gap-2 [&>*]:flex-1 sm:[&>*]:flex-none">{actions}</div> : null}
    </header>
  );
}
