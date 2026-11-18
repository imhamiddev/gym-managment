import { GymSettingsForm } from "@/components/GymSettingsForm";
import { PageHeader } from "@/components/ui/page-header";
import { requireGym } from "@/lib/coach";

export const metadata = { title: "تنظیمات باشگاه" };

export default async function GymSettingsPage() {
  const gym = await requireGym();
  return (
    <>
      <PageHeader title="تنظیمات باشگاه" description="نام، کد دعوت، روزها و ساعت کاری، شماره کارت" icon="setting" />
      <GymSettingsForm
        initial={{ name: gym.name, code: gym.code, workDays: gym.workDays, openTime: gym.openTime, closeTime: gym.closeTime, cardNumber: gym.cardNumber }}
      />
    </>
  );
}
