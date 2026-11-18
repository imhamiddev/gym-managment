"use client";

import { useState, useTransition } from "react";
import { FormError } from "@/components/FormError";
import { Avatar } from "@/components/ui/avatar";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Icon } from "@/components/ui/icon";
import { Input } from "@/components/ui/input";
import { useToast } from "@/components/ui/toast";
import { addCoach, removeCoach } from "@/lib/actions";

type Coach = { userId: string; name: string; phone: string };

/** Owner-only: add assistant coaches by phone number (they must already have a coach account). */
export function CoachesCard({ owner, coaches }: { owner: string; coaches: Coach[] }) {
  const toast = useToast();
  const [phone, setPhone] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [confirmId, setConfirmId] = useState<string | null>(null);
  const [pending, start] = useTransition();

  const add = () =>
    start(async () => {
      setError(null);
      const r = await addCoach(phone);
      if (r.error) setError(r.error);
      else {
        toast.success("مربی به باشگاه اضافه شد");
        setPhone("");
      }
    });

  const remove = (c: Coach) =>
    start(async () => {
      const r = await removeCoach(c.userId);
      if (r.error) setError(r.error);
      else {
        toast.success("مربی از باشگاه حذف شد", c.name);
        setConfirmId(null);
      }
    });

  return (
    <Card className="shadow-card">
      <CardHeader>
        <CardTitle as="h2" className="text-base">مربیان باشگاه</CardTitle>
        <CardDescription>
          مربیان دستیار می‌توانند اعضا، شهریه و حضور و غیاب را مدیریت کنند، اما تنظیمات باشگاه فقط دست شماست. مربی باید قبلاً با نقش «مربی» ثبت‌نام کرده باشد.
        </CardDescription>
      </CardHeader>
      <CardContent className="flex flex-col gap-4">
        <ul className="divide-y divide-border overflow-hidden rounded-2xl border border-border">
          <li className="flex items-center gap-3 px-3.5 py-3">
            <Avatar name={owner} firstName={owner.split(" ")[0] ?? ""} lastName={owner.split(" ").slice(1).join(" ")} size="sm" />
            <span className="min-w-0 flex-1 truncate font-medium">{owner}</span>
            <Badge tone="primary">صاحب باشگاه</Badge>
          </li>
          {coaches.map((c) => (
            <li key={c.userId} className="flex items-center gap-3 px-3.5 py-3">
              <Avatar name={c.name} firstName={c.name.split(" ")[0] ?? ""} lastName={c.name.split(" ").slice(1).join(" ")} size="sm" />
              <div className="min-w-0 flex-1">
                <p className="truncate font-medium">{c.name}</p>
                <p className="ltr text-xs text-muted-foreground">{c.phone}</p>
              </div>
              {confirmId === c.userId ? (
                <div className="flex items-center gap-1.5">
                  <Button size="sm" variant="secondary" onClick={() => setConfirmId(null)}>خیر</Button>
                  <Button size="sm" variant="danger" loading={pending} onClick={() => remove(c)}>حذف</Button>
                </div>
              ) : (
                <Button size="icon-sm" variant="ghost" aria-label={`حذف ${c.name}`} className="hover:bg-danger/10 hover:text-danger" onClick={() => setConfirmId(c.userId)}>
                  <Icon name="delete" size={18} />
                </Button>
              )}
            </li>
          ))}
        </ul>
        <div className="flex items-end gap-2">
          <Input className="min-w-0 flex-1" label="شماره تلفن مربی" dir="ltr" inputMode="tel" placeholder="09121234567" value={phone} onChange={setPhone} leftIcon={<Icon name="call" size={18} />} />
          <Button onClick={add} loading={pending} disabled={!phone.trim()} className="shrink-0">
            <Icon name="addUser" size={18} />
            افزودن
          </Button>
        </div>
        <FormError message={error} />
      </CardContent>
    </Card>
  );
}
