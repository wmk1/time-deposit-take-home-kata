package org.ikigaidigital;

import java.util.List;

public record TimeDepositWithWithdrawals(TimeDeposit deposit, List<Withdrawal> withdrawals) {
    public TimeDepositWithWithdrawals {
        withdrawals = List.copyOf(withdrawals);
    }
}
