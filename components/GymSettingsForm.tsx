"use client";

import { useState, useTransition } from "react";
import { FormError } from "@/components/FormError";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Icon } from "@/components/ui/icon";
import { Input } from "@/components/ui/input";
import { useToast } from "@/components/ui/toast";
import { suggestGymCode, updateGymSettings } from "@/lib/actions";
import { toEn, toFa } from "@/lib/date";
import { DAY_PRESETS, WEEKDAY_NAMES, WEEK_ORDER, cleanCard } from "@/lib/gym";
import { cn } from "@/lib/utils";

type Initial = { name: string; code: string; workDays: number[]; openTime: string; closeTime: string; cardNumber: string | null };

/** "6037991234567890" -> "6037 9912 3456 7890" while typing (Latin digits, max 16). */
const groupCard = (raw: string) => toEn(raw).replace(/\D/g, "").slice(0, 16).replace(/(\d{4})(?=\d)/g, "$1 ");

const sameSet = (a: number[], b: number[]) => a.length === b.length && a.every((d) => b.includes(d));

function Section({ title, description, children }: { title: string; description: string; children: React.ReactNode }) {
  return (
    <Card className="shadow-card">
      <CardHeader>
        <CardTitle as="h2" className="text-base">{title}</CardTitle>
        <CardDescription>{description}</CardDescription>
      </CardHeader>
      <CardContent className="flex flex-col gap-4">{children}</CardContent>
    </Card>
  );
}

export function GymSettingsForm({ initial }: { initial: Initial }) {
  const toast = useToast();
  const [name, setName] = useState(initial.name);
  const [code, setCode] = useState(initial.code);
  const [days, setDays] = useState<number[]>(initial.workDays);
  const [openTime, setOpenTime] = useState(initial.openTime);
  const [closeTime, setCloseTime] = useState(initial.closeTime);
  const [card, setCard] = useState(initial.cardNumber ? groupCard(initial.cardNumber) : "");
  const [error, setError] = useState<string | null>(null);
  const [pending, start] = useTransition();
  const [suggesting, startSuggest] = useTransition();

  const cardDigits = toEn(card).replace(/\D/g, "");
  const cardInvalid = cardDigits.length > 0 && !cleanCard(card);

  const toggleDay = (d: number) => setDays((cur) => (cur.includes(d) ? cur.filter((x) => x !== d) : [...cur, d]));

  const newCode = () =>
    startSuggest(async () => {
      const r = await suggestGymCode();
      if (r.code) setCode(r.code);
      else if (r.error) setError(r.error);
    });

  const save = () =>
    start(async () => {
      setError(null);
      const r = await updateGymSettings({ name, code, workDays: days, openTime, closeTime, cardNumber: card });
      if (r.error) setError(r.error);
      else toast.success("تنظیمات باشگاه ذخیره شد");
    });

  return (
    <div className="flex flex-col gap-5">
      <Section title="مشخصات باشگاه" description="نام باشگاه و کدی که ورزشکاران برای عضویت وارد می‌کنند.">
        <Input label="نام باشگاه" value={name} onChange={setName} maxLength={60} leftIcon={<Icon name="home" size={18} />} />
        <div className="flex flex-col gap-1.5">
          <div className="flex items-end gap-2">
            <Input
              className="min-w-0 flex-1"
              label="کد دعوت"
              dir="ltr"
              maxLength={6}
              autoCapitalize="characters"
              autoComplete="off"
              value={code}
              onChange={(v) => setCode(toEn(v).toUpperCase().replace(/[^A-Z0-9]/g, ""))}
              leftIcon={<Icon name="ticket" size={18} />}
              classNames={{ input: "uppercase tracking-[0.25em]" }}
            />
            <Button variant="secondary" onClick={newCode} loading={suggesting} className="shrink-0">
              کد جدید
            </Button>
          </div>
          <p className="px-1 text-xs leading-6 text-muted-foreground">۴ تا ۶ حرف انگلیسی یا عدد. با تغییر کد، کد قبلی دیگر کار نمی‌کند (اعضای فعلی تغییری نمی‌کنند).</p>
        </div>
      </Section>

      <Section title="روزها و ساعت کاری" description="حضور و غیاب فقط برای روزهایی که باشگاه فعال است ثبت می‌شود.">
        <div className="flex flex-wrap gap-2" role="group" aria-label="انتخاب سریع روزها">
          {DAY_PRESETS.map((p) => {
            const active = sameSet(p.days, days);
            return (
              <button
                key={p.key}
                type="button"
                aria-pressed={active}
                onClick={() => setDays([...p.days])}
                className={cn(
                  "h-10 rounded-full border px-4 text-sm font-medium transition-[background-color,color,transform] active:scale-95",
                  active ? "border-primary bg-primary text-primary-foreground" : "border-border-strong text-muted-foreground hover:bg-card hover:text-foreground",
                )}
              >
                {p.label}
              </button>
            );
          })}
        </div>
        <div className="grid grid-cols-4 gap-2 sm:grid-cols-7" role="group" aria-label="روزهای هفته">
          {WEEK_ORDER.map((d) => {
            const on = days.includes(d);
            return (
              <button
                key={d}
                type="button"
                aria-pressed={on}
                onClick={() => toggleDay(d)}
                className={cn(
                  "flex h-12 items-center justify-center rounded-2xl border text-[13px] font-medium transition-[background-color,color,transform] active:scale-95",
                  on ? "border-attendance/40 bg-attendance/12 text-attendance" : "border-border bg-card text-muted-foreground hover:text-foreground",
                )}
              >
                {WEEKDAY_NAMES[d]}
              </button>
            );
          })}
        </div>
        <div className="grid grid-cols-2 gap-3">
          <Input label="ساعت شروع" type="time" dir="ltr" value={openTime} onChange={setOpenTime} leftIcon={<Icon name="time" size={18} />} />
          <Input label="ساعت پایان" type="time" dir="ltr" value={closeTime} onChange={setCloseTime} leftIcon={<Icon name="time" size={18} />} />
        </div>
      </Section>

      <Section title="شماره کارت واریز شهریه" description="این شماره به ورزشکاران در پنلشان نمایش داده می‌شود. خالی بگذارید تا نمایش داده نشود.">
        <div className="flex flex-col gap-1.5">
          <Input
            label="شماره کارت (۱۶ رقم)"
            dir="ltr"
            inputMode="numeric"
            autoComplete="off"
            placeholder="6037 9912 3456 7890"
            value={card}
            onChange={(v) => setCard(groupCard(v))}
            error={cardInvalid ? "شماره کارت باید ۱۶ رقم باشد" : undefined}
            leftIcon={<Icon name="wallet" size={18} />}
          />
          <p className="px-1 text-xs text-muted-foreground">{toFa(cardDigits.length)} از ۱۶ رقم</p>
        </div>
      </Section>

      <FormError message={error} />
      <div className="glass sticky bottom-[max(0.75rem,env(safe-area-inset-bottom))] z-20 rounded-3xl border border-border-strong p-3 shadow-card">
        <Button size="lg" className="w-full" loading={pending} onClick={save}>
          <Icon name="check" size={18} />
          ذخیره تنظیمات
        </Button>
      </div>
    </div>
  );
}
