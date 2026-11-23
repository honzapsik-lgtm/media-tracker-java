import type { Metadata } from "next";
import { Providers } from "@/components/Providers";
import { Geist, Geist_Mono } from "next/font/google";
import "./globals.css";
import Navbar from "@/components/Navbar";
import UsernamePromptModal from "@/components/UsernamePromptModal";
import ScrollToTopOnNavigate from "@/components/ScrollToTopOnNavigate";
import { getRatingStyle } from "@/lib/preferences";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: "Media Tracker",
  description: "Track and rate movies, TV shows, anime, manga and video games in one place.",
};

export default async function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  const ratingStyle = await getRatingStyle();
  return (
    <html lang="en">
      <body className={`${geistSans.variable} ${geistMono.variable} antialiased bg-gray-950 text-white min-h-screen`}>
        <Providers ratingStyle={ratingStyle}>
          <Navbar />
          <ScrollToTopOnNavigate />
          <UsernamePromptModal />
          <div className="pt-16">{children}</div>
        </Providers>
      </body>
    </html>
  );
}
