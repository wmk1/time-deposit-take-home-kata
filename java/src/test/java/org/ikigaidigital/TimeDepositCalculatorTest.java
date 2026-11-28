package org.ikigaidigital;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * Characterization tests: they pin the behaviour of {@code updateBalance} as it was before the
 * refactoring. Expected balances are derived by hand as
 * {@code balance + round_half_up(balance * rate / 12, 2)} and asserted as exact doubles,
 * because the behaviour must stay bit-for-bit identical.
 */
class TimeDepositCalculatorTest {

    private final TimeDepositCalculator calculator = new TimeDepositCalculator();

    @ParameterizedTest(name = "{0} plan, day {1}: {2} -> {3}")
    @DisplayName("interest is paid only inside the day window of each plan")
    @CsvSource({
            "basic,   30, 1200.00, 1200.00", // first 30 days: no interest
            "basic,   31, 1200.00, 1201.00", // 1200 * 0.01 / 12 = 1.00
            "basic, 3650, 1200.00, 1201.00", // no upper day limit: 1.00
            "student, 30, 1200.00, 1200.00", // first 30 days: no interest
            "student, 31, 1200.00, 1203.00", // 1200 * 0.03 / 12 = 3.00
            "student, 365, 1200.00, 1203.00", // last day of the first year: 3.00
            "student, 366, 1200.00, 1200.00", // no interest after one year
            "premium, 30, 1200.00, 1200.00", // first 30 days: no interest
            "premium, 31, 1200.00, 1200.00", // premium interest starts after 45 days
            "premium, 45, 1200.00, 1200.00", // still no interest
            "premium, 46, 1200.00, 1205.00", // 1200 * 0.05 / 12 = 5.00
            "premium, 3650, 1200.00, 1205.00" // no upper day limit: 5.00
    })
    void paysInterestOnlyInsideThePlanDayWindow(String planType, int days, double balance, double expected) {
        assertThat(balanceAfterUpdate(planType, days, balance)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "{0} plan, day {1}: {2} -> {3}")
    @DisplayName("each plan adds one month of interest at its own rate")
    @CsvSource({
            "basic,    45, 1234567.00, 1235595.81", // 1234567 * 0.01 / 12 = 1028.8058.. -> 1028.81
            "student, 200,    5000.00,    5012.50", // 5000 * 0.03 / 12 = 12.50
            "premium, 400,   10000.00,   10041.67" // 10000 * 0.05 / 12 = 41.666.. -> 41.67
    })
    void addsOneMonthOfInterestAtThePlanRate(String planType, int days, double balance, double expected) {
        assertThat(balanceAfterUpdate(planType, days, balance)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "plan type \"{0}\" gets no interest")
    @DisplayName("unknown plan types get no interest; plan type matching is case-sensitive")
    @CsvSource({"gold", "Basic", "Premium"})
    void paysNoInterestForUnknownPlanType(String planType) {
        // day 100 is inside the interest window of every known plan
        assertThat(balanceAfterUpdate(planType, 100, 1200.00)).isEqualTo(1200.00);
    }

    @ParameterizedTest(name = "{0} plan keeps a zero balance at zero")
    @DisplayName("a zero balance stays zero")
    @CsvSource({"basic", "student", "premium"})
    void keepsZeroBalanceAtZero(String planType) {
        // 0 * rate / 12 = 0.00; day 100 is inside the interest window of every plan
        assertThat(balanceAfterUpdate(planType, 100, 0.00)).isEqualTo(0.00);
    }

    @ParameterizedTest(name = "{0} plan, day {1}: {2} -> {3}")
    @DisplayName("interest is rounded HALF_UP to cents from its exact double value")
    @CsvSource({
            // 18 * 0.01 / 12 is the double 0.0149999999999999994.. (not 0.015) -> 0.01;
            // BigDecimal.valueOf(double) would round "0.015" up to 0.02 and give 18.02
            "basic,   31, 18.00, 18.01",
            // 6 * 0.01 / 12 is the double 0.0050000000000000001.. (just above 0.005) -> 0.01
            "basic,   31,  6.00,  6.01",
            // 50 * 0.03 / 12 is exactly 0.125 -> HALF_UP gives 0.13 (HALF_EVEN would give 0.12)
            "student, 31, 50.00, 50.13"
    })
    void roundsInterestHalfUpFromItsExactDoubleValue(String planType, int days, double balance, double expected) {
        assertThat(balanceAfterUpdate(planType, days, balance)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "{0} plan, day {1}: {2} -> {3}")
    @DisplayName("interest is computed as (balance * rate) / 12 - any other order of the double operations changes the result")
    @CsvSource({
            // 30 * 0.01 / 12 = 0.02499999.. -> 0.02; 30 / 12 * 0.01 and 30 * (0.01 / 12) give 0.02500000.. -> 0.03
            "basic,   31, 30.00, 30.02",
            // 18 * 0.03 / 12 = 0.04500000.. -> 0.05; 18 / 12 * 0.03 and 18 * (0.03 / 12) give 0.04499999.. -> 0.04
            "student, 31, 18.00, 18.05",
            // 3.6 * 0.05 / 12 = 0.01500000.. -> 0.02; 3.6 / 12 * 0.05 and 3.6 * (0.05 / 12) give 0.01499999.. -> 0.01
            "premium, 46,  3.60,  3.62"
    })
    void computesInterestAsBalanceTimesRateDividedByTwelve(String planType, int days, double balance, double expected) {
        assertThat(balanceAfterUpdate(planType, days, balance)).isEqualTo(expected);
    }

    @Test
    @DisplayName("every deposit in the list is updated in place; id, plan type and days stay unchanged")
    void updatesEveryDepositInPlaceIndependently() {
        // An immutable list: the calculator may only update the deposits, not the list itself.
        List<TimeDeposit> deposits = List.of(
                new TimeDeposit(1, "basic", 1200.00, 45),
                new TimeDeposit(2, "student", 1200.00, 400),
                new TimeDeposit(3, "premium", 1200.00, 46),
                new TimeDeposit(4, "gold", 1200.00, 100));

        calculator.updateBalance(deposits);

        assertThat(deposits)
                .extracting(TimeDeposit::getId, TimeDeposit::getPlanType, TimeDeposit::getBalance, TimeDeposit::getDays)
                .containsExactly(
                        tuple(1, "basic", 1201.00, 45),     // 1200 * 0.01 / 12 = 1.00
                        tuple(2, "student", 1200.00, 400),  // student: no interest after one year
                        tuple(3, "premium", 1205.00, 46),   // 1200 * 0.05 / 12 = 5.00
                        tuple(4, "gold", 1200.00, 100));    // unknown plan type: no interest
    }

    private double balanceAfterUpdate(String planType, int days, double balance) {
        TimeDeposit deposit = new TimeDeposit(1, planType, balance, days);
        calculator.updateBalance(List.of(deposit));
        return deposit.getBalance();
    }
}
