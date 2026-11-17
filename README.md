# باشگاه‌یار (Next.js + Prisma + Neon + Arc UI)

## اجرا روی لوکال
```bash
npm install
npm run arc                 # نصب کامپوننت‌های Arc (به سوال‌های CLI جواب yes بده)
cp .env.example .env        # مقادیر Neon و AUTH_SECRET را پر کن
npx prisma db push
npm run dev
```

## دیپلوی روی Vercel
1. پروژه را در GitHub بگذار و در Vercel ایمپورت کن.
2. Vercel > Storage > Create > Neon و به پروژه وصلش کن (متغیر DATABASE_URL خودکار ست می‌شود).
3. در Environment Variables مقدار `AUTH_SECRET` را اضافه کن (خروجی `openssl rand -base64 32`).
4. Deploy. دستور build خودش کامپوننت‌های Arc را نصب می‌کند و جدول‌ها را روی Neon می‌سازد؛ نیازی به commit کردن `components/arc` نیست.

## PWA و اعلان‌ها
1. کلید VAPID بساز (یک بار): `npx web-push generate-vapid-keys`
2. در Vercel > Environment Variables این‌ها را اضافه کن: `VAPID_PUBLIC_KEY`، `VAPID_PRIVATE_KEY`، `VAPID_SUBJECT` (مثلا `mailto:you@example.com`) و `CRON_SECRET` (خروجی `openssl rand -base64 32`).
3. Redeploy. فایل `vercel.json` هر روز ساعت ۰۹:۰۰ به وقت تهران مسیر `/api/cron/reminders` را اجرا می‌کند و برای ورزشکارانی که ۲ روز، ۱ روز یا همین امروز به پایان شهریه‌شان مانده اعلان می‌فرستد (هر یادآوری فقط یک بار).
4. ورزشکار از صفحه‌ی «پروفایل» اپ را نصب می‌کند و اعلان‌ها را فعال می‌کند. در آیفون اعلان فقط بعد از «Add to Home Screen» کار می‌کند.
