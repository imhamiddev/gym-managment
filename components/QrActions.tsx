"use client";

import { useState, useTransition } from "react";
import { FormError } from "@/components/FormError";
import { Button, ButtonLink } from "@/components/ui/button";
import { Icon } from "@/components/ui/icon";
import { useToast } from "@/components/ui/toast";
import { regenerateCheckinToken } from "@/lib/actions";

/** Print (opens the clean poster page) and, for the owner, regenerate the QR. */
export function QrActions({ canRegenerate }: { canRegenerate: boolean }) {
  const toast = useToast();
  const [confirming, setConfirming] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [pending, start] = useTransition();

  const regenerate = () =>
    start(async () => {
      setError(null);
      const r = await regenerateCheckinToken();
      if (r.error) setError(r.error);
      else {
        setConfirming(false);
        toast.success("QR جدید ساخته شد", "QR چاپ‌شده‌ی قبلی دیگر کار نمی‌کند.");
      }
    });

  return (
    <div className="flex w-full flex-col gap-3">
      <ButtonLink href="/qr-print" target="_blank" size="lg">
        <Icon name="document" size={18} />
        چاپ QR
      </ButtonLink>
      {canRegenerate ? (
        confirming ? (
          <div className="flex flex-col gap-2 rounded-2xl border border-danger/30 bg-danger/8 p-3">
            <p className="text-sm leading-7">QR جدید ساخته می‌شود و QR چاپ‌شده‌ی فعلی از کار می‌افتد. باید دوباره چاپش کنید. ادامه می‌دهید؟</p>
            <div className="flex gap-2">
              <Button variant="secondary" className="flex-1" onClick={() => setConfirming(false)}>انصراف</Button>
              <Button variant="danger" className="flex-1" loading={pending} onClick={regenerate}>بله، تعویض شود</Button>
            </div>
          </div>
        ) : (
          <Button variant="secondary" onClick={() => setConfirming(true)}>
            <Icon name="activity" size={18} />
            تعویض QR
          </Button>
        )
      ) : null}
      <FormError message={error} />
    </div>
  );
}
