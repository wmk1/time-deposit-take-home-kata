package org.ikigaidigital.app;

import java.util.List;
import org.ikigaidigital.BasicInterestPolicy;
import org.ikigaidigital.InterestPolicy;
import org.ikigaidigital.PremiumInterestPolicy;
import org.ikigaidigital.StudentInterestPolicy;
import org.ikigaidigital.TimeDepositCalculator;
import org.ikigaidigital.persistence.JdbcTimeDepositRepository;
import org.ikigaidigital.port.in.GetAllTimeDepositsUseCase;
import org.ikigaidigital.port.in.UpdateAllBalancesUseCase;
import org.ikigaidigital.port.out.TimeDepositRepository;
import org.ikigaidigital.rest.TimeDepositController;
import org.ikigaidigital.service.GetAllTimeDepositsService;
import org.ikigaidigital.service.UpdateAllBalancesService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration(proxyBeanMethods = false)
class TimeDepositConfiguration {
    @Bean
    BasicInterestPolicy basicInterestPolicy() {
        return new BasicInterestPolicy();
    }

    @Bean
    StudentInterestPolicy studentInterestPolicy() {
        return new StudentInterestPolicy();
    }

    @Bean
    PremiumInterestPolicy premiumInterestPolicy() {
        return new PremiumInterestPolicy();
    }

    @Bean
    TimeDepositCalculator timeDepositCalculator(List<InterestPolicy> policies) {
        return new TimeDepositCalculator(policies);
    }

    @Bean
    TimeDepositRepository timeDepositRepository(JdbcClient jdbc) {
        return new JdbcTimeDepositRepository(jdbc);
    }

    @Bean
    GetAllTimeDepositsUseCase getAllTimeDeposits(TimeDepositRepository repository) {
        return new GetAllTimeDepositsService(repository);
    }

    @Bean
    UpdateAllBalancesUseCase updateAllBalances(TimeDepositRepository repository, TimeDepositCalculator calculator,
            PlatformTransactionManager transactionManager) {
        var service = new UpdateAllBalancesService(repository, calculator);
        var transaction = new TransactionTemplate(transactionManager);
        // The locked read and every balance write must use the same transaction.
        return () -> transaction.executeWithoutResult(status -> service.updateAllBalances());
    }

    @Bean
    TimeDepositController timeDepositController(GetAllTimeDepositsUseCase getAllTimeDeposits,
            UpdateAllBalancesUseCase updateAllBalances) {
        return new TimeDepositController(getAllTimeDeposits, updateAllBalances);
    }
}
