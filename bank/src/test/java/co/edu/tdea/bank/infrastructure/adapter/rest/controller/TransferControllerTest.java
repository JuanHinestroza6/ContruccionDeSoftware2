package co.edu.tdea.bank.infrastructure.adapter.rest.controller;

import co.edu.tdea.bank.domain.Exceptions.InsufficientFundsException;
import co.edu.tdea.bank.domain.enums.AccountType;
import co.edu.tdea.bank.domain.enums.CurrencyType;
import co.edu.tdea.bank.domain.enums.TransferStatus;
import co.edu.tdea.bank.domain.models.BankAccount;
import co.edu.tdea.bank.domain.models.IndividualClient;
import co.edu.tdea.bank.domain.models.Transfer;
import co.edu.tdea.bank.domain.models.User;
import co.edu.tdea.bank.domain.ports.in.ApproveTransferUseCase;
import co.edu.tdea.bank.domain.ports.in.CreateTransferUseCase;
import co.edu.tdea.bank.domain.ports.in.FindTransferUseCase;
import co.edu.tdea.bank.domain.ports.in.RejectTransferUseCase;
import co.edu.tdea.bank.infrastructure.adapter.rest.dto.transfer.ApproveTransferRequest;
import co.edu.tdea.bank.infrastructure.adapter.rest.dto.transfer.CreateTransferRequest;
import co.edu.tdea.bank.infrastructure.adapter.rest.dto.transfer.RejectTransferRequest;
import co.edu.tdea.bank.infrastructure.config.SecurityConfig;
import co.edu.tdea.bank.infrastructure.security.JwtAuthenticationFilter;
import co.edu.tdea.bank.testfixtures.DomainFixtures;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
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
 * MockMvc slice tests for {@link TransferController}.
 *
 * <p>Verifies happy paths, exception mapping
 * ({@link InsufficientFundsException} → 409) and pure delegation to the input
 * ports. Domain construction relies on {@link DomainFixtures}.
 *
 * <p>Security wiring (S4): excludes the production security graph and imports
 * {@link WebMvcTestSecurityConfig} instead. Each test declares the role that
 * satisfies the controller's {@code @PreAuthorize} matrix.
 */
@WebMvcTest(
        controllers = TransferController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, JwtAuthenticationFilter.class}
        )
)
@Import(WebMvcTestSecurityConfig.class)
class TransferControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private CreateTransferUseCase createTransferUseCase;
    @MockBean private ApproveTransferUseCase approveTransferUseCase;
    @MockBean private RejectTransferUseCase rejectTransferUseCase;
    @MockBean private FindTransferUseCase findTransferUseCase;

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

    private BankAccount account(String number, IndividualClient holder, BigDecimal balance) {
        return BankAccount.open(number, AccountType.SAVINGS, holder, balance, CurrencyType.COP);
    }

    private Transfer pendingTransfer(IndividualClient holder, User creator, BigDecimal amount) {
        BankAccount src = account("ACC-SRC", holder, new BigDecimal("10000000"));
        BankAccount dst = account("ACC-DST", holder, new BigDecimal("0"));
        return Transfer.reconstruct(
                100L,
                src, dst,
                amount,
                LocalDateTime.of(2026, 1, 1, 10, 0),
                creator,
                null,
                TransferStatus.PENDING_APPROVAL,
                null
        );
    }

    private Transfer executedTransfer(IndividualClient holder, User creator, User approver, BigDecimal amount) {
        BankAccount src = account("ACC-SRC", holder, new BigDecimal("10000000"));
        BankAccount dst = account("ACC-DST", holder, new BigDecimal("0"));
        return Transfer.reconstruct(
                100L,
                src, dst,
                amount,
                LocalDateTime.of(2026, 1, 1, 10, 0),
                creator,
                LocalDateTime.of(2026, 1, 1, 10, 5),
                TransferStatus.EXECUTED,
                approver
        );
    }

    private Transfer rejectedTransfer(IndividualClient holder, User creator, User approver, BigDecimal amount) {
        BankAccount src = account("ACC-SRC", holder, new BigDecimal("10000000"));
        BankAccount dst = account("ACC-DST", holder, new BigDecimal("0"));
        return Transfer.reconstruct(
                100L,
                src, dst,
                amount,
                LocalDateTime.of(2026, 1, 1, 10, 0),
                creator,
                null,
                TransferStatus.REJECTED,
                approver
        );
    }

    @Test
    @WithMockUser(roles = "COMMERCIAL_EMPLOYEE")
    void should_return_201_when_creating_valid_transfer() throws Exception {
        // Arrange
        IndividualClient holder = anyClient();
        User creator = DomainFixtures.validUser();
        BigDecimal amount = new BigDecimal("100000");
        CreateTransferRequest payload = new CreateTransferRequest(
                "ACC-SRC",
                "ACC-DST",
                amount,
                creator.getUserId(),
                LocalDateTime.of(2026, 1, 1, 10, 0),
                new BigDecimal("500000")
        );
        Transfer created = pendingTransfer(holder, creator, amount);
        when(createTransferUseCase.createTransfer(
                anyString(), anyString(), any(BigDecimal.class), any(UUID.class),
                any(LocalDateTime.class), any(BigDecimal.class)))
                .thenReturn(created);

        // Act + Assert
        mockMvc.perform(post("/api/v1/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transferId").value(100))
                .andExpect(jsonPath("$.transferStatus").value("PENDING_APPROVAL"))
                .andExpect(jsonPath("$.sourceAccountNumber").value("ACC-SRC"))
                .andExpect(jsonPath("$.destinationAccountNumber").value("ACC-DST"))
                .andExpect(jsonPath("$.amount").value(100000));

        verify(createTransferUseCase).createTransfer(
                eq(payload.sourceAccountNumber()),
                eq(payload.destinationAccountNumber()),
                eq(payload.amount()),
                eq(payload.createdByUserId()),
                eq(payload.creationDateTime()),
                eq(payload.approvalThreshold())
        );
    }

    @Test
    @WithMockUser(roles = "COMMERCIAL_EMPLOYEE")
    void should_return_409_when_insufficient_funds() throws Exception {
        // Arrange
        User creator = DomainFixtures.validUser();
        CreateTransferRequest payload = new CreateTransferRequest(
                "ACC-SRC",
                "ACC-DST",
                new BigDecimal("99999999"),
                creator.getUserId(),
                LocalDateTime.of(2026, 1, 1, 10, 0),
                new BigDecimal("500000")
        );
        when(createTransferUseCase.createTransfer(
                anyString(), anyString(), any(BigDecimal.class), any(UUID.class),
                any(LocalDateTime.class), any(BigDecimal.class)))
                .thenThrow(new InsufficientFundsException(
                        "ACC-SRC", new BigDecimal("1000"), new BigDecimal("99999999")));

        // Act + Assert
        mockMvc.perform(post("/api/v1/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("INSUFFICIENT_FUNDS"));
    }

    @Test
    @WithMockUser(roles = "COMPANY_SUPERVISOR")
    void should_return_200_when_approving_transfer() throws Exception {
        // Arrange
        IndividualClient holder = anyClient();
        User creator = DomainFixtures.validUser();
        User approver = DomainFixtures.validUser();
        BigDecimal amount = new BigDecimal("100000");
        ApproveTransferRequest payload = new ApproveTransferRequest(
                approver.getUserId(),
                LocalDateTime.of(2026, 1, 1, 10, 5)
        );
        Transfer executed = executedTransfer(holder, creator, approver, amount);
        when(approveTransferUseCase.approveTransfer(eq(100L), any(UUID.class), any(LocalDateTime.class)))
                .thenReturn(executed);

        // Act + Assert
        mockMvc.perform(patch("/api/v1/transfers/{transferId}/approve", 100L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transferId").value(100))
                .andExpect(jsonPath("$.transferStatus").value("EXECUTED"))
                .andExpect(jsonPath("$.approvedByUserId").value(approver.getUserId().toString()));

        verify(approveTransferUseCase).approveTransfer(
                eq(100L),
                eq(payload.approverId()),
                eq(payload.approvalDateTime())
        );
    }

    @Test
    @WithMockUser(roles = "COMPANY_SUPERVISOR")
    void should_return_200_when_rejecting_transfer() throws Exception {
        // Arrange
        IndividualClient holder = anyClient();
        User creator = DomainFixtures.validUser();
        User approver = DomainFixtures.validUser();
        BigDecimal amount = new BigDecimal("100000");
        RejectTransferRequest payload = new RejectTransferRequest(approver.getUserId());
        Transfer rejected = rejectedTransfer(holder, creator, approver, amount);
        when(rejectTransferUseCase.rejectTransfer(eq(100L), any(UUID.class))).thenReturn(rejected);

        // Act + Assert
        mockMvc.perform(patch("/api/v1/transfers/{transferId}/reject", 100L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transferId").value(100))
                .andExpect(jsonPath("$.transferStatus").value("REJECTED"))
                .andExpect(jsonPath("$.approvedByUserId").value(approver.getUserId().toString()));

        verify(rejectTransferUseCase).rejectTransfer(eq(100L), eq(payload.approverId()));
    }

    @Test
    @WithMockUser(roles = "INTERNAL_ANALYST")
    void should_return_200_with_transfer_list_by_account() throws Exception {
        // Arrange — INTERNAL_ANALYST short-circuits the ownership SpEL guard.
        String accountNumber = "ACC-SRC";
        IndividualClient holder = anyClient();
        User creator = DomainFixtures.validUser();
        Transfer t1 = pendingTransfer(holder, creator, new BigDecimal("100000"));
        when(findTransferUseCase.findBySourceAccountNumber(accountNumber)).thenReturn(List.of(t1));

        // Act + Assert
        mockMvc.perform(get("/api/v1/transfers/by-account/{accountNumber}", accountNumber))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].transferId").value(100))
                .andExpect(jsonPath("$[0].sourceAccountNumber").value("ACC-SRC"));

        verify(findTransferUseCase).findBySourceAccountNumber(accountNumber);
    }
}
