package org.ikigaidigital.app;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.json.JsonMapper;

@Testcontainers
// SPRING_PROFILES_ACTIVE=local, left set in the shell after a local run, would add the demo data location;
// test properties take precedence over profile files, so the test always runs only the schema migrations.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.flyway.locations=classpath:db/migration")
class TimeDepositApplicationIT {
    @Container
    private static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18");

    private static final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private final JdbcClient jdbc;
    private final String baseUrl;

    @Autowired
    TimeDepositApplicationIT(JdbcClient jdbc, @LocalServerPort int port) {
        this.jdbc = jdbc;
        this.baseUrl = "http://localhost:" + port;
    }

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", postgres::getJdbcUrl);
        properties.add("spring.datasource.username", postgres::getUsername);
        properties.add("spring.datasource.password", postgres::getPassword);
    }

    @AfterAll
    static void closeClient() {
        http.close();
    }

    @BeforeEach
    void clearRows() {
        jdbc.sql("""
                TRUNCATE withdrawals, "timeDeposits"
                """).update();
    }

    @Test
    void postPersistsUpdatedBalancesThatGetReturnsWithWithdrawalHistory() throws Exception {
        jdbc.sql("""
                INSERT INTO "timeDeposits" (id, "planType", days, balance)
                VALUES (1, 'basic', 31, 1200.00),
                       (2, 'student', 366, 1200.00),
                       (3, 'premium', 46, 2500.00)
                """).update();
        jdbc.sql("""
                INSERT INTO withdrawals (id, "timeDepositId", amount, date)
                VALUES (14, 3, 125.50, DATE '2024-11-02')
                """).update();

        var update = updateBalances();
        assertThat(update.statusCode()).isEqualTo(204);
        assertThat(update.body()).isEmpty();

        var response = get("/time-deposits");
        assertThat(response.statusCode()).isEqualTo(200);
        // Basic earns 1200 * .01 / 12 = 1.00. Student is ineligible on day 366.
        // Premium earns 2500 * .05 / 12 = 10.416..., rounded to 10.42.
        JSONAssert.assertEquals("""
                [
                  {"id": 1, "planType": "basic", "balance": 1201.00, "days": 31, "withdrawals": []},
                  {"id": 2, "planType": "student", "balance": 1200.00, "days": 366, "withdrawals": []},
                  {
                    "id": 3, "planType": "premium", "balance": 2510.42, "days": 46,
                    "withdrawals": [{"id": 14, "amount": 125.50, "date": "2024-11-02"}]
                  }
                ]
                """, response.body(), JSONCompareMode.STRICT);
    }

    @Test
    void keepsTheLargestSupportedBalanceExactlyThroughTheDoubleDomain() throws Exception {
        // 9999999999999.99 is the largest NUMERIC(15,2) value (15 significant digits). Basic on day 30 earns
        // no interest, so the balance only makes the round trip through the double of TimeDeposit.
        jdbc.sql("""
                INSERT INTO "timeDeposits" (id, "planType", days, balance)
                VALUES (1, 'basic', 30, 9999999999999.99)
                """).update();

        assertThat(updateBalances().statusCode()).isEqualTo(204);
        assertThat(jdbc.sql("""
                        SELECT balance FROM "timeDeposits" WHERE id = 1
                        """).query(BigDecimal.class).single())
                .isEqualTo(new BigDecimal("9999999999999.99"));
    }

    @Test
    void rollsBackTheFirstWriteWhenTheSecondWriteFails() throws Exception {
        jdbc.sql("""
                INSERT INTO "timeDeposits" (id, "planType", days, balance)
                VALUES (1, 'basic', 31, 1200.00), (2, 'basic', 31, 1200.00)
                """).update();
        // Both balances would become 1201.00. Reject deposit 2 to fail after deposit 1 has been written.
        jdbc.sql("""
                ALTER TABLE "timeDeposits" ADD CONSTRAINT reject_second_update
                CHECK (id <> 2 OR balance <= 1200.00)
                """).update();
        try {
            assertThat(updateBalances().statusCode()).isEqualTo(500);
            assertThat(jdbc.sql("""
                            SELECT balance FROM "timeDeposits" ORDER BY id
                            """).query(BigDecimal.class).list())
                    .containsExactly(new BigDecimal("1200.00"), new BigDecimal("1200.00"));
        } finally {
            jdbc.sql("""
                    ALTER TABLE "timeDeposits" DROP CONSTRAINT reject_second_update
                    """).update();
        }
    }

    @Test
    void publishesExactlyTheTwoContractOperations() throws Exception {
        var response = get("/v3/api-docs");
        assertThat(response.statusCode()).isEqualTo(200);

        var paths = JsonMapper.builder().build().readTree(response.body()).path("paths");
        assertThat(paths.propertyNames()).containsExactlyInAnyOrder("/time-deposits", "/time-deposits/balance-updates");
        var query = paths.path("/time-deposits");
        var command = paths.path("/time-deposits/balance-updates");
        assertThat(query.propertyNames()).containsExactly("get");
        assertThat(command.propertyNames()).containsExactly("post");
        assertThat(query.path("get").path("responses").propertyNames()).containsExactly("200");
        assertThat(command.path("post").path("responses").propertyNames()).containsExactly("204");
    }

    @Test
    void runsOnlyTheSchemaMigrationOutsideTheLocalProfile() {
        assertThat(jdbc.sql("""
                        SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank
                        """).query(String.class).list())
                .containsExactly("1");
    }

    private HttpResponse<String> get(String path) throws Exception {
        return http.send(request(path).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> updateBalances() throws Exception {
        return http.send(request("/time-deposits/balance-updates")
                .POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpRequest.Builder request(String path) {
        return HttpRequest.newBuilder(URI.create(baseUrl + path)).timeout(Duration.ofSeconds(10));
    }
}
