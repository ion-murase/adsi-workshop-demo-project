"use client";

import { useQuery } from "@tanstack/react-query";
import { apiClient } from "@/lib/api-client";

interface HealthResponse {
  status: string;
  application: string;
  timestamp: string;
}

export default function Home() {
  const { data, isLoading, isError, error } = useQuery({
    queryKey: ["system", "health"],
    queryFn: () => apiClient.get<HealthResponse>("/api/system/health"),
  });

  return (
    <div className="flex min-h-screen items-center justify-center">
      <div className="w-full max-w-md space-y-6 p-8">
        <h1 className="text-2xl font-bold text-center">勤怠管理システム</h1>

        <div className="rounded-lg border p-4 space-y-2">
          <h2 className="font-semibold text-sm text-muted-foreground">Backend 疎通確認</h2>
          {isLoading && <p className="text-sm">接続中...</p>}
          {isError && (
            <p className="text-sm text-red-600">
              接続失敗: {error instanceof Error ? error.message : "Unknown error"}
            </p>
          )}
          {data && (
            <dl className="grid grid-cols-[auto_1fr] gap-x-4 gap-y-1 text-sm">
              <dt className="text-muted-foreground">Status</dt>
              <dd className="font-mono">{data.status}</dd>
              <dt className="text-muted-foreground">Application</dt>
              <dd className="font-mono">{data.application}</dd>
              <dt className="text-muted-foreground">Timestamp</dt>
              <dd className="font-mono">{data.timestamp}</dd>
            </dl>
          )}
        </div>
      </div>
    </div>
  );
}
