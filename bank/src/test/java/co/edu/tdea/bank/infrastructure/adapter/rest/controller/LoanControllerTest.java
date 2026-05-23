package co.edu.tdea.bank.infrastructure.adapter.rest.controller;

import co.edu.tdea.bank.domain.Exceptions.InvalidStateTransitionException;
import co.edu.tdea.bank.domain.enums.AccountType;
import co.edu.tdea.bank.domain.enums.CurrencyType;
import co.edu.tdea.bank.domain.enums.LoanStatus;
import co.edu.tdea.bank.domain.enums.LoanType;
import co.edu.tdea.bank.domain.models.BankAccount;
import co.edu.tdea.bank.domain.models.IndividualClient;
import co.edu.tdea.bank.domain.models.Loan;
import co.edu.tdea.bank.domain.ports.in.ApproveLoanUseCase;
import co.edu.tdea.bank.domain.ports.in.DisburseLoanUseCase;
import co.edu.tdea.bank.domain.ports.in.FindLoanUseCase;
import co.edu.tdea.bank.domain.ports.in.RejectLoanUseCase;
import co.edu.tdea.bank.domain.ports.in.RequestLoanUseCase;
import co.edu.tdea.bank.infrastructure.adapter.rest.dto.loan.ApproveLoanRequest;
import co.edu.tdea.bank.infrastructure.adapter.rest.dto.loan.DisburseLoanRequest;
import co.edu.tdea.bank.infrastructure.adapter.rest.dto.loan.RequestLoanRequest;
import co.edu.tdea.bank.infrastructure.config.SecurityConfig;
import co.edu.tdea.bank.infrastructure.security.JwtAuthenticationFilter;
import co.edu.tdea.bank.testfixtures.security.WebMvcTestSecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MockMvc slice tests for {@link LoanController}.
 *
 * <p>Verifies request/response wiring, validation, state-transition exception
 * mapping ({@link InvalidStateTransitionException} → 409) and delegation to
 * each input port.
 *
 * <p>Security wiring (S4): excludes the production security graph and imports
 * {@link WebMvcTestSecurityConfig} instead. Each test declares the role that
 * satisfies the controller's {@code @PreAuthorize} matrix.
 */
@WebMvcTest(
        controllers = LoanController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, JwtAuthenticationFilter.class}
        )
)
@Import(WebMvcTestSecurityConfig.class)
class LoanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private RequestLoanUseCase requestLoanUseCase;
    @MockBean private ApproveLoanUseCase approveLoanUseCase;
    @MockBean private RejectLoanUseCase rejectLoanUseCase;
    @MockBean private DisburseLoanUseCase disburseLoanUseCase;
    @MockBean private FindLoanUseCase findLoanUseCase;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    private IndividualClient anyClient() {
        return IndividualClient.create(
                "1001234567",
                "jane.doe@example.com",
                "+57 300 1234567",
                "Cra 50 # 10-20, Medellin",
                "Jane Doe",
                LocalDate.now().minusYears(30)
        );
    }

    private Loan underReviewLoan(IndividualClient client) {
        return Loan.reconstruct(
                42L,
                LoanType.PERSONAL,
                client,
                new BigDecimal("5000000"),
                24,
                null, null, LoanStatus.UNDER_REVIEW, null, null, null
        );
    }

    private Loan approvedLoan(IndividualClient client) {
        return Loan.reconstruct(
                42L,
                LoanType.PERSONAL,
                client,
                new BigDecimal("5000000"),
                24,
                new BigDecimal("4500000"),
                new BigDecimal("0.18"),
                LoanStatus.APPROVED,
                LocalDate.now().minusDays(1),
                null, null
        );
    }

    private Loan disbursedLoan(IndividualClient client) {
        BankAccount target = BankAccount.open(
                "ACC-DEST-01",
                AccountType.SAVINGS,
                client,
                new BigDecimal("0"),
                CurrencyType.COP
        );
        return Loan.reconstruct(
                42L,
                LoanType.PERSONAL,
                client,
                new BigDecimal("5000000"),
                24,
                new BigDecimal("4500000"),
                new BigDecimal("0.18"),
                LoanStatus.DISBURSED,
                LocalDate.now().minusDays(2),
                LocalDate.now(),
                target
        );
    }

    @Test
    @WithMockUser(roles = "COMMERCIAL_EMPLOYEE")
    void should_return_201_when_requesting_valid_loan() throws Exception {
        // Arrange
        IndividualClient client = anyClient();
        RequestLoanRequest payload = new RequestLoanRequest(
                client.getClientId(),
                LoanType.PERSONAL,
                new BigDecimal("5000000"),
                24
        );
        Loan created = underReviewLoan(client);
        when(requestLoanUseCase.requestLoan(any(UUID.class), any(LoanType.class), any(BigDecimal.class), any(Integer.class)))
                .thenReturn(created);

        // Act + Assert
        mockMvc.perform(post("/api/v1/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.loanId").value(42))
                .andExpect(jsonPath("$.loanType").value("PERSONAL"))
                .andExpect(jsonPath("$.loanStatus").value("UNDER_REVIEW"))
                .andExpect(jsonPath("$.termInMonths").value(24));

        verify(requestLoanUseCase).requestLoan(
                eq(payload.clientId()),
                eq(payload.loanType()),
                eq(payload.requestedAmount()),
                eq(payload.termInMonths())
        );
    }

    @Test
    @WithMockUser(roles = "COMMERCIAL_EMPLOYEE")
    void should_return_400_when_amount_is_zero_or_missing() throws Exception {
        // Arrange — null requestedAmount triggers @NotNull (declared first)
        UUID clientId = UUID.randomUUID();
        String invalidPayload = """
                {
                  "clientId": "%s",
                  "loanType": "PERSONAL",
                  "requestedAmount": null,
                  "termInMonths": 24
                }
                """.formatted(clientId);

        // Act + Assert
        mockMvc.perform(post("/api/v1/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.requestedAmount").exists());
    }

    @Test
    @WithMockUser(roles = "INTERNAL_ANALYST")
    void should_return_200_when_approving_loan() throws Exception {
        // Arrange
        IndividualClient client = anyClient();
        ApproveLoanRequest payload = new ApproveLoanRequest(
                UUID.randomUUID(),
                new BigDecimal("4500000"),
                new BigDecimal("0.18"),
                LocalDate.now().minusDays(1)
        );
        Loan approved = approvedLoan(client);
        when(approveLoanUseCase.approveLoan(eq(42L), any(UUID.class), any(BigDecimal.class), any(BigDecimal.class), any(LocalDate.class)))
                .thenReturn(approved);

        // Act + Assert
        mockMvc.perform(patch("/api/v1/loans/{loanId}/approve", 42L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loanId").value(42))
                .andExpect(jsonPath("$.loanStatus").value("APPROVED"))
                .andExpect(jsonPath("$.approvedAmount").value(4500000));

        verify(approveLoanUseCase).approveLoan(
                eq(42L),
                eq(payload.approverId()),
                eq(payload.approvedAmount()),
                eq(payload.interestRate()),
                eq(payload.approvalDate())
        );
    }

    @Test
    @WithMockUser(roles = "INTERNAL_ANALYST")
    void should_return_409_when_approving_loan_in_invalid_state() throws Exception {
        // Arrange — use case throws InvalidStateTransitionException (extends BusinessException)
        ApproveLoanRequest payload = new ApproveLoanRequest(
                UUID.randomUUID(),
                new BigDecimal("4500000"),
                new BigDecimal("0.18"),
                LocalDate.now().minusDays(1)
        );
        when(approveLoanUseCase.approveLoan(anyLong(), any(UUID.class), any(BigDecimal.class), any(BigDecimal.class), any(LocalDate.class)))
                .thenThrow(new InvalidStateTransitionException("Loan", LoanStatus.REJECTED, "approve"));

        // Act + Assert
        mockMvc.perform(patch("/api/v1/loans/{loanId}/approve", 42L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("INVALID_STATE_TRANSITION"));
    }

    @Test
    @WithMockUser(roles = "INTERNAL_ANALYST")
    void should_return_200_when_disbursing_loan() throws Exception {
        // Arrange
        IndividualClient client = anyClient();
        DisburseLoanRequest payload = new DisburseLoanRequest(
                "ACC-DEST-01",
                LocalDate.now(),
                UUID.randomUUID()
        );
        Loan disbursed = disbursedLoan(client);
        when(disburseLoanUseCase.disburseLoan(eq(42L), anyString(), any(LocalDate.class), any(UUID.class)))
                .thenReturn(disbursed);

        // Act + Assert
        mockMvc.perform(patch("/api/v1/loans/{loanId}/disburse", 42L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loanId").value(42))
                .andExpect(jsonPath("$.loanStatus").value("DISBURSED"))
                .andExpect(jsonPath("$.disbursementAccountNumber").value("ACC-DEST-01"));

        verify(disburseLoanUseCase).disburseLoan(
                eq(42L),
                eq(payload.destinationAccountNumber()),
                eq(payload.disbursementDate()),
                eq(payload.analystUserId())
        );
    }

    @Test
    @WithMockUser(roles = "INTERNAL_ANALYST")
    void should_return_200_with_loan_list_when_finding_by_client() throws Exception {
        // Arrange — INTERNAL_ANALYST short-circuits the ownership SpEL guard.
        IndividualClient client = anyClient();
        UUID clientId = client.getClientId();
        Loan loan = underReviewLoan(client);
        when(findLoanUseCase.findByClientId(clientId)).thenReturn(List.of(loan));

        // Act + Assert
        mockMvc.perform(get("/api/v1/loans/by-client/{clientId}", clientId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].loanId").value(42))
                .andExpect(jsonPath("$[0].loanStatus").value("UNDER_REVIEW"));

        verify(findLoanUseCase).findByClientId(clientId);
    }
}
