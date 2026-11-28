package org.ikigaidigital;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** A new plan type is added by passing its policy to the calculator, without changing existing classes. */
class CustomInterestPolicyTest {

    /** A flat raw interest a month for one plan type, from day one - unlike the existing plans. */
    private static final class FlatInterestPolicy implements InterestPolicy {
        private final String planType;
        private final double interest;

        FlatInterestPolicy(String planType, double interest) {
            this.planType = planType;
            this.interest = interest;
        }

        @Override
        public boolean supports(String planType) {
            return this.planType.equals(planType);
        }

        @Override
        public double monthlyInterest(TimeDeposit deposit) {
            return interest;
        }
    }

    private final TimeDepositCalculator calculator =
            new TimeDepositCalculator(List.of(new FlatInterestPolicy("loyalty", 0.125)));

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

    @Test
    @DisplayName("the first policy that supports a plan type wins, so it can replace a default policy")
    void appliesOnlyTheFirstPolicyThatSupportsThePlanType() {
        TimeDepositCalculator overridingBasic = new TimeDepositCalculator(
                List.of(new FlatInterestPolicy("basic", 0.125), new BasicInterestPolicy()));
        TimeDeposit deposit = new TimeDeposit(1, "basic", 1200.00, 100);

        overridingBasic.updateBalance(List.of(deposit));

        // flat 0.125 -> 0.13; the default basic policy alone would add 1200 * 0.01 / 12 = 1.00
        assertThat(deposit.getBalance()).isEqualTo(1200.13);
    }
}
