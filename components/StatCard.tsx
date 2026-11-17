import { Icon, type IconName } from "@/components/ui/icon";
import { TONE, type Tone } from "@/components/ui/badge";
import { FaNumber } from "@/components/ui/fa-number";
import { cn } from "@/lib/utils";

export function StatCard({ icon, tone, label, value }: { icon: IconName; tone: Tone; label: string; value: number }) {
  return (
    <div className="flex flex-col gap-3 rounded-3xl border border-border bg-background p-4 shadow-card sm:p-5">
      <span className={cn("grid size-10 place-items-center rounded-2xl", TONE[tone].soft)}>
        <Icon name={icon} size={22} set="bulk" />
      </span>
      <div>
        <FaNumber value={value} className="block text-3xl font-bold leading-10" />
        <span className="text-[13px] leading-6 text-muted-foreground">{label}</span>
      </div>
    </div>
  );
}
