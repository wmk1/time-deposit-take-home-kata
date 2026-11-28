package org.ikigaidigital.service;

import org.ikigaidigital.TimeDepositCalculator;
import org.ikigaidigital.port.in.UpdateAllBalancesUseCase;
import org.ikigaidigital.port.out.TimeDepositRepository;

public final class UpdateAllBalancesService implements UpdateAllBalancesUseCase {
    private final TimeDepositRepository repository;
    private final TimeDepositCalculator calculator;

    public UpdateAllBalancesService(TimeDepositRepository repository, TimeDepositCalculator calculator) {
        this.repository = repository;
        this.calculator = calculator;
    }

    @Override
    public void updateAllBalances() {
        var deposits = repository.findAllForBalanceUpdate();
        // Each call applies one month of interest. Withdrawals are historical and do not affect it.
        calculator.updateBalance(deposits);
        repository.saveBalances(deposits);
    }
}
