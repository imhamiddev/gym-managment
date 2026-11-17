import type { Metadata, Viewport } from "next";
import "@fontsource-variable/vazirmatn";
import "./globals.css";
import { Providers } from "@/components/Providers";
import { RegisterSW } from "@/components/RegisterSW";

export const metadata: Metadata = {
  title: { default: "باشگاه‌یار | مدیریت شهریه، اعضا و حضور و غیاب", template: "%s | باشگاه‌یار" },
  description: "نرم‌افزار ساده برای مدیریت باشگاه ورزشی",
  applicationName: "باشگاه‌یار",
  appleWebApp: { capable: true, title: "باشگاه‌یار", statusBarStyle: "default" },
  icons: { icon: "/icons/icon-192.png", apple: "/icons/apple-touch-icon.png" },
};

export const viewport: Viewport = {
  width: "device-width",
  initialScale: 1,
  viewportFit: "cover",
  themeColor: [
    { media: "(prefers-color-scheme: light)", color: "#f6f7f9" },
    { media: "(prefers-color-scheme: dark)", color: "#0d0d0f" },
  ],
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="fa" dir="rtl" suppressHydrationWarning>
      <body>
        <Providers>{children}</Providers>
        <RegisterSW />
      </body>
    </html>
  );
}
