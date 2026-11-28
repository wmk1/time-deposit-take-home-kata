package org.ikigaidigital.port.out;

import java.util.List;
import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.TimeDepositWithWithdrawals;

public interface TimeDepositRepository {
    List<TimeDepositWithWithdrawals> findAllWithWithdrawals();

    // Called in the same transaction as saveBalances so simultaneous updates do not lose interest.
    List<TimeDeposit> findAllForBalanceUpdate();

    void saveBalances(List<TimeDeposit> deposits);
}
