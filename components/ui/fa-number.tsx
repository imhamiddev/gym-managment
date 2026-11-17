"use client";

import { AnimatedNumber } from "./animated-number";
import { toFa } from "@/lib/date";

/** Counts up to `value` and shows Persian digits. */
export function FaNumber({ value, className }: { value: number; className?: string }) {
  return <AnimatedNumber value={value} duration={0.9} format={(n) => toFa(Math.round(n))} className={className} />;
}
