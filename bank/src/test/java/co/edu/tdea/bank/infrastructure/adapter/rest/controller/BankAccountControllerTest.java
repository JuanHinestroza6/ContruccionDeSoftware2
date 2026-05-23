package co.edu.tdea.bank.infrastructure.adapter.rest.controller;

import co.edu.tdea.bank.domain.Exceptions.ResourceNotFoundException;
import co.edu.tdea.bank.domain.enums.AccountType;
import co.edu.tdea.bank.domain.enums.CurrencyType;
import co.edu.tdea.bank.domain.models.BankAccount;
import co.edu.tdea.bank.domain.models.IndividualClient;
import co.edu.tdea.bank.domain.ports.in.FindBankAccountUseCase;
import co.edu.tdea.bank.domain.ports.in.OpenBankAccountUseCase;
import co.edu.tdea.bank.infrastructure.adapter.rest.dto.account.OpenBankAccountRequest;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MockMvc slice tests for {@link BankAccountController}.
 *
 * <p>Covers happy paths, validation failures and the
 * {@code ResourceNotFoundException} translation performed by
 * {@link co.edu.tdea.bank.infrastructure.adapter.rest.exception.GlobalExceptionHandler}.
 *
 * <p>Security wiring (S4): excludes the production security graph and imports
 * {@link WebMvcTestSecurityConfig} instead. Each test declares the role that
 * satisfies the controller's {@code @PreAuthorize} matrix.
 */
@WebMvcTest(
        controllers = BankAccountController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, JwtAuthenticationFilter.class}
        )
)
@Import(WebMvcTestSecurityConfig.class)
class BankAccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OpenBankAccountUseCase openBankAccountUseCase;

    @MockBean
    private FindBankAccountUseCase findBankAccountUseCase;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    private IndividualClient anyIndividualClient() {
        return IndividualClient.create(
                "1001234567",
                "jane.doe@example.com",
                "+57 300 1234567",
                "Cra 50 # 10-20, Medellin",
                "Jane Doe",
                LocalDate.now().minusYears(30)
        );
    }

    @Test
    @WithMockUser(roles = "TELLER_EMPLOYEE")
    void should_return_201_when_opening_valid_account() throws Exception {
        // Arrange
        UUID clientId = UUID.randomUUID();
        OpenBankAccountRequest payload = new OpenBankAccountRequest(
                clientId,
                "ACC-12345678",
                AccountType.SAVINGS,
                new BigDecimal("250000.00"),
                CurrencyType.COP
        );
        BankAccount created = BankAccount.open(
                payload.accountNumber(),
                payload.accountType(),
                anyIndividualClient(),
                payload.initialBalance(),
                payload.currency()
        );
        when(openBankAccountUseCase.openAccount(
                any(UUID.class), anyString(), any(AccountType.class), any(BigDecimal.class), any(CurrencyType.class)))
                .thenReturn(created);

        // Act + Assert
        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountNumber").value(payload.accountNumber()))
                .andExpect(jsonPath("$.accountType").value("SAVINGS"))
                .andExpect(jsonPath("$.currency").value("COP"))
                .andExpect(jsonPath("$.accountStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.currentBalance").value(250000.00));

        verify(openBankAccountUseCase).openAccount(
                eq(payload.clientId()),
                eq(payload.accountNumber()),
                eq(payload.accountType()),
                eq(payload.initialBalance()),
                eq(payload.currency())
        );
    }

    @Test
    @WithMockUser(roles = "TELLER_EMPLOYEE")
    void should_return_400_when_holderClientId_is_null() throws Exception {
        // Arrange — the DTO field is named {@code clientId}; the spec uses
        // "holderClientId" as the conceptual name. Sending a payload with
        // {@code clientId} = null must trigger @NotNull and yield 400.
        String invalidPayload = """
                {
                  "clientId": null,
                  "accountNumber": "ACC-12345678",
                  "accountType": "SAVINGS",
                  "initialBalance": 250000.00,
                  "currency": "COP"
                }
                """;

        // Act + Assert
        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.clientId").exists());
    }

    @Test
    @WithMockUser(roles = "INTERNAL_ANALYST")
    void should_return_200_when_account_found_by_number() throws Exception {
        // Arrange — INTERNAL_ANALYST short-circuits the `hasRole(...) or @authz.*`
        // ownership guard, so the controller proceeds to the use case.
        String accountNumber = "ACC-EXISTING";
        BankAccount existing = BankAccount.open(
                accountNumber,
                AccountType.CHECKING,
                anyIndividualClient(),
                new BigDecimal("100.00"),
                CurrencyType.USD
        );
        when(findBankAccountUseCase.findByAccountNumber(accountNumber)).thenReturn(existing);

        // Act + Assert
        mockMvc.perform(get("/api/v1/accounts/{accountNumber}", accountNumber))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value(accountNumber))
                .andExpect(jsonPath("$.accountType").value("CHECKING"))
                .andExpect(jsonPath("$.currency").value("USD"));

        verify(findBankAccountUseCase).findByAccountNumber(accountNumber);
    }

    @Test
    @WithMockUser(roles = "INTERNAL_ANALYST")
    void should_return_404_when_account_not_found() throws Exception {
        // Arrange
        String accountNumber = "ACC-MISSING";
        when(findBankAccountUseCase.findByAccountNumber(accountNumber))
                .thenThrow(new ResourceNotFoundException("Account not found: " + accountNumber));

        // Act + Assert
        mockMvc.perform(get("/api/v1/accounts/{accountNumber}", accountNumber))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));

        verify(findBankAccountUseCase).findByAccountNumber(accountNumber);
    }
}
