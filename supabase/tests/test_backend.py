import subprocess, json, uuid, sys
def psql(sql, role=None, uid=None, expect_error=False):
    pre = ''
    if role: pre += f"set role {role}; "
    if uid: pre += f"select set_config('request.jwt.claim.sub','{uid}',false) \\g /dev/null\n"
    p = subprocess.run(['su','postgres','-c',f"psql -X -At -q -d gymtest -v ON_ERROR_STOP=1"], input=pre+sql, capture_output=True, text=True)
    return p.returncode, p.stdout.strip(), p.stderr.strip()
def sup(sql):
    rc,o,e = psql(sql); assert rc==0, (sql, e); return o
def rpc(uid, call, role='authenticated'):
    rc,o,e = psql(f"select public.{call};", role=role, uid=uid)
    if rc: return {'_sqlerr': e.splitlines()[0] if e else 'err'}
    return json.loads(o) if o else None
fails=0
def check(name, cond, extra=''):
    global fails
    print(('PASS ' if cond else 'FAIL ')+name+(' -> '+str(extra) if not cond else ''))
    if not cond: fails+=1
def mk(phone, first, last, role):
    uid = str(uuid.uuid4())
    sup(f"insert into auth.users(id,email,raw_user_meta_data) values ('{uid}','{phone}@gymyar.app','{json.dumps({'first_name':first,'last_name':last,'role':role}, ensure_ascii=False)}');")
    return uid
sup("truncate auth.users cascade;")
# trigger validation
rc,_,e = psql("insert into auth.users(email,raw_user_meta_data) values ('abc@gymyar.app','{\"first_name\":\"a\",\"last_name\":\"b\",\"role\":\"COACH\"}');")
check('bad phone rejected by trigger', rc!=0)
rc,_,e = psql("insert into auth.users(email,raw_user_meta_data) values ('09120000009@gymyar.app','{\"role\":\"COACH\"}');")
check('missing name rejected by trigger', rc!=0)

A = mk('09120000001','علی','محمدی','COACH')
B = mk('09120000002','رضا','احمدی','COACH')
X = mk('09130000001','سارا','کریمی','ATHLETE')
Y = mk('09130000002','نیما','حسینی','ATHLETE')
check('profile row created', sup("select count(*) from app.profiles")=='4')
print('me:', rpc(A,'me()'))
check('me coach', rpc(A,'me()')['role']=='COACH')
check('anon cannot call rpc', 'permission denied' in str(rpc(A,'me()',role='anon')))
rc,_,e = psql("select * from app.gyms;", role='authenticated', uid=A); check('authenticated cannot read tables', rc!=0)
rc,_,e = psql("select * from public.me();", role='authenticated', uid=A)

# role guards
check('athlete cannot create gym', 'error' in rpc(X,"create_gym('باشگاه')"))
check('coach home without gym', rpc(A,'coach_home()')=={'gym':None})
check('short name rejected', 'error' in rpc(A,"create_gym('ا')"))
check('create gym ok', rpc(A,"create_gym('  باشگاه   آرمان ')")=={'ok':True})
check('second gym rejected', 'error' in rpc(A,"create_gym('دوم')"))
home = rpc(A,'coach_home()'); g = home['gym']
check('gym name cleaned', g['name']=='باشگاه آرمان', g)
check('gym default days', g['work_days']==[0,1,2,3,4,5,6] and g['is_owner'] is True)
code = g['code']; check('code 6 chars', len(code)==6)

# settings
check('settings bad code', 'error' in rpc(A,"update_gym_settings('باشگاه آرمان','ab',array[0,1],'06:00','22:00','')"))
check('settings bad time order', 'error' in rpc(A,"update_gym_settings('باشگاه آرمان','ARMAN1',array[0,1],'22:00','06:00','')"))
check('settings bad card', 'error' in rpc(A,"update_gym_settings('باشگاه آرمان','ARMAN1',array[0,1],'06:00','22:00','1234')"))
check('settings no days', 'error' in rpc(A,"update_gym_settings('باشگاه آرمان','ARMAN1',array[]::int[],'06:00','22:00','')"))
check('settings ok (persian digits card)', rpc(A,"update_gym_settings('باشگاه آرمان','arman1',array[6,1,3,3,9],'00:00','23:59','۶۰۳۷ ۹۹۱۲-۳۴۵۶-۷۸۹۰')")=={'ok':True})
g = rpc(A,'coach_home()')['gym']
check('settings saved', g['code']=='ARMAN1' and g['work_days']==[1,3,6] and g['card_number']=='6037991234567890', g)
check('suggest code', len(rpc(A,'suggest_gym_code()')['code'])==6)
check('athlete cannot update settings', 'error' in rpc(X,"update_gym_settings('x','ARMAN2',array[1],'06:00','22:00','')"))

# join flow
check('wrong code', rpc(X,"join_gym('ZZZZZZ')")['error']=='باشگاهی با این کد پیدا نشد.')
check('join (lowercase code)', rpc(X,"join_gym('arman1')")=={'ok':True})
check('join twice rejected', 'error' in rpc(X,"join_gym('ARMAN1')"))
ah = rpc(X,'athlete_home()')['membership']
check('athlete pending', ah['status']=='PENDING' and ah['gym']['name']=='باشگاه آرمان' and ah['gym']['coach_name']=='علی محمدی', ah)
check('Y joins then cancels', rpc(Y,"join_gym('ARMAN1')")=={'ok':True} and rpc(Y,'cancel_request()')=={'ok':True} and rpc(Y,'athlete_home()')['membership'] is None)
pend = rpc(A,'coach_home()')['pending']; check('pending listed', len(pend)==1 and pend[0]['phone']=='09130000001', pend)
mid = pend[0]['id']
check('other coach (no gym) cannot approve', 'error' in rpc(B,f"respond_request('{mid}',true)"))
check('approve', rpc(A,f"respond_request('{mid}',true)")=={'ok':True})
check('approve unknown', 'error' in rpc(A,f"respond_request('{mid}',true)"))

# profile
check('profile bad height', 'error' in rpc(A,f"update_member_profile('{mid}',10,70,null)"))
check('profile future birth', 'error' in rpc(A,f"update_member_profile('{mid}',180,70,date '2999-01-01')"))
check('profile ok', rpc(A,f"update_member_profile('{mid}',180.4,72.46,date '1995-03-21')")=={'ok':True})

# payments
check('payment bad duration', 'error' in rpc(A,f"save_payment('{mid}',1000,current_date,45)"))
check('payment bad amount', 'error' in rpc(A,f"save_payment('{mid}',0,current_date,30)"))
check('payment ok', rpc(A,f"save_payment('{mid}',500000,current_date - 10,30)")=={'ok':True})
check('payment 2 ok', rpc(A,f"save_payment('{mid}',900000,current_date - 3,60)")=={'ok':True})
mem = rpc(A,'coach_home()')['members']
check('member listed with payments', len(mem)==1 and len(mem[0]['payments'])==2 and mem[0]['payments'][0]['amount']==900000, mem)
check('profile saved', mem[0]['height_cm']==180 and float(mem[0]['weight_kg'])==72.5 and mem[0]['birth_date']=='1995-03-21', mem[0])
pid = mem[0]['payments'][1]['id']
check('payment edit', rpc(A,f"save_payment('{mid}',600000,current_date - 10,30,'{pid}')")=={'ok':True})
check('payment delete', rpc(A,f"delete_payment('{pid}')")=={'ok':True} and len(rpc(A,'coach_home()')['members'][0]['payments'])==1)
check('payment delete unknown', 'error' in rpc(A,f"delete_payment('{pid}')"))

# athlete view: no coach-only data
ah = rpc(X,'athlete_home()')['membership']
check('athlete approved sees own data (no plan/notes fields)', ah['status']=='APPROVED' and 'program' not in ah and 'notes' not in ah and 'goal' not in ah and len(ah['payments'])==1, ah)
check('athlete cannot call coach_home', 'error' in rpc(X,'coach_home()'))
check('athlete cannot save payment', 'error' in rpc(X,f"save_payment('{mid}',1000,current_date,30)"))

# attendance (work days 1,3,6 = Mon, Wed, Sat)
mon = sup("select (date_trunc('week', current_date) + interval '0 day')::date"); tue = sup("select (date_trunc('week', current_date) + interval '1 day')::date")
check('attendance on closed day rejected', 'error' in rpc(A,f"save_attendance(date '{tue}', '[{{\"membership_id\":\"{mid}\",\"present\":true}}]'::jsonb)"))
check('attendance on open day', rpc(A,f"save_attendance(date '{mon}', '[{{\"membership_id\":\"{mid}\",\"present\":true}}]'::jsonb)")=={'ok':True})
check('attendance overwrite', rpc(A,f"save_attendance(date '{mon}', '[{{\"membership_id\":\"{mid}\",\"present\":false}}]'::jsonb)")=={'ok':True})
rec = rpc(A,f"coach_attendance(date '{mon}')")['records']; check('attendance read', rec==[{'membership_id':mid,'present':False}], rec)
check('attendance bad payload', 'error' in rpc(A,f"save_attendance(date '{mon}', '{{}}'::jsonb)"))

# check-in
info = rpc(A,'checkin_info()'); check('qr info', info['is_owner'] and len(info['token'])==64, info)
gid, tok = info['gym_id'], info['token']
today_dow = int(sup("select extract(dow from (now() at time zone 'Asia/Tehran')::date)::int"))
# open every day for the check-in test
rpc(A,"update_gym_settings('باشگاه آرمان','ARMAN1',array[0,1,2,3,4,5,6],'00:00','23:59','')")
check('checkin wrong token', 'error' in rpc(X,f"check_in('{gid}','nope')"))
check('checkin ok', rpc(X,f"check_in('{gid}','{tok}')")['ok'] is True)
check('checkin idempotent', rpc(X,f"check_in('{gid}','{tok}')")['ok'] is True)
ah = rpc(X,'athlete_home()')['membership']; check('athlete attendance shows today', any(a['present'] for a in ah['attendance']), ah['attendance'])
check('coach cannot check in', 'error' in rpc(A,f"check_in('{gid}','{tok}')"))
check('checkin: not a member (Y)', 'error' in rpc(Y,f"check_in('{gid}','{tok}')"))
check('regenerate by owner', rpc(A,'regenerate_checkin_token()')=={'ok':True})
check('old qr dead', 'error' in rpc(X,f"check_in('{gid}','{tok}')"))
# closed hours
rpc(A,"update_gym_settings('باشگاه آرمان','ARMAN1',array[0,1,2,3,4,5,6],'03:00','03:30','')")
tok2 = rpc(A,'checkin_info()')['token']
nowmin = int(sup("select (extract(hour from now() at time zone 'Asia/Tehran')*60+extract(minute from now() at time zone 'Asia/Tehran'))::int"))
res = rpc(X,f"check_in('{gid}','{tok2}')")
check('checkin outside hours (window 02:30-04:00)', ('error' in res) == (not (150 <= nowmin <= 240)), (nowmin,res))
rpc(A,"update_gym_settings('باشگاه آرمان','ARMAN1',array[0,1,2,3,4,5,6],'06:00','22:00','')")

# assistant coach
check('add coach: unknown phone', 'error' in rpc(A,"add_coach('09999999999')"))
check('add coach: athlete phone', 'error' in rpc(A,"add_coach('09130000001')"))
check('add coach: owner themself', 'error' in rpc(A,"add_coach('09120000001')"))
check('add coach ok (persian digits)', rpc(A,"add_coach('۰۹۱۲۰۰۰۰۰۰۲')")=={'ok':True})
check('add coach twice', 'error' in rpc(A,"add_coach('09120000002')"))
bh = rpc(B,'coach_home()'); check('assistant sees gym, not owner', bh['gym']['id']==gid and bh['gym']['is_owner'] is False and len(bh['members'])==1, bh['gym'])
check('assistant can save payment', rpc(B,f"save_payment('{mid}',700000,current_date,30)")=={'ok':True})
check('assistant cannot edit settings', 'error' in rpc(B,"update_gym_settings('x','ARMAN9',array[1],'06:00','22:00','')"))
check('assistant cannot manage plans', 'error' in rpc(B,"save_plan('پلن',100000,30)"))
check('assistant can view qr, not owner', rpc(B,'checkin_info()')['is_owner'] is False)
check('assistant cannot regenerate', 'error' in rpc(B,'regenerate_checkin_token()'))
cs = rpc(A,'coach_coaches()'); check('coaches list', len(cs['coaches'])==1 and cs['coaches'][0]['phone']=='09120000002', cs)
check('assistant cannot list coaches', 'error' in rpc(B,'coach_coaches()'))
check('remove coach', rpc(A,f"remove_coach('{B}')")=={'ok':True} and rpc(B,'coach_home()')=={'gym':None})

# plans
check('plan bad name', 'error' in rpc(A,"save_plan('ا',100000,30)"))
check('plan ok', rpc(A,"save_plan('ماهانه',1500000,30)")=={'ok':True})
pl = rpc(A,'coach_home()')['plans']; check('plan listed', len(pl)==1 and pl[0]['amount']==1500000, pl)
check('plan edit', rpc(A,f"save_plan('سه ماهه',4000000,90,'{pl[0]['id']}')")=={'ok':True} and rpc(A,'coach_home()')['plans'][0]['duration_days']==90)
for i in range(11): rpc(A,f"save_plan('پلن {i}',1000,30)")
check('plan limit 12', 'error' in rpc(A,"save_plan('اضافه',1000,30)"))
check('plan delete', rpc(A,f"delete_plan('{pl[0]['id']}')")=={'ok':True})

# member removal cascades
check('remove member', rpc(A,f"remove_member('{mid}')")=={'ok':True})
check('cascade cleaned', sup("select (select count(*) from app.payments)||','||(select count(*) from app.attendance)")=='0,0')
check('athlete back to no gym', rpc(X,'athlete_home()')['membership'] is None)
check('update my profile', rpc(X,"update_my_profile('  سارا ','  کریمی   زاده ')")=={'ok':True} and rpc(X,'me()')['last_name']=='کریمی زاده')
check('update my profile empty', 'error' in rpc(X,"update_my_profile('','x')"))
# deleting an auth user cascades everything
sup(f"delete from auth.users where id='{A}'")
check('owner delete cascades gym', sup("select count(*) from app.gyms")=='0')
print('\nFAILS:', fails); sys.exit(1 if fails else 0)
