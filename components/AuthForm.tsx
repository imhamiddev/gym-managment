"use client";
import { useActionState, useState } from "react";
import Link from "next/link";
import { Button } from "@/components/arc/button/button";
import { Input } from "@/components/arc/input/input";
import SegmentedControl from "@/components/arc/segmented-control/segmented-control";
import { login, register, type FormState } from "@/lib/actions";

export function AuthForm({ mode, initialRole = "ATHLETE" }: { mode: "login" | "register"; initialRole?: string }) {
  const isRegister = mode === "register";
  const [state, action, pending] = useActionState<FormState, FormData>(isRegister ? register : login, {});
  const [role, setRole] = useState(initialRole);

  return (
    <div className="auth-wrap">
      <div className="panel auth-card stack">
        <div>
          <h1>{isRegister ? "ثبت‌نام" : "ورود"}</h1>
          <p className="muted">{isRegister ? "حساب خود را بسازید و نقش‌تان را انتخاب کنید." : "با شماره تلفن و رمز عبور وارد شوید."}</p>
        </div>
        <form action={action} className="stack">
          {isRegister && (
            <>
              <SegmentedControl
                label="نقش شما"
                fullWidth
                value={role}
                onValueChange={setRole}
                options={[
                  { value: "ATHLETE", label: "ورزشکار" },
                  { value: "COACH", label: "مربی" },
                ]}
              />
              <input type="hidden" name="role" value={role} />
              <Input label="نام" name="firstName" autoComplete="given-name" required />
              <Input label="نام خانوادگی" name="lastName" autoComplete="family-name" required />
            </>
          )}
          <Input label="شماره تلفن" name="phone" type="tel" dir="ltr" inputMode="tel" autoComplete="tel" placeholder="09121234567" required />
          <Input label="رمز عبور" name="password" type="password" dir="ltr" autoComplete={isRegister ? "new-password" : "current-password"} required />
          {state.error && <p role="alert" className="form-error">{state.error}</p>}
          <Button type="submit" size="lg" loading={pending}>{isRegister ? "ساخت حساب" : "ورود"}</Button>
        </form>
        <p className="muted">
          {isRegister ? "حساب دارید؟ " : "حساب ندارید؟ "}
          <Link href={isRegister ? "/login" : "/register"} style={{ textDecoration: "underline" }}>
            {isRegister ? "وارد شوید" : "ثبت‌نام کنید"}
          </Link>
        </p>
      </div>
    </div>
  );
}
