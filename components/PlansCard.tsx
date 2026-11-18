"use client";

import { useState, useTransition } from "react";
import { DurationPicker } from "@/components/DurationPicker";
import { FormError } from "@/components/FormError";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Icon } from "@/components/ui/icon";
import { Input } from "@/components/ui/input";
import { useToast } from "@/components/ui/toast";
import { deletePlan, savePlan } from "@/lib/actions";
import { formatDays, formatToman, toEn } from "@/lib/date";

export type PlanView = { id: string; name: string; amount: number; durationDays: number };
type Draft = { id?: string; name: string; amount: string; duration: string };

const groupDigits = (raw: string) => {
  const d = toEn(raw).replace(/[^\d]/g, "");
  return d ? Number(d).toLocaleString("en-US") : "";
};

/** Owner-only: define reusable tuition plans (name + amount + duration). */
export function PlansCard({ plans }: { plans: PlanView[] }) {
  const toast = useToast();
  const [draft, setDraft] = useState<Draft | null>(null);
  const [confirmId, setConfirmId] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [pending, start] = useTransition();

  const save = () =>
    start(async () => {
      if (!draft) return;
      const amount = Number(toEn(draft.amount).replace(/[^\d]/g, ""));
      const r = await savePlan({ id: draft.id, name: draft.name, amount, durationDays: Number(draft.duration) });
      if (r.error) setError(r.error);
      else {
        toast.success(draft.id ? "پلن ذخیره شد" : "پلن اضافه شد", draft.name);
        setDraft(null);
      }
    });

  const remove = (p: PlanView) =>
    start(async () => {
      const r = await deletePlan(p.id);
      if (r.error) setError(r.error);
      else {
        toast.success("پلن حذف شد", p.name);
        setConfirmId(null);
      }
    });

  return (
    <Card className="shadow-card">
      <CardHeader>
        <CardTitle as="h2" className="text-base">پلن‌های شهریه</CardTitle>
        <CardDescription>مبلغ و مدت‌های پرکاربرد را یک‌بار تعریف کنید تا موقع ثبت شهریه با یک ضربه انتخاب شوند. پرداخت‌های قبلی تغییری نمی‌کنند.</CardDescription>
      </CardHeader>
      <CardContent className="flex flex-col gap-4">
        {plans.length ? (
          <ul className="divide-y divide-border overflow-hidden rounded-2xl border border-border">
            {plans.map((p) => (
              <li key={p.id} className="flex items-center gap-2 px-3.5 py-3">
                <div className="min-w-0 flex-1">
                  <p className="truncate font-medium">{p.name}</p>
                  <p className="text-xs text-muted-foreground">
                    {formatToman(p.amount)} · {formatDays(p.durationDays)}
                  </p>
                </div>
                {confirmId === p.id ? (
                  <div className="flex items-center gap-1.5">
                    <Button size="sm" variant="secondary" onClick={() => setConfirmId(null)}>خیر</Button>
                    <Button size="sm" variant="danger" loading={pending} onClick={() => remove(p)}>حذف</Button>
                  </div>
                ) : (
                  <>
                    <Button size="icon-sm" variant="ghost" aria-label={`ویرایش ${p.name}`} onClick={() => { setError(null); setDraft({ id: p.id, name: p.name, amount: String(p.amount), duration: String(p.durationDays) }); }}>
                      <Icon name="edit" size={18} />
                    </Button>
                    <Button size="icon-sm" variant="ghost" aria-label={`حذف ${p.name}`} className="hover:bg-danger/10 hover:text-danger" onClick={() => setConfirmId(p.id)}>
                      <Icon name="delete" size={18} />
                    </Button>
                  </>
                )}
              </li>
            ))}
          </ul>
        ) : !draft ? (
          <p className="rounded-2xl bg-card px-4 py-5 text-center text-sm text-muted-foreground">هنوز پلنی تعریف نشده است.</p>
        ) : null}

        {draft ? (
          <div className="flex flex-col gap-4 rounded-3xl border border-border bg-card p-4">
            <Input label="نام پلن" placeholder="مثلاً ماهانه" value={draft.name} onChange={(v) => setDraft({ ...draft, name: v })} maxLength={40} />
            <Input
              label="مبلغ (تومان)"
              dir="ltr"
              inputMode="numeric"
              placeholder="1,500,000"
              value={groupDigits(draft.amount)}
              onChange={(v) => setDraft({ ...draft, amount: v })}
              leftIcon={<Icon name="wallet" size={18} />}
            />
            <div className="flex flex-col gap-1.5">
              <span className="px-1 text-sm font-medium">مدت</span>
              <DurationPicker value={draft.duration} onChange={(v) => setDraft({ ...draft, duration: v })} startYmd={null} />
            </div>
            <FormError message={error} />
            <div className="flex gap-2">
              <Button variant="secondary" className="flex-1" onClick={() => setDraft(null)}>انصراف</Button>
              <Button className="flex-[2]" loading={pending} onClick={save}>{draft.id ? "ذخیره پلن" : "افزودن پلن"}</Button>
            </div>
          </div>
        ) : (
          <>
            <FormError message={error} />
            <Button variant="secondary" onClick={() => { setError(null); setDraft({ name: "", amount: "", duration: "30" }); }}>
              <Icon name="plus" size={18} />
              پلن جدید
            </Button>
          </>
        )}
      </CardContent>
    </Card>
  );
}
