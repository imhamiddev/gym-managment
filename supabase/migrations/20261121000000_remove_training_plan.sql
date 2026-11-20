-- حذف «برنامه تمرینی»، «هدف» و «یادداشت خصوصی مربی» از پایگاه داده.
-- فقط برای پروژه‌هایی که قبلاً init.sql را اجرا کرده‌اند (یک بار در SQL Editor اجرا کنید).
-- نصب تازه نیازی به این فایل ندارد؛ init.sql همین تغییرات را دارد.

alter table app.memberships drop column if exists goal;
alter table app.memberships drop column if exists program;
alter table app.memberships drop column if exists notes;

-- امضای تابع عوض شده (p_goal / p_program / p_notes حذف شدند)
drop function if exists public.update_member_profile(uuid, numeric, numeric, text, text, text, date);

create or replace function public.update_member_profile(
  p_membership uuid, p_height numeric, p_weight numeric, p_birth date
) returns jsonb
  language plpgsql security definer set search_path = '' as $$
declare
  uid uuid := auth.uid();
  gid uuid := app.coach_gym_id(uid);
  v_h int;
  v_w numeric(4, 1);
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
  if p_birth is not null and (p_birth > app.tehran_today() or p_birth < date '1900-01-01') then
    return app.fail('تاریخ تولد معتبر نیست.');
  end if;

  update app.memberships
     set height_cm = v_h, weight_kg = v_w, birth_date = p_birth
   where id = p_membership and gym_id = gid and status = 'APPROVED';
  get diagnostics n = row_count;
  if n = 0 then return app.fail('عضو پیدا نشد.'); end if;
  return app.ok();
end $$;

create or replace function public.coach_home() returns jsonb
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
        'height_cm', m.height_cm, 'weight_kg', m.weight_kg, 'birth_date', m.birth_date,
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

create or replace function public.athlete_home() returns jsonb
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
    'height_cm', m.height_cm, 'weight_kg', m.weight_kg, 'birth_date', m.birth_date,
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

revoke all on function public.update_member_profile(uuid, numeric, numeric, date) from public, anon;
grant execute on function public.update_member_profile(uuid, numeric, numeric, date) to authenticated;
