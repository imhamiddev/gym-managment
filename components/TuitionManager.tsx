"use client";

import { AnimatePresence, motion } from "motion/react";
import { useMemo, useState, useTransition } from "react";
import { DurationPicker } from "@/components/DurationPicker";
import { FormError } from "@/components/FormError";
import { JalaliField } from "@/components/JalaliField";
import type { PlanView } from "@/components/PlansCard";
import { RemainingBadge } from "@/components/RemainingBadge";
import { Avatar } from "@/components/ui/avatar";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Dialog } from "@/components/ui/dialog";
import { EmptyState } from "@/components/ui/empty-state";
import { Icon } from "@/components/ui/icon";
import { Input } from "@/components/ui/input";
import { useToast } from "@/components/ui/toast";
import { deletePayment, savePayment } from "@/lib/actions";
import { formatDays, formatJalali, formatToman, jalaliToYmd, toEn, toFa, ymdAdd, ymdTehran, ymdToJalali } from "@/lib/date";
import type { MemberView, PaymentView } from "@/lib/queries";

type Form = { id?: string; amount: string; date: string; duration: string };

const groupDigits = (raw: string) => {
  const d = toEn(raw).replace(/[^\d]/g, "");
  return d ? Number(d).toLocaleString("en-US") : "";
};

export function TuitionManager({ members, payments, plans }: { members: MemberView[]; payments: PaymentView[]; plans: PlanView[] }) {
  const toast = useToast();
  const [q, setQ] = useState("");
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [form, setForm] = useState<Form | null>(null);
  const [confirmId, setConfirmId] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [pending, start] = useTransition();

  const total = payments.reduce((sum, p) => sum + p.amount, 0);
  const selected = members.find((m) => m.id === selectedId) ?? null;
  const memberPayments = useMemo(() => payments.filter((p) => p.membershipId === selectedId), [payments, selectedId]);
  const list = useMemo(() => members.filter((m) => `${m.firstName} ${m.lastName}`.includes(q.trim())), [members, q]);

  const blankForm = (m: MemberView): Form => ({
    amount: m.lastPayment ? String(m.lastPayment.amount) : "",
    date: ymdToJalali(ymdTehran(new Date())),
    duration: String(m.lastPayment?.durationDays ?? 30),
  });

  const openMember = (m: MemberView) => {
    setError(null);
    setConfirmId(null);
    setSelectedId(m.id);
    setForm(blankForm(m));
  };
  const closeMember = () => {
    setSelectedId(null);
    setForm(null);
    setConfirmId(null);
    setError(null);
  };
  const startEdit = (p: PaymentView) => {
    setError(null);
    setConfirmId(null);
    setForm({ id: p.id, amount: String(p.amount), date: ymdToJalali(p.paidYmd), duration: String(p.durationDays) });
  };

  const submit = () =>
    start(async () => {
      if (!form || !selected) return;
      const amount = Number(toEn(form.amount).replace(/[^\d]/g, ""));
      const paidYmd = jalaliToYmd(form.date);
      if (!amount) return setError("مبلغ شهریه را وارد کنید.");
      if (!paidYmd) return setError("تاریخ پرداخت معتبر نیست.");
      const r = await savePayment({ id: form.id, membershipId: selected.id, amount, paidYmd, durationDays: Number(form.duration) });
      if (r.error) setError(r.error);
      else {
        toast.success(form.id ? "تغییرات شهریه ذخیره شد" : "شهریه ثبت شد", `${selected.firstName} ${selected.lastName} · ${formatToman(amount)}`);
        closeMember();
      }
    });

  const remove = (p: PaymentView) =>
    start(async () => {
      const r = await deletePayment(p.id);
      if (r.error) setError(r.error);
      else {
        toast.success("شهریه حذف شد");
        setConfirmId(null);
        if (form?.id === p.id && selected) setForm(blankForm(selected));
      }
    });

  const startYmd = form ? jalaliToYmd(form.date) : null;

  return (
    <>
      <div className="grid grid-cols-2 gap-3">
        <div className="rounded-3xl border border-border bg-background p-4 shadow-card">
          <p className="text-[13px] text-muted-foreground">پرداخت ثبت‌شده</p>
          <p className="text-2xl font-bold leading-10">{toFa(payments.length)}</p>
        </div>
        <div className="rounded-3xl border border-border bg-background p-4 shadow-card">
          <p className="text-[13px] text-muted-foreground">مجموع مبلغ</p>
          <p className="truncate text-xl font-bold leading-10 sm:text-2xl">{formatToman(total)}</p>
        </div>
      </div>

      <div className="flex flex-col gap-3">
        <h2 className="text-base font-semibold">اعضای باشگاه</h2>
        {members.length > 0 ? (
          <Input placeholder="جستجوی نام یا نام خانوادگی" aria-label="جستجوی عضو" value={q} onChange={setQ} leftIcon={<Icon name="search" size={18} />} />
        ) : null}
      </div>

      <Card className="gap-0 overflow-hidden py-0 shadow-card">
        {members.length === 0 ? (
          <EmptyState icon="wallet" tone="tuition" title="ابتدا یک عضو لازم است" description="حداقل یک ورزشکار را تایید کنید، بعد شهریه ثبت کنید." />
        ) : list.length === 0 ? (
          <EmptyState icon="search" tone="tuition" title="عضوی پیدا نشد" description="نام دیگری را جستجو کنید." />
        ) : (
          <ul className="divide-y divide-border">
            {list.map((m) => {
              const name = `${m.firstName} ${m.lastName}`;
              return (
                <li key={m.id}>
                  <button
                    type="button"
                    onClick={() => openMember(m)}
                    aria-label={`شهریه ${name}`}
                    className="group flex w-full items-center gap-3 px-4 py-3.5 text-start transition-colors hover:bg-card/70 active:bg-card sm:px-5"
                  >
                    <Avatar name={name} firstName={m.firstName} lastName={m.lastName} />
                    <span className="min-w-0 flex-1">
                      <span className="block truncate font-medium">{name}</span>
                      <span className="block truncate text-xs text-muted-foreground">
                        {m.lastPayment ? (
                          <>
                            آخرین پرداخت: {formatToman(m.lastPayment.amount)} · <span className="ltr">{formatJalali(m.lastPayment.paidYmd)}</span>
                          </>
                        ) : (
                          "هنوز شهریه‌ای ثبت نشده"
                        )}
                      </span>
                    </span>
                    <RemainingBadge days={m.remaining} />
                    <Icon name="chevronLeft" size={18} className="shrink-0 text-muted-foreground transition-transform group-hover:-translate-x-0.5" />
                  </button>
                </li>
              );
            })}
          </ul>
        )}
      </Card>

      <Dialog
        open={!!selected}
        onOpenChange={(o) => !o && closeMember()}
        title={selected ? `شهریه ${selected.firstName} ${selected.lastName}` : "شهریه"}
        description={selected ? (selected.remaining === null ? "هنوز شهریه‌ای ثبت نشده" : selected.remaining === 0 ? "شهریه منقضی شده است" : `${formatDays(selected.remaining)} از شهریه باقی مانده`) : undefined}
        icon="wallet"
        tone="tuition"
      >
        {selected && form ? (
          <div className="flex flex-col gap-5">
            <section className="flex flex-col gap-4" aria-label={form.id ? "ویرایش شهریه" : "ثبت شهریه جدید"}>
              <div className="flex items-center justify-between gap-2">
                <h3 className="text-sm font-semibold">{form.id ? "ویرایش پرداخت" : "ثبت شهریه جدید"}</h3>
                {form.id ? (
                  <Button size="sm" variant="ghost" onClick={() => { setForm(blankForm(selected)); setError(null); }}>
                    لغو ویرایش
                  </Button>
                ) : null}
              </div>
              {plans.length ? (
                <div className="flex flex-col gap-1.5">
                  <span className="px-1 text-sm font-medium">پلن آماده</span>
                  <div className="-mx-1 flex flex-wrap gap-2 px-1" role="group" aria-label="پلن‌های شهریه">
                    {plans.map((p) => {
                      const on = form.amount === String(p.amount) && form.duration === String(p.durationDays);
                      return (
                        <button
                          key={p.id}
                          type="button"
                          aria-pressed={on}
                          onClick={() => setForm({ ...form, amount: String(p.amount), duration: String(p.durationDays) })}
                          className={`flex flex-col items-start rounded-2xl border px-3.5 py-2 text-start transition-[background-color,border-color,transform] active:scale-95 ${on ? "border-tuition bg-tuition/10" : "border-border bg-card hover:border-border-strong"}`}
                        >
                          <span className="text-sm font-semibold leading-6">{p.name}</span>
                          <span className="text-[11px] leading-5 text-muted-foreground">{formatToman(p.amount)} · {formatDays(p.durationDays)}</span>
                        </button>
                      );
                    })}
                  </div>
                </div>
              ) : null}
              <Input
                label="مبلغ (تومان)"
                dir="ltr"
                inputMode="numeric"
                value={groupDigits(form.amount)}
                onChange={(v) => setForm({ ...form, amount: v })}
                placeholder="1,500,000"
                leftIcon={<Icon name="wallet" size={18} />}
              />
              <JalaliField label="تاریخ پرداخت" value={form.date} onChange={(v) => setForm({ ...form, date: v })} />
              <div className="flex flex-col gap-1.5">
                <span className="px-1 text-sm font-medium">مدت شهریه</span>
                <DurationPicker value={form.duration} onChange={(v) => setForm({ ...form, duration: v })} startYmd={startYmd} />
              </div>
              <FormError message={error} />
              <Button loading={pending} onClick={submit} className="w-full">
                {form.id ? "ذخیره تغییرات" : "ثبت شهریه"}
              </Button>
            </section>

            <section className="flex flex-col gap-2" aria-label="تاریخچه پرداخت‌ها">
              <h3 className="text-sm font-semibold">تاریخچه پرداخت‌ها</h3>
              {memberPayments.length === 0 ? (
                <p className="rounded-2xl bg-card px-4 py-5 text-center text-sm text-muted-foreground">برای این عضو پرداختی ثبت نشده است.</p>
              ) : (
                <ul className="divide-y divide-border overflow-hidden rounded-2xl border border-border">
                  <AnimatePresence initial={false}>
                    {memberPayments.map((p) => (
                      <motion.li key={p.id} layout="position" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0, x: -24 }} transition={{ duration: 0.2 }} className="px-3.5 py-3">
                        {confirmId === p.id ? (
                          <div className="flex items-center gap-2">
                            <span className="flex-1 text-sm">این پرداخت حذف شود؟</span>
                            <Button size="sm" variant="secondary" onClick={() => setConfirmId(null)}>خیر</Button>
                            <Button size="sm" variant="danger" loading={pending} onClick={() => remove(p)}>حذف</Button>
                          </div>
                        ) : (
                          <div className={`flex items-center gap-2 ${form.id === p.id ? "opacity-60" : ""}`}>
                            <div className="min-w-0 flex-1">
                              <p className="text-[15px] font-semibold text-tuition">{formatToman(p.amount)}</p>
                              <p className="text-xs text-muted-foreground">
                                <span className="ltr">{formatJalali(p.paidYmd)}</span> تا <span className="ltr">{formatJalali(ymdAdd(p.paidYmd, p.durationDays))}</span>
                              </p>
                            </div>
                            <Badge>{formatDays(p.durationDays)}</Badge>
                            <Button size="icon-sm" variant="ghost" aria-label="ویرایش پرداخت" onClick={() => startEdit(p)}>
                              <Icon name="edit" size={18} />
                            </Button>
                            <Button size="icon-sm" variant="ghost" aria-label="حذف پرداخت" className="hover:bg-danger/10 hover:text-danger" onClick={() => { setError(null); setConfirmId(p.id); }}>
                              <Icon name="delete" size={18} />
                            </Button>
                          </div>
                        )}
                      </motion.li>
                    ))}
                  </AnimatePresence>
                </ul>
              )}
            </section>
          </div>
        ) : null}
      </Dialog>
    </>
  );
}
