"use client";

import { useEffect, useState } from "react";
import { Globe, Lock, Users } from "lucide-react";

type Visibility = "PUBLIC" | "FRIENDS_ONLY" | "PRIVATE";
type Key = "profile_visibility" | "ratings_visibility" | "watchlist_visibility" | "activity_visibility";
type Settings = Record<Key, Visibility>;

const ROWS: { key: Key; label: string }[] = [
  { key: "profile_visibility", label: "Profile" },
  { key: "ratings_visibility", label: "Ratings & reviews" },
  { key: "watchlist_visibility", label: "Watchlist & progress" },
  { key: "activity_visibility", label: "Activity" },
];

const OPTIONS: { id: Visibility; label: string; Icon: typeof Globe }[] = [
  { id: "PUBLIC", label: "Public", Icon: Globe },
  { id: "FRIENDS_ONLY", label: "Friends", Icon: Users },
  { id: "PRIVATE", label: "Private", Icon: Lock },
];

const DEFAULTS: Settings = {
  profile_visibility: "PUBLIC",
  ratings_visibility: "PUBLIC",
  watchlist_visibility: "PUBLIC",
  activity_visibility: "PUBLIC",
};

export default function PrivacySetting() {
  const [settings, setSettings] = useState<Settings | null>(null);
  const [savingKey, setSavingKey] = useState<Key | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetch("/api/profile/privacy")
      .then((res) => (res.ok ? res.json() : Promise.reject(new Error("Could not load privacy settings."))))
      .then((data) => {
        const s = data.settings || {};
        setSettings({
          profile_visibility: s.profile_visibility || s.profileVisibility || "PUBLIC",
          ratings_visibility: s.ratings_visibility || s.ratingsVisibility || "PUBLIC",
          watchlist_visibility: s.watchlist_visibility || s.watchlistVisibility || "PUBLIC",
          activity_visibility: s.activity_visibility || s.activityVisibility || "PUBLIC",
        });
      })
      .catch((err) => {
        setSettings(DEFAULTS);
        setError(err.message);
      });
  }, []);

  const choose = async (key: Key, value: Visibility) => {
    if (!settings || settings[key] === value || savingKey) return;
    const previous = settings;
    const next = { ...settings, [key]: value };
    setSettings(next);
    setSavingKey(key);
    setError(null);
    try {
      const res = await fetch("/api/profile/privacy", {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(next),
      });
      if (!res.ok) throw new Error("Could not save the setting.");
    } catch (err) {
      setSettings(previous);
      setError(err instanceof Error ? err.message : "Could not save the setting.");
    } finally {
      setSavingKey(null);
    }
  };

  return (
    <section className="p-6 bg-gray-900 border border-gray-800 rounded-xl">
      <h2 className="text-lg font-black mb-5">Privacy</h2>
      <div className="space-y-3">
        {ROWS.map(({ key, label }) => (
          <div key={key} className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
            <span className="font-bold text-sm text-gray-200">{label}</span>
            <div role="radiogroup" aria-label={label} className="flex bg-gray-950 border border-gray-800 rounded-lg p-1 gap-1">
              {OPTIONS.map(({ id, label: optLabel, Icon }) => {
                const selected = settings?.[key] === id;
                return (
                  <button
                    key={id}
                    type="button"
                    role="radio"
                    aria-checked={selected}
                    disabled={!settings || savingKey !== null}
                    onClick={() => choose(key, id)}
                    className={`flex items-center gap-1.5 px-3 py-1.5 rounded-md text-xs font-bold transition-colors disabled:cursor-wait ${
                      selected ? "bg-blue-600 text-white" : "text-gray-400 hover:text-white hover:bg-gray-800"
                    }`}
                  >
                    <Icon className="w-3.5 h-3.5" aria-hidden="true" />
                    {optLabel}
                  </button>
                );
              })}
            </div>
          </div>
        ))}
      </div>
      <p className="text-xs mt-3 h-4 text-red-400" aria-live="polite">{error}</p>
    </section>
  );
}
