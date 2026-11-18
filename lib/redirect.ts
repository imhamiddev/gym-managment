/** Only same-site paths ("/checkin?x=1"); rejects "//host", "https://..." and backslashes. */
export function safeNext(next: string | null | undefined) {
  const n = (next ?? "").trim();
  return n.startsWith("/") && !n.startsWith("//") && !n.includes("\\") ? n : null;
}
