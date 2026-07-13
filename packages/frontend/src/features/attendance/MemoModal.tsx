"use client";

import { useState } from "react";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { MemoInput } from "./MemoInput";

interface MemoModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  memo: string;
  editable: boolean;
  onSave?: (memo: string) => void;
  saving?: boolean;
}

export function MemoModal({ open, onOpenChange, memo, editable, onSave, saving }: MemoModalProps) {
  const [draft, setDraft] = useState(memo);

  const handleOpenChange = (next: boolean) => {
    if (next) {
      setDraft(memo);
    }
    onOpenChange(next);
  };

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>備考</DialogTitle>
        </DialogHeader>
        {editable ? (
          <MemoInput value={draft} onChange={setDraft} disabled={saving} />
        ) : (
          <p className="text-sm whitespace-pre-wrap py-2">{memo || "メモはありません"}</p>
        )}
        <DialogFooter>
          {editable && (
            <Button onClick={() => onSave?.(draft)} disabled={saving}>
              {saving ? "保存中..." : "保存"}
            </Button>
          )}
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            閉じる
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
