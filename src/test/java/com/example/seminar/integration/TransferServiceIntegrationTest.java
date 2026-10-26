package com.example.seminar.integration;

import com.example.seminar.domain.Account;
import com.example.seminar.exception.AccountNotFoundException;
import com.example.seminar.repository.AccountRepository;
import com.example.seminar.service.TransferService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class TransferServiceIntegrationTest {

    @Autowired
    TransferService transferService;

    @Autowired
    AccountRepository accountRepository;

    @AfterEach
    void cleanup() {
        accountRepository.deleteAll();
    }

    // 1. 정상 이체 플로우
    @Test
    @DisplayName("실제 DB에서 정상적인 계좌 이체가 이루어진다")
    void transferSuccess() {

        // Given
        Account sender = accountRepository.save(
                new Account("John", new BigDecimal("1000"))
        );

        Account receiver = accountRepository.save(
                new Account("Jane", new BigDecimal("1000"))
        );

        // When
        transferService.transferMoney(
                sender.getId(),
                receiver.getId(),
                new BigDecimal("100")
        );

        // Then
        Account updatedSender = accountRepository
                .findById(sender.getId())
                .orElseThrow();

        Account updatedReceiver = accountRepository
                .findById(receiver.getId())
                .orElseThrow();

        assertThat(updatedSender.getAmount())
                .isEqualByComparingTo("900");

        assertThat(updatedReceiver.getAmount())
                .isEqualByComparingTo("1100");
    }

    // 2. 예외 플로우 - 발신인 계좌 없음
    @Test
    @DisplayName("발신인 계좌가 없으면 예외가 발생한다")
    void senderNotFound() {

        // Given
        // ID 999인 계좌가 DB에 존재하지 않는지 확인
        assertThat(accountRepository.existsById(999L))
                .isFalse();

        // When & Then
        assertThatThrownBy(() ->
                transferService.transferMoney(
                        999L, 2L, new BigDecimal("100")
                )
        )
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessageContaining("Sender");
    }

    // 3. 예외 플로우 - 수취인 계좌 없음
    @Test
    @DisplayName("수취인 계좌가 없으면 잔액이 변경되지 않는다")
    void receiverNotFound() {

        // Given
        Account sender = accountRepository.save(
                new Account("John", new BigDecimal("1000"))
        );

        // When & Then
        assertThatThrownBy(() ->
                transferService.transferMoney(
                        sender.getId(),
                        999L,
                        new BigDecimal("100")
                )
        )
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessageContaining("Receiver");

        // DB에서 다시 조회
        Account updatedSender = accountRepository
                .findById(sender.getId())
                .orElseThrow();

        assertThat(updatedSender.getAmount())
                .isEqualByComparingTo("1000");
    }
}