import { useEffect } from "react";
import { X } from "lucide-react";
import { Button } from "./Button";

interface Props {
  open: boolean;
  onClose: () => void;
  title?: string;
  message?: string;
}

export function DownloadModal({
  open,
  onClose,
  title = "下载入口待配置",
  message = "该下载或预约链接尚未配置。请稍后再试，或联系我们获取内测通道。",
}: Props) {
  useEffect(() => {
    if (!open) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose();
    };
    document.addEventListener("keydown", onKey);
    const prev = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    return () => {
      document.removeEventListener("keydown", onKey);
      document.body.style.overflow = prev;
    };
  }, [open, onClose]);

  if (!open) return null;

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="download-modal-title"
      className="fixed inset-0 z-50 grid place-items-center px-4"
    >
      <div
        aria-hidden
        className="absolute inset-0 bg-ink/50 backdrop-blur-sm"
        onClick={onClose}
      />
      <div className="relative w-full max-w-md rounded-2xl bg-card p-6 shadow-2xl">
        <button
          type="button"
          aria-label="关闭"
          onClick={onClose}
          className="absolute right-3 top-3 grid h-9 w-9 place-items-center rounded-full hover:bg-brand-soft/60"
        >
          <X className="size-4" />
        </button>
        <h2 id="download-modal-title" className="text-lg font-semibold text-ink">
          {title}
        </h2>
        <p className="mt-2 text-sm text-muted-foreground">{message}</p>
        <div className="mt-6 flex justify-end">
          <Button onClick={onClose}>我知道了</Button>
        </div>
      </div>
    </div>
  );
}
