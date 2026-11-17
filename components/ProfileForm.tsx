"use client";

import { useTheme } from "next-themes";
import { useActionState, useEffect, useState } from "react";
import { FormError } from "@/components/FormError";
import { Avatar } from "@/components/ui/avatar";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Icon, type IconName } from "@/components/ui/icon";
import { Input } from "@/components/ui/input";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { useToast } from "@/components/ui/toast";
import { logout, updateProfile, type FormState } from "@/lib/actions";

export type ProfileInfo = {
  firstName: string;
  lastName: string;
  phone: string;
  roleLabel: string;
  joined: string;
  gymLabel: string | null;
  gymValue: string | null;
};

function InfoRow({ icon, label, children }: { icon: IconName; label: string; children: React.ReactNode }) {
  return (
    <div className="flex items-center gap-3 py-3">
      <span className="grid size-9 shrink-0 place-items-center rounded-xl bg-card text-muted-foreground">
        <Icon name={icon} size={18} />
      </span>
      <span className="text-sm text-muted-foreground">{label}</span>
      <span className="ms-auto text-end text-sm font-medium">{children}</span>
    </div>
  );
}

export function ProfileForm({ info }: { info: ProfileInfo }) {
  const toast = useToast();
  const [state, action, pending] = useActionState<FormState, FormData>(updateProfile, {});
  const [first, setFirst] = useState(info.firstName);
  const [last, setLast] = useState(info.lastName);
  const { theme, setTheme } = useTheme();
  const [mounted, setMounted] = useState(false);
  useEffect(() => setMounted(true), []);

  useEffect(() => {
    if (state.ok) toast.success("پروفایل به‌روزرسانی شد");
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [state]);

  const name = `${info.firstName} ${info.lastName}`;
  const unchanged = first.trim() === info.firstName && last.trim() === info.lastName;

  return (
    <div className="grid gap-5 lg:grid-cols-[320px_minmax(0,1fr)]">
      {/* Identity */}
      <Card className="h-fit items-center gap-4 py-8 text-center shadow-card">
        <Avatar name={name} firstName={info.firstName} lastName={info.lastName} size="xl" />
        <div>
          <p className="text-lg font-bold leading-8">{name}</p>
          <Badge tone="primary" className="mt-1">{info.roleLabel}</Badge>
        </div>
        <dl className="w-full divide-y divide-border px-6 text-start">
          <InfoRow icon="call" label="شماره تلفن"><span className="ltr">{info.phone}</span></InfoRow>
          <InfoRow icon="calendar" label="تاریخ ثبت‌نام">{info.joined}</InfoRow>
          {info.gymLabel ? <InfoRow icon="home" label={info.gymLabel}>{info.gymValue}</InfoRow> : null}
        </dl>
      </Card>

      <div className="flex flex-col gap-5">
        <Card className="shadow-card">
          <form action={action} className="flex flex-col gap-5">
            <CardHeader>
              <CardTitle as="h2" className="text-base">اطلاعات شخصی</CardTitle>
              <CardDescription>نام و نام خانوادگی شما در پنل و برای مربی یا ورزشکاران نمایش داده می‌شود.</CardDescription>
            </CardHeader>
            <CardContent className="flex flex-col gap-4">
              <div className="grid gap-4 sm:grid-cols-2">
                <Input label="نام" name="firstName" value={first} onChange={setFirst} autoComplete="given-name" required leftIcon={<Icon name="user" size={18} />} />
                <Input label="نام خانوادگی" name="lastName" value={last} onChange={setLast} autoComplete="family-name" required leftIcon={<Icon name="user" size={18} />} />
              </div>
              <FormError message={state.error} />
              <div className="flex justify-end">
                <Button type="submit" loading={pending} disabled={unchanged} className="w-full sm:w-auto">
                  <Icon name="check" size={18} />
                  ذخیره تغییرات
                </Button>
              </div>
            </CardContent>
          </form>
        </Card>

        <Card className="shadow-card">
          <CardHeader>
            <CardTitle as="h2" className="text-base">نمایش</CardTitle>
            <CardDescription>ظاهر برنامه را انتخاب کنید. حالت «خودکار» از تنظیم دستگاه شما پیروی می‌کند.</CardDescription>
          </CardHeader>
          <CardContent>
            {mounted ? (
              <Tabs value={theme ?? "system"} onValueChange={setTheme} variant="pill">
                <TabsList aria-label="حالت نمایش" className="w-full [&>*]:flex-1">
                  <TabsTrigger value="light" icon={<Icon name="sun" size={16} />}>روشن</TabsTrigger>
                  <TabsTrigger value="dark" icon={<Icon name="moon" size={16} />}>تیره</TabsTrigger>
                  <TabsTrigger value="system" icon={<Icon name="setting" size={16} />}>خودکار</TabsTrigger>
                </TabsList>
              </Tabs>
            ) : (
              <div className="skeleton h-11 rounded-full" />
            )}
          </CardContent>
        </Card>

        <form action={logout}>
          <Button type="submit" variant="danger-soft" size="lg" className="w-full">
            <Icon name="logout" size={20} />
            خروج از حساب
          </Button>
        </form>
      </div>
    </div>
  );
}
