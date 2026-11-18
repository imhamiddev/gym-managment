"use client";

import { AnimatePresence, motion } from "motion/react";
import { useMemo, useState, useTransition } from "react";
import { FormError } from "@/components/FormError";
import { MemberProfileSection } from "@/components/MemberProfileSection";
import { RemainingBadge } from "@/components/RemainingBadge";
import { Avatar } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Dialog, DialogActions } from "@/components/ui/dialog";
import { EmptyState } from "@/components/ui/empty-state";
import { Icon, type IconName } from "@/components/ui/icon";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { useToast } from "@/components/ui/toast";
import { removeMember } from "@/lib/actions";
import { formatDays, formatJalali, formatToman, isBirthday, toFa, ymdTehran } from "@/lib/date";
import type { MemberView } from "@/lib/queries";

type Filter = "all" | "active" | "expiring" | "expired";

const matches = (m: MemberView, f: Filter) =>
  f === "all" ||
  (f === "active" && (m.remaining ?? 0) > 7) ||
  (f === "expiring" && m.remaining !== null && m.remaining > 0 && m.remaining <= 7) ||
  (f === "expired" && !m.remaining);

function DetailRow({ icon, label, children }: { icon: IconName; label: string; children: React.ReactNode }) {
  return (
    <div className="flex items-center gap-3 py-3">
      <span className="grid size-9 shrink-0 place-items-center rounded-xl bg-card text-muted-foreground">
        <Icon name={icon} size={18} />
      </span>
      <dt className="text-sm text-muted-foreground">{label}</dt>
      <dd className="ms-auto text-end text-sm font-medium">{children}</dd>
    </div>
  );
}

export function MembersTable({ members }: { members: MemberView[] }) {
  const toast = useToast();
  const [selectedId, setSelectedId] = useState<string | null>(null);
  // Read from props so edits (e.g. the profile) show up right after they are saved.
  const selected = members.find((m) => m.id === selectedId) ?? null;
  const today = ymdTehran(new Date());
  const [open, setOpen] = useState(false);
  const [confirming, setConfirming] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [q, setQ] = useState("");
  const [filter, setFilter] = useState<Filter>("all");
  const [pending, start] = useTransition();

  const list = useMemo(
    () => members.filter((m) => matches(m, filter) && `${m.firstName} ${m.lastName}`.includes(q.trim())),
    [members, q, filter],
  );

  const show = (m: MemberView) => {
    setSelectedId(m.id);
    setConfirming(false);
    setError(null);
    setOpen(true);
  };
  const remove = () =>
    start(async () => {
      if (!selected) return;
      const name = `${selected.firstName} ${selected.lastName}`;
      const r = await removeMember(selected.id);
      if (r.error) setError(r.error);
      else {
        setOpen(false);
        toast.success("عضو از باشگاه حذف شد", name);
      }
    });

  return (
    <>
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
        <Input
          className="sm:max-w-sm sm:flex-1"
          placeholder="جستجوی نام یا نام خانوادگی"
          aria-label="جستجوی عضو"
          value={q}
          onChange={setQ}
          leftIcon={<Icon name="search" size={18} />}
        />
        <div className="-mx-4 overflow-x-auto px-4 scrollbar-hide sm:mx-0 sm:px-0">
          <Tabs value={filter} onValueChange={(v) => setFilter(v as Filter)} variant="pill">
            <TabsList aria-label="فیلتر وضعیت شهریه">
              <TabsTrigger value="all">همه</TabsTrigger>
              <TabsTrigger value="active">فعال</TabsTrigger>
              <TabsTrigger value="expiring">رو به پایان</TabsTrigger>
              <TabsTrigger value="expired">منقضی</TabsTrigger>
            </TabsList>
          </Tabs>
        </div>
      </div>

      <Card className="gap-0 overflow-hidden py-0 shadow-card">
        {list.length === 0 ? (
          <EmptyState
            icon="members"
            tone="members"
            title={members.length ? "عضوی پیدا نشد" : "هنوز عضوی ندارید"}
            description={
              members.length
                ? "نام دیگری را جستجو کنید یا فیلتر را تغییر دهید."
                : "کد باشگاه را برای ورزشکاران بفرستید و درخواست‌هایشان را تایید کنید."
            }
          />
        ) : (
          <ul className="divide-y divide-border">
            <AnimatePresence initial={false}>
              {list.map((m, i) => (
                <motion.li
                  key={m.id}
                  layout="position"
                  initial={{ opacity: 0 }}
                  animate={{ opacity: 1 }}
                  exit={{ opacity: 0 }}
                  transition={{ duration: 0.2, delay: Math.min(i, 8) * 0.025 }}
                >
                  <button
                    type="button"
                    onClick={() => show(m)}
                    className="group flex w-full items-center gap-3 px-4 py-3.5 text-start transition-colors hover:bg-card active:bg-card sm:px-5"
                  >
                    <Avatar name={`${m.firstName} ${m.lastName}`} firstName={m.firstName} lastName={m.lastName} />
                    <span className="min-w-0 flex-1">
                      <span className="block truncate font-medium">
                        {m.firstName} {m.lastName}
                      </span>
                      <span className="ltr text-xs text-muted-foreground">{m.phone}</span>
                    </span>
                    {isBirthday(m.birthYmd, today) ? <Badge tone="attendance">تولد امروز</Badge> : null}
                    <RemainingBadge days={m.remaining} />
                    <Icon
                      name="chevronLeft"
                      size={18}
                      className="hidden text-muted-foreground transition-transform group-hover:-translate-x-0.5 sm:inline-flex"
                    />
                  </button>
                </motion.li>
              ))}
            </AnimatePresence>
          </ul>
        )}
      </Card>
      {list.length > 0 ? <p className="px-1 text-xs text-muted-foreground">{toFa(list.length)} عضو نمایش داده می‌شود</p> : null}

      <Dialog
        open={open}
        onOpenChange={setOpen}
        title={selected ? `${selected.firstName} ${selected.lastName}` : ""}
        description="مشخصات کامل عضو"
        icon="user"
        tone="members"
      >
        {selected ? (
          <>
            <dl className="divide-y divide-border">
              <DetailRow icon="call" label="شماره تلفن">
                <span className="ltr">{selected.phone}</span>
              </DetailRow>
              <DetailRow icon="calendar" label="تاریخ عضویت">{formatJalali(selected.joinedYmd)}</DetailRow>
              <DetailRow icon="time" label="شهریه باقی‌مانده">
                <RemainingBadge days={selected.remaining} />
              </DetailRow>
              <DetailRow icon="wallet" label="آخرین پرداخت">
                {selected.lastPayment ? (
                  <span className="block leading-6">
                    {formatToman(selected.lastPayment.amount)}
                    <span className="block text-xs font-normal text-muted-foreground">
                      {formatJalali(selected.lastPayment.paidYmd)} ({formatDays(selected.lastPayment.durationDays)})
                    </span>
                  </span>
                ) : (
                  "ثبت نشده"
                )}
              </DetailRow>
              <DetailRow icon="attendance" label="حضور در ۳۰ روز اخیر">{toFa(selected.presentLast30)} جلسه</DetailRow>
            </dl>
            <MemberProfileSection key={selected.id} member={selected} />
            <div className="mt-3">
              <FormError message={error} />
            </div>
            <DialogActions>
              {confirming ? (
                <>
                  <Button variant="secondary" onClick={() => setConfirming(false)}>
                    انصراف
                  </Button>
                  <Button variant="danger" loading={pending} onClick={remove}>
                    بله، حذف شود
                  </Button>
                </>
              ) : (
                <>
                  <Button variant="secondary" onClick={() => setOpen(false)}>
                    بستن
                  </Button>
                  <Button variant="danger-soft" onClick={() => setConfirming(true)}>
                    <Icon name="delete" size={18} />
                    حذف از باشگاه
                  </Button>
                </>
              )}
            </DialogActions>
          </>
        ) : null}
      </Dialog>
    </>
  );
}
