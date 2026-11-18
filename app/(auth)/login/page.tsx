import { redirect } from "next/navigation";
import { getUser } from "@/lib/auth";
import { safeNext } from "@/lib/redirect";
import { AuthForm } from "@/components/AuthForm";

export default async function LoginPage({ searchParams }: { searchParams: Promise<{ next?: string }> }) {
  const next = safeNext((await searchParams).next);
  const user = await getUser();
  if (user) redirect(next ?? (user.role === "COACH" ? "/coach" : "/athlete"));
  return <AuthForm mode="login" next={next ?? undefined} />;
}
