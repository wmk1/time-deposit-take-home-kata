package org.ikigaidigital;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** A new plan type is added by passing its policy to the calculator, without changing existing classes. */
class CustomInterestPolicyTest {

    /** A plan that is not one of the existing plans: a flat raw interest of 0.125 a month from day one. */
    private static final class LoyaltyInterestPolicy implements InterestPolicy {
        @Override
        public boolean supports(String planType) {
            return "loyalty".equals(planType);
        }

        @Override
        public double monthlyInterest(TimeDeposit deposit) {
            return 0.125;
        }
    }

    private final TimeDepositCalculator calculator = new TimeDepositCalculator(List.of(new LoyaltyInterestPolicy()));

    @Test
    @DisplayName("a custom policy adds interest for a new plan type, rounded HALF_UP by the calculator")
    void appliesCustomPolicyToNewPlanType() {
        // Day 10: the 30 interest-free days belong to the existing plans, not to the calculator.
        TimeDeposit deposit = new TimeDeposit(1, "loyalty", 100.00, 10);

        calculator.updateBalance(List.of(deposit));

        // 0.125 -> HALF_UP -> 0.13; 100.00 + 0.13 = 100.13
        assertThat(deposit.getBalance()).isEqualTo(100.13);
    }

    @Test
    @DisplayName("custom policies replace the default ones instead of being added to them")
    void usesOnlyTheGivenPolicies() {
        // Day 100 would earn interest under the default basic policy.
        TimeDeposit deposit = new TimeDeposit(1, "basic", 1200.00, 100);

        calculator.updateBalance(List.of(deposit));

        assertThat(deposit.getBalance()).isEqualTo(1200.00);
    }
}
