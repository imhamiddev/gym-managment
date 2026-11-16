"use client";
import { useState, useTransition } from "react";
import { useRouter } from "next/navigation";
import { Button } from "@/components/arc/button/button";
import SegmentedControl from "@/components/arc/segmented-control/segmented-control";
import { JalaliField } from "./JalaliField";
import { saveAttendance } from "@/lib/actions";
import { jalaliToYmd, toFa, ymdAdd, ymdToJalali } from "@/lib/date";

export function AttendanceBoard({
  ymd,
  members,
  initial,
}: {
  ymd: string;
  members: { id: string; name: string }[];
  initial: Record<string, boolean>;
}) {
  const router = useRouter();
  const [dateText, setDateText] = useState(ymdToJalali(ymd));
  const [marks, setMarks] = useState<Record<string, string>>(() =>
    Object.fromEntries(members.map((m) => [m.id, initial[m.id] ? "present" : "absent"])),
  );
  const [saved, setSaved] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [pending, start] = useTransition();

  const go = (next: string) => router.push(`/coach/attendance?d=${next}`);
  const change = (text: string) => {
    setDateText(text);
    const next = jalaliToYmd(text);
    if (next && next !== ymd) go(next);
  };
  const setMark = (id: string, v: string) => { setMarks({ ...marks, [id]: v }); setSaved(false); };
  const presentCount = Object.values(marks).filter((v) => v === "present").length;

  const save = () =>
    start(async () => {
      setError(null);
      const r = await saveAttendance(ymd, members.map((m) => ({ membershipId: m.id, present: marks[m.id] === "present" })));
      if (r.error) setError(r.error);
      else setSaved(true);
    });

  return (
    <>
      <div className="row" style={{ alignItems: "flex-start" }}>
        <div style={{ width: 220 }}><JalaliField label="تاریخ جلسه" value={dateText} onChange={change} /></div>
        <div className="row" style={{ marginTop: 26 }}>
          <Button variant="secondary" size="sm" onClick={() => go(ymdAdd(ymd, -1))}>روز قبل</Button>
          <Button variant="secondary" size="sm" onClick={() => go(ymdAdd(ymd, 1))}>روز بعد</Button>
        </div>
      </div>

      <div className="panel panel-flush">
        {members.length === 0 ? (
          <div className="empty">برای ثبت حضور و غیاب، ابتدا عضوی تایید کنید.</div>
        ) : (
          members.map((m) => (
            <div className="att-row" key={m.id}>
              <span>{m.name}</span>
              <SegmentedControl
                label={`حضور ${m.name}`}
                value={marks[m.id]}
                onValueChange={(v) => setMark(m.id, v)}
                options={[
                  { value: "present", label: "حاضر" },
                  { value: "absent", label: "غایب" },
                ]}
              />
            </div>
          ))
        )}
      </div>

      {members.length > 0 && (
        <div className="row-between">
          <span className="muted">{toFa(presentCount)} حاضر از {toFa(members.length)} نفر</span>
          <div className="row">
            <Button
              variant="secondary"
              onClick={() => { setMarks(Object.fromEntries(members.map((m) => [m.id, "present"]))); setSaved(false); }}
            >
              همه حاضر
            </Button>
            <Button loading={pending} onClick={save}>{saved ? "ذخیره شد" : "ذخیره حضور و غیاب"}</Button>
          </div>
        </div>
      )}
      {error && <p role="alert" className="form-error">{error}</p>}
    </>
  );
}
