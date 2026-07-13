"use client";

import { useState } from "react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { formatTime } from "./format";
import { MemoModal } from "./MemoModal";
import { useTodayStatus, useUpdateMemo } from "./useAttendance";

export function TodayRecords() {
  const { data: todayStatus, isLoading } = useTodayStatus();
  const updateMemoMutation = useUpdateMemo();
  const [editingRecordId, setEditingRecordId] = useState<string | null>(null);

  if (isLoading) {
    return (
      <div className="rounded-lg border p-6 space-y-3">
        <Skeleton className="h-5 w-32" />
        <Skeleton className="h-4 w-full" />
        <Skeleton className="h-4 w-full" />
      </div>
    );
  }

  const records = todayStatus?.records ?? [];

  if (records.length === 0) {
    return (
      <div className="rounded-lg border p-6">
        <h3 className="text-sm font-medium mb-2">本日の打刻記録</h3>
        <p className="text-sm text-muted-foreground">打刻記録はありません</p>
      </div>
    );
  }

  return (
    <div className="rounded-lg border p-6">
      <h3 className="text-sm font-medium mb-3">本日の打刻記録</h3>
      <div className="space-y-2">
        {records.map((record) => (
          <div
            key={record.id}
            className="flex items-center justify-between text-sm py-1.5 border-b last:border-b-0"
          >
            <div className="flex items-center gap-3">
              <span className="font-medium">{formatTime(record.clockIn)}</span>
              <span className="text-muted-foreground">~</span>
              <span className="font-medium">
                {record.clockOut ? formatTime(record.clockOut) : "--:--"}
              </span>
              {record.memo && (
                <span className="text-xs text-muted-foreground truncate max-w-[120px]">
                  {record.memo}
                </span>
              )}
            </div>
            <div className="flex items-center gap-2">
              <Button
                variant="outline"
                size="sm"
                className="text-sky-600 border-sky-300 hover:bg-sky-50 hover:text-sky-700"
                onClick={() => setEditingRecordId(record.id)}
              >
                {record.memo ? "備考" : "メモ追加"}
              </Button>
              {record.corrected && <Badge variant="outline">修正済み</Badge>}
            </div>
          </div>
        ))}
      </div>
      {editingRecordId && (
        <MemoModal
          open={!!editingRecordId}
          onOpenChange={(open) => {
            if (!open) setEditingRecordId(null);
          }}
          memo={records.find((r) => r.id === editingRecordId)?.memo ?? ""}
          editable={true}
          saving={updateMemoMutation.isPending}
          onSave={(newMemo) => {
            updateMemoMutation.mutate(
              { recordId: editingRecordId, memo: newMemo },
              { onSuccess: () => setEditingRecordId(null) },
            );
          }}
        />
      )}
    </div>
  );
}
