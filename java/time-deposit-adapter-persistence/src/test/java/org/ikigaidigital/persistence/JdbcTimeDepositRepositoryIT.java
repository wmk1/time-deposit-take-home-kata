package org.ikigaidigital.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.Withdrawal;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
class JdbcTimeDepositRepositoryIT {
    @Container
    private static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18");

    private static JdbcClient jdbc;

    @BeforeAll
    static void migrate() {
        var dataSource = new DriverManagerDataSource(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        Flyway.configure().dataSource(dataSource).load().migrate();
        jdbc = JdbcClient.create(dataSource);
    }

    @BeforeEach
    void seed() {
        jdbc.sql("""
                TRUNCATE withdrawals, "timeDeposits"
                """).update();
        jdbc.sql("""
                INSERT INTO "timeDeposits" (id, "planType", days, balance)
                VALUES (2, 'student', 365, 1200.25), (1, 'basic', 30, 1000.50)
                """).update();
    }

    @Test
    void readsDepositsWithAndWithoutWithdrawals() {
        jdbc.sql("""
                INSERT INTO withdrawals (id, "timeDepositId", amount, date)
                VALUES (9, 1, 24.90, DATE '2024-07-19'), (7, 1, 15.50, DATE '2024-07-09')
                """).update();

        var deposits = new JdbcTimeDepositRepository(jdbc).findAllWithWithdrawals();

        assertThat(deposits).extracting(row -> row.deposit().getId(), row -> row.deposit().getPlanType(),
                row -> row.deposit().getDays(), row -> row.deposit().getBalance(), row -> row.withdrawals())
                .containsExactly(
                        tuple(1, "basic", 30, 1000.50, List.of(
                                new Withdrawal(7, new BigDecimal("15.50"), LocalDate.of(2024, 7, 9)),
                                new Withdrawal(9, new BigDecimal("24.90"), LocalDate.of(2024, 7, 19)))),
                        tuple(2, "student", 365, 1200.25, List.of()));
    }

    @Test
    void savesBalancesToTwoDecimalPlacesWithoutChangingDaysOrPlan() {
        new JdbcTimeDepositRepository(jdbc).saveBalances(List.of(
                new TimeDeposit(1, "unused", 1001.125, 31),
                new TimeDeposit(2, "unused", -20.124, 366)));

        // HALF_UP rounds the tie to 1001.13 and the magnitude below the tie to 20.12.
        assertThat(jdbc.sql("""
                        SELECT id, "planType", days, balance FROM "timeDeposits" ORDER BY id
                        """)
                .query((rs, row) -> tuple(rs.getInt("id"), rs.getString("planType"),
                        rs.getInt("days"), rs.getBigDecimal("balance"))).list())
                .containsExactly(tuple(1, "basic", 30, new BigDecimal("1001.13")),
                        tuple(2, "student", 365, new BigDecimal("-20.12")));
    }
}
