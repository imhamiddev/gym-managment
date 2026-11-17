"use client";

import Link from "next/link";
import { AnimatePresence, motion } from "motion/react";
import { type ComponentProps, forwardRef } from "react";
import { Button as BaseButton, type ButtonProps as BaseProps, buttonClass, type ButtonSize, type ButtonVariant } from "./button-base";
import { Loader } from "./loader";

export type { ButtonSize, ButtonVariant };

export interface ButtonProps extends BaseProps {
  /** Shows a spinner, disables the button and sets aria-busy. */
  loading?: boolean;
}

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(function Button(
  { loading = false, disabled, children, ...rest },
  ref,
) {
  return (
    <BaseButton ref={ref} disabled={disabled || loading} aria-busy={loading || undefined} {...rest}>
      <AnimatePresence initial={false}>
        {loading ? (
          <motion.span
            key="spinner"
            initial={{ width: 0, opacity: 0 }}
            animate={{ width: 16, opacity: 1 }}
            exit={{ width: 0, opacity: 0 }}
            transition={{ duration: 0.18 }}
            className="inline-flex shrink-0 items-center justify-center overflow-hidden"
          >
            <Loader variant="spinner" size={16} />
          </motion.span>
        ) : null}
      </AnimatePresence>
      {children}
    </BaseButton>
  );
});

type LinkProps = ComponentProps<typeof Link> & { variant?: ButtonVariant; size?: ButtonSize };

/** A next/link that looks and presses like a Button. */
export function ButtonLink({ variant, size, className, ...rest }: LinkProps) {
  return <Link className={buttonClass(variant, size, className)} {...rest} />;
}
