package org.ikigaidigital.rest;

import java.util.List;
import org.ikigaidigital.TimeDepositWithWithdrawals;
import org.ikigaidigital.port.in.GetAllTimeDepositsUseCase;
import org.ikigaidigital.port.in.UpdateAllBalancesUseCase;
import org.ikigaidigital.rest.generated.api.TimeDepositsApi;
import org.ikigaidigital.rest.generated.model.TimeDepositResponse;
import org.ikigaidigital.rest.generated.model.WithdrawalResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public final class TimeDepositController implements TimeDepositsApi {
    private final GetAllTimeDepositsUseCase getAllTimeDeposits;
    private final UpdateAllBalancesUseCase updateAllBalances;

    public TimeDepositController(GetAllTimeDepositsUseCase getAllTimeDeposits, UpdateAllBalancesUseCase updateAllBalances) {
        this.getAllTimeDeposits = getAllTimeDeposits;
        this.updateAllBalances = updateAllBalances;
    }

    @Override
    public ResponseEntity<List<TimeDepositResponse>> getAllTimeDeposits() {
        return ResponseEntity.ok(getAllTimeDeposits.getAllTimeDeposits().stream()
                .map(TimeDepositController::toResponse)
                .toList());
    }

    @Override
    public ResponseEntity<Void> updateAllBalances() {
        updateAllBalances.updateAllBalances();
        return ResponseEntity.noContent().build();
    }

    private static TimeDepositResponse toResponse(TimeDepositWithWithdrawals row) {
        var deposit = row.deposit();
        var withdrawals = row.withdrawals().stream()
                .map(withdrawal -> new WithdrawalResponse(withdrawal.id(), withdrawal.amount(), withdrawal.date()))
                .toList();
        return new TimeDepositResponse(
                deposit.getId(), deposit.getPlanType(), deposit.getBalance(), deposit.getDays(), withdrawals);
    }
}
