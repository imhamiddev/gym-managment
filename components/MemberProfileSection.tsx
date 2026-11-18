"use client";

import { useState, useTransition } from "react";
import { FormError } from "@/components/FormError";
import { JalaliField } from "@/components/JalaliField";
import { Button } from "@/components/ui/button";
import { Icon } from "@/components/ui/icon";
import { Input } from "@/components/ui/input";
import { useToast } from "@/components/ui/toast";
import { updateMemberProfile } from "@/lib/actions";
import { ageOn, formatJalali, jalaliToYmd, toFa, ymdTehran, ymdToJalali } from "@/lib/date";
import type { MemberView } from "@/lib/queries";

const area =
  "min-h-24 w-full resize-y rounded-3xl border border-border bg-transparent px-3.5 py-3 text-base leading-7 outline-none transition-colors placeholder:text-muted-foreground/60 focus:border-foreground/40 focus:ring-2 focus:ring-ring/40";

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="flex flex-col gap-1.5">
      <span className="px-1 text-sm font-medium">{label}</span>
      {children}
    </div>
  );
}

/** Coach view of a member's sporting profile (height, weight, birthday, goal, program, private notes). */
export function MemberProfileSection({ member }: { member: MemberView }) {
  const toast = useToast();
  const [editing, setEditing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [pending, start] = useTransition();
  const [f, setF] = useState({ height: "", weight: "", birth: "", goal: "", program: "", notes: "" });

  const today = ymdTehran(new Date());
  const begin = () => {
    setError(null);
    setF({
      height: member.heightCm ? String(member.heightCm) : "",
      weight: member.weightKg ? String(member.weightKg) : "",
      birth: member.birthYmd ? ymdToJalali(member.birthYmd) : "",
      goal: member.goal ?? "",
      program: member.program ?? "",
      notes: member.notes ?? "",
    });
    setEditing(true);
  };

  const save = () =>
    start(async () => {
      const birthYmd = f.birth.trim() ? jalaliToYmd(f.birth) : "";
      if (birthYmd === null) return setError("تاریخ تولد معتبر نیست.");
      const r = await updateMemberProfile({
        membershipId: member.id,
        heightCm: f.height,
        weightKg: f.weight,
        goal: f.goal,
        program: f.program,
        notes: f.notes,
        birthYmd,
      });
      if (r.error) setError(r.error);
      else {
        toast.success("مشخصات ورزشی ذخیره شد");
        setEditing(false);
      }
    });

  if (editing) {
    return (
      <section className="mt-4 flex flex-col gap-4 rounded-3xl border border-border bg-card p-4" aria-label="ویرایش مشخصات ورزشی">
        <h3 className="text-sm font-semibold">مشخصات ورزشی</h3>
        <div className="grid grid-cols-2 gap-3">
          <Input label="قد (سانتی‌متر)" dir="ltr" inputMode="numeric" placeholder="175" value={f.height} onChange={(v) => setF({ ...f, height: v })} />
          <Input label="وزن (کیلوگرم)" dir="ltr" inputMode="decimal" placeholder="72.5" value={f.weight} onChange={(v) => setF({ ...f, weight: v })} />
        </div>
        <JalaliField label="تاریخ تولد" value={f.birth} onChange={(v) => setF({ ...f, birth: v })} hideHint />
        <Input label="هدف ورزشی" placeholder="مثلاً کاهش وزن" maxLength={80} value={f.goal} onChange={(v) => setF({ ...f, goal: v })} />
        <Field label="برنامه تمرینی (به ورزشکار نمایش داده می‌شود)">
          <textarea className={area} maxLength={2000} value={f.program} onChange={(e) => setF({ ...f, program: e.target.value })} placeholder="روز اول: سینه و پشت بازو..." />
        </Field>
        <Field label="یادداشت خصوصی مربی (ورزشکار نمی‌بیند)">
          <textarea className={area} maxLength={1000} value={f.notes} onChange={(e) => setF({ ...f, notes: e.target.value })} />
        </Field>
        <FormError message={error} />
        <div className="flex gap-2">
          <Button variant="secondary" className="flex-1" onClick={() => setEditing(false)}>انصراف</Button>
          <Button className="flex-[2]" loading={pending} onClick={save}>ذخیره</Button>
        </div>
      </section>
    );
  }

  const empty = !member.heightCm && !member.weightKg && !member.birthYmd && !member.goal && !member.program && !member.notes;
  return (
    <section className="mt-4 rounded-3xl border border-border bg-card p-4" aria-label="مشخصات ورزشی">
      <div className="mb-2 flex items-center justify-between gap-2">
        <h3 className="text-sm font-semibold">مشخصات ورزشی</h3>
        <Button size="sm" variant="ghost" onClick={begin}>
          <Icon name="edit" size={16} />
          {empty ? "افزودن" : "ویرایش"}
        </Button>
      </div>
      {empty ? (
        <p className="text-sm leading-7 text-muted-foreground">قد، وزن، تاریخ تولد، هدف و برنامه‌ی تمرینی عضو را اینجا ثبت کنید.</p>
      ) : (
        <dl className="grid grid-cols-2 gap-x-4 gap-y-3 text-sm">
          {member.heightCm ? <Item label="قد">{toFa(member.heightCm)} سانتی‌متر</Item> : null}
          {member.weightKg ? <Item label="وزن">{toFa(member.weightKg)} کیلوگرم</Item> : null}
          {member.birthYmd ? (
            <Item label="تاریخ تولد">
              <span className="ltr">{formatJalali(member.birthYmd)}</span> ({toFa(ageOn(member.birthYmd, today))} ساله)
            </Item>
          ) : null}
          {member.goal ? <Item label="هدف">{member.goal}</Item> : null}
          {member.program ? <Item label="برنامه تمرینی" wide><span className="whitespace-pre-wrap leading-7">{member.program}</span></Item> : null}
          {member.notes ? <Item label="یادداشت خصوصی" wide><span className="whitespace-pre-wrap leading-7">{member.notes}</span></Item> : null}
        </dl>
      )}
    </section>
  );
}

function Item({ label, wide, children }: { label: string; wide?: boolean; children: React.ReactNode }) {
  return (
    <div className={wide ? "col-span-2" : undefined}>
      <dt className="text-xs text-muted-foreground">{label}</dt>
      <dd className="font-medium">{children}</dd>
    </div>
  );
}
