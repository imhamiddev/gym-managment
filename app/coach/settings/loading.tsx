import { HeaderSkeleton, PageLoading } from "@/components/ui/skeletons";
import { Skeleton } from "@/components/ui/skeleton";

export default function Loading() {
  return (
    <PageLoading>
      <HeaderSkeleton />
      <Skeleton className="h-52 rounded-3xl" />
      <Skeleton className="h-64 rounded-3xl" />
      <Skeleton className="h-40 rounded-3xl" />
    </PageLoading>
  );
}
