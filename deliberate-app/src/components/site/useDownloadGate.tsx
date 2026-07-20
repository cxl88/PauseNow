import { useState } from "react";
import { DownloadModal } from "./DownloadModal";

export function useDownloadGate() {
  const [open, setOpen] = useState(false);
  const [msg, setMsg] = useState<string | undefined>();
  const trigger = (url?: string, message?: string) => {
    if (url && url.trim().length > 0) {
      window.open(url, "_blank", "noopener,noreferrer");
      return;
    }
    setMsg(message);
    setOpen(true);
  };
  const modal = (
    <DownloadModal open={open} onClose={() => setOpen(false)} message={msg} />
  );
  return { trigger, modal };
}
