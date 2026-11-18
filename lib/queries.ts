import { db } from "./db";
import { remainingDays, ymdAdd, ymdTehran } from "./date";

export type MemberView = {
  id: string;
  firstName: string;
  lastName: string;
  phone: string;
  joinedYmd: string;
  remaining: number | null;
  lastPayment: { amount: number; paidYmd: string; durationDays: number } | null;
  presentLast30: number;
  heightCm: number | null;
  weightKg: number | null;
  goal: string | null;
  program: string | null;
  notes: string | null;
  birthYmd: string | null;
};

export type PaymentView = {
  id: string;
  membershipId: string;
  memberName: string;
  amount: number;
  paidYmd: string;
  durationDays: number;
};

const paidYmd = (d: Date) => d.toISOString().slice(0, 10);

export async function loadMembers(gymId: string): Promise<MemberView[]> {
  const today = ymdTehran(new Date());
  const since = ymdAdd(today, -29);
  const rows = await db.membership.findMany({
    where: { gymId, status: "APPROVED" },
    include: {
      athlete: true,
      payments: { orderBy: { paidAt: "desc" } },
      attendances: { where: { present: true, date: { gte: since } }, select: { id: true } },
    },
    orderBy: { athlete: { lastName: "asc" } },
  });
  return rows.map((m) => {
    const payments = m.payments.map((p) => ({ amount: p.amount, paidYmd: paidYmd(p.paidAt), durationDays: p.durationDays }));
    return {
      id: m.id,
      firstName: m.athlete.firstName,
      lastName: m.athlete.lastName,
      phone: m.athlete.phone,
      joinedYmd: ymdTehran(m.approvedAt ?? m.createdAt),
      remaining: remainingDays(payments, today),
      lastPayment: payments[0] ?? null,
      presentLast30: m.attendances.length,
      heightCm: m.heightCm,
      weightKg: m.weightKg,
      goal: m.goal,
      program: m.program,
      notes: m.notes,
      birthYmd: m.birthYmd,
    };
  });
}

export async function loadPayments(gymId: string): Promise<PaymentView[]> {
  const rows = await db.payment.findMany({
    where: { membership: { gymId } },
    include: { membership: { include: { athlete: true } } },
    orderBy: [{ paidAt: "desc" }, { createdAt: "desc" }],
  });
  return rows.map((p) => ({
    id: p.id,
    membershipId: p.membershipId,
    memberName: `${p.membership.athlete.firstName} ${p.membership.athlete.lastName}`,
    amount: p.amount,
    paidYmd: paidYmd(p.paidAt),
    durationDays: p.durationDays,
  }));
}

export async function loadPending(gymId: string) {
  const rows = await db.membership.findMany({
    where: { gymId, status: "PENDING" },
    include: { athlete: true },
    orderBy: { createdAt: "asc" },
  });
  return rows.map((m) => ({
    id: m.id,
    name: `${m.athlete.firstName} ${m.athlete.lastName}`,
    phone: m.athlete.phone,
    requestedYmd: ymdTehran(m.createdAt),
  }));
}
