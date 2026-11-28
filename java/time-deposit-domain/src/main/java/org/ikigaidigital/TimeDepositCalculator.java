package org.ikigaidigital;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class TimeDepositCalculator {
    private final List<InterestPolicy> policies = List.of(new BasicInterestPolicy(), new StudentInterestPolicy());

    public void updateBalance(List<TimeDeposit> xs) {
        for (int i = 0; i < xs.size(); i++) {
            double interest = monthlyInterest(xs.get(i));

            double a2d = xs.get(i).getBalance() + (new BigDecimal(interest).setScale(2, RoundingMode.HALF_UP)).doubleValue();
            xs.get(i).setBalance(a2d);
        }
    }

    private double monthlyInterest(TimeDeposit deposit) {
        for (InterestPolicy policy : policies) {
            if (policy.supports(deposit.getPlanType())) {
                return policy.monthlyInterest(deposit);
            }
        }

        double interest = 0;
        if (deposit.getDays() > 30) {
            if (deposit.getPlanType().equals("premium")) {
                if (deposit.getDays() > 45) {
                    interest += deposit.getBalance() * 0.05 / 12;
                }
            }
        }
        return interest;
    }
}
