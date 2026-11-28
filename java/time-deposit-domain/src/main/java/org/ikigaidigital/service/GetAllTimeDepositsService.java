package org.ikigaidigital.service;

import java.util.List;
import org.ikigaidigital.TimeDepositWithWithdrawals;
import org.ikigaidigital.port.in.GetAllTimeDepositsUseCase;
import org.ikigaidigital.port.out.TimeDepositRepository;

public final class GetAllTimeDepositsService implements GetAllTimeDepositsUseCase {
    private final TimeDepositRepository repository;

    public GetAllTimeDepositsService(TimeDepositRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<TimeDepositWithWithdrawals> getAllTimeDeposits() {
        return repository.findAllWithWithdrawals();
    }
}
