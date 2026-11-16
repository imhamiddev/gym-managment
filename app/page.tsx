import Link from "next/link";
import { Button } from "@/components/arc/button/button";
import { Badge } from "@/components/arc/badge/badge";
import { getUser } from "@/lib/auth";

const sample = [
  { name: "سارا احمدی", days: "۲۳ روز", tone: "success" as const },
  { name: "امیر کریمی", days: "۶ روز", tone: "warning" as const },
  { name: "نگار رستمی", days: "۴۱ روز", tone: "success" as const },
  { name: "رضا مرادی", days: "منقضی شده", tone: "danger" as const },
];

export default async function Landing() {
  const user = await getUser();
  const panel = user ? (user.role === "COACH" ? "/coach" : "/athlete") : null;

  return (
    <div className="container">
      <header className="land-nav">
        <span className="brand">باشگاه‌یار</span>
        <div className="row">
          {panel ? (
            <Link href={panel}><Button size="sm">ورود به پنل</Button></Link>
          ) : (
            <>
              <Link href="/login"><Button variant="ghost" size="sm">ورود</Button></Link>
              <Link href="/register"><Button size="sm">ثبت‌نام</Button></Link>
            </>
          )}
        </div>
      </header>

      <section className="hero">
        <div>
          <h1>شهریه، اعضا و حضور و غیاب باشگاه، یک‌جا</h1>
          <p className="muted">مربی باشگاه را می‌سازد و ورزشکاران با یک کد عضو می‌شوند. دیگر دفترچه و پیام‌های پراکنده لازم نیست.</p>
          <div className="row">
            <Link href="/register?role=coach"><Button size="lg">ساخت باشگاه</Button></Link>
            <Link href="/register"><Button size="lg" variant="secondary">عضویت با کد باشگاه</Button></Link>
          </div>
        </div>
        <div className="preview" aria-label="نمونه‌ای از لیست اعضا">
          <div className="preview-head"><span>اعضای باشگاه</span><span className="muted">شهریه باقی‌مانده</span></div>
          {sample.map((s) => (
            <div className="att-row" key={s.name}>
              <span>{s.name}</span>
              <Badge tone={s.tone} dot>{s.days}</Badge>
            </div>
          ))}
        </div>
      </section>

      <section className="features">
        <div>
          <h3>مدیریت شهریه</h3>
          <p className="muted">مبلغ، تاریخ شمسی و مدت ۳۰، ۶۰ یا ۹۰ روزه را ثبت کنید. هر زمان خواستید ویرایش یا حذف کنید.</p>
        </div>
        <div>
          <h3>مدیریت اعضا</h3>
          <p className="muted">فهرست اعضا با تعداد روزهای باقی‌مانده، و مشخصات کامل هر نفر با یک ضربه.</p>
        </div>
        <div>
          <h3>حضور و غیاب</h3>
          <p className="muted">برای هر جلسه حاضر و غایب را علامت بزنید. ورزشکار سابقه خودش را می‌بیند.</p>
        </div>
      </section>

      <section className="steps">
        <h2>شروع در سه قدم</h2>
        <ol>
          <li><div><b>مربی باشگاه می‌سازد.</b><p className="muted">نام باشگاه را ثبت می‌کند و یک کد اختصاصی می‌گیرد.</p></div></li>
          <li><div><b>ورزشکار کد را وارد می‌کند.</b><p className="muted">بعد از ثبت‌نام، با کد باشگاه درخواست عضویت می‌فرستد.</p></div></li>
          <li><div><b>مربی تایید می‌کند.</b><p className="muted">درخواست را تایید یا لغو می‌کند و مدیریت باشگاه شروع می‌شود.</p></div></li>
        </ol>
      </section>

      <section className="cta">
        <h2>باشگاه‌تان را امروز راه بیندازید</h2>
        <Link href="/register?role=coach"><Button size="lg">ثبت‌نام مربی</Button></Link>
      </section>
      <footer className="foot">باشگاه‌یار</footer>
    </div>
  );
}
