import type { IconName } from "@/components/ui/icon";
import type { Tone } from "@/components/ui/badge";

export type NavLink = { href: string; label: string; icon: IconName; tone: Tone; exact?: boolean };

export const COACH_LINKS: NavLink[] = [
  { href: "/coach", label: "نمای کلی", icon: "overview", tone: "primary", exact: true },
  { href: "/coach/members", label: "مدیریت اعضا", icon: "members", tone: "members" },
  { href: "/coach/tuition", label: "مدیریت شهریه", icon: "wallet", tone: "tuition" },
  { href: "/coach/attendance", label: "حضور و غیاب", icon: "attendance", tone: "attendance" },
  { href: "/coach/profile", label: "پروفایل", icon: "user", tone: "neutral" },
];

export const ATHLETE_LINKS: NavLink[] = [
  { href: "/athlete", label: "پنل من", icon: "overview", tone: "primary", exact: true },
  { href: "/athlete/profile", label: "پروفایل", icon: "user", tone: "neutral" },
];
