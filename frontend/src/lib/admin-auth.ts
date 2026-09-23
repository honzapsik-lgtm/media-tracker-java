import { getServerSession } from "next-auth";
import { authOptions } from "@/lib/auth";

export const ADMIN_AUTH_ERRORS = {
  AUTH_REQUIRED: "AUTH_REQUIRED",
  ADMIN_REQUIRED: "ADMIN_REQUIRED",
  ADMIN_USER_NOT_FOUND: "ADMIN_USER_NOT_FOUND",
} as const;

export type AdminAuthErrorCode = keyof typeof ADMIN_AUTH_ERRORS;

export type AdminUser = {
  id: string;
  email: string | null;
  name: string | null;
  role: string;
};

export class AdminAuthError extends Error {
  code: AdminAuthErrorCode;

  constructor(code: AdminAuthErrorCode) {
    super(code);
    this.code = code;
  }
}

export function isAdminUser(user: { role?: string | null }) {
  return (user.role || "").toLowerCase() === "admin";
}

export function adminStatusForError(error: unknown) {
  if (!(error instanceof AdminAuthError)) return 500;
  if (error.code === ADMIN_AUTH_ERRORS.AUTH_REQUIRED) return 401;
  return 403;
}

export async function requireAdmin(): Promise<AdminUser> {
  const session = await getServerSession(authOptions);
  if (!session?.user) {
    throw new AdminAuthError(ADMIN_AUTH_ERRORS.AUTH_REQUIRED);
  }

  const role = (session.user.role || "").toLowerCase();
  if (role !== "admin") {
    throw new AdminAuthError(ADMIN_AUTH_ERRORS.ADMIN_REQUIRED);
  }

  return {
    id: session.user.id || "",
    email: session.user.email || null,
    name: session.user.name || null,
    role: "admin",
  };
}
