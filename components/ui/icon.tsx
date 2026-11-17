"use client";

import type { ReactNode } from "react";
import {
  Activity,
  AddUser,
  ArrowLeft,
  ArrowRight,
  Calendar,
  Category,
  ChevronDown,
  ChevronLeft,
  ChevronRight,
  ChevronUp,
  Danger,
  Delete,
  Document,
  Edit,
  Hide,
  Home,
  InfoCircle,
  Lock,
  Login,
  Logout,
  MoreCircle,
  Password,
  Plus,
  Search,
  Setting,
  ShieldDone,
  Show,
  Star,
  TickSquare,
  Ticket,
  TimeCircle,
  TwoUsers,
  User,
  Wallet,
  Call,
  Notification,
  Filter,
} from "react-iconly";
import { cn } from "@/lib/utils";

/**
 * Iconly icon set (react-iconly). A few glyphs Iconly does not ship
 * (sun, moon, copy, menu, plain check and close) are drawn here on the same
 * 24px grid with the same 1.5px rounded stroke so they sit next to the rest.
 */

export type IconSet = "light" | "bold" | "bulk" | "broken" | "two-tone" | "curved";

const ICONLY = {
  home: Home,
  overview: Category,
  members: TwoUsers,
  user: User,
  addUser: AddUser,
  wallet: Wallet,
  attendance: TickSquare,
  calendar: Calendar,
  ticket: Ticket,
  logout: Logout,
  login: Login,
  setting: Setting,
  edit: Edit,
  delete: Delete,
  plus: Plus,
  search: Search,
  show: Show,
  hide: Hide,
  lock: Lock,
  password: Password,
  call: Call,
  danger: Danger,
  info: InfoCircle,
  time: TimeCircle,
  activity: Activity,
  document: Document,
  star: Star,
  more: MoreCircle,
  shield: ShieldDone,
  bell: Notification,
  filter: Filter,
  chevronDown: ChevronDown,
  chevronUp: ChevronUp,
  chevronLeft: ChevronLeft,
  chevronRight: ChevronRight,
  arrowLeft: ArrowLeft,
  arrowRight: ArrowRight,
} as const;

const SVG = {
  sun: (
    <>
      <circle cx="12" cy="12" r="4" />
      <path d="M12 2.5v2M12 19.5v2M4.6 4.6l1.4 1.4M18 18l1.4 1.4M2.5 12h2M19.5 12h2M4.6 19.4L6 18M18 6l1.4-1.4" />
    </>
  ),
  moon: <path d="M20.4 14.3A8.5 8.5 0 0 1 9.7 3.6a8.5 8.5 0 1 0 10.7 10.7Z" />,
  copy: (
    <>
      <rect x="9" y="9" width="11" height="11" rx="3" />
      <path d="M15 9V7a3 3 0 0 0-3-3H7a3 3 0 0 0-3 3v5a3 3 0 0 0 3 3h2" />
    </>
  ),
  menu: <path d="M4 7h16M4 12h16M10 17h10" />,
  check: <path d="M5 12.5l4.5 4.5L19 7.5" />,
  close: <path d="M6 6l12 12M18 6L6 18" />,
} satisfies Record<string, ReactNode>;

export type IconName = keyof typeof ICONLY | keyof typeof SVG;

export interface IconProps {
  name: IconName;
  size?: number;
  /** Iconly set. `light` is the outline, `bold` the filled one. */
  set?: IconSet;
  className?: string;
  /** Accessible name. Icons are decorative (aria-hidden) when omitted. */
  label?: string;
}

export function Icon({ name, size = 20, set = "light", className, label }: IconProps) {
  const a11y = label ? { role: "img", "aria-label": label } : { "aria-hidden": true as const };
  const shell = cn("inline-flex shrink-0 items-center justify-center leading-none", className);

  if (name in SVG) {
    return (
      <span className={shell} style={{ width: size, height: size }} {...a11y}>
        <svg
          width={size}
          height={size}
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth={1.6}
          strokeLinecap="round"
          strokeLinejoin="round"
        >
          {SVG[name as keyof typeof SVG]}
        </svg>
      </span>
    );
  }

  const Glyph = ICONLY[name as keyof typeof ICONLY];
  return (
    <span className={shell} style={{ width: size, height: size }} {...a11y}>
      <Glyph set={set} size={size} primaryColor="currentColor" secondaryColor="currentColor" />
    </span>
  );
}
