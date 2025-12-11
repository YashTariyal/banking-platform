package com.banking.support.service;

import com.banking.support.repository.SupportCaseRepository;
import java.util.Random;
import org.springframework.stereotype.Service;

@Service
public class CaseNumberGenerator {

    private static final String PREFIX = "CASE";
    private static final int NUMBER_LENGTH = 10;
    private final SupportCaseRepository caseRepository;
    private final Random random = new Random();

    public CaseNumberGenerator(SupportCaseRepository caseRepository) {
        this.caseRepository = caseRepository;
    }

    public String generateCaseNumber() {
        String caseNumber;
        int attempts = 0;
        do {
            caseNumber = PREFIX + generateRandomDigits(NUMBER_LENGTH);
            attempts++;
            if (attempts > 100) {
                throw new IllegalStateException("Failed to generate unique case number after 100 attempts");
            }
        } while (caseRepository.findByCaseNumber(caseNumber).isPresent());

        return caseNumber;
    }

    private String generateRandomDigits(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
}

