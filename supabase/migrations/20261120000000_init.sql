-- =====================================================================
--  باشگاه‌یار — Supabase backend
--
--  How it works
--   * Tables live in the private schema "app". anon / authenticated have NO access to it,
--     so nothing can be read or written through the REST API directly (RLS is enabled as a 2nd lock).
--   * The Android app only calls the SECURITY DEFINER functions in schema "public" (RPC).
--     They contain the same rules the old web app had in its server actions
--     (validation, Tehran time, work days, check-in window, roles).
--   * Users sign in with  <phone>@gymyar.app  + password (no SMS). A trigger creates the profile.
--
--  Run once in Supabase  >  SQL Editor.  Also turn OFF
--  Authentication > Providers > Email > "Confirm email".
-- =====================================================================

create schema if not exists app;
revoke all on schema app from public, anon, authenticated;

-- ---------------------------------------------------------------- tables

create table app.profiles (
  id          uuid primary key references auth.users (id) on delete cascade,
  phone       text not null unique check (phone ~ '^09[0-9]{9}$'),
  first_name  text not null check (char_length(first_name) between 1 and 40),
  last_name   text not null check (char_length(last_name) between 1 and 60),
  role        text not null check (role in ('COACH', 'ATHLETE')),
  created_at  timestamptz not null default now()
);

create table app.gyms (
  id            uuid primary key default gen_random_uuid(),
  name          text not null check (char_length(name) between 2 and 60),
  code          text not null unique check (code ~ '^[A-Z0-9]{4,6}$'),
  owner_id      uuid not null unique references app.profiles (id) on delete cascade,
  work_days     int[] not null default '{0,1,2,3,4,5,6}',   -- 0 = Sunday ... 6 = Saturday
  open_time     text not null default '06:00' check (open_time ~ '^([01][0-9]|2[0-3]):[0-5][0-9]$'),
  close_time    text not null default '22:00' check (close_time ~ '^([01][0-9]|2[0-3]):[0-5][0-9]$'),
  card_number   text check (card_number ~ '^[0-9]{16}$'),
  checkin_token text not null default replace(gen_random_uuid()::text || gen_random_uuid()::text, '-', ''),
  created_at    timestamptz not null default now()
);

-- assistant coaches (the owner is gyms.owner_id)
create table app.gym_coaches (
  id          uuid primary key default gen_random_uuid(),
  gym_id      uuid not null references app.gyms (id) on delete cascade,
  user_id     uuid not null unique references app.profiles (id) on delete cascade,
  created_at  timestamptz not null default now()
);
create index on app.gym_coaches (gym_id);

create table app.memberships (
  id          uuid primary key default gen_random_uuid(),
  gym_id      uuid not null references app.gyms (id) on delete cascade,
  athlete_id  uuid not null unique references app.profiles (id) on delete cascade,
  status      text not null default 'PENDING' check (status in ('PENDING', 'APPROVED')),
  created_at  timestamptz not null default now(),
  approved_at timestamptz,
  height_cm   int check (height_cm between 50 and 260),
  weight_kg   numeric(4, 1) check (weight_kg between 20 and 300),
  goal        text check (char_length(goal) <= 80),
  program     text check (char_length(program) <= 2000),
  notes       text check (char_length(notes) <= 1000),   -- private coach notes, never returned to athletes
  birth_date  date
);
create index on app.memberships (gym_id);

create table app.payments (
  id            uuid primary key default gen_random_uuid(),
  membership_id uuid not null references app.memberships (id) on delete cascade,
  amount        int not null check (amount > 0 and amount <= 2000000000),   -- toman
  paid_on       date not null,
  duration_days int not null check (duration_days in (30, 60, 90)),
  created_at    timestamptz not null default now()
);
create index on app.payments (membership_id);

create table app.attendance (
  id            uuid primary key default gen_random_uuid(),
  membership_id uuid not null references app.memberships (id) on delete cascade,
  day           date not null,
  present       boolean not null,
  unique (membership_id, day)
);

create table app.plans (
  id            uuid primary key default gen_random_uuid(),
  gym_id        uuid not null references app.gyms (id) on delete cascade,
  name          text not null check (char_length(name) between 2 and 40),
  amount        int not null check (amount > 0 and amount <= 2000000000),
  duration_days int not null check (duration_days in (30, 60, 90)),
  created_at    timestamptz not null default now()
);
create index on app.plans (gym_id);

alter table app.profiles    enable row level security;
alter table app.gyms        enable row level security;
alter table app.gym_coaches enable row level security;
alter table app.memberships enable row level security;
alter table app.payments    enable row level security;
alter table app.attendance  enable row level security;
alter table app.plans       enable row level security;
revoke all on all tables in schema app from public, anon, authenticated;

-- ---------------------------------------------------------------- helpers (private)

create function app.fail(msg text) returns jsonb
  language sql immutable set search_path = '' as $$ select jsonb_build_object('error', msg) $$;

create function app.ok() returns jsonb
  language sql immutable set search_path = '' as $$ select jsonb_build_object('ok', true) $$;

-- Persian / Arabic-Indic digits -> ASCII
create function app.to_en(t text) returns text
  language sql immutable set search_path = '' as $$
  select translate(coalesce(t, ''), '۰۱۲۳۴۵۶۷۸۹٠١٢٣٤٥٦٧٨٩', '01234567890123456789')
$$;

-- trim + collapse whitespace
create function app.clean_text(t text) returns text
  language sql immutable set search_path = '' as $$
  select regexp_replace(btrim(coalesce(t, '')), '\s+', ' ', 'g')
$$;

create function app.tehran_today() returns date
  language sql stable set search_path = '' as $$
  select (now() at time zone 'Asia/Tehran')::date
$$;

create function app.tehran_minutes() returns int
  language sql stable set search_path = '' as $$
  select (extract(hour from t) * 60 + extract(minute from t))::int
  from (select now() at time zone 'Asia/Tehran' as t) s
$$;

create function app.hhmm_to_min(t text) returns int
  language sql immutable set search_path = '' as $$
  select split_part(t, ':', 1)::int * 60 + split_part(t, ':', 2)::int
$$;

create function app.role_of(p_user uuid) returns text
  language sql stable set search_path = '' as $$
  select role from app.profiles where id = p_user
$$;

-- the gym a coach works in: owned, or joined as assistant
create function app.coach_gym_id(p_user uuid) returns uuid
  language sql stable set search_path = '' as $$
  select id from (
    select g.id, 0 as ord from app.gyms g where g.owner_id = p_user
    union all
    select c.gym_id, 1 from app.gym_coaches c where c.user_id = p_user
  ) x order by ord limit 1
$$;

create function app.make_code() returns text
  language plpgsql volatile set search_path = '' as $$
declare
  alphabet text := 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
  s text := '';
begin
  for i in 1..6 loop
    s := s || substr(alphabet, 1 + floor(random() * 32)::int, 1);
  end loop;
  return s;
end $$;

-- new auth user -> profile.  Email is "<phone>@gymyar.app".
create function app.handle_new_user() returns trigger
  language plpgsql security definer set search_path = '' as $$
declare
  v_phone text := split_part(new.email, '@', 1);
  v_first text := app.clean_text(new.raw_user_meta_data ->> 'first_name');
  v_last  text := app.clean_text(new.raw_user_meta_data ->> 'last_name');
  v_role  text := case when new.raw_user_meta_data ->> 'role' = 'COACH' then 'COACH' else 'ATHLETE' end;
begin
  if v_phone !~ '^09[0-9]{9}$' then
    raise exception 'Gymyar: invalid phone number';
  end if;
  if char_length(v_first) not between 1 and 40 or char_length(v_last) not between 1 and 60 then
    raise exception 'Gymyar: invalid name';
  end if;
  insert into app.profiles (id, phone, first_name, last_name, role)
  values (new.id, v_phone, v_first, v_last, v_role);
  return new;
end $$;

create trigger on_auth_user_created
  after insert on auth.users
  for each row execute function app.handle_new_user();

-- ---------------------------------------------------------------- API: account

create function public.me() returns jsonb
  language plpgsql security definer set search_path = '' as $$
declare p app.profiles;
begin
  select * into p from app.profiles where id = auth.uid();
  if not found then return app.fail('حساب کاربری پیدا نشد.'); end if;
  return jsonb_build_object('id', p.id, 'phone', p.phone, 'first_name', p.first_name,
                            'last_name', p.last_name, 'role', p.role);
end $$;

create function public.update_my_profile(p_first text, p_last text) returns jsonb
  language plpgsql security definer set search_path = '' as $$
declare
  uid uuid := auth.uid();
  f text := app.clean_text(p_first);
  l text := app.clean_text(p_last);
begin
  if uid is null then return app.fail('دسترسی لازم را ندارید.'); end if;
  if f = '' or l = '' then return app.fail('نام و نام خانوادگی را وارد کنید.'); end if;
  if char_length(f) > 40 or char_length(l) > 60 then
    return app.fail('نام یا نام خانوادگی بیش از حد طولانی است.');
  end if;
  update app.profiles set first_name = f, last_name = l where id = uid;
  return app.ok();
end $$;

-- ---------------------------------------------------------------- API: coach reads

create function public.coach_home() returns jsonb
  language plpgsql security definer set search_path = '' as $$
declare
  uid uuid := auth.uid();
  gid uuid;
  g app.gyms;
begin
  if app.role_of(uid) is distinct from 'COACH' then return app.fail('دسترسی لازم را ندارید.'); end if;
  gid := app.coach_gym_id(uid);
  if gid is null then return jsonb_build_object('gym', null); end if;
  select * into g from app.gyms where id = gid;

  return jsonb_build_object(
    'gym', jsonb_build_object(
      'id', g.id, 'name', g.name, 'code', g.code, 'work_days', to_jsonb(g.work_days),
      'open_time', g.open_time, 'close_time', g.close_time, 'card_number', g.card_number,
      'is_owner', g.owner_id = uid),
    'members', coalesce((
      select jsonb_agg(jsonb_build_object(
        'id', m.id, 'first_name', p.first_name, 'last_name', p.last_name, 'phone', p.phone,
        'joined', ((coalesce(m.approved_at, m.created_at)) at time zone 'Asia/Tehran')::date,
        'height_cm', m.height_cm, 'weight_kg', m.weight_kg, 'goal', m.goal, 'program', m.program,
        'notes', m.notes, 'birth_date', m.birth_date,
        'present_last_30', (select count(*) from app.attendance a
                            where a.membership_id = m.id and a.present and a.day >= app.tehran_today() - 29),
        'payments', coalesce((
          select jsonb_agg(jsonb_build_object('id', y.id, 'amount', y.amount, 'paid_on', y.paid_on,
                                              'duration_days', y.duration_days)
                           order by y.paid_on desc, y.created_at desc)
          from app.payments y where y.membership_id = m.id), '[]'::jsonb)
      ))
      from app.memberships m join app.profiles p on p.id = m.athlete_id
      where m.gym_id = gid and m.status = 'APPROVED'), '[]'::jsonb),
    'pending', coalesce((
      select jsonb_agg(jsonb_build_object(
        'id', m.id, 'name', p.first_name || ' ' || p.last_name, 'phone', p.phone,
        'requested', (m.created_at at time zone 'Asia/Tehran')::date) order by m.created_at)
      from app.memberships m join app.profiles p on p.id = m.athlete_id
      where m.gym_id = gid and m.status = 'PENDING'), '[]'::jsonb),
    'plans', coalesce((
      select jsonb_agg(jsonb_build_object('id', pl.id, 'name', pl.name, 'amount', pl.amount,
                                          'duration_days', pl.duration_days) order by pl.created_at)
      from app.plans pl where pl.gym_id = gid), '[]'::jsonb)
  );
end $$;

create function public.coach_attendance(p_day date) returns jsonb
  language plpgsql security definer set search_path = '' as $$
declare
  uid uuid := auth.uid();
  gid uuid := app.coach_gym_id(uid);
begin
  if app.role_of(uid) is distinct from 'COACH' or gid is null then return app.fail('دسترسی لازم را ندارید.'); end if;
  return jsonb_build_object('day', p_day, 'records', coalesce((
    select jsonb_agg(jsonb_build_object('membership_id', a.membership_id, 'present', a.present))
    from app.attendance a join app.memberships m on m.id = a.membership_id
    where m.gym_id = gid and a.day = p_day), '[]'::jsonb));
end $$;

create function public.coach_coaches() returns jsonb
  language plpgsql security definer set search_path = '' as $$
declare uid uuid := auth.uid(); g app.gyms;
begin
  select * into g from app.gyms where owner_id = uid;
  if not found then return app.fail('دسترسی لازم را ندارید.'); end if;
  return jsonb_build_object('coaches', coalesce((
    select jsonb_agg(jsonb_build_object('user_id', p.id, 'name', p.first_name || ' ' || p.last_name, 'phone', p.phone)
                     order by c.created_at)
    from app.gym_coaches c join app.profiles p on p.id = c.user_id
    where c.gym_id = g.id), '[]'::jsonb));
end $$;

-- ---------------------------------------------------------------- API: gym

create function public.create_gym(p_name text) returns jsonb
  language plpgsql security definer set search_path = '' as $$
declare
  uid uuid := auth.uid();
  v_name text := app.clean_text(p_name);
begin
  if app.role_of(uid) is distinct from 'COACH' then return app.fail('دسترسی لازم را ندارید.'); end if;
  if char_length(v_name) < 2 then return app.fail('نام باشگاه را وارد کنید.'); end if;
  if char_length(v_name) > 60 then return app.fail('نام باشگاه بیش از حد طولانی است.'); end if;
  if app.coach_gym_id(uid) is not null then return app.fail('شما قبلاً در یک باشگاه مربی هستید.'); end if;
  for i in 1..5 loop
    begin
      insert into app.gyms (name, code, owner_id) values (v_name, app.make_code(), uid);
      return app.ok();
    exception when unique_violation then
      if app.coach_gym_id(uid) is not null then return app.ok(); end if;  -- double tap: already created
    end;
  end loop;
  return app.fail('ساخت باشگاه انجام نشد. دوباره تلاش کنید.');
end $$;

create function public.suggest_gym_code() returns jsonb
  language plpgsql security definer set search_path = '' as $$
declare uid uuid := auth.uid(); c text;
begin
  if not exists (select 1 from app.gyms where owner_id = uid) then return app.fail('دسترسی لازم را ندارید.'); end if;
  for i in 1..5 loop
    c := app.make_code();
    if not exists (select 1 from app.gyms where code = c) then return jsonb_build_object('code', c); end if;
  end loop;
  return app.fail('کد جدید ساخته نشد. دوباره تلاش کنید.');
end $$;

create function public.update_gym_settings(
  p_name text, p_code text, p_work_days int[], p_open text, p_close text, p_card text
) returns jsonb
  language plpgsql security definer set search_path = '' as $$
declare
  uid uuid := auth.uid();
  g app.gyms;
  v_name text := app.clean_text(p_name);
  v_code text := upper(btrim(app.to_en(p_code)));
  v_days int[];
  v_card text := regexp_replace(app.to_en(p_card), '[\s\-_.]', '', 'g');
begin
  select * into g from app.gyms where owner_id = uid;
  if not found then return app.fail('دسترسی لازم را ندارید.'); end if;

  if char_length(v_name) < 2 then return app.fail('نام باشگاه را وارد کنید.'); end if;
  if char_length(v_name) > 60 then return app.fail('نام باشگاه بیش از حد طولانی است.'); end if;
  if v_code !~ '^[A-Z0-9]{4,6}$' then return app.fail('کد دعوت باید ۴ تا ۶ حرف انگلیسی یا عدد باشد.'); end if;

  v_days := array(select distinct d from unnest(coalesce(p_work_days, '{}')) d where d between 0 and 6 order by d);
  if coalesce(array_length(v_days, 1), 0) = 0 then return app.fail('حداقل یک روز برای باشگاه انتخاب کنید.'); end if;

  if coalesce(p_open, '') !~ '^([01][0-9]|2[0-3]):[0-5][0-9]$'
     or coalesce(p_close, '') !~ '^([01][0-9]|2[0-3]):[0-5][0-9]$' then
    return app.fail('ساعت شروع و پایان معتبر نیست.');
  end if;
  if p_close <= p_open then return app.fail('ساعت پایان باید بعد از ساعت شروع باشد.'); end if;

  if v_card = '' then v_card := null;
  elsif v_card !~ '^[0-9]{16}$' then return app.fail('شماره کارت باید ۱۶ رقم باشد.');
  end if;

  if v_code <> g.code and exists (select 1 from app.gyms where code = v_code) then
    return app.fail('این کد دعوت قبلاً استفاده شده است. کد دیگری انتخاب کنید.');
  end if;

  update app.gyms
     set name = v_name, code = v_code, work_days = v_days,
         open_time = p_open, close_time = p_close, card_number = v_card
   where id = g.id;
  return app.ok();
exception when others then
  return app.fail('ذخیره تنظیمات انجام نشد. دوباره تلاش کنید.');
end $$;

-- ---------------------------------------------------------------- API: memberships

create function public.join_gym(p_code text) returns jsonb
  language plpgsql security definer set search_path = '' as $$
declare
  uid uuid := auth.uid();
  v_code text := upper(btrim(app.to_en(p_code)));
  gid uuid;
begin
  if app.role_of(uid) is distinct from 'ATHLETE' then return app.fail('دسترسی لازم را ندارید.'); end if;
  if exists (select 1 from app.memberships where athlete_id = uid) then
    return app.fail('شما قبلاً عضو یک باشگاه هستید یا درخواست فعال دارید.');
  end if;
  select id into gid from app.gyms where code = v_code;
  if gid is null then return app.fail('باشگاهی با این کد پیدا نشد.'); end if;
  insert into app.memberships (gym_id, athlete_id) values (gid, uid);
  return app.ok();
end $$;

create function public.cancel_request() returns jsonb
  language plpgsql security definer set search_path = '' as $$
declare uid uuid := auth.uid();
begin
  if uid is null then return app.fail('دسترسی لازم را ندارید.'); end if;
  delete from app.memberships where athlete_id = uid and status = 'PENDING';
  return app.ok();
end $$;

create function public.respond_request(p_id uuid, p_approve boolean) returns jsonb
  language plpgsql security definer set search_path = '' as $$
declare uid uuid := auth.uid(); gid uuid := app.coach_gym_id(uid);
begin
  if app.role_of(uid) is distinct from 'COACH' or gid is null then return app.fail('دسترسی لازم را ندارید.'); end if;
  if not exists (select 1 from app.memberships where id = p_id and gym_id = gid and status = 'PENDING') then
    return app.fail('این درخواست پیدا نشد.');
  end if;
  if p_approve then
    update app.memberships set status = 'APPROVED', approved_at = now() where id = p_id;
  else
    delete from app.memberships where id = p_id;
  end if;
  return app.ok();
end $$;

create function public.remove_member(p_id uuid) returns jsonb
  language plpgsql security definer set search_path = '' as $$
declare uid uuid := auth.uid(); gid uuid := app.coach_gym_id(uid); n int;
begin
  if app.role_of(uid) is distinct from 'COACH' or gid is null then return app.fail('دسترسی لازم را ندارید.'); end if;
  delete from app.memberships where id = p_id and gym_id = gid;
  get diagnostics n = row_count;
  if n = 0 then return app.fail('عضو پیدا نشد.'); end if;
  return app.ok();
end $$;

create function public.update_member_profile(
  p_membership uuid, p_height numeric, p_weight numeric,
  p_goal text, p_program text, p_notes text, p_birth date
) returns jsonb
  language plpgsql security definer set search_path = '' as $$
declare
  uid uuid := auth.uid();
  gid uuid := app.coach_gym_id(uid);
  v_h int;
  v_w numeric(4, 1);
  v_goal text := nullif(btrim(coalesce(p_goal, '')), '');
  v_prog text := nullif(btrim(coalesce(p_program, '')), '');
  v_notes text := nullif(btrim(coalesce(p_notes, '')), '');
  n int;
begin
  if app.role_of(uid) is distinct from 'COACH' or gid is null then return app.fail('دسترسی لازم را ندارید.'); end if;
  if p_height is not null then
    v_h := round(p_height)::int;
    if v_h not between 50 and 260 then return app.fail('قد باید بین ۵۰ تا ۲۶۰ سانتی‌متر باشد.'); end if;
  end if;
  if p_weight is not null then
    if round(p_weight, 1) not between 20 and 300 then return app.fail('وزن باید بین ۲۰ تا ۳۰۰ کیلوگرم باشد.'); end if;
    v_w := round(p_weight, 1);
  end if;
  if char_length(coalesce(v_goal, '')) > 80 then return app.fail('هدف حداکثر ۸۰ حرف باشد.'); end if;
  if char_length(coalesce(v_prog, '')) > 2000 then return app.fail('برنامه حداکثر ۲۰۰۰ حرف باشد.'); end if;
  if char_length(coalesce(v_notes, '')) > 1000 then return app.fail('یادداشت حداکثر ۱۰۰۰ حرف باشد.'); end if;
  if p_birth is not null and (p_birth > app.tehran_today() or p_birth < date '1900-01-01') then
    return app.fail('تاریخ تولد معتبر نیست.');
  end if;

  update app.memberships
     set height_cm = v_h, weight_kg = v_w, goal = v_goal, program = v_prog, notes = v_notes, birth_date = p_birth
   where id = p_membership and gym_id = gid and status = 'APPROVED';
  get diagnostics n = row_count;
  if n = 0 then return app.fail('عضو پیدا نشد.'); end if;
  return app.ok();
end $$;

-- ---------------------------------------------------------------- API: tuition

create function public.save_payment(
  p_membership uuid, p_amount bigint, p_paid_on date, p_duration int, p_id uuid default null
) returns jsonb
  language plpgsql security definer set search_path = '' as $$
declare uid uuid := auth.uid(); gid uuid := app.coach_gym_id(uid); n int;
begin
  if app.role_of(uid) is distinct from 'COACH' or gid is null then return app.fail('دسترسی لازم را ندارید.'); end if;
  if p_amount is null or p_amount <= 0 or p_amount > 2000000000 then return app.fail('مبلغ شهریه را درست وارد کنید.'); end if;
  if p_duration is null or p_duration not in (30, 60, 90) then return app.fail('مدت شهریه باید ۳۰، ۶۰ یا ۹۰ روز باشد.'); end if;
  if p_paid_on is null then return app.fail('تاریخ پرداخت معتبر نیست.'); end if;
  if not exists (select 1 from app.memberships where id = p_membership and gym_id = gid and status = 'APPROVED') then
    return app.fail('عضو انتخاب‌شده پیدا نشد.');
  end if;

  if p_id is not null then
    update app.payments y
       set membership_id = p_membership, amount = p_amount::int, paid_on = p_paid_on, duration_days = p_duration
     where y.id = p_id and exists (select 1 from app.memberships m where m.id = y.membership_id and m.gym_id = gid);
    get diagnostics n = row_count;
    if n = 0 then return app.fail('پرداخت پیدا نشد.'); end if;
  else
    insert into app.payments (membership_id, amount, paid_on, duration_days)
    values (p_membership, p_amount::int, p_paid_on, p_duration);
  end if;
  return app.ok();
end $$;

create function public.delete_payment(p_id uuid) returns jsonb
  language plpgsql security definer set search_path = '' as $$
declare uid uuid := auth.uid(); gid uuid := app.coach_gym_id(uid); n int;
begin
  if app.role_of(uid) is distinct from 'COACH' or gid is null then return app.fail('دسترسی لازم را ندارید.'); end if;
  delete from app.payments y
   where y.id = p_id and exists (select 1 from app.memberships m where m.id = y.membership_id and m.gym_id = gid);
  get diagnostics n = row_count;
  if n = 0 then return app.fail('پرداخت پیدا نشد.'); end if;
  return app.ok();
end $$;

-- ---------------------------------------------------------------- API: attendance

create function public.save_attendance(p_day date, p_records jsonb) returns jsonb
  language plpgsql security definer set search_path = '' as $$
declare uid uuid := auth.uid(); gid uuid := app.coach_gym_id(uid); g app.gyms;
begin
  if app.role_of(uid) is distinct from 'COACH' or gid is null then return app.fail('دسترسی لازم را ندارید.'); end if;
  if p_day is null then return app.fail('تاریخ معتبر نیست.'); end if;
  select * into g from app.gyms where id = gid;
  if not (extract(dow from p_day)::int = any (g.work_days)) then
    return app.fail('باشگاه در این روز فعال نیست و حضور و غیاب ثبت نمی‌شود.');
  end if;
  if p_records is null or jsonb_typeof(p_records) <> 'array' then return app.fail('اطلاعات حضور و غیاب معتبر نیست.'); end if;

  insert into app.attendance (membership_id, day, present)
  select m.id, p_day, coalesce((r ->> 'present')::boolean, false)
  from jsonb_array_elements(p_records) r
  join app.memberships m on m.id = (r ->> 'membership_id')::uuid and m.gym_id = gid and m.status = 'APPROVED'
  on conflict (membership_id, day) do update set present = excluded.present;
  return app.ok();
end $$;

-- ---------------------------------------------------------------- API: assistant coaches (owner only)

create function public.add_coach(p_phone text) returns jsonb
  language plpgsql security definer set search_path = '' as $$
declare
  uid uuid := auth.uid();
  g app.gyms;
  v_phone text := regexp_replace(app.to_en(p_phone), '[\s-]', '', 'g');
  t app.profiles;
begin
  select * into g from app.gyms where owner_id = uid;
  if not found then return app.fail('دسترسی لازم را ندارید.'); end if;
  if v_phone !~ '^09[0-9]{9}$' then return app.fail('شماره تلفن باید مثل ۰۹۱۲۱۲۳۴۵۶۷ باشد.'); end if;
  select * into t from app.profiles where phone = v_phone;
  if not found or t.role <> 'COACH' then
    return app.fail('مربی‌ای با این شماره پیدا نشد. اول باید با نقش «مربی» ثبت‌نام کند.');
  end if;
  if t.id = g.owner_id then return app.fail('این شماره صاحب باشگاه است.'); end if;
  if app.coach_gym_id(t.id) is not null then return app.fail('این مربی قبلاً در یک باشگاه عضو است.'); end if;
  insert into app.gym_coaches (gym_id, user_id) values (g.id, t.id);
  return app.ok();
end $$;

create function public.remove_coach(p_user uuid) returns jsonb
  language plpgsql security definer set search_path = '' as $$
declare uid uuid := auth.uid(); g app.gyms; n int;
begin
  select * into g from app.gyms where owner_id = uid;
  if not found then return app.fail('دسترسی لازم را ندارید.'); end if;
  delete from app.gym_coaches where gym_id = g.id and user_id = p_user;
  get diagnostics n = row_count;
  if n = 0 then return app.fail('مربی پیدا نشد.'); end if;
  return app.ok();
end $$;

-- ---------------------------------------------------------------- API: tuition plans (owner only)

create function public.save_plan(p_name text, p_amount bigint, p_duration int, p_id uuid default null) returns jsonb
  language plpgsql security definer set search_path = '' as $$
declare
  uid uuid := auth.uid();
  g app.gyms;
  v_name text := app.clean_text(p_name);
  n int;
begin
  select * into g from app.gyms where owner_id = uid;
  if not found then return app.fail('دسترسی لازم را ندارید.'); end if;
  if char_length(v_name) < 2 or char_length(v_name) > 40 then return app.fail('نام پلن باید ۲ تا ۴۰ حرف باشد.'); end if;
  if p_amount is null or p_amount <= 0 or p_amount > 2000000000 then return app.fail('مبلغ پلن را درست وارد کنید.'); end if;
  if p_duration is null or p_duration not in (30, 60, 90) then return app.fail('مدت پلن باید ۳۰، ۶۰ یا ۹۰ روز باشد.'); end if;

  if p_id is not null then
    update app.plans set name = v_name, amount = p_amount::int, duration_days = p_duration
     where id = p_id and gym_id = g.id;
    get diagnostics n = row_count;
    if n = 0 then return app.fail('پلن پیدا نشد.'); end if;
  else
    if (select count(*) from app.plans where gym_id = g.id) >= 12 then
      return app.fail('حداکثر ۱۲ پلن می‌توانید داشته باشید.');
    end if;
    insert into app.plans (gym_id, name, amount, duration_days) values (g.id, v_name, p_amount::int, p_duration);
  end if;
  return app.ok();
end $$;

create function public.delete_plan(p_id uuid) returns jsonb
  language plpgsql security definer set search_path = '' as $$
declare uid uuid := auth.uid(); g app.gyms; n int;
begin
  select * into g from app.gyms where owner_id = uid;
  if not found then return app.fail('دسترسی لازم را ندارید.'); end if;
  delete from app.plans where id = p_id and gym_id = g.id;
  get diagnostics n = row_count;
  if n = 0 then return app.fail('پلن پیدا نشد.'); end if;
  return app.ok();
end $$;

-- ---------------------------------------------------------------- API: check-in QR

-- Any coach of the gym can show / print the QR; only the owner can replace it.
create function public.checkin_info() returns jsonb
  language plpgsql security definer set search_path = '' as $$
declare uid uuid := auth.uid(); gid uuid := app.coach_gym_id(uid); g app.gyms;
begin
  if app.role_of(uid) is distinct from 'COACH' or gid is null then return app.fail('دسترسی لازم را ندارید.'); end if;
  select * into g from app.gyms where id = gid;
  return jsonb_build_object('gym_id', g.id, 'gym_name', g.name, 'token', g.checkin_token, 'is_owner', g.owner_id = uid);
end $$;

create function public.regenerate_checkin_token() returns jsonb
  language plpgsql security definer set search_path = '' as $$
declare uid uuid := auth.uid(); g app.gyms;
begin
  select * into g from app.gyms where owner_id = uid;
  if not found then return app.fail('دسترسی لازم را ندارید.'); end if;
  update app.gyms
     set checkin_token = replace(gen_random_uuid()::text || gen_random_uuid()::text, '-', '')
   where id = g.id;
  return app.ok();
end $$;

-- Athlete scanned the gym QR.
create function public.check_in(p_gym uuid, p_token text) returns jsonb
  language plpgsql security definer set search_path = '' as $$
declare
  uid uuid := auth.uid();
  m app.memberships;
  g app.gyms;
  today date := app.tehran_today();
  mins int := app.tehran_minutes();
begin
  if app.role_of(uid) is distinct from 'ATHLETE' then return app.fail('ثبت حضور با QR فقط برای ورزشکاران است.'); end if;
  select * into m from app.memberships where athlete_id = uid;
  if not found or m.status <> 'APPROVED' or m.gym_id is distinct from p_gym then
    return app.fail('شما عضو تاییدشده‌ی این باشگاه نیستید.');
  end if;
  select * into g from app.gyms where id = m.gym_id;
  if p_token is null or p_token <> g.checkin_token then
    return app.fail('این QR دیگر معتبر نیست. QR جدید را از مربی بگیرید.');
  end if;
  if not (extract(dow from today)::int = any (g.work_days)) then return app.fail('باشگاه امروز فعال نیست.'); end if;
  if mins < app.hhmm_to_min(g.open_time) - 30 or mins > app.hhmm_to_min(g.close_time) + 30 then
    return app.fail('الان خارج از ساعت کاری باشگاه است.');
  end if;
  insert into app.attendance (membership_id, day, present) values (m.id, today, true)
  on conflict (membership_id, day) do update set present = true;
  return jsonb_build_object('ok', true, 'gym_name', g.name);
end $$;

-- ---------------------------------------------------------------- API: athlete

create function public.athlete_home() returns jsonb
  language plpgsql security definer set search_path = '' as $$
declare
  uid uuid := auth.uid();
  m app.memberships;
  g app.gyms;
  o app.profiles;
begin
  if app.role_of(uid) is distinct from 'ATHLETE' then return app.fail('دسترسی لازم را ندارید.'); end if;
  select * into m from app.memberships where athlete_id = uid;
  if not found then return jsonb_build_object('membership', null); end if;
  select * into g from app.gyms where id = m.gym_id;
  select * into o from app.profiles where id = g.owner_id;

  return jsonb_build_object('membership', jsonb_build_object(
    'id', m.id, 'status', m.status,
    'gym', jsonb_build_object('id', g.id, 'name', g.name, 'coach_name', o.first_name || ' ' || o.last_name,
                              'work_days', to_jsonb(g.work_days), 'open_time', g.open_time,
                              'close_time', g.close_time, 'card_number', g.card_number),
    'height_cm', m.height_cm, 'weight_kg', m.weight_kg, 'goal', m.goal, 'program', m.program,
    'birth_date', m.birth_date,
    'payments', coalesce((
      select jsonb_agg(jsonb_build_object('id', y.id, 'amount', y.amount, 'paid_on', y.paid_on,
                                          'duration_days', y.duration_days)
                       order by y.paid_on desc, y.created_at desc)
      from app.payments y where y.membership_id = m.id), '[]'::jsonb),
    'attendance', coalesce((
      select jsonb_agg(jsonb_build_object('day', a.day, 'present', a.present) order by a.day desc)
      from app.attendance a where a.membership_id = m.id and a.day >= app.tehran_today() - 29), '[]'::jsonb)
  ));
end $$;

-- ---------------------------------------------------------------- grants: only signed-in users may call the API

do $$
declare f record;
begin
  for f in
    select p.oid::regprocedure as sig
    from pg_proc p join pg_namespace n on n.oid = p.pronamespace
    where n.nspname = 'public'
      and p.proname = any (array[
        'me', 'update_my_profile', 'coach_home', 'coach_attendance', 'coach_coaches', 'create_gym',
        'suggest_gym_code', 'update_gym_settings', 'join_gym', 'cancel_request', 'respond_request',
        'remove_member', 'update_member_profile', 'save_payment', 'delete_payment', 'save_attendance',
        'add_coach', 'remove_coach', 'save_plan', 'delete_plan', 'checkin_info',
        'regenerate_checkin_token', 'check_in', 'athlete_home'])
  loop
    execute format('revoke all on function %s from public, anon', f.sig);
    execute format('grant execute on function %s to authenticated', f.sig);
  end loop;
end $$;
