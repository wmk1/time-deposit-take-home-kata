package org.ikigaidigital;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** Adds one month of interest to every deposit; the interest rule of each plan is an {@link InterestPolicy}. */
public class TimeDepositCalculator {
    private final List<InterestPolicy> policies =
            List.of(new BasicInterestPolicy(), new StudentInterestPolicy(), new PremiumInterestPolicy());

    public void updateBalance(List<TimeDeposit> deposits) {
        for (TimeDeposit deposit : deposits) {
            // Rounding exactly as before the refactoring: new BigDecimal(double) takes the exact binary value
            // of the interest, while BigDecimal.valueOf(double) would round its decimal string and change
            // some cents (e.g. 0.01499.. would become 0.02 instead of 0.01).
            double roundedInterest = new BigDecimal(monthlyInterest(deposit)).setScale(2, RoundingMode.HALF_UP).doubleValue();
            deposit.setBalance(deposit.getBalance() + roundedInterest);
        }
    }

    private double monthlyInterest(TimeDeposit deposit) {
        return policies.stream()
                .filter(policy -> policy.supports(deposit.getPlanType()))
                .mapToDouble(policy -> policy.monthlyInterest(deposit))
                .findFirst()
                .orElse(0); // unknown plan type: no interest
    }
}
