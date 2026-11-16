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
