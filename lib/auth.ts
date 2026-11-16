import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { SignJWT, jwtVerify } from "jose";
import { db } from "./db";

function secret() {
  const s = process.env.AUTH_SECRET;
  if (!s && process.env.NODE_ENV === "production") throw new Error("AUTH_SECRET is not set");
  return new TextEncoder().encode(s ?? "dev-only-secret-change-me-in-env-file");
}

export async function createSession(userId: string) {
  const token = await new SignJWT({})
    .setProtectedHeader({ alg: "HS256" })
    .setSubject(userId)
    .setExpirationTime("30d")
    .sign(secret());
  (await cookies()).set("session", token, {
    httpOnly: true,
    sameSite: "lax",
    secure: process.env.NODE_ENV === "production",
    path: "/",
    maxAge: 60 * 60 * 24 * 30,
  });
}

export async function destroySession() {
  (await cookies()).delete("session");
}

export async function getUser() {
  const token = (await cookies()).get("session")?.value;
  if (!token) return null;
  try {
    const { payload } = await jwtVerify(token, secret());
    return payload.sub ? await db.user.findUnique({ where: { id: payload.sub } }) : null;
  } catch {
    return null;
  }
}

export async function requireUser(role?: "COACH" | "ATHLETE") {
  const user = await getUser();
  if (!user) redirect("/login");
  if (role && user.role !== role) redirect(user.role === "COACH" ? "/coach" : "/athlete");
  return user;
}
