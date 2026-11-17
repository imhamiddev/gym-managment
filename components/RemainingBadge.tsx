import { Badge } from "@/components/ui/badge";
import { formatDays } from "@/lib/date";

export function RemainingBadge({ days }: { days: number | null }) {
  if (days === null) return <Badge tone="neutral">بدون شهریه</Badge>;
  if (days === 0) return <Badge tone="danger" dot>منقضی شده</Badge>;
  return <Badge tone={days <= 7 ? "warning" : "success"} dot>{formatDays(days)}</Badge>;
}
