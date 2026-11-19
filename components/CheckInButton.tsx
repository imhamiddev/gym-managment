"use client";

import Link from "next/link";
import { useCallback, useEffect, useRef, useState } from "react";
import { Button } from "@/components/ui/button";
import { Icon } from "@/components/ui/icon";
import { Loader } from "@/components/ui/loader";
import { checkIn } from "@/lib/actions";

type State = { kind: "loading" } | { kind: "done"; time: string } | { kind: "error"; message: string };

const timeFormat = new Intl.DateTimeFormat("fa-IR", { hour: "2-digit", minute: "2-digit", hour12: false, timeZone: "Asia/Tehran" });

/**
 * Records today's attendance as soon as the page opens (the member already
 * proved who they are by being signed in and scanning the gym's QR), then
 * shows a confirmation. checkIn() is an upsert, so reopening the link is safe.
 */
export function CheckInButton({ gymId, token }: { gymId: string; token: string }) {
  const [state, setState] = useState<State>({ kind: "loading" });
  const started = useRef(false);

  const run = useCallback(async () => {
    setState({ kind: "loading" });
    try {
      const r = await checkIn(gymId, token);
      if (r.ok) setState({ kind: "done", time: timeFormat.format(new Date()) });
      else setState({ kind: "error", message: r.error ?? "ثبت حضور انجام نشد." });
    } catch {
      setState({ kind: "error", message: "ارتباط برقرار نشد. اینترنت را بررسی کنید و دوباره تلاش کنید." });
    }
  }, [gymId, token]);

  useEffect(() => {
    // Guard against React Strict Mode's double effect so the check-in runs once per visit.
    if (started.current) return;
    started.current = true;
    void run();
  }, [run]);

  if (state.kind === "loading") {
    return (
      <div className="flex flex-col items-center gap-4 py-2" role="status" aria-live="polite">
        <Loader variant="spinner" size={32} />
        <p className="font-semibold leading-8">در حال ثبت حضور…</p>
      </div>
    );
  }

  if (state.kind === "done") {
    return (
      <div className="flex flex-col items-center gap-4" role="status" aria-live="polite">
        <span className="grid size-16 place-items-center rounded-3xl bg-success/12 text-success">
          <Icon name="check" size={32} />
        </span>
        <div className="flex flex-col gap-1">
          <p className="text-lg font-bold leading-8">حضور امروز ثبت شد</p>
          <p className="text-sm text-muted-foreground">ساعت {state.time}</p>
        </div>
        <Link href="/athlete" className="text-sm font-medium text-primary underline-offset-4 hover:underline">رفتن به پنل من</Link>
      </div>
    );
  }

  return (
    <div className="flex w-full flex-col items-center gap-4" role="alert">
      <span className="grid size-16 place-items-center rounded-3xl bg-danger/12 text-danger">
        <Icon name="close" size={32} />
      </span>
      <p className="font-semibold leading-8">{state.message}</p>
      <Button size="lg" className="w-full" onClick={() => void run()}>
        تلاش دوباره
      </Button>
      <Link href="/athlete" className="text-sm font-medium text-primary underline-offset-4 hover:underline">رفتن به پنل من</Link>
    </div>
  );
}
