package org.ikigaidigital;

/** Basic plan: 1% a year, paid monthly after the first 30 days, without an upper day limit. */
public final class BasicInterestPolicy implements InterestPolicy {

    @Override
    public boolean supports(String planType) {
        return "basic".equals(planType);
    }

    @Override
    public double monthlyInterest(TimeDeposit deposit) {
        if (deposit.getDays() <= 30) {
            return 0;
        }
        return deposit.getBalance() * 0.01 / 12;
    }
}
