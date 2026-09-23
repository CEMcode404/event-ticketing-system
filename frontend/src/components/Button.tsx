import type { ButtonHTMLAttributes } from "react";

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: "primary" | "secondary" | "danger";
  size?: "md" | "lg";
}

const VARIANT_CLASSES: Record<string, string> = {
  primary: "bg-accent text-accent-ink font-semibold",
  secondary: "border border-border text-muted",
  danger: "border border-urgent text-urgent",
};

const SIZE_CLASSES: Record<string, string> = {
  md: "px-4 py-2",
  lg: "px-5 py-2.5",
};

export function Button({ variant = "primary", size = "md", className = "", ...props }: ButtonProps) {
  return (
    <button
      className={`cursor-pointer rounded disabled:opacity-60 ${VARIANT_CLASSES[variant]} ${SIZE_CLASSES[size]} ${className}`}
      {...props}
    />
  );
}