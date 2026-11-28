package org.ikigaidigital;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * Contract of the policies that the calculator-level characterization tests cannot see:
 * the interest is returned unrounded, and a null plan type is simply not supported.
 */
class InterestPolicyTest {

    static Stream<Arguments> unroundedInterest() {
        return Stream.of(
                // 10 * 0.01 / 12 = 0.0083333.. as the nearest double, not rounded to cents
                arguments(named("basic", new BasicInterestPolicy()), new TimeDeposit(1, "basic", 10.00, 31), 0.008333333333333333));
    }

    static Stream<Arguments> policies() {
        return Stream.of(arguments(named("basic", new BasicInterestPolicy())));
    }

    @ParameterizedTest(name = "{0} policy")
    @MethodSource("unroundedInterest")
    @DisplayName("returns the raw monthly interest; rounding is left to the calculator")
    void returnsTheUnroundedMonthlyInterest(InterestPolicy policy, TimeDeposit deposit, double expected) {
        assertThat(policy.monthlyInterest(deposit)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "{0} policy")
    @MethodSource("policies")
    @DisplayName("does not support a null plan type")
    void doesNotSupportNullPlanType(InterestPolicy policy) {
        assertThat(policy.supports(null)).isFalse();
    }
}
