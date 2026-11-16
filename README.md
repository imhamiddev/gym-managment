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
2. Vercel > Storage > Create > Neon و به پروژه وصلش کن (DATABASE_URL و DATABASE_URL_UNPOOLED خودکار ست می‌شوند).
3. در Environment Variables مقدار `AUTH_SECRET` را اضافه کن (خروجی `openssl rand -base64 32`).
4. Deploy. دستور build جدول‌ها را خودش روی Neon می‌سازد (`prisma db push`).
