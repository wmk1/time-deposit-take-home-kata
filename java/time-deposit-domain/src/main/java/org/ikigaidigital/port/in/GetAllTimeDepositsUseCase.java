package org.ikigaidigital.port.in;

import java.util.List;
import org.ikigaidigital.TimeDepositWithWithdrawals;

public interface GetAllTimeDepositsUseCase {
    List<TimeDepositWithWithdrawals> getAllTimeDeposits();
}
