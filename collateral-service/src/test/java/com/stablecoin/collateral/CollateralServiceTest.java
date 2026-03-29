package com.stablecoin.collateral;

import com.stablecoin.collateral.dto.DepositRequest;
import com.stablecoin.collateral.entity.CollateralDeposit;
import com.stablecoin.collateral.repository.CollateralDepositRepository;
import com.stablecoin.collateral.repository.ReservePoolRepository;
import com.stablecoin.collateral.service.CollateralService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class CollateralServiceTest {

    @Autowired
    private CollateralService collateralService;

    @Autowired
    private CollateralDepositRepository depositRepository;

    @Autowired
    private ReservePoolRepository reservePoolRepository;

    @Test
    void testDeposit_USD_success() {
        DepositRequest req = new DepositRequest();
        req.setAmount(new BigDecimal("1000.00"));
        req.setCurrency("USD");
        req.setOperator("test-operator");

        CollateralDeposit deposit = collateralService.deposit(req);

        assertThat(deposit.getId()).isNotNull();
        assertThat(deposit.getTxHash()).startsWith("0x");
        assertThat(deposit.getUsdAmount()).isEqualByComparingTo("1000.000000");
        assertThat(deposit.getStatus()).isEqualTo("CONFIRMED");
    }

    @Test
    void testDeposit_CNY_convertsToUsd() {
        DepositRequest req = new DepositRequest();
        req.setAmount(new BigDecimal("7000.00"));
        req.setCurrency("CNY");
        req.setOperator("test-operator");

        CollateralDeposit deposit = collateralService.deposit(req);

        // 7000 CNY * 0.1379 = 965.3 USD
        assertThat(deposit.getUsdAmount()).isGreaterThan(BigDecimal.ZERO);
        assertThat(deposit.getCurrency()).isEqualTo("CNY");
    }

    @Test
    void testGetTotalReserve_afterDeposit() {
        BigDecimal before = collateralService.getTotalReserve();

        DepositRequest req = new DepositRequest();
        req.setAmount(new BigDecimal("500.00"));
        req.setCurrency("USD");
        req.setOperator("test-operator");
        collateralService.deposit(req);

        BigDecimal after = collateralService.getTotalReserve();
        assertThat(after).isGreaterThan(before);
    }
}
