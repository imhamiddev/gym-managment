"use client";

import { useActionState, useState } from "react";
import Link from "next/link";
import { FormError } from "@/components/FormError";
import { HeroDemo } from "@/components/landing/HeroDemo";
import { Button } from "@/components/ui/button";
import { Icon } from "@/components/ui/icon";
import { Input } from "@/components/ui/input";
import { Brand } from "@/components/ui/logo";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { ThemeToggle } from "@/components/ui/theme-toggle";
import { login, register, type FormState } from "@/lib/actions";

export function AuthForm({ mode, initialRole = "ATHLETE" }: { mode: "login" | "register"; initialRole?: string }) {
  const isRegister = mode === "register";
  const [state, action, pending] = useActionState<FormState, FormData>(isRegister ? register : login, {});
  const [role, setRole] = useState(initialRole);
  const [show, setShow] = useState(false);

  return (
    <div className="grid min-h-dvh lg:grid-cols-[minmax(0,1fr)_minmax(0,1.05fr)]">
      <div className="flex flex-col px-5 pb-8 pt-safe sm:px-10">
        <div className="flex h-20 items-center justify-between">
          <Link href="/" aria-label="صفحه اصلی"><Brand /></Link>
          <ThemeToggle variant="circle" start="top-left" className="size-11 rounded-full text-muted-foreground transition-colors hover:bg-card hover:text-foreground active:scale-90" />
        </div>

        <div className="mx-auto flex w-full max-w-sm flex-1 flex-col justify-center py-6">
          <h1 className="text-2xl font-bold leading-10 sm:text-3xl">{isRegister ? "ساخت حساب جدید" : "خوش برگشتید"}</h1>
          <p className="mb-7 mt-1 text-sm leading-7 text-muted-foreground">
            {isRegister ? "حساب خود را بسازید و نقش‌تان را انتخاب کنید." : "با شماره تلفن و رمز عبور وارد شوید."}
          </p>

          <form action={action} className="flex flex-col gap-4">
            {isRegister ? (
              <>
                <div className="flex flex-col gap-1.5">
                  <span className="px-1 text-sm font-medium">نقش شما</span>
                  <Tabs value={role} onValueChange={setRole} variant="pill">
                    <TabsList aria-label="نقش شما" className="w-full [&>*]:flex-1">
                      <TabsTrigger value="ATHLETE" icon={<Icon name="user" size={16} />}>ورزشکار</TabsTrigger>
                      <TabsTrigger value="COACH" icon={<Icon name="star" size={16} />}>مربی</TabsTrigger>
                    </TabsList>
                  </Tabs>
                </div>
                <input type="hidden" name="role" value={role} />
                <div className="grid gap-4 sm:grid-cols-2">
                  <Input label="نام" name="firstName" autoComplete="given-name" required />
                  <Input label="نام خانوادگی" name="lastName" autoComplete="family-name" required />
                </div>
              </>
            ) : null}
            <Input label="شماره تلفن" name="phone" type="tel" dir="ltr" inputMode="tel" autoComplete="tel" placeholder="09121234567" required leftIcon={<Icon name="call" size={18} />} />
            <Input
              label="رمز عبور"
              name="password"
              type={show ? "text" : "password"}
              dir="ltr"
              autoComplete={isRegister ? "new-password" : "current-password"}
              required
              leftIcon={<Icon name="lock" size={18} />}
              rightIcon={
                <button type="button" onClick={() => setShow((s) => !s)} aria-label={show ? "پنهان کردن رمز" : "نمایش رمز"} className="transition-colors hover:text-foreground active:scale-90">
                  <Icon name={show ? "hide" : "show"} size={18} />
                </button>
              }
            />
            <FormError message={state.error} />
            <Button type="submit" size="lg" loading={pending} className="mt-1">
              {isRegister ? "ساخت حساب" : "ورود"}
            </Button>
          </form>

          <p className="mt-6 text-center text-sm text-muted-foreground">
            {isRegister ? "حساب دارید؟ " : "حساب ندارید؟ "}
            <Link href={isRegister ? "/login" : "/register"} className="font-medium text-primary underline-offset-4 hover:underline">
              {isRegister ? "وارد شوید" : "ثبت‌نام کنید"}
            </Link>
          </p>
        </div>
      </div>

      {/* Desktop side panel */}
      <aside aria-hidden className="relative hidden items-center justify-center overflow-hidden bg-card p-12 lg:flex">
        <div className="w-full max-w-md">
          <p className="mb-6 text-2xl font-bold leading-[1.7]">
            شهریه، اعضا و حضور و غیاب،
            <br />
            همه در یک صفحه.
          </p>
          <HeroDemo />
        </div>
      </aside>
    </div>
  );
}
