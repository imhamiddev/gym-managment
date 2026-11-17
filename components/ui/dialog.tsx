"use client";

import { AnimatePresence, motion, useDragControls, useReducedMotion } from "motion/react";
import { type ReactNode, useEffect, useId, useRef, useSyncExternalStore } from "react";
import { EASE_OUT, SPRING_PANEL } from "@/lib/ease";
import { useFocusTrap } from "@/lib/hooks/use-focus-trap";
import { PresenceGate } from "@/lib/presence-gate";
import { cn } from "@/lib/utils";
import { Icon } from "./icon";
import { TONE, type Tone } from "./badge";

const DESKTOP = "(min-width: 640px)";
const subscribe = (cb: () => void) => {
  const mq = window.matchMedia(DESKTOP);
  mq.addEventListener("change", cb);
  return () => mq.removeEventListener("change", cb);
};
const useIsDesktop = () =>
  useSyncExternalStore(subscribe, () => window.matchMedia(DESKTOP).matches, () => true);

export interface DialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  title: string;
  description?: ReactNode;
  /** Tints the icon chip beside the title. */
  tone?: Tone;
  icon?: Parameters<typeof Icon>[0]["name"];
  children?: ReactNode;
  className?: string;
}

/**
 * Bottom sheet on phones (drag the handle down to dismiss), centered card from
 * 640px up. Same overlay recipe as the beUI Drawer: scrim and panel are
 * separate layers, both released through PresenceGate while they exit.
 */
export function Dialog({ open, onOpenChange, title, description, tone = "primary", icon, children, className }: DialogProps) {
  const reduce = useReducedMotion();
  const isDesktop = useIsDesktop();
  const panelRef = useRef<HTMLDivElement>(null);
  const controls = useDragControls();
  const titleId = useId();

  useFocusTrap(open, panelRef, { onEscape: () => onOpenChange(false) });

  useEffect(() => {
    if (!open) return;
    const prev = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    return () => {
      document.body.style.overflow = prev;
    };
  }, [open]);

  const enter = reduce ? { opacity: 0 } : isDesktop ? { opacity: 0, scale: 0.96, y: 12 } : { y: "100%" };
  const shown = reduce ? { opacity: 1 } : isDesktop ? { opacity: 1, scale: 1, y: 0 } : { y: 0 };

  return (
    <AnimatePresence>
      {open ? (
        <PresenceGate key="scrim">
          {({ gate }) => (
            <motion.button
              type="button"
              aria-label="بستن"
              tabIndex={-1}
              onClick={() => onOpenChange(false)}
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              transition={{ duration: 0.22, ease: EASE_OUT }}
              {...gate}
              className="fixed inset-0 z-50 h-full w-full cursor-default bg-black/45 backdrop-blur-[3px]"
            />
          )}
        </PresenceGate>
      ) : null}
      {open ? (
        <PresenceGate key="panel">
          {({ isPresent, gate }) => (
            <div inert={!isPresent} className="pointer-events-none fixed inset-0 z-50 flex items-end justify-center sm:items-center sm:p-6">
              <motion.div
                ref={panelRef}
                tabIndex={-1}
                role="dialog"
                aria-modal="true"
                aria-labelledby={titleId}
                initial={enter}
                animate={shown}
                exit={enter}
                transition={reduce ? { duration: 0.15 } : isDesktop ? SPRING_PANEL : { type: "spring", stiffness: 380, damping: 38, mass: 0.7 }}
                drag={!isDesktop && !reduce ? "y" : false}
                dragControls={controls}
                dragListener={false}
                dragConstraints={{ top: 0, bottom: 0 }}
                dragElastic={{ top: 0, bottom: 0.7 }}
                onDragEnd={(_, info) => {
                  if (info.offset.y > 110 || info.velocity.y > 600) onOpenChange(false);
                }}
                {...gate}
                className={cn(
                  "relative flex max-h-[92dvh] w-full flex-col overflow-hidden bg-background shadow-2xl outline-none",
                  "rounded-t-[28px] border border-b-0 border-border-strong sm:max-w-md sm:rounded-[28px] sm:border-b",
                  className,
                )}
              >
                {/* drag handle (phones only) */}
                <div
                  onPointerDown={(e) => controls.start(e)}
                  className="flex h-6 shrink-0 cursor-grab touch-none items-end justify-center pb-1 sm:hidden"
                >
                  <span className="h-1.5 w-10 rounded-full bg-border-strong" />
                </div>

                <div className="flex items-start gap-3 px-5 pt-3 sm:px-6 sm:pt-6">
                  {icon ? (
                    <span className={cn("grid size-11 shrink-0 place-items-center rounded-2xl", TONE[tone].soft)}>
                      <Icon name={icon} size={22} set="bulk" />
                    </span>
                  ) : null}
                  <div className="min-w-0 flex-1">
                    <h2 id={titleId} className="text-lg font-semibold leading-8">
                      {title}
                    </h2>
                    {description ? <p className="text-sm leading-7 text-muted-foreground">{description}</p> : null}
                  </div>
                  <button
                    type="button"
                    onClick={() => onOpenChange(false)}
                    aria-label="بستن"
                    className="-me-1 -mt-1 grid size-10 shrink-0 place-items-center rounded-full text-muted-foreground transition-colors hover:bg-card hover:text-foreground active:scale-90"
                  >
                    <Icon name="close" size={20} />
                  </button>
                </div>

                <div className="overflow-y-auto overscroll-contain px-5 pb-[max(1.25rem,env(safe-area-inset-bottom))] pt-4 sm:px-6 sm:pb-6">
                  {children}
                </div>
              </motion.div>
            </div>
          )}
        </PresenceGate>
      ) : null}
    </AnimatePresence>
  );
}

/** Action row: full-width stacked buttons on phones, a right-aligned row on desktop. */
export function DialogActions({ className, children }: { className?: string; children: ReactNode }) {
  return <div className={cn("mt-6 flex flex-col-reverse gap-2 sm:flex-row sm:justify-end [&>*]:w-full sm:[&>*]:w-auto", className)}>{children}</div>;
}
