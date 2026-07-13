"use client";

import { useState } from "react";
import { type Column, DataTable } from "@/components/DataTable";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import type { DailyAttendanceResponse } from "./attendance-api";
import { formatDate, formatMinutes, formatTime } from "./format";
import { MemoModal } from "./MemoModal";

function firstClockIn(day: DailyAttendanceResponse): string {
  const record = day.records[0];
  return record ? formatTime(record.clockIn) : "--:--";
}

function lastClockOut(day: DailyAttendanceResponse): string {
  const last = day.records[day.records.length - 1];
  return last?.clockOut ? formatTime(last.clockOut) : "--:--";
}

function hasCorrected(day: DailyAttendanceResponse): boolean {
  return day.records.some((r) => r.corrected);
}

function getMemo(day: DailyAttendanceResponse): string | null {
  const record = day.records.find((r) => r.memo);
  return record?.memo ?? null;
}

const columns: Column<DailyAttendanceResponse>[] = [
  {
    key: "date",
    header: "日付",
    render: (day) => formatDate(day.date),
  },
  {
    key: "clockIn",
    header: "出勤",
    render: (day) => firstClockIn(day),
  },
  {
    key: "clockOut",
    header: "退勤",
    render: (day) => lastClockOut(day),
  },
  {
    key: "workMinutes",
    header: "勤務時間",
    render: (day) => (day.workMinutes > 0 ? formatMinutes(day.workMinutes) : "-"),
  },
  {
    key: "breakMinutes",
    header: "休憩",
    render: (day) => (day.breakMinutes > 0 ? formatMinutes(day.breakMinutes) : "-"),
  },
  {
    key: "overtimeMinutes",
    header: "残業",
    render: (day) => (day.overtimeMinutes > 0 ? formatMinutes(day.overtimeMinutes) : "-"),
  },
  {
    key: "corrected",
    header: "",
    render: (day) => (hasCorrected(day) ? <Badge variant="outline">修正</Badge> : null),
  },
];

interface AttendanceTableProps {
  days: DailyAttendanceResponse[];
}

export function AttendanceTable({ days }: AttendanceTableProps) {
  const [viewingMemo, setViewingMemo] = useState<string | null>(null);

  const allColumns: Column<DailyAttendanceResponse>[] = [
    ...columns,
    {
      key: "memo",
      header: "備考",
      render: (day) => {
        const memo = getMemo(day);
        if (!memo) return null;
        return (
          <Button variant="ghost" size="sm" onClick={() => setViewingMemo(memo)}>
            備考
          </Button>
        );
      },
    },
  ];

  return (
    <>
      <DataTable<DailyAttendanceResponse & Record<string, unknown>>
        columns={allColumns as Column<DailyAttendanceResponse & Record<string, unknown>>[]}
        data={days as (DailyAttendanceResponse & Record<string, unknown>)[]}
        rowKey={(item) => item.date}
        emptyMessage="勤怠データがありません"
      />
      <MemoModal
        open={viewingMemo !== null}
        onOpenChange={(open) => {
          if (!open) setViewingMemo(null);
        }}
        memo={viewingMemo ?? ""}
        editable={false}
      />
    </>
  );
}
