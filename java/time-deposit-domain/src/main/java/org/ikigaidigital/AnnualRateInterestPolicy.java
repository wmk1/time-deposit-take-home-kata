package org.ikigaidigital;

/**
 * Shared rule of the existing plans: a fixed annual rate paid monthly, and no interest in the first 30 days.
 *
 * <p>The 30-day rule lives here and not in {@link TimeDepositCalculator}: the README applies it to the
 * "existing plans", so a future plan can implement {@link InterestPolicy} directly and pay from day one.
 * Keeping it in one base class avoids repeating it in every plan.
 */
abstract class AnnualRateInterestPolicy implements InterestPolicy {
    private static final int INTEREST_FREE_DAYS = 30;

    private final String planType;
    private final double annualRate;

    AnnualRateInterestPolicy(String planType, double annualRate) {
        this.planType = planType;
        this.annualRate = annualRate;
    }

    /**
     * Exact, case-sensitive match, as before the refactoring ("Basic" gets no interest).
     * Assumption: a null plan type is unsupported and gets no interest; the original code threw a
     * NullPointerException after day 30, and the README does not require handling invalid input.
     */
    @Override
    public boolean supports(String planType) {
        return this.planType.equals(planType);
    }

    @Override
    public double monthlyInterest(TimeDeposit deposit) {
        int days = deposit.getDays();
        if (days <= INTEREST_FREE_DAYS || !paysInterestOnDay(days)) {
            return 0;
        }
        // Keep the order (balance * rate) / 12 of the original code: for some balances another order
        // of these double operations, e.g. balance * (rate / 12), rounds to a different cent.
        return deposit.getBalance() * annualRate / 12;
    }

    /** Plan-specific day rule, checked only after the 30 interest-free days. */
    abstract boolean paysInterestOnDay(int days);
}
