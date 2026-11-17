"use client";

import { MotionConfig } from "motion/react";
import { ThemeProvider } from "next-themes";
import type { ReactNode } from "react";
import { ToastProvider } from "@/components/ui/toast";
import { LocaleProvider } from "@/lib/i18n";
import { faMessages } from "@/lib/messages-fa";

export function Providers({ children }: { children: ReactNode }) {
  return (
    <ThemeProvider attribute="class" defaultTheme="system" enableSystem disableTransitionOnChange={false}>
      <LocaleProvider locale="fa" messages={faMessages}>
        <MotionConfig reducedMotion="user">
          <ToastProvider>{children}</ToastProvider>
        </MotionConfig>
      </LocaleProvider>
    </ThemeProvider>
  );
}
