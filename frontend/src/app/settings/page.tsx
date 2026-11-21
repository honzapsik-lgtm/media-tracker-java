import { getServerSession } from "next-auth";
import { redirect } from "next/navigation";
import { authOptions } from "@/lib/auth";

export const dynamic = "force-dynamic";

export default async function SettingsPage() {
  const session = await getServerSession(authOptions);
  if (!session?.user?.id) redirect("/");

  return (
    <main className="min-h-screen bg-gray-950 text-white pt-24 pb-12 px-8">
      <div className="max-w-3xl mx-auto">
        <h1 className="text-3xl font-black tracking-tight mb-2">Settings</h1>
        <p className="text-sm text-gray-500 mb-8">Manage your account and preferences.</p>

        <div className="p-8 bg-gray-900 border border-gray-800 border-dashed rounded-xl text-center text-sm text-gray-500 italic">
          Nothing here yet. Settings are coming soon.
        </div>
      </div>
    </main>
  );
}
