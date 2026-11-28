package org.ikigaidigital.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.TimeDepositWithWithdrawals;
import org.ikigaidigital.Withdrawal;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class TimeDepositControllerTest {
    private final AtomicInteger updates = new AtomicInteger();
    private final List<TimeDepositWithWithdrawals> deposits = List.of(
            new TimeDepositWithWithdrawals(new TimeDeposit(1, "student", 1025.75, 365),
                    List.of(new Withdrawal(17, new BigDecimal("25.50"), LocalDate.of(2024, 10, 8)))),
            new TimeDepositWithWithdrawals(new TimeDeposit(2, "premium", 700.25, 45), List.of()));
    private final MockMvc mvc = MockMvcBuilders
            .standaloneSetup(new TimeDepositController(() -> deposits, updates::incrementAndGet))
            .build();

    @Test
    void getReturnsTheCompleteDepositAndWithdrawalJson() throws Exception {
        mvc.perform(get("/time-deposits"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        [
                          {
                            "id": 1, "planType": "student", "balance": 1025.75, "days": 365,
                            "withdrawals": [{"id": 17, "amount": 25.50, "date": "2024-10-08"}]
                          },
                          {
                            "id": 2, "planType": "premium", "balance": 700.25, "days": 45,
                            "withdrawals": []
                          }
                        ]
                        """, JsonCompareMode.STRICT));
    }

    @Test
    void postInvokesTheUpdateOnceAndReturnsNoContent() throws Exception {
        var response = mvc.perform(post("/time-deposits/balance-updates"))
                .andExpect(status().isNoContent())
                .andReturn().getResponse();

        assertThat(response.getContentAsString()).isEmpty();
        assertThat(updates.get()).isEqualTo(1);
    }
}
