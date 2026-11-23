import { getServerSession } from "next-auth";
import { redirect } from "next/navigation";
import { authOptions } from "@/lib/auth";
import RatingStyleSetting from "@/components/RatingStyleSetting";
import PrivacySetting from "@/components/PrivacySetting";

export const dynamic = "force-dynamic";

export default async function SettingsPage() {
  const session = await getServerSession(authOptions);
  if (!session?.user?.id) redirect("/");

  return (
    <main className="min-h-screen bg-gray-950 text-white pt-24 pb-12 px-8">
      <div className="max-w-3xl mx-auto">
        <h1 className="text-3xl font-black tracking-tight mb-8">Settings</h1>

        <div className="space-y-6">
          <RatingStyleSetting />
          <PrivacySetting />
        </div>
      </div>
    </main>
  );
}
