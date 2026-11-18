"use client";

import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Icon } from "@/components/ui/icon";
import { useToast } from "@/components/ui/toast";
import { formatCard } from "@/lib/gym";

/** Gym's bank card for tuition transfers, with one-tap copy (copies Latin digits). */
export function CardNumberCard({ card }: { card: string }) {
  const toast = useToast();
  const [copied, setCopied] = useState(false);
  const copy = async () => {
    try {
      await navigator.clipboard.writeText(card);
      setCopied(true);
      toast.success("شماره کارت کپی شد");
      setTimeout(() => setCopied(false), 1800);
    } catch {
      toast.error("کپی انجام نشد", "شماره کارت را دستی کپی کنید.");
    }
  };
  return (
    <section className="flex flex-col gap-3 rounded-[28px] border border-tuition/25 bg-tuition/8 p-5 sm:flex-row sm:items-center sm:justify-between sm:p-6">
      <div className="flex items-center gap-4">
        <span className="grid size-12 shrink-0 place-items-center rounded-2xl bg-tuition/15 text-tuition">
          <Icon name="wallet" size={24} set="bulk" />
        </span>
        <div className="min-w-0">
          <p className="text-sm text-muted-foreground">شماره کارت برای واریز شهریه</p>
          <p className="ltr text-xl font-bold leading-9 tracking-wider sm:text-2xl">{formatCard(card)}</p>
        </div>
      </div>
      <Button variant="soft" onClick={copy}>
        <Icon name={copied ? "check" : "copy"} size={18} />
        {copied ? "کپی شد" : "کپی شماره کارت"}
      </Button>
    </section>
  );
}
