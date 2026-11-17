import type { ReactNode } from "react";
import { cn } from "@/lib/utils";

/** Helper text under an Input. */
export function FieldHint({ children, className }: { children: ReactNode; className?: string }) {
  return <p className={cn("-mt-0.5 px-1 text-xs leading-5 text-muted-foreground", className)}>{children}</p>;
}
