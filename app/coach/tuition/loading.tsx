import { HeaderSkeleton, ListSkeleton, PageLoading, StatsSkeleton } from "@/components/ui/skeletons";
import { Skeleton } from "@/components/ui/skeleton";

export default function Loading() {
  return (
    <PageLoading>
      <HeaderSkeleton />
      <div className="grid grid-cols-2 gap-3"><Skeleton className="h-24 rounded-3xl" /><Skeleton className="h-24 rounded-3xl" /></div><ListSkeleton rows={6} />
    </PageLoading>
  );
}
