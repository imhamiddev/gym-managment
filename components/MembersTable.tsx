"use client";
import { useMemo, useState, useTransition } from "react";
import { Button } from "@/components/arc/button/button";
import { Input } from "@/components/arc/input/input";
import { Dialog, DialogClose, DialogContent } from "@/components/arc/dialog/dialog";
import { RemainingBadge } from "./RemainingBadge";
import { formatDays, formatJalali, formatToman, toFa } from "@/lib/date";
import { removeMember } from "@/lib/actions";
import type { MemberView } from "@/lib/queries";

export function MembersTable({ members }: { members: MemberView[] }) {
  const [selected, setSelected] = useState<MemberView | null>(null);
  const [open, setOpen] = useState(false);
  const [confirming, setConfirming] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [q, setQ] = useState("");
  const [pending, start] = useTransition();

  const list = useMemo(
    () => members.filter((m) => `${m.firstName} ${m.lastName}`.includes(q.trim())),
    [members, q],
  );

  const show = (m: MemberView) => { setSelected(m); setConfirming(false); setError(null); setOpen(true); };
  const remove = () =>
    start(async () => {
      if (!selected) return;
      const r = await removeMember(selected.id);
      if (r.error) setError(r.error);
      else setOpen(false);
    });

  return (
    <>
      <div style={{ maxWidth: 360 }}>
        <Input label="جستجوی عضو" value={q} onChange={(e) => setQ(e.target.value)} />
      </div>
      <div className="panel panel-flush">
        {list.length === 0 ? (
          <div className="empty">{members.length ? "عضوی با این نام پیدا نشد." : "هنوز عضوی ندارید. کد باشگاه را برای ورزشکاران بفرستید و درخواست‌ها را تایید کنید."}</div>
        ) : (
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr><th>نام و نام خانوادگی</th><th>شهریه باقی‌مانده</th></tr>
              </thead>
              <tbody>
                {list.map((m) => (
                  <tr key={m.id} className="clickable" onClick={() => show(m)}>
                    <td>
                      <button className="link-btn" onClick={(e) => { e.stopPropagation(); show(m); }}>
                        {m.firstName} {m.lastName}
                      </button>
                    </td>
                    <td><RemainingBadge days={m.remaining} /></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <Dialog open={open} onOpenChange={setOpen}>
        {selected && (
          <DialogContent title={`${selected.firstName} ${selected.lastName}`} description="مشخصات کامل عضو">
            <dl className="detail">
              <div><dt>نام</dt><dd>{selected.firstName}</dd></div>
              <div><dt>نام خانوادگی</dt><dd>{selected.lastName}</dd></div>
              <div><dt>شماره تلفن</dt><dd className="ltr">{selected.phone}</dd></div>
              <div><dt>تاریخ عضویت</dt><dd>{formatJalali(selected.joinedYmd)}</dd></div>
              <div><dt>شهریه باقی‌مانده</dt><dd><RemainingBadge days={selected.remaining} /></dd></div>
              <div>
                <dt>آخرین پرداخت</dt>
                <dd>
                  {selected.lastPayment
                    ? `${formatToman(selected.lastPayment.amount)}، ${formatJalali(selected.lastPayment.paidYmd)} (${formatDays(selected.lastPayment.durationDays)})`
                    : "ثبت نشده"}
                </dd>
              </div>
              <div><dt>حضور در ۳۰ روز اخیر</dt><dd>{toFa(selected.presentLast30)} جلسه</dd></div>
            </dl>
            {error && <p role="alert" className="form-error">{error}</p>}
            <div className="dialog-actions">
              {confirming ? (
                <>
                  <Button variant="secondary" onClick={() => setConfirming(false)}>انصراف</Button>
                  <Button variant="danger" loading={pending} onClick={remove}>بله، حذف شود</Button>
                </>
              ) : (
                <>
                  <Button variant="danger" onClick={() => setConfirming(true)}>حذف از باشگاه</Button>
                  <DialogClose asChild><Button variant="secondary">بستن</Button></DialogClose>
                </>
              )}
            </div>
          </DialogContent>
        )}
      </Dialog>
    </>
  );
}
