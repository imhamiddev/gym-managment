import { NextResponse } from "next/server";
import { getUser } from "@/lib/auth";
import { pushReady, sendPushToUser } from "@/lib/push";

export async function POST() {
  const user = await getUser();
  if (!user) return NextResponse.json({ error: "unauthorized" }, { status: 401 });
  if (!pushReady()) return NextResponse.json({ error: "not-configured" }, { status: 503 });
  const sent = await sendPushToUser(user.id, {
    title: "باشگاه‌یار",
    body: "اعلان‌ها با موفقیت فعال شد ✅",
    url: user.role === "COACH" ? "/coach" : "/athlete",
    tag: "test",
  });
  return NextResponse.json({ ok: sent > 0, sent });
}
