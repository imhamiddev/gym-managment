import { redirect } from "next/navigation";
import { getUser } from "@/lib/auth";
import { AuthForm } from "@/components/AuthForm";

export default async function LoginPage() {
  const user = await getUser();
  if (user) redirect(user.role === "COACH" ? "/coach" : "/athlete");
  return <AuthForm mode="login" />;
}
