import * as React from "react";
import { cn } from "@/lib/utils";

interface CardProps extends React.HTMLAttributes<HTMLDivElement> {
  tone?: "default" | "brand" | "soft";
}

export function Card({ className, tone = "default", ...props }: CardProps) {
  const toneCls =
    tone === "brand"
      ? "bg-brand text-brand-foreground border-transparent"
      : tone === "soft"
        ? "bg-brand-soft text-ink border-transparent"
        : "bg-card text-card-foreground border border-border";
  return (
    <div
      className={cn(
        "rounded-2xl p-6 sm:p-8",
        toneCls,
        className,
      )}
      {...props}
    />
  );
}
