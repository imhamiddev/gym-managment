"use client";

import Link from "next/link";
import { useState, useTransition } from "react";
import { FormError } from "@/components/FormError";
import { Button } from "@/components/ui/button";
import { Icon } from "@/components/ui/icon";
import { checkIn } from "@/lib/actions";

export function CheckInButton({ gymId, token }: { gymId: string; token: string }) {
  const [done, setDone] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [pending, start] = useTransition();

  if (done) {
    return (
      <div className="flex flex-col items-center gap-4" role="status">
        <span className="grid size-16 place-items-center rounded-3xl bg-success/12 text-success">
          <Icon name="check" size={32} />
        </span>
        <p className="text-lg font-bold">حضور شما ثبت شد</p>
        <Link href="/athlete" className="text-sm font-medium text-primary underline-offset-4 hover:underline">رفتن به پنل من</Link>
      </div>
    );
  }
  return (
    <div className="flex w-full flex-col gap-3">
      <Button
        size="lg"
        loading={pending}
        onClick={() =>
          start(async () => {
            setError(null);
            const r = await checkIn(gymId, token);
            if (r.ok) setDone(true);
            else setError(r.error ?? "ثبت حضور انجام نشد.");
          })
        }
      >
        <Icon name="attendance" size={20} />
        ثبت حضور من
      </Button>
      <FormError message={error} />
    </div>
  );
}
