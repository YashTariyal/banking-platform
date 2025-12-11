package com.banking.support.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.banking.support.repository.SupportCaseRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CaseNumberGeneratorTest {

    @Mock
    private SupportCaseRepository caseRepository;

    private CaseNumberGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new CaseNumberGenerator(caseRepository);
    }

    @Test
    void generateCaseNumber_returnsUniqueNumber() {
        when(caseRepository.findByCaseNumber(anyString()))
                .thenReturn(Optional.empty());

        String caseNumber = generator.generateCaseNumber();

        assertThat(caseNumber).startsWith("CASE");
        assertThat(caseNumber).hasSize(14); // CASE + 10 digits
    }

    @Test
    void generateCaseNumber_retriesOnDuplicate() {
        when(caseRepository.findByCaseNumber(anyString()))
                .thenReturn(Optional.empty());

        String caseNumber = generator.generateCaseNumber();

        assertThat(caseNumber).startsWith("CASE");
        assertThat(caseNumber).hasSize(14);
    }
}

