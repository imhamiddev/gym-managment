"use client";

import { AnimatePresence, motion } from "motion/react";
import { useState, useTransition } from "react";
import { FormError } from "@/components/FormError";
import { JalaliField } from "@/components/JalaliField";
import { Avatar } from "@/components/ui/avatar";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Dialog, DialogActions } from "@/components/ui/dialog";
import { EmptyState } from "@/components/ui/empty-state";
import { Icon } from "@/components/ui/icon";
import { Input } from "@/components/ui/input";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { useToast } from "@/components/ui/toast";
import { deletePayment, savePayment } from "@/lib/actions";
import { formatDays, formatJalali, formatToman, jalaliToYmd, toEn, toFa, ymdAdd, ymdTehran, ymdToJalali } from "@/lib/date";
import type { PaymentView } from "@/lib/queries";

type Form = { id?: string; membershipId: string; amount: string; date: string; duration: string };

export function TuitionManager({
  members,
  payments,
}: {
  members: { id: string; name: string }[];
  payments: PaymentView[];
}) {
  const toast = useToast();
  const [form, setForm] = useState<Form | null>(null);
  const [toDelete, setToDelete] = useState<PaymentView | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [pending, start] = useTransition();

  const total = payments.reduce((sum, p) => sum + p.amount, 0);
  const nameParts = (name: string) => {
    const [first, ...rest] = name.split(" ");
    return [first ?? "", rest.join(" ")] as const;
  };

  const openNew = () => {
    setError(null);
    setForm({ membershipId: "", amount: "", date: ymdToJalali(ymdTehran(new Date())), duration: "30" });
  };
  const openEdit = (p: PaymentView) => {
    setError(null);
    setForm({ id: p.id, membershipId: p.membershipId, amount: String(p.amount), date: ymdToJalali(p.paidYmd), duration: String(p.durationDays) });
  };

  const submit = () =>
    start(async () => {
      if (!form) return;
      const amount = Number(toEn(form.amount).replace(/[^\d]/g, ""));
      const paidYmd = jalaliToYmd(form.date);
      if (!form.membershipId) return setError("عضو را انتخاب کنید.");
      if (!amount) return setError("مبلغ شهریه را وارد کنید.");
      if (!paidYmd) return setError("تاریخ پرداخت معتبر نیست.");
      const r = await savePayment({ id: form.id, membershipId: form.membershipId, amount, paidYmd, durationDays: Number(form.duration) });
      if (r.error) setError(r.error);
      else {
        toast.success(form.id ? "تغییرات شهریه ذخیره شد" : "شهریه ثبت شد", formatToman(amount));
        setForm(null);
      }
    });

  const confirmDelete = () =>
    start(async () => {
      if (!toDelete) return;
      const r = await deletePayment(toDelete.id);
      if (r.error) setError(r.error);
      else {
        toast.success("شهریه حذف شد");
        setToDelete(null);
      }
    });

  const amountDigits = form ? toEn(form.amount).replace(/[^\d]/g, "") : "";

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

      <div className="flex items-center justify-between gap-3">
        <h2 className="text-base font-semibold">تاریخچه پرداخت‌ها</h2>
        <Button onClick={openNew} disabled={members.length === 0} className="bg-tuition text-white hover:bg-tuition/90 dark:text-[#04201e]">
          <Icon name="plus" size={18} />
          ثبت شهریه
        </Button>
      </div>

      <Card className="gap-0 overflow-hidden py-0 shadow-card">
        {payments.length === 0 ? (
          <EmptyState
            icon="wallet"
            tone="tuition"
            title={members.length ? "هنوز شهریه‌ای ثبت نشده" : "ابتدا یک عضو لازم است"}
            description={members.length ? "اولین شهریه را ثبت کنید تا روزهای باقی‌مانده هر عضو محاسبه شود." : "حداقل یک ورزشکار را تایید کنید، بعد شهریه ثبت کنید."}
            action={members.length ? <Button onClick={openNew}>ثبت شهریه</Button> : undefined}
          />
        ) : (
          <>
            <div className="hidden grid-cols-[1.4fr_1fr_1fr_1fr_auto] gap-4 border-b border-border bg-card/60 px-5 py-3 text-xs font-medium text-muted-foreground md:grid">
              <span>عضو</span>
              <span>مبلغ</span>
              <span>تاریخ پرداخت</span>
              <span>پایان اشتراک</span>
              <span className="w-[84px]" />
            </div>
            <ul className="divide-y divide-border">
              <AnimatePresence initial={false}>
                {payments.map((p) => {
                  const [first, last] = nameParts(p.memberName);
                  return (
                    <motion.li
                      key={p.id}
                      layout="position"
                      initial={{ opacity: 0 }}
                      animate={{ opacity: 1 }}
                      exit={{ opacity: 0, x: -24 }}
                      transition={{ duration: 0.2 }}
                      className="grid grid-cols-[1fr_auto] items-center gap-x-3 gap-y-2 px-4 py-4 sm:px-5 md:grid-cols-[1.4fr_1fr_1fr_1fr_auto] md:gap-4"
                    >
                      <div className="flex min-w-0 items-center gap-3">
                        <Avatar name={p.memberName} firstName={first} lastName={last} size="sm" />
                        <div className="min-w-0">
                          <p className="truncate font-medium">{p.memberName}</p>
                          <p className="text-xs text-muted-foreground md:hidden">
                            <span className="ltr">{formatJalali(p.paidYmd)}</span> ({formatDays(p.durationDays)})
                          </p>
                        </div>
                      </div>
                      <p className="col-start-1 row-start-2 text-[15px] font-semibold text-tuition md:col-start-auto md:row-start-auto">
                        {formatToman(p.amount)}
                      </p>
                      <p className="hidden text-sm md:block">
                        {formatJalali(p.paidYmd)}
                        <Badge className="ms-2 h-6">{formatDays(p.durationDays)}</Badge>
                      </p>
                      <p className="hidden text-sm md:block">{formatJalali(ymdAdd(p.paidYmd, p.durationDays))}</p>
                      <div className="col-start-2 row-span-2 row-start-1 flex items-center gap-1 md:col-start-auto md:row-span-1 md:row-start-auto">
                        <Button size="icon-sm" variant="ghost" aria-label={`ویرایش شهریه ${p.memberName}`} onClick={() => openEdit(p)}>
                          <Icon name="edit" size={18} />
                        </Button>
                        <Button
                          size="icon-sm"
                          variant="ghost"
                          aria-label={`حذف شهریه ${p.memberName}`}
                          className="hover:bg-danger/10 hover:text-danger"
                          onClick={() => {
                            setError(null);
                            setToDelete(p);
                          }}
                        >
                          <Icon name="delete" size={18} />
                        </Button>
                      </div>
                    </motion.li>
                  );
                })}
              </AnimatePresence>
            </ul>
          </>
        )}
      </Card>

      <Dialog
        open={!!form}
        onOpenChange={(o) => !o && setForm(null)}
        title={form?.id ? "ویرایش شهریه" : "ثبت شهریه"}
        description="مبلغ به تومان و تاریخ به شمسی وارد شود."
        icon="wallet"
        tone="tuition"
      >
        {form ? (
          <div className="flex flex-col gap-4">
            <div className="flex flex-col gap-1.5">
              <span className="px-1 text-sm font-medium">عضو</span>
              <Select value={form.membershipId || undefined} onValueChange={(v) => setForm({ ...form, membershipId: v })} disabled={!!form.id}>
                <div className="relative">
                  <SelectTrigger className="h-11 text-start">
                    <SelectValue placeholder="انتخاب عضو" />
                  </SelectTrigger>
                  <SelectContent className="max-h-56 overflow-y-auto">
                    {members.map((m) => (
                      <SelectItem key={m.id} value={m.id}>
                        {m.name}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </div>
              </Select>
            </div>
            <Input
              label="مبلغ (تومان)"
              dir="ltr"
              inputMode="numeric"
              value={amountDigits ? Number(amountDigits).toLocaleString("en-US") : ""}
              onChange={(v) => setForm({ ...form, amount: v })}
              placeholder="1,500,000"
              leftIcon={<Icon name="wallet" size={18} />}
            />
            <JalaliField label="تاریخ پرداخت" value={form.date} onChange={(v) => setForm({ ...form, date: v })} />
            <div className="flex flex-col gap-1.5">
              <span className="px-1 text-sm font-medium">مدت شهریه</span>
              <Tabs value={form.duration} onValueChange={(v) => setForm({ ...form, duration: v })} variant="pill">
                <TabsList aria-label="مدت شهریه" className="w-full [&>*]:flex-1">
                  <TabsTrigger value="30">۳۰ روز</TabsTrigger>
                  <TabsTrigger value="60">۶۰ روز</TabsTrigger>
                  <TabsTrigger value="90">۹۰ روز</TabsTrigger>
                </TabsList>
              </Tabs>
            </div>
            <FormError message={error} />
            <DialogActions className="mt-1">
              <Button variant="secondary" onClick={() => setForm(null)}>
                انصراف
              </Button>
              <Button loading={pending} onClick={submit}>
                {form.id ? "ذخیره تغییرات" : "ثبت شهریه"}
              </Button>
            </DialogActions>
          </div>
        ) : null}
      </Dialog>

      <Dialog
        open={!!toDelete}
        onOpenChange={(o) => !o && setToDelete(null)}
        title="حذف این شهریه؟"
        description={toDelete ? `${toDelete.memberName}، ${formatToman(toDelete.amount)}. روزهای باقی‌مانده عضو دوباره حساب می‌شود.` : ""}
        icon="danger"
        tone="danger"
      >
        <FormError message={error} />
        <DialogActions className="mt-2">
          <Button variant="secondary" onClick={() => setToDelete(null)}>
            انصراف
          </Button>
          <Button variant="danger" loading={pending} onClick={confirmDelete}>
            حذف شهریه
          </Button>
        </DialogActions>
      </Dialog>
    </>
  );
}
