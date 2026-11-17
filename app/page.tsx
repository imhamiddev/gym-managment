import Link from "next/link";
import { HeroDemo } from "@/components/landing/HeroDemo";
import { ButtonLink } from "@/components/ui/button";
import { Icon, type IconName } from "@/components/ui/icon";
import { Brand } from "@/components/ui/logo";
import { TONE, type Tone } from "@/components/ui/badge";
import { ThemeToggle } from "@/components/ui/theme-toggle";
import { getUser } from "@/lib/auth";
import { cn } from "@/lib/utils";

const FEATURES: { icon: IconName; tone: Tone; title: string; text: string }[] = [
  { icon: "wallet", tone: "tuition", title: "مدیریت شهریه", text: "مبلغ، تاریخ شمسی و مدت ۳۰، ۶۰ یا ۹۰ روزه را ثبت کنید. هر زمان خواستید ویرایش یا حذف کنید." },
  { icon: "members", tone: "members", title: "مدیریت اعضا", text: "فهرست اعضا با تعداد روزهای باقی‌مانده، و مشخصات کامل هر نفر با یک ضربه." },
  { icon: "attendance", tone: "attendance", title: "حضور و غیاب", text: "برای هر جلسه حاضر و غایب را علامت بزنید. ورزشکار سابقه خودش را می‌بیند." },
];

const STEPS = [
  { title: "مربی باشگاه می‌سازد.", text: "نام باشگاه را ثبت می‌کند و یک کد اختصاصی می‌گیرد." },
  { title: "ورزشکار کد را وارد می‌کند.", text: "بعد از ثبت‌نام، با کد باشگاه درخواست عضویت می‌فرستد." },
  { title: "مربی تایید می‌کند.", text: "درخواست را تایید یا لغو می‌کند و مدیریت باشگاه شروع می‌شود." },
];

const ghost = "inline-flex h-9 items-center rounded-full px-4 text-[13px] font-medium text-muted-foreground transition-colors hover:bg-card hover:text-foreground";

export default async function Landing() {
  const user = await getUser();
  const panel = user ? (user.role === "COACH" ? "/coach" : "/athlete") : null;

  return (
    <div>
      <header className="glass sticky top-0 z-30 border-b border-border pt-safe">
        <div className="mx-auto flex h-16 w-full max-w-6xl items-center justify-between px-4 sm:px-6">
          <Brand />
          <div className="flex items-center gap-1.5">
            <ThemeToggle variant="circle" start="top-left" className="size-10 rounded-full text-muted-foreground transition-colors hover:bg-card hover:text-foreground active:scale-90" />
            {panel ? (
              <ButtonLink href={panel} size="sm">ورود به پنل</ButtonLink>
            ) : (
              <>
                <Link href="/login" className={ghost}>ورود</Link>
                <ButtonLink href="/register" size="sm">ثبت‌نام</ButtonLink>
              </>
            )}
          </div>
        </div>
      </header>

      <main className="mx-auto w-full max-w-6xl px-4 sm:px-6">
        <section className="grid items-center gap-10 py-12 sm:py-16 lg:grid-cols-[1.05fr_1fr] lg:gap-16 lg:py-24">
          <div>
            <h1 className="text-[2rem] font-extrabold leading-[1.5] sm:text-5xl sm:leading-[1.45]">شهریه، اعضا و حضور و غیاب باشگاه، یک‌جا</h1>
            <p className="mb-8 mt-5 max-w-[34ch] text-base leading-8 text-muted-foreground sm:text-lg">
              مربی باشگاه را می‌سازد و ورزشکاران با یک کد عضو می‌شوند. دیگر دفترچه و پیام‌های پراکنده لازم نیست.
            </p>
            <div className="flex flex-col gap-3 sm:flex-row">
              <ButtonLink href="/register?role=coach" size="lg">ساخت باشگاه</ButtonLink>
              <ButtonLink href="/register" size="lg" variant="secondary">عضویت با کد باشگاه</ButtonLink>
            </div>
          </div>
          <div aria-label="نمونه‌ای از حضور و غیاب">
            <HeroDemo />
            <p className="mt-3 text-center text-xs text-muted-foreground">روی هر نفر بزنید و وضعیت حضور را عوض کنید.</p>
          </div>
        </section>

        <section className="grid gap-4 md:grid-cols-3" aria-label="امکانات">
          {FEATURES.map((f) => (
            <article key={f.title} className="rounded-[28px] border border-border bg-background p-6 shadow-card">
              <span className={cn("mb-5 grid size-12 place-items-center rounded-2xl", TONE[f.tone].soft)}>
                <Icon name={f.icon} size={26} set="bulk" />
              </span>
              <h2 className="mb-2 text-lg font-bold">{f.title}</h2>
              <p className="text-sm leading-7 text-muted-foreground">{f.text}</p>
            </article>
          ))}
        </section>

        <section className="grid gap-8 py-16 lg:grid-cols-[1fr_1.5fr] lg:gap-16 lg:py-24">
          <h2 className="text-2xl font-bold sm:text-3xl">شروع در سه قدم</h2>
          <ol className="grid gap-4">
            {STEPS.map((s, i) => (
              <li key={s.title} className="flex gap-4 rounded-3xl border border-border bg-background p-5">
                <span className="grid size-10 shrink-0 place-items-center rounded-full bg-primary/12 font-bold text-primary">{["۱", "۲", "۳"][i]}</span>
                <div>
                  <p className="font-semibold">{s.title}</p>
                  <p className="text-sm leading-7 text-muted-foreground">{s.text}</p>
                </div>
              </li>
            ))}
          </ol>
        </section>

        <section className="mb-10 flex flex-col items-center gap-5 rounded-[32px] bg-primary px-6 py-12 text-center text-primary-foreground">
          <h2 className="text-2xl font-bold leading-10 sm:text-3xl">باشگاه‌تان را امروز راه بیندازید</h2>
          <Link href="/register?role=coach" className="inline-flex h-12 items-center rounded-full bg-background px-7 font-medium text-foreground transition-transform active:scale-95">
            ثبت‌نام مربی
          </Link>
        </section>
      </main>
      <footer className="px-4 pb-[max(1.5rem,env(safe-area-inset-bottom))] pt-4 text-center text-sm text-muted-foreground">باشگاه‌یار</footer>
    </div>
  );
}
