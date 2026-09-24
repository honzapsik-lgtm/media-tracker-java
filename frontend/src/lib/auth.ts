import type { NextAuthOptions } from "next-auth";
import DiscordProvider from "next-auth/providers/discord";
import GoogleProvider from "next-auth/providers/google";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080/api";
const GATEWAY_SECRET = process.env.INTERNAL_GATEWAY_SECRET || "default-internal-secret-change-in-prod-123456";

export const authOptions: NextAuthOptions = {
  session: {
    strategy: "jwt",
  },
  providers: [
    DiscordProvider({
      clientId: process.env.DISCORD_CLIENT_ID!,
      clientSecret: process.env.DISCORD_CLIENT_SECRET!,
    }),
    GoogleProvider({
      clientId: process.env.GOOGLE_CLIENT_ID!,
      clientSecret: process.env.GOOGLE_CLIENT_SECRET!,
    }),
  ],
  callbacks: {
    async jwt({ token, user, account, trigger, session }) {
      if (account && user) {
        try {
          const res = await fetch(`${API_BASE_URL}/auth/oauth-sync`, {
            method: "POST",
            headers: {
              "Content-Type": "application/json",
              "X-Internal-Gateway-Key": GATEWAY_SECRET,
            },
            body: JSON.stringify({
              provider: account.provider,
              providerAccountId: account.providerAccountId,
              email: user.email || null,
              name: user.name || null,
              image: user.image || null,
            }),
          });
          if (res.ok) {
            const dbUser = await res.json();
            token.id = dbUser.id;
            token.username = dbUser.username || null;
            token.role = dbUser.role || "user";
            token.name = dbUser.name || user.name;
            token.email = dbUser.email || user.email;
            token.picture = dbUser.image || user.image;
          } else {
            console.error("[NextAuth] Failed to sync oauth user, status:", res.status);
            token.id = user.id;
          }
        } catch (err) {
          console.error("[NextAuth] Error syncing oauth user with backend:", err);
          token.id = user.id;
        }
      }

      if (trigger === "update" && session?.username) {
        token.username = session.username;
      }

      if (token.id && !token.username && !account) {
        try {
          const res = await fetch(`${API_BASE_URL}/auth/me`, {
            headers: {
              "X-Internal-Gateway-Key": GATEWAY_SECRET,
              "X-User-Id": token.id as string,
            },
          });
          if (res.ok) {
            const freshUser = await res.json();
            if (freshUser.username) {
              token.username = freshUser.username;
            }
            if (freshUser.role) {
              token.role = freshUser.role;
            }
          }
        } catch {
          // ignore background fetch error
        }
      }

      return token;
    },
    async session({ session, token }) {
      if (session.user) {
        session.user.id = token.id as string;
        session.user.role = token.role as string;
        session.user.username = token.username as string;
        if (token.picture) session.user.image = token.picture as string;
        if (token.name) session.user.name = token.name as string;
        if (token.email) session.user.email = token.email as string;
      }
      return session;
    },
  },
};
