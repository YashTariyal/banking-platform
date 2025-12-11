package com.banking.ledger.web;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.banking.ledger.domain.EntryType;
import com.banking.ledger.domain.JournalStatus;
import com.banking.ledger.domain.LedgerEntry;
import com.banking.ledger.domain.LedgerJournal;
import com.banking.ledger.repository.LedgerEntryRepository;
import com.banking.ledger.service.LedgerService;
import com.banking.ledger.web.dto.LedgerEntryResponse;
import com.banking.ledger.web.dto.LedgerJournalResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = LedgerJournalController.class)
class LedgerJournalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LedgerService ledgerService;

    @MockBean
    private LedgerEntryRepository ledgerEntryRepository;

    @MockBean
    private LedgerMapper mapper;

    private LedgerJournal journal;
    private LedgerEntry debitEntry;

    @BeforeEach
    void setup() {
        journal = new LedgerJournal();
        journal.setId(UUID.randomUUID());
        journal.setReferenceId("REF-123");
        journal.setStatus(JournalStatus.POSTED);
        journal.setPostedAt(Instant.now());

        debitEntry = new LedgerEntry();
        debitEntry.setId(UUID.randomUUID());
        debitEntry.setJournalId(journal.getId());
        debitEntry.setLedgerAccountId(UUID.randomUUID());
        debitEntry.setEntryType(EntryType.DEBIT);
        debitEntry.setAmount(new BigDecimal("50.00"));
        debitEntry.setCurrency("USD");
        debitEntry.setPostedAt(Instant.now());
    }

    @Test
    void postJournal_createsJournal() throws Exception {
        LedgerJournalResponse response = new LedgerJournalResponse();
        response.setId(journal.getId());
        response.setReferenceId(journal.getReferenceId());
        response.setStatus(journal.getStatus());

        when(ledgerService.postJournal(any(LedgerJournal.class), anyList())).thenReturn(journal);
        when(ledgerEntryRepository.findByJournalId(journal.getId())).thenReturn(List.of(debitEntry));
        when(mapper.toResponse(eq(journal), anyList())).thenReturn(response);

        mockMvc.perform(post("/api/ledger/journals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "referenceId": "REF-123",
                                  "description": "fund transfer",
                                  "entries": [
                                    {
                                      "ledgerAccountId": "00000000-0000-0000-0000-000000000001",
                                      "entryType": "DEBIT",
                                      "amount": 50.00,
                                      "currency": "USD"
                                    },
                                    {
                                      "ledgerAccountId": "00000000-0000-0000-0000-000000000002",
                                      "entryType": "CREDIT",
                                      "amount": 50.00,
                                      "currency": "USD"
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.referenceId", is("REF-123")));
    }

    @Test
    void getEntries_returnsPagedEntries() throws Exception {
        LedgerEntryResponse response = new LedgerEntryResponse();
        response.setId(debitEntry.getId());
        response.setJournalId(journal.getId());
        response.setLedgerAccountId(debitEntry.getLedgerAccountId());
        response.setEntryType(debitEntry.getEntryType());
        response.setAmount(debitEntry.getAmount());
        response.setCurrency("USD");

        when(ledgerService.getEntriesForAccount(debitEntry.getLedgerAccountId(), PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(debitEntry)));
        when(mapper.toPageResponse(any(), any())).thenCallRealMethod();
        when(mapper.toResponse(debitEntry)).thenReturn(response);

        mockMvc.perform(get("/api/ledger/accounts/{accountId}/entries?page=0&size=20", debitEntry.getLedgerAccountId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].currency", is("USD")));
    }
}

