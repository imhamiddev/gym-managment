"use client";

import { createContext, type ReactNode, useContext, useMemo } from "react";
import { AnimatedToastStack, useAnimatedToastStack } from "./toast-stack";
import { Icon } from "./icon";

type Notify = (title: string, description?: string) => void;
type Toaster = { success: Notify; error: Notify; info: Notify };

const ToastContext = createContext<Toaster | null>(null);

export function ToastProvider({ children }: { children: ReactNode }) {
  const { toasts, showToast, dismissToast } = useAnimatedToastStack({ limit: 3, defaultDuration: 3800 });

  const api = useMemo<Toaster>(
    () => ({
      success: (title, description) => void showToast({ title, description, status: "success" }),
      error: (title, description) => void showToast({ title, description, status: "error", duration: 6000 }),
      info: (title, description) => void showToast({ title, description, status: "info" }),
    }),
    [showToast],
  );

  return (
    <ToastContext.Provider value={api}>
      {children}
      <AnimatedToastStack
        toasts={toasts}
        onDismiss={dismissToast}
        position="bottom-center"
        fixed
        portal
        className="mb-[env(safe-area-inset-bottom)]"
        icons={{
          success: <Icon name="check" size={16} />,
          error: <Icon name="danger" size={16} />,
          info: <Icon name="info" size={16} />,
        }}
      />
    </ToastContext.Provider>
  );
}

export function useToast() {
  const ctx = useContext(ToastContext);
  if (!ctx) throw new Error("useToast must be used inside <ToastProvider>");
  return ctx;
}
