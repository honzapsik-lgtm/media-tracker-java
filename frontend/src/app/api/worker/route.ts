import { NextResponse } from "next/server";

export async function processWorkerBatch(_options?: { requestId?: string; batchSize?: number }) {
  return {
    ok: true,
    workerId: "spring-boot-managed",
    processed: 0,
    completed: 0,
    retried: 0,
    failed: 0,
  };
}

export async function POST() {
  return NextResponse.json(await processWorkerBatch());
}
