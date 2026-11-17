"use client";

import { AnimatePresence, motion } from "motion/react";
import { useActionState, useState, useTransition } from "react";
import { FormError } from "@/components/FormError";
import { Avatar } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { EmptyState } from "@/components/ui/empty-state";
import { Icon } from "@/components/ui/icon";
import { Input } from "@/components/ui/input";
import { useToast } from "@/components/ui/toast";
import { cancelRequest, createGym, joinGym, respondRequest, type FormState } from "@/lib/actions";

export function CreateGymForm() {
  const [state, action, pending] = useActionState<FormState, FormData>(createGym, {});
  return (
    <Card className="mx-auto w-full max-w-lg shadow-card">
      <form action={action} className="flex flex-col gap-5">
        <CardHeader>
          <span className="mb-2 grid size-14 place-items-center rounded-2xl bg-primary/12 text-primary">
            <Icon name="home" size={28} set="bulk" />
          </span>
          <CardTitle as="h2" className="text-lg">باشگاه خود را بسازید</CardTitle>
          <CardDescription>
            بعد از ساخت، یک کد اختصاصی می‌گیرید و ورزشکاران با آن درخواست عضویت می‌دهند. هر مربی یک باشگاه می‌تواند داشته باشد.
          </CardDescription>
        </CardHeader>
        <CardContent className="flex flex-col gap-4">
          <Input label="نام باشگاه" name="name" placeholder="مثلا باشگاه آریا" required leftIcon={<Icon name="home" size={18} />} />
          <FormError message={state.error} />
          <Button type="submit" size="lg" loading={pending}>ساخت باشگاه</Button>
        </CardContent>
      </form>
    </Card>
  );
}

export function JoinGymForm() {
  const [state, action, pending] = useActionState<FormState, FormData>(joinGym, {});
  return (
    <Card className="mx-auto w-full max-w-lg shadow-card">
      <form action={action} className="flex flex-col gap-5">
        <CardHeader>
          <span className="mb-2 grid size-14 place-items-center rounded-2xl bg-members/12 text-members">
            <Icon name="ticket" size={28} set="bulk" />
          </span>
          <CardTitle as="h2" className="text-lg">عضو یک باشگاه شوید</CardTitle>
          <CardDescription>کد باشگاه را از مربی‌تان بگیرید و اینجا وارد کنید. درخواست شما برای تایید به مربی می‌رسد.</CardDescription>
        </CardHeader>
        <CardContent className="flex flex-col gap-4">
          <Input
            label="کد باشگاه"
            name="code"
            dir="ltr"
            maxLength={6}
            autoCapitalize="characters"
            autoComplete="off"
            placeholder="ABC123"
            required
            leftIcon={<Icon name="ticket" size={18} />}
            classNames={{ input: "uppercase tracking-[0.3em]" }}
          />
          <FormError message={state.error} />
          <Button type="submit" size="lg" loading={pending}>ارسال درخواست</Button>
        </CardContent>
      </form>
    </Card>
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
  const toast = useToast();
  return (
    <Button
      variant="soft"
      onClick={async () => {
        try {
          await navigator.clipboard.writeText(code);
          setCopied(true);
          toast.success("کد باشگاه کپی شد");
          setTimeout(() => setCopied(false), 1800);
        } catch {
          toast.error("کپی انجام نشد", "کد را به صورت دستی کپی کنید.");
        }
      }}
    >
      <span className="relative grid size-[18px] place-items-center">
        <AnimatePresence initial={false} mode="popLayout">
          <motion.span
            key={copied ? "ok" : "copy"}
            initial={{ opacity: 0, scale: 0.5, rotate: -30 }}
            animate={{ opacity: 1, scale: 1, rotate: 0 }}
            exit={{ opacity: 0, scale: 0.5 }}
            transition={{ duration: 0.16 }}
            className="col-start-1 row-start-1"
          >
            <Icon name={copied ? "check" : "copy"} size={18} />
          </motion.span>
        </AnimatePresence>
      </span>
      {copied ? "کپی شد" : "کپی کد"}
    </Button>
  );
}

export function RequestsList({ requests }: { requests: { id: string; name: string; phone: string }[] }) {
  const toast = useToast();
  const [busy, setBusy] = useState<string | null>(null);
  const [gone, setGone] = useState<Set<string>>(new Set());
  const [error, setError] = useState<string | null>(null);
  const [, start] = useTransition();

  const respond = (id: string, name: string, approve: boolean) => {
    setBusy(id + approve);
    setError(null);
    start(async () => {
      const r = await respondRequest(id, approve);
      if (r.error) setError(r.error);
      else {
        setGone((g) => new Set(g).add(id));
        toast.success(approve ? "عضویت تایید شد" : "درخواست لغو شد", name);
      }
      setBusy(null);
    });
  };

  const rows = requests.filter((r) => !gone.has(r.id));
  if (!rows.length && !error) {
    return <EmptyState icon="addUser" tone="members" title="درخواست جدیدی ندارید" description="وقتی ورزشکاری با کد باشگاه درخواست بدهد، اینجا نمایش داده می‌شود." className="py-10" />;
  }
  return (
    <div>
      <ul className="divide-y divide-border">
        <AnimatePresence initial={false}>
          {rows.map((r) => {
            const [first, ...rest] = r.name.split(" ");
            return (
              <motion.li
                key={r.id}
                layout="position"
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0, x: 32 }}
                transition={{ duration: 0.22 }}
                className="flex items-center gap-3 px-4 py-3.5 sm:px-5"
              >
                <Avatar name={r.name} firstName={first} lastName={rest.join(" ")} />
                <div className="min-w-0 flex-1">
                  <p className="truncate font-medium">{r.name}</p>
                  <p className="ltr text-xs text-muted-foreground">{r.phone}</p>
                </div>
                <div className="flex shrink-0 gap-2">
                  <Button size="icon" aria-label={`تایید ${r.name}`} loading={busy === r.id + true} onClick={() => respond(r.id, r.name, true)} className="bg-success text-white hover:bg-success/90 dark:text-[#052313]">
                    <Icon name="check" size={20} />
                  </Button>
                  <Button size="icon" variant="danger-soft" aria-label={`رد ${r.name}`} loading={busy === r.id + false} onClick={() => respond(r.id, r.name, false)}>
                    <Icon name="close" size={20} />
                  </Button>
                </div>
              </motion.li>
            );
          })}
        </AnimatePresence>
      </ul>
      <div className="px-4 pb-3 sm:px-5"><FormError message={error} /></div>
    </div>
  );
}
