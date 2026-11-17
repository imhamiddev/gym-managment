"use client";

import { useCallback, useEffect, useState } from "react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Icon } from "@/components/ui/icon";
import { useToast } from "@/components/ui/toast";

type InstallEvent = Event & { prompt: () => Promise<void>; userChoice: Promise<{ outcome: string }> };

function urlBase64ToUint8Array(b64: string) {
  const pad = "=".repeat((4 - (b64.length % 4)) % 4);
  const raw = atob((b64 + pad).replace(/-/g, "+").replace(/_/g, "/"));
  return Uint8Array.from(raw, (c) => c.charCodeAt(0));
}

const isStandalone = () =>
  window.matchMedia("(display-mode: standalone)").matches || (navigator as unknown as { standalone?: boolean }).standalone === true;
const isIOS = () => /iphone|ipad|ipod/i.test(navigator.userAgent);

/** "Install on phone" card (PWA). */
export function InstallCard() {
  const [evt, setEvt] = useState<InstallEvent | null>(null);
  const [installed, setInstalled] = useState(false);
  const [ios, setIos] = useState(false);

  useEffect(() => {
    setInstalled(isStandalone());
    setIos(isIOS());
    const onPrompt = (e: Event) => {
      e.preventDefault();
      setEvt(e as InstallEvent);
    };
    const onInstalled = () => { setInstalled(true); setEvt(null); };
    window.addEventListener("beforeinstallprompt", onPrompt);
    window.addEventListener("appinstalled", onInstalled);
    return () => {
      window.removeEventListener("beforeinstallprompt", onPrompt);
      window.removeEventListener("appinstalled", onInstalled);
    };
  }, []);

  async function install() {
    if (!evt) return;
    await evt.prompt();
    await evt.userChoice;
    setEvt(null);
  }

  return (
    <Card className="shadow-card">
      <CardHeader>
        <CardTitle as="h2" className="text-base">نصب روی گوشی</CardTitle>
        <CardDescription>برنامه را مثل یک اپ روی صفحه‌ی اصلی گوشی داشته باشید.</CardDescription>
      </CardHeader>
      <CardContent className="flex flex-col gap-3">
        {installed ? (
          <Badge tone="success" className="w-fit">نصب شده است</Badge>
        ) : evt ? (
          <Button onClick={install} className="w-fit"><Icon name="home" size={18} />نصب برنامه</Button>
        ) : ios ? (
          <p className="text-sm leading-7 text-muted-foreground">
            در Safari دکمه‌ی اشتراک‌گذاری (Share) را بزنید و «Add to Home Screen» را انتخاب کنید.
          </p>
        ) : (
          <p className="text-sm leading-7 text-muted-foreground">
            از منوی مرورگر (⋮) گزینه‌ی «Install app» یا «افزودن به صفحه‌ی اصلی» را بزنید.
          </p>
        )}
      </CardContent>
    </Card>
  );
}

type State = "loading" | "unsupported" | "needs-install" | "denied" | "off" | "on";

/** Notification permission + push subscription card. */
export function NotificationsCard({ vapidPublicKey }: { vapidPublicKey: string | null }) {
  const toast = useToast();
  const [state, setState] = useState<State>("loading");
  const [busy, setBusy] = useState(false);

  const refresh = useCallback(async () => {
    if (!vapidPublicKey || !("serviceWorker" in navigator) || !("PushManager" in window) || !("Notification" in window)) {
      // iOS only exposes push to installed web apps
      setState(isIOS() && !isStandalone() ? "needs-install" : "unsupported");
      return;
    }
    if (Notification.permission === "denied") return setState("denied");
    const reg = await navigator.serviceWorker.ready;
    const sub = await reg.pushManager.getSubscription();
    setState(sub && Notification.permission === "granted" ? "on" : "off");
  }, [vapidPublicKey]);

  useEffect(() => { void refresh(); }, [refresh]);

  async function enable() {
    setBusy(true);
    try {
      const perm = await Notification.requestPermission();
      if (perm !== "granted") return setState(perm === "denied" ? "denied" : "off");
      const reg = await navigator.serviceWorker.ready;
      const sub =
        (await reg.pushManager.getSubscription()) ??
        (await reg.pushManager.subscribe({ userVisibleOnly: true, applicationServerKey: urlBase64ToUint8Array(vapidPublicKey!) }));
      const res = await fetch("/api/push/subscribe", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(sub.toJSON()),
      });
      if (!res.ok) throw new Error();
      setState("on");
      toast.success("اعلان‌ها فعال شد");
    } catch {
      toast.error("فعال‌سازی اعلان‌ها انجام نشد");
    } finally {
      setBusy(false);
    }
  }

  async function disable() {
    setBusy(true);
    try {
      const reg = await navigator.serviceWorker.ready;
      const sub = await reg.pushManager.getSubscription();
      if (sub) {
        await fetch("/api/push/unsubscribe", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ endpoint: sub.endpoint }),
        });
        await sub.unsubscribe();
      }
      setState("off");
      toast.info("اعلان‌ها غیرفعال شد");
    } finally {
      setBusy(false);
    }
  }

  async function sendTest() {
    setBusy(true);
    try {
      const res = await fetch("/api/push/test", { method: "POST" });
      if (!res.ok) toast.error("ارسال اعلان آزمایشی انجام نشد");
    } finally {
      setBusy(false);
    }
  }

  return (
    <Card className="shadow-card">
      <CardHeader>
        <CardTitle as="h2" className="text-base">اعلان‌ها</CardTitle>
        <CardDescription>۲ روز مانده، ۱ روز مانده و روز پایان شهریه به شما یادآوری می‌شود.</CardDescription>
      </CardHeader>
      <CardContent className="flex flex-col gap-3">
        {state === "loading" ? null : state === "unsupported" ? (
          <p className="text-sm leading-7 text-muted-foreground">این مرورگر یا تنظیمات سرور از اعلان پشتیبانی نمی‌کند.</p>
        ) : state === "needs-install" ? (
          <p className="text-sm leading-7 text-muted-foreground">
            در آیفون ابتدا برنامه را به صفحه‌ی اصلی اضافه کنید (کارت «نصب روی گوشی») و از روی آیکن آن وارد شوید؛ بعد اعلان‌ها قابل فعال‌سازی است.
          </p>
        ) : state === "denied" ? (
          <p className="text-sm leading-7 text-muted-foreground">
            اعلان‌ها در مرورگر مسدود شده است. از تنظیمات سایت در مرورگر، دسترسی اعلان را روی «Allow» بگذارید.
          </p>
        ) : state === "on" ? (
          <div className="flex flex-wrap items-center gap-2">
            <Badge tone="success">فعال</Badge>
            <Button variant="soft" size="sm" onClick={sendTest} disabled={busy}>اعلان آزمایشی</Button>
            <Button variant="ghost" size="sm" onClick={disable} disabled={busy}>غیرفعال‌سازی</Button>
          </div>
        ) : (
          <Button onClick={enable} loading={busy} className="w-fit"><Icon name="bell" size={18} />فعال‌سازی اعلان‌ها</Button>
        )}
      </CardContent>
    </Card>
  );
}
