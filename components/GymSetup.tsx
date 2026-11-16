"use client";
import { useActionState, useState, useTransition } from "react";
import { Button } from "@/components/arc/button/button";
import { Input } from "@/components/arc/input/input";
import { createGym, joinGym, respondRequest, cancelRequest, type FormState } from "@/lib/actions";

export function CreateGymForm() {
  const [state, action, pending] = useActionState<FormState, FormData>(createGym, {});
  return (
    <form action={action} className="panel stack" style={{ maxWidth: 460 }}>
      <div>
        <h2>باشگاه خود را بسازید</h2>
        <p className="muted">بعد از ساخت، یک کد اختصاصی می‌گیرید و ورزشکاران با آن درخواست عضویت می‌دهند. هر مربی یک باشگاه می‌تواند داشته باشد.</p>
      </div>
      <Input label="نام باشگاه" name="name" required />
      {state.error && <p role="alert" className="form-error">{state.error}</p>}
      <div><Button type="submit" loading={pending}>ساخت باشگاه</Button></div>
    </form>
  );
}

export function JoinGymForm() {
  const [state, action, pending] = useActionState<FormState, FormData>(joinGym, {});
  return (
    <form action={action} className="panel stack" style={{ maxWidth: 460 }}>
      <div>
        <h2>عضو یک باشگاه شوید</h2>
        <p className="muted">کد باشگاه را از مربی‌تان بگیرید و اینجا وارد کنید. درخواست شما برای تایید به مربی می‌رسد.</p>
      </div>
      <Input label="کد باشگاه" name="code" dir="ltr" maxLength={6} autoCapitalize="characters" placeholder="ABC123" required />
      {state.error && <p role="alert" className="form-error">{state.error}</p>}
      <div><Button type="submit" loading={pending}>ارسال درخواست</Button></div>
    </form>
  );
}

export function CancelRequestButton() {
  const [pending, start] = useTransition();
  return (
    <Button variant="secondary" loading={pending} onClick={() => start(async () => { await cancelRequest(); })}>
      لغو درخواست
    </Button>
  );
}

export function CodeCopy({ code }: { code: string }) {
  const [copied, setCopied] = useState(false);
  return (
    <Button
      variant="secondary"
      size="sm"
      onClick={async () => {
        try { await navigator.clipboard.writeText(code); setCopied(true); setTimeout(() => setCopied(false), 1800); } catch {}
      }}
    >
      {copied ? "کپی شد" : "کپی کد"}
    </Button>
  );
}

export function RequestsList({ requests }: { requests: { id: string; name: string; phone: string }[] }) {
  const [busy, setBusy] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [, start] = useTransition();

  const respond = (id: string, approve: boolean) => {
    setBusy(id + approve);
    setError(null);
    start(async () => {
      const r = await respondRequest(id, approve);
      if (r.error) setError(r.error);
      setBusy(null);
    });
  };

  if (!requests.length) return <div className="empty">درخواست جدیدی ندارید.</div>;
  return (
    <div>
      {requests.map((r) => (
        <div className="att-row" key={r.id}>
          <div>
            <div>{r.name}</div>
            <div className="muted ltr" style={{ fontSize: 14, textAlign: "start" }}>{r.phone}</div>
          </div>
          <div className="row">
            <Button size="sm" loading={busy === r.id + true} onClick={() => respond(r.id, true)}>تایید</Button>
            <Button size="sm" variant="secondary" loading={busy === r.id + false} onClick={() => respond(r.id, false)}>لغو</Button>
          </div>
        </div>
      ))}
      {error && <p role="alert" className="form-error" style={{ padding: 16 }}>{error}</p>}
    </div>
  );
}
