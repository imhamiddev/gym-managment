"use client";
import { useState, useTransition } from "react";
import { Button } from "@/components/arc/button/button";
import { Input } from "@/components/arc/input/input";
import { Select } from "@/components/arc/select/select";
import SegmentedControl from "@/components/arc/segmented-control/segmented-control";
import { Dialog, DialogClose, DialogContent } from "@/components/arc/dialog/dialog";
import { JalaliField } from "./JalaliField";
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
  const [form, setForm] = useState<Form | null>(null);
  const [toDelete, setToDelete] = useState<PaymentView | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [pending, start] = useTransition();

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
      else setForm(null);
    });

  const confirmDelete = () =>
    start(async () => {
      if (!toDelete) return;
      const r = await deletePayment(toDelete.id);
      if (r.error) setError(r.error);
      else setToDelete(null);
    });

  const amountDigits = form ? toEn(form.amount).replace(/[^\d]/g, "") : "";

  return (
    <>
      <div className="row-between">
        <p className="muted">{toFa(payments.length)} پرداخت ثبت شده</p>
        <Button onClick={openNew} disabled={members.length === 0}>ثبت شهریه</Button>
      </div>

      <div className="panel panel-flush">
        {payments.length === 0 ? (
          <div className="empty">{members.length ? "هنوز شهریه‌ای ثبت نشده است." : "ابتدا باید حداقل یک عضو داشته باشید."}</div>
        ) : (
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr><th>عضو</th><th>مبلغ</th><th>تاریخ پرداخت</th><th>مدت</th><th>پایان اشتراک</th><th /></tr>
              </thead>
              <tbody>
                {payments.map((p) => (
                  <tr key={p.id}>
                    <td>{p.memberName}</td>
                    <td>{formatToman(p.amount)}</td>
                    <td>{formatJalali(p.paidYmd)}</td>
                    <td>{formatDays(p.durationDays)}</td>
                    <td>{formatJalali(ymdAdd(p.paidYmd, p.durationDays))}</td>
                    <td>
                      <div className="row" style={{ flexWrap: "nowrap" }}>
                        <Button size="sm" variant="secondary" onClick={() => openEdit(p)}>ویرایش</Button>
                        <Button size="sm" variant="ghost" onClick={() => { setError(null); setToDelete(p); }}>حذف</Button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <Dialog open={!!form} onOpenChange={(o) => !o && setForm(null)}>
        {form && (
          <DialogContent title={form.id ? "ویرایش شهریه" : "ثبت شهریه"} description="مبلغ به تومان و تاریخ به شمسی وارد شود.">
            <div className="field-grid">
              <Select
                label="عضو"
                placeholder="انتخاب عضو"
                value={form.membershipId}
                onValueChange={(v) => setForm({ ...form, membershipId: v })}
                options={members.map((m) => ({ value: m.id, label: m.name }))}
                disabled={!!form.id}
              />
              <Input
                label="مبلغ (تومان)"
                dir="ltr"
                inputMode="numeric"
                value={amountDigits ? Number(amountDigits).toLocaleString("en-US") : ""}
                onChange={(e) => setForm({ ...form, amount: e.target.value })}
                placeholder="1,500,000"
              />
              <JalaliField label="تاریخ پرداخت" value={form.date} onChange={(v) => setForm({ ...form, date: v })} />
              <SegmentedControl
                label="مدت شهریه"
                fullWidth
                value={form.duration}
                onValueChange={(v) => setForm({ ...form, duration: v })}
                options={[
                  { value: "30", label: "۳۰ روز" },
                  { value: "60", label: "۶۰ روز" },
                  { value: "90", label: "۹۰ روز" },
                ]}
              />
            </div>
            {error && <p role="alert" className="form-error" style={{ marginTop: 12 }}>{error}</p>}
            <div className="dialog-actions">
              <DialogClose asChild><Button variant="secondary">انصراف</Button></DialogClose>
              <Button loading={pending} onClick={submit}>{form.id ? "ذخیره تغییرات" : "ثبت شهریه"}</Button>
            </div>
          </DialogContent>
        )}
      </Dialog>

      <Dialog open={!!toDelete} onOpenChange={(o) => !o && setToDelete(null)}>
        {toDelete && (
          <DialogContent title="حذف این شهریه؟" description={`${toDelete.memberName}، ${formatToman(toDelete.amount)}. روزهای باقی‌مانده عضو دوباره حساب می‌شود.`}>
            {error && <p role="alert" className="form-error">{error}</p>}
            <div className="dialog-actions">
              <DialogClose asChild><Button variant="secondary">انصراف</Button></DialogClose>
              <Button variant="danger" loading={pending} onClick={confirmDelete}>حذف</Button>
            </div>
          </DialogContent>
        )}
      </Dialog>
    </>
  );
}
