"use client";

import { AnimatePresence, motion } from "motion/react";
import { Icon } from "@/components/ui/icon";

/** Inline form error that eases in and out instead of popping. */
export function FormError({ message }: { message?: string | null }) {
  return (
    <AnimatePresence initial={false}>
      {message ? (
        <motion.div
          key={message}
          role="alert"
          initial={{ opacity: 0, height: 0, y: -4 }}
          animate={{ opacity: 1, height: "auto", y: 0 }}
          exit={{ opacity: 0, height: 0 }}
          transition={{ duration: 0.22 }}
          className="overflow-hidden"
        >
          <div className="flex items-start gap-2.5 rounded-2xl bg-danger/10 px-4 py-3 text-sm leading-6 text-danger">
            <Icon name="danger" size={20} set="bold" className="mt-0.5" />
            <span>{message}</span>
          </div>
        </motion.div>
      ) : null}
    </AnimatePresence>
  );
}
