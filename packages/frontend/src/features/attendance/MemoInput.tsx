"use client";

import { Textarea } from "@/components/ui/textarea";

const MAX_LENGTH = 300;

interface MemoInputProps {
  value: string;
  onChange: (value: string) => void;
  disabled?: boolean;
}

export function MemoInput({ value, onChange, disabled }: MemoInputProps) {
  return (
    <div className="space-y-1">
      <label htmlFor="memo-input" className="text-sm text-muted-foreground">
        メモ（任意）
      </label>
      <Textarea
        id="memo-input"
        value={value}
        onChange={(e) => onChange(e.target.value.slice(0, MAX_LENGTH))}
        placeholder="在宅勤務、遅刻理由など"
        maxLength={MAX_LENGTH}
        rows={2}
        disabled={disabled}
        className="resize-none"
      />
      <p className="text-xs text-muted-foreground text-right">
        {value.length}/{MAX_LENGTH}
      </p>
    </div>
  );
}
