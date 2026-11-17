import { NextResponse } from "next/server";
import { getUser } from "@/lib/auth";
import { db } from "@/lib/db";

export async function POST(req: Request) {
  const user = await getUser();
  if (!user) return NextResponse.json({ error: "unauthorized" }, { status: 401 });
  const body = await req.json().catch(() => null);
  if (typeof body?.endpoint !== "string") return NextResponse.json({ error: "invalid" }, { status: 400 });
  await db.pushSubscription.deleteMany({ where: { endpoint: body.endpoint, userId: user.id } });
  return NextResponse.json({ ok: true });
}
