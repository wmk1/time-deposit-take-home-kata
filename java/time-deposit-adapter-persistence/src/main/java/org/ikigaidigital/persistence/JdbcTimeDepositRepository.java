package org.ikigaidigital.persistence;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.TimeDepositWithWithdrawals;
import org.ikigaidigital.Withdrawal;
import org.ikigaidigital.port.out.TimeDepositRepository;
import org.springframework.jdbc.core.simple.JdbcClient;

public final class JdbcTimeDepositRepository implements TimeDepositRepository {
    private final JdbcClient jdbc;

    public JdbcTimeDepositRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<TimeDepositWithWithdrawals> findAllWithWithdrawals() {
        return jdbc.sql("""
                SELECT d.id, d."planType", d.balance, d.days, w.id AS withdrawal_id, w.amount, w.date
                FROM "timeDeposits" d
                LEFT JOIN withdrawals w ON w."timeDepositId" = d.id
                ORDER BY d.id, w.id
                """).query(JdbcTimeDepositRepository::groupWithdrawalsByDeposit);
    }

    @Override
    public List<TimeDeposit> findAllForBalanceUpdate() {
        return jdbc.sql("""
                SELECT id, "planType", balance, days
                FROM "timeDeposits"
                ORDER BY id
                FOR NO KEY UPDATE
                """).query((row, rowNumber) -> mapDeposit(row)).list();
    }

    @Override
    public void saveBalances(List<TimeDeposit> deposits) {
        for (TimeDeposit deposit : deposits) {
            // Keep the existing double API and round to cents at the database boundary.
            var balance = BigDecimal.valueOf(deposit.getBalance()).setScale(2, RoundingMode.HALF_UP);
            // The monthly command accrues interest only. Days are maintained independently.
            jdbc.sql("""
                    UPDATE "timeDeposits"
                    SET balance = :balance
                    WHERE id = :id
                    """)
                    .param("balance", balance)
                    .param("id", deposit.getId())
                    .update();
        }
    }

    private static List<TimeDepositWithWithdrawals> groupWithdrawalsByDeposit(ResultSet rows) throws SQLException {
        Map<Integer, DepositGroup> deposits = new LinkedHashMap<>();
        while (rows.next()) {
            int depositId = rows.getInt("id");
            DepositGroup group = deposits.get(depositId);
            if (group == null) {
                group = new DepositGroup(mapDeposit(rows), new ArrayList<>());
                deposits.put(depositId, group);
            }
            Integer withdrawalId = rows.getObject("withdrawal_id", Integer.class);
            if (withdrawalId != null) {
                group.withdrawals().add(new Withdrawal(
                        withdrawalId, rows.getBigDecimal("amount"), rows.getObject("date", LocalDate.class)));
            }
        }
        return deposits.values().stream()
                .map(group -> new TimeDepositWithWithdrawals(group.deposit(), group.withdrawals()))
                .toList();
    }

    private static TimeDeposit mapDeposit(ResultSet row) throws SQLException {
        return new TimeDeposit(
                row.getInt("id"), row.getString("planType"), row.getDouble("balance"), row.getInt("days"));
    }

    private record DepositGroup(TimeDeposit deposit, List<Withdrawal> withdrawals) {}
}
