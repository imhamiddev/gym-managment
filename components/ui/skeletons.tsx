import { Skeleton } from "./skeleton";

export function HeaderSkeleton() {
  return (
    <div className="flex items-center gap-3.5" aria-hidden>
      <Skeleton className="size-12 rounded-2xl" />
      <div className="flex flex-col gap-2">
        <Skeleton className="h-6 w-44" />
        <Skeleton className="h-4 w-28" />
      </div>
    </div>
  );
}

export function StatsSkeleton({ count = 4 }: { count?: number }) {
  return (
    <div className="grid grid-cols-2 gap-3 lg:grid-cols-4" aria-hidden>
      {Array.from({ length: count }, (_, i) => (
        <div key={i} className="flex flex-col gap-3 rounded-3xl border border-border bg-background p-4">
          <Skeleton className="size-10 rounded-2xl" />
          <Skeleton className="h-8 w-14" />
          <Skeleton className="h-4 w-24" />
        </div>
      ))}
    </div>
  );
}

export function ListSkeleton({ rows = 6 }: { rows?: number }) {
  return (
    <div className="divide-y divide-border overflow-hidden rounded-3xl border border-border bg-background" aria-hidden>
      {Array.from({ length: rows }, (_, i) => (
        <div key={i} className="flex items-center gap-3 px-4 py-3.5">
          <Skeleton className="size-9 shrink-0 rounded-full" />
          <div className="flex flex-1 flex-col gap-2">
            <Skeleton className="h-4 w-36" />
            <Skeleton className="h-3 w-24" />
          </div>
          <Skeleton className="h-7 w-20 rounded-full" />
        </div>
      ))}
    </div>
  );
}

export function PageLoading({ children }: { children: React.ReactNode }) {
  return (
    <div role="status" aria-label="در حال بارگذاری" className="flex flex-col gap-6">
      {children}
    </div>
  );
}
