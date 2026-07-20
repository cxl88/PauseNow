import * as React from "react";
import { cn } from "@/lib/utils";

interface PillProps extends React.HTMLAttributes<HTMLSpanElement> {
  tone?: "brand" | "muted";
}

export function Pill({ className, tone = "brand", ...props }: PillProps) {
  return (
    <span
      className={cn(
        "inline-flex items-center rounded-full px-3 py-1 text-xs font-medium",
        tone === "brand"
          ? "bg-brand-soft text-brand"
          : "bg-muted text-muted-foreground",
        className,
      )}
      {...props}
    />
  );
}
