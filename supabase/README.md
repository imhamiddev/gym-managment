# راه‌اندازی Supabase

۱. در داشبورد Supabase به **SQL Editor** برو، محتوای فایل
`supabase/migrations/20261120000000_init.sql` را کامل Paste و **Run** کن (فقط یک بار).

۲. به **Authentication → Providers → Email** برو و گزینه‌ی **Confirm email** را **خاموش** کن
(ورود با «شماره + رمز» انجام می‌شود و ایمیل واقعی در کار نیست).

نکته‌ها:
- کاربر با ایمیل ساختگی `<شماره>@gymyar.app` ساخته می‌شود؛ کاربران ساخته‌شده از داشبورد (بدون این الگو) رد می‌شوند.
- جدول‌ها داخل schema خصوصی `app` هستند و از بیرون قابل دسترس نیستند؛ اپ فقط توابع `public.*` را صدا می‌زند.
- کلید `anon` داخل اپ است (طراحی Supabase برای اپ‌های کلاینت). کلید `service_role` را **هرگز** در اپ یا گیت نگذار.

## تست محلی (اختیاری)
با یک PostgreSQL محلی (بدون Supabase):

```bash
createdb gymtest
psql -d gymtest -f supabase/tests/mock_supabase.sql
psql -d gymtest -f supabase/migrations/20261120000000_init.sql
python3 supabase/tests/test_backend.py
```
