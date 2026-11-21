-- باشگاه‌یار: انتخاب «مربی اصلی» که به ورزشکاران نشان داده می‌شود.
-- پیش‌فرض (null) همان صاحب باشگاه است؛ صاحب باشگاه می‌تواند یکی از مربیان کمکی را انتخاب کند.
-- فقط یک بار در SQL Editor اجرا کنید (بعد از init و remove_training_plan).

alter table app.gyms
  add column if not exists display_coach_id uuid references app.profiles (id) on delete set null;

-- ---------------------------------------------------------------- owner: who is shown as the main coach

create or replace function public.set_display_coach(p_user uuid default null) returns jsonb
  language plpgsql security definer set search_path = '' as $$
declare uid uuid := auth.uid(); g app.gyms;
begin
  select * into g from app.gyms where owner_id = uid;
  if not found then return app.fail('دسترسی لازم را ندارید.'); end if;
  if p_user is null or p_user = g.owner_id then
    update app.gyms set display_coach_id = null where id = g.id;
    return app.ok();
  end if;
  if not exists (select 1 from app.gym_coaches c where c.gym_id = g.id and c.user_id = p_user) then
    return app.fail('این مربی جزو مربیان باشگاه نیست.');
  end if;
  update app.gyms set display_coach_id = p_user where id = g.id;
  return app.ok();
end $$;

-- removing an assistant coach who was the displayed main coach falls back to the owner
create or replace function public.remove_coach(p_user uuid) returns jsonb
  language plpgsql security definer set search_path = '' as $$
declare uid uuid := auth.uid(); g app.gyms; n int;
begin
  select * into g from app.gyms where owner_id = uid;
  if not found then return app.fail('دسترسی لازم را ندارید.'); end if;
  delete from app.gym_coaches where gym_id = g.id and user_id = p_user;
  get diagnostics n = row_count;
  if n = 0 then return app.fail('مربی پیدا نشد.'); end if;
  update app.gyms set display_coach_id = null where id = g.id and display_coach_id = p_user;
  return app.ok();
end $$;

-- the owner is returned too, so the app can show every coach with the main one marked
create or replace function public.coach_coaches() returns jsonb
  language plpgsql security definer set search_path = '' as $$
declare uid uuid := auth.uid(); g app.gyms; o app.profiles;
begin
  select * into g from app.gyms where owner_id = uid;
  if not found then return app.fail('دسترسی لازم را ندارید.'); end if;
  select * into o from app.profiles where id = g.owner_id;
  return jsonb_build_object(
    'owner', jsonb_build_object('user_id', o.id, 'name', o.first_name || ' ' || o.last_name, 'phone', o.phone),
    'display_coach_id', g.display_coach_id,
    'coaches', coalesce((
      select jsonb_agg(jsonb_build_object('user_id', p.id, 'name', p.first_name || ' ' || p.last_name, 'phone', p.phone)
                       order by c.created_at)
      from app.gym_coaches c join app.profiles p on p.id = c.user_id
      where c.gym_id = g.id), '[]'::jsonb));
end $$;

-- athletes see the chosen main coach (coach_name) plus the full list of coaches
create or replace function public.athlete_home() returns jsonb
  language plpgsql security definer set search_path = '' as $$
declare
  uid uuid := auth.uid();
  m app.memberships;
  g app.gyms;
  o app.profiles;
  main_id uuid;
begin
  if app.role_of(uid) is distinct from 'ATHLETE' then return app.fail('دسترسی لازم را ندارید.'); end if;
  select * into m from app.memberships where athlete_id = uid;
  if not found then return jsonb_build_object('membership', null); end if;
  select * into g from app.gyms where id = m.gym_id;
  main_id := coalesce(g.display_coach_id, g.owner_id);
  select * into o from app.profiles where id = main_id;

  return jsonb_build_object('membership', jsonb_build_object(
    'id', m.id, 'status', m.status,
    'gym', jsonb_build_object('id', g.id, 'name', g.name, 'coach_name', o.first_name || ' ' || o.last_name,
                              'coaches', coalesce((
                                select jsonb_agg(x.j order by x.ord, x.created_at)
                                from (
                                  select jsonb_build_object('name', p.first_name || ' ' || p.last_name,
                                                            'is_main', p.id = main_id, 'is_owner', true) as j,
                                         (p.id <> main_id)::int as ord, g.created_at as created_at
                                  from app.profiles p where p.id = g.owner_id
                                  union all
                                  select jsonb_build_object('name', p.first_name || ' ' || p.last_name,
                                                            'is_main', p.id = main_id, 'is_owner', false),
                                         (p.id <> main_id)::int, c.created_at
                                  from app.gym_coaches c join app.profiles p on p.id = c.user_id
                                  where c.gym_id = g.id
                                ) x), '[]'::jsonb),
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

revoke all on function public.set_display_coach(uuid) from public, anon;
grant execute on function public.set_display_coach(uuid) to authenticated;
