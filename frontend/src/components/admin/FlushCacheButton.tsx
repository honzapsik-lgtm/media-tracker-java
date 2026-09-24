"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";

export function FlushCacheButton() {
  const [loading, setLoading] = useState(false);
  const router = useRouter();

  const handleFlush = async () => {
    if (!window.confirm("Are you sure you want to flush the entire cache? All cached provider responses will be cleared.")) {
      return;
    }

    setLoading(true);
    try {
      const res = await fetch("/api/admin/cache/flush-all", { method: "POST" });
      if (!res.ok) {
        const text = await res.text();
        throw new Error(text || "Failed to flush cache");
      }
      const data = await res.json().catch(() => ({}));
      window.alert(`Cache flushed successfully. Deleted ${data.deletedCount ?? 0} entries.`);
      router.refresh();
    } catch (err: any) {
      window.alert("Error flushing cache: " + err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <button
      onClick={handleFlush}
      disabled={loading}
      className="rounded bg-rose-700 hover:bg-rose-600 px-5 py-2 text-sm font-black text-white transition-all disabled:opacity-50"
    >
      {loading ? "Flushing..." : "Delete entire cache"}
    </button>
  );
}
