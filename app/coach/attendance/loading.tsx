import { HeaderSkeleton, ListSkeleton, PageLoading, StatsSkeleton } from "@/components/ui/skeletons";
import { Skeleton } from "@/components/ui/skeleton";

export default function Loading() {
  return (
    <PageLoading>
      <HeaderSkeleton />
      <Skeleton className="h-[88px] rounded-3xl" /><ListSkeleton rows={7} />
    </PageLoading>
  );
}
