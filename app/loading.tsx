import { LogoMark } from "@/components/ui/logo";
import { Loader } from "@/components/ui/loader";

export default function Loading() {
  return (
    <div className="grid min-h-dvh place-items-center" role="status" aria-label="در حال بارگذاری">
      <div className="flex flex-col items-center gap-5">
        <LogoMark className="size-16 rounded-3xl [&_svg]:size-8 motion-safe:animate-pulse" />
        <Loader variant="dots" size={28} />
      </div>
    </div>
  );
}
