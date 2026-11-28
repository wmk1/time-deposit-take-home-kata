package org.ikigaidigital.persistence;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.tuple;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import org.flywaydb.core.Flyway;
import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.TimeDepositCalculator;
import org.ikigaidigital.Withdrawal;
import org.ikigaidigital.service.UpdateAllBalancesService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
class JdbcTimeDepositRepositoryIT {
    @Container
    private static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18");

    private static DriverManagerDataSource dataSource;
    private static JdbcClient jdbc;

    @BeforeAll
    static void migrate() {
        dataSource = new DriverManagerDataSource(
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

    @Test
    void serializesBalanceUpdatesBehindTheLockedRead() throws Exception {
        var calculator = new TimeDepositCalculator();
        var service = new UpdateAllBalancesService(new JdbcTimeDepositRepository(jdbc), calculator);
        var transaction = new TransactionTemplate(new JdbcTransactionManager(dataSource));
        transaction.setTimeout(15);
        var updaterPid = new CompletableFuture<Integer>();

        try (var executor = Executors.newSingleThreadExecutor(); var connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            var lockingJdbc = JdbcClient.create(new SingleConnectionDataSource(connection, true));
            var lockingRepository = new JdbcTimeDepositRepository(lockingJdbc);
            var lockedDeposits = lockingRepository.findAllForBalanceUpdate();
            int lockingPid = lockingJdbc.sql("SELECT pg_backend_pid()").query(Integer.class).single();

            var secondUpdate = executor.submit(() -> transaction.executeWithoutResult(status -> {
                updaterPid.complete(jdbc.sql("SELECT pg_backend_pid()").query(Integer.class).single());
                service.updateAllBalances();
            }));

            awaitUpdateBlockedBy(lockingJdbc, updaterPid.get(5, SECONDS), lockingPid, secondUpdate);
            assertThatExceptionOfType(TimeoutException.class)
                    .isThrownBy(() -> secondUpdate.get(200, MILLISECONDS));

            calculator.updateBalance(lockedDeposits);
            lockingRepository.saveBalances(lockedDeposits);
            connection.commit();
            secondUpdate.get(5, SECONDS);
        }

        // The student earns 1200.25 * 0.03 / 12 = 3.000625, rounded to 3.00.
        // The next month earns 1203.25 * 0.03 / 12 = 3.008125, rounded to 3.01.
        assertThat(jdbc.sql("""
                        SELECT balance FROM "timeDeposits" WHERE id = 2
                        """).query(BigDecimal.class).single())
                .isEqualTo(new BigDecimal("1206.26"));
    }

    private static void awaitUpdateBlockedBy(JdbcClient observer, int waiterPid, int blockerPid, Future<?> update)
            throws InterruptedException, ExecutionException {
        // A running worker may still be opening its connection or preparing SQL. Check the database lock wait.
        var blockedBy = observer.sql("SELECT :blocker = ANY (pg_blocking_pids(:waiter))")
                .param("blocker", blockerPid)
                .param("waiter", waiterPid);
        long deadline = System.nanoTime() + SECONDS.toNanos(5);
        do {
            if (blockedBy.query(Boolean.class).single()) {
                return;
            }
            try {
                update.get(25, MILLISECONDS);
            } catch (TimeoutException stillRunning) {
                continue;
            }
            throw new AssertionError("The second balance update finished while the first transaction was open");
        } while (System.nanoTime() < deadline);
        throw new AssertionError("PostgreSQL did not report the second balance update waiting on the first");
    }
}
