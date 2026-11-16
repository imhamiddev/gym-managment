import { redirect } from "next/navigation";
import { getUser } from "@/lib/auth";
import { AuthForm } from "@/components/AuthForm";

export default async function RegisterPage({ searchParams }: { searchParams: Promise<{ role?: string }> }) {
  const user = await getUser();
  if (user) redirect(user.role === "COACH" ? "/coach" : "/athlete");
  const { role } = await searchParams;
  return <AuthForm mode="register" initialRole={role === "coach" ? "COACH" : "ATHLETE"} />;
}
