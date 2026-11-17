import { HeaderSkeleton, ListSkeleton, PageLoading, StatsSkeleton } from "@/components/ui/skeletons";
import { Skeleton } from "@/components/ui/skeleton";

export default function Loading() {
  return (
    <PageLoading>
      <HeaderSkeleton />
      <div className="grid gap-5 lg:grid-cols-[320px_1fr]"><Skeleton className="h-80 rounded-3xl" /><div className="flex flex-col gap-5"><Skeleton className="h-60 rounded-3xl" /><Skeleton className="h-36 rounded-3xl" /></div></div>
    </PageLoading>
  );
}
