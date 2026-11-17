import { HeaderSkeleton, ListSkeleton, PageLoading, StatsSkeleton } from "@/components/ui/skeletons";
import { Skeleton } from "@/components/ui/skeleton";

export default function Loading() {
  return (
    <PageLoading>
      <HeaderSkeleton />
      <Skeleton className="h-24 rounded-[28px]" /><StatsSkeleton /><ListSkeleton rows={3} />
    </PageLoading>
  );
}
