package org.ikigaidigital.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.TimeDepositCalculator;
import org.ikigaidigital.TimeDepositWithWithdrawals;
import org.ikigaidigital.Withdrawal;
import org.ikigaidigital.port.out.TimeDepositRepository;
import org.junit.jupiter.api.Test;

class TimeDepositServicesTest {
    private final TimeDeposit eligible = new TimeDeposit(1, "basic", 1200.0, 31);
    private final TimeDeposit ineligible = new TimeDeposit(2, "premium", 1200.0, 45);
    private final Withdrawal withdrawal = new Withdrawal(8, new BigDecimal("250.50"), LocalDate.of(2024, 5, 3));
    private final List<TimeDepositWithWithdrawals> deposits = List.of(
            new TimeDepositWithWithdrawals(eligible, List.of(withdrawal)),
            new TimeDepositWithWithdrawals(ineligible, List.of()));
    private final InMemoryTimeDepositRepository repository = new InMemoryTimeDepositRepository(deposits);

    @Test
    void savesAllBalancesAfterApplyingOneMonthOfInterest() {
        new UpdateAllBalancesService(repository, new TimeDepositCalculator()).updateAllBalances();

        // Basic earns 1200 * 0.01 / 12 = 1.00, regardless of the withdrawal. Premium is on day 45.
        assertThat(repository.saved)
                .extracting(TimeDeposit::getId, TimeDeposit::getBalance)
                .containsExactly(tuple(1, 1201.0), tuple(2, 1200.0));
    }

    @Test
    void returnsDepositsWithTheirWithdrawals() {
        assertThat(new GetAllTimeDepositsService(repository).getAllTimeDeposits()).containsExactly(
                new TimeDepositWithWithdrawals(eligible, List.of(withdrawal)),
                new TimeDepositWithWithdrawals(ineligible, List.of()));
    }

    private static final class InMemoryTimeDepositRepository implements TimeDepositRepository {
        private final List<TimeDepositWithWithdrawals> deposits;
        private List<TimeDeposit> saved = List.of();

        private InMemoryTimeDepositRepository(List<TimeDepositWithWithdrawals> deposits) {
            this.deposits = deposits;
        }

        @Override
        public List<TimeDepositWithWithdrawals> findAllWithWithdrawals() {
            return deposits;
        }

        @Override
        public List<TimeDeposit> findAllForBalanceUpdate() {
            return deposits.stream()
                    .map(TimeDepositWithWithdrawals::deposit)
                    .map(deposit -> new TimeDeposit(
                            deposit.getId(), deposit.getPlanType(), deposit.getBalance(), deposit.getDays()))
                    .toList();
        }

        @Override
        public void saveBalances(List<TimeDeposit> deposits) {
            saved = List.copyOf(deposits);
        }
    }
}
