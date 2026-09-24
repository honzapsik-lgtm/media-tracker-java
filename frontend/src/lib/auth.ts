import type { NextAuthOptions } from "next-auth";
import DiscordProvider from "next-auth/providers/discord";
import GoogleProvider from "next-auth/providers/google";

const API_BASE_URL = process.env.BACKEND_API_URL || "http://localhost:8080/api";
const GATEWAY_SECRET = process.env.INTERNAL_GATEWAY_SECRET || "";

type BackendUser = {
  id: string;
  username: string | null;
  role: string;
  name: string | null;
  email: string | null;
  image: string | null;
};

export const authOptions: NextAuthOptions = {
  session: { strategy: "jwt" },
  providers: [
    ...(process.env.DISCORD_CLIENT_ID && process.env.DISCORD_CLIENT_SECRET ? [DiscordProvider({
      clientId: process.env.DISCORD_CLIENT_ID,
      clientSecret: process.env.DISCORD_CLIENT_SECRET,
    })] : []),
    ...(process.env.GOOGLE_CLIENT_ID && process.env.GOOGLE_CLIENT_SECRET ? [GoogleProvider({
      clientId: process.env.GOOGLE_CLIENT_ID,
      clientSecret: process.env.GOOGLE_CLIENT_SECRET,
    })] : []),
  ],
  callbacks: {
    async jwt({ token, user, account }) {
      let dbUser: BackendUser | null = null;
      if (account && user) {
        const response = await fetch(`${API_BASE_URL}/auth/oauth-sync`, {
          method: "POST",
          headers: { "Content-Type": "application/json", "X-Internal-Gateway-Key": GATEWAY_SECRET },
          body: JSON.stringify({
            provider: account.provider,
            providerAccountId: account.providerAccountId,
            email: user.email || null,
            name: user.name || null,
            image: user.image || null,
          }),
        });
        if (!response.ok) throw new Error("Unable to synchronize OAuth account with the backend");
        dbUser = await response.json();
      } else if (token.id) {
        try {
          const response = await fetch(`${API_BASE_URL}/auth/me`, {
            headers: { "X-Internal-Gateway-Key": GATEWAY_SECRET, "X-User-Id": String(token.id) },
            cache: "no-store",
          });
          if (response.ok) dbUser = await response.json();
          else token.role = "user";
        } catch {
          // Keep the session through temporary outages; Java authorizes every request.
          token.role = "user";
        }
      }
      if (dbUser) {
        token.id = dbUser.id;
        token.username = dbUser.username;
        token.role = dbUser.role;
        token.name = dbUser.name;
        token.email = dbUser.email;
        token.picture = dbUser.image;
      }
      return token;
    },
    async session({ session, token }) {
      if (session.user) {
        session.user.id = token.id as string;
        session.user.role = token.role as string;
        session.user.username = token.username as string | null;
        session.user.image = token.picture;
        session.user.name = token.name;
        session.user.email = token.email;
      }
      return session;
    },
  },
};
