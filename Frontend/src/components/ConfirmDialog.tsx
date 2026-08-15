import { useRef, useState } from "react";

type ConfirmOptions = { title: string; message: string; confirmLabel?: string; cancelLabel?: string };

export function useConfirm() {
  const [options, setOptions] = useState<ConfirmOptions | null>(null);
  const resolver = useRef<((confirmed: boolean) => void) | null>(null);
  function requestConfirmation(next: ConfirmOptions) {
    resolver.current?.(false);
    setOptions(next);
    return new Promise<boolean>((resolve) => { resolver.current = resolve; });
  }
  function close(confirmed: boolean) {
    resolver.current?.(confirmed);
    resolver.current = null;
    setOptions(null);
  }
  const confirmationDialog = options ? <div className="overwrite-backdrop" role="presentation" onMouseDown={() => close(false)}><section aria-labelledby="confirm-dialog-title" aria-modal="true" className="progress-card overwrite-confirm" role="alertdialog" onMouseDown={(event) => event.stopPropagation()}><p className="eyebrow">Potvrda</p><h2 id="confirm-dialog-title">{options.title}</h2><p>{options.message}</p><div><button className="secondary-button" onClick={() => close(false)}>{options.cancelLabel ?? "Odustani"}</button><button className="primary-button" autoFocus onClick={() => close(true)}>{options.confirmLabel ?? "Potvrdi"}</button></div></section></div> : null;
  return { requestConfirmation, confirmationDialog };
}
