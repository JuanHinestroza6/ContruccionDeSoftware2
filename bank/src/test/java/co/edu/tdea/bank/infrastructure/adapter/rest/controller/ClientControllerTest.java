package co.edu.tdea.bank.infrastructure.adapter.rest.controller;

import co.edu.tdea.bank.domain.Exceptions.BusinessException;
import co.edu.tdea.bank.domain.Exceptions.ResourceNotFoundException;
import co.edu.tdea.bank.domain.models.BusinessClient;
import co.edu.tdea.bank.domain.models.IndividualClient;
import co.edu.tdea.bank.domain.ports.in.FindClientUseCase;
import co.edu.tdea.bank.domain.ports.in.RegisterBusinessClientUseCase;
import co.edu.tdea.bank.domain.ports.in.RegisterIndividualClientUseCase;
import co.edu.tdea.bank.infrastructure.adapter.rest.dto.client.RegisterBusinessClientRequest;
import co.edu.tdea.bank.infrastructure.adapter.rest.dto.client.RegisterIndividualClientRequest;
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
 * MockMvc slice tests for {@link ClientController}.
 *
 * <p>Verifies HTTP wiring only — DTO validation, status code mapping via
 * {@link co.edu.tdea.bank.infrastructure.adapter.rest.exception.GlobalExceptionHandler}
 * and pure delegation to the input ports. Business logic is owned by the
 * use cases and is mocked out.
 *
 * <p>Security wiring (S4): the production {@link SecurityConfig} and
 * {@link JwtAuthenticationFilter} are excluded from the slice; this test
 * imports {@link WebMvcTestSecurityConfig} instead so {@code @PreAuthorize}
 * runs against the {@code @WithMockUser} principal without dragging the JWT
 * pipeline into the slice. The full JWT round-trip is covered separately by
 * {@code AuthSecurityTest}.
 */
@WebMvcTest(
        controllers = ClientController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, JwtAuthenticationFilter.class}
        )
)
@Import(WebMvcTestSecurityConfig.class)
class ClientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RegisterIndividualClientUseCase registerIndividualClientUseCase;

    @MockBean
    private RegisterBusinessClientUseCase registerBusinessClientUseCase;

    @MockBean
    private FindClientUseCase findClientUseCase;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @WithMockUser(roles = "COMMERCIAL_EMPLOYEE")
    void should_return_201_when_creating_valid_individual_client() throws Exception {
        // Arrange
        RegisterIndividualClientRequest payload = new RegisterIndividualClientRequest(
                "1001234567",
                "jane.doe@example.com",
                "+57 300 1234567",
                "Cra 50 # 10-20, Medellin",
                "Jane Doe",
                LocalDate.now().minusYears(30)
        );
        IndividualClient created = IndividualClient.create(
                payload.identificationId(),
                payload.email(),
                payload.phone(),
                payload.address(),
                payload.fullName(),
                payload.birthDate()
        );
        when(registerIndividualClientUseCase.registerIndividualClient(
                anyString(), anyString(), anyString(), anyString(), anyString(), any(LocalDate.class)))
                .thenReturn(created);

        // Act + Assert
        mockMvc.perform(post("/api/v1/clients/individual")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.clientId").value(created.getClientId().toString()))
                .andExpect(jsonPath("$.identificationId").value(payload.identificationId()))
                .andExpect(jsonPath("$.email").value(payload.email()))
                .andExpect(jsonPath("$.clientType").value("INDIVIDUAL"))
                .andExpect(jsonPath("$.fullName").value(payload.fullName()));

        verify(registerIndividualClientUseCase).registerIndividualClient(
                eq(payload.identificationId()),
                eq(payload.email()),
                eq(payload.phone()),
                eq(payload.address()),
                eq(payload.fullName()),
                eq(payload.birthDate())
        );
    }

    @Test
    @WithMockUser(roles = "COMMERCIAL_EMPLOYEE")
    void should_return_400_with_fieldErrors_when_email_is_invalid() throws Exception {
        // Arrange — email fails @Email validation
        RegisterIndividualClientRequest payload = new RegisterIndividualClientRequest(
                "1001234567",
                "not-an-email",
                "+57 300 1234567",
                "Cra 50 # 10-20, Medellin",
                "Jane Doe",
                LocalDate.now().minusYears(30)
        );

        // Act + Assert
        mockMvc.perform(post("/api/v1/clients/individual")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.email").exists());
    }

    @Test
    @WithMockUser(roles = "COMMERCIAL_EMPLOYEE")
    void should_return_201_when_creating_valid_business_client() throws Exception {
        // Arrange
        RegisterBusinessClientRequest payload = new RegisterBusinessClientRequest(
                "900123456-7",
                "contact@acme.co",
                "+57 604 1234567",
                "Cl 1 # 2-3, Medellin",
                "ACME S.A.S.",
                "John Doe"
        );
        BusinessClient created = BusinessClient.create(
                payload.identificationId(),
                payload.email(),
                payload.phone(),
                payload.address(),
                payload.companyName(),
                payload.legalRepresentative()
        );
        when(registerBusinessClientUseCase.registerBusinessClient(
                anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(created);

        // Act + Assert
        mockMvc.perform(post("/api/v1/clients/business")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.clientId").value(created.getClientId().toString()))
                .andExpect(jsonPath("$.clientType").value("BUSINESS"))
                .andExpect(jsonPath("$.companyName").value(payload.companyName()))
                .andExpect(jsonPath("$.legalRepresentative").value(payload.legalRepresentative()));

        verify(registerBusinessClientUseCase).registerBusinessClient(
                eq(payload.identificationId()),
                eq(payload.email()),
                eq(payload.phone()),
                eq(payload.address()),
                eq(payload.companyName()),
                eq(payload.legalRepresentative())
        );
    }

    @Test
    @WithMockUser(roles = "INTERNAL_ANALYST")
    void should_return_404_when_client_not_found_by_id() throws Exception {
        // Arrange — INTERNAL_ANALYST short-circuits the `hasRole(...) or @authz.*`
        // ownership guard so the controller is invoked and the use case can throw.
        UUID clientId = UUID.randomUUID();
        when(findClientUseCase.findById(clientId))
                .thenThrow(new ResourceNotFoundException("Client not found: " + clientId));

        // Act + Assert
        mockMvc.perform(get("/api/v1/clients/{clientId}", clientId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));

        verify(findClientUseCase).findById(clientId);
    }

    @Test
    @WithMockUser(roles = "INTERNAL_ANALYST")
    void should_return_200_with_client_when_found_by_id() throws Exception {
        // Arrange
        IndividualClient existing = IndividualClient.create(
                "1001234567",
                "jane.doe@example.com",
                "+57 300 1234567",
                "Cra 50 # 10-20, Medellin",
                "Jane Doe",
                LocalDate.now().minusYears(30)
        );
        when(findClientUseCase.findById(existing.getClientId())).thenReturn(existing);

        // Act + Assert
        mockMvc.perform(get("/api/v1/clients/{clientId}", existing.getClientId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientId").value(existing.getClientId().toString()))
                .andExpect(jsonPath("$.identificationId").value("1001234567"))
                .andExpect(jsonPath("$.clientType").value("INDIVIDUAL"));

        verify(findClientUseCase).findById(existing.getClientId());
    }

    @Test
    @WithMockUser(roles = "COMMERCIAL_EMPLOYEE")
    void should_return_200_when_found_by_identification_id() throws Exception {
        // Arrange
        String identification = "900123456-7";
        BusinessClient existing = BusinessClient.create(
                identification,
                "contact@acme.co",
                "+57 604 1234567",
                "Cl 1 # 2-3, Medellin",
                "ACME S.A.S.",
                "John Doe"
        );
        when(findClientUseCase.findByIdentificationId(identification)).thenReturn(existing);

        // Act + Assert
        mockMvc.perform(get("/api/v1/clients/by-identification/{identificationId}", identification))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identificationId").value(identification))
                .andExpect(jsonPath("$.clientType").value("BUSINESS"))
                .andExpect(jsonPath("$.companyName").value("ACME S.A.S."));

        verify(findClientUseCase).findByIdentificationId(identification);
    }

    @Test
    @WithMockUser(roles = "COMMERCIAL_EMPLOYEE")
    void should_return_404_when_not_found_by_identification_id() throws Exception {
        // Arrange
        String identification = "999999999";
        when(findClientUseCase.findByIdentificationId(identification))
                .thenThrow(new ResourceNotFoundException("Client not found: " + identification));

        // Act + Assert
        mockMvc.perform(get("/api/v1/clients/by-identification/{identificationId}", identification))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));

        verify(findClientUseCase).findByIdentificationId(identification);
    }

    @Test
    @WithMockUser(roles = "COMMERCIAL_EMPLOYEE")
    void should_return_422_when_creating_duplicate_client() throws Exception {
        // Arrange — semantically valid payload, but use case rejects with BusinessException
        RegisterIndividualClientRequest payload = new RegisterIndividualClientRequest(
                "1001234567",
                "jane.doe@example.com",
                "+57 300 1234567",
                "Cra 50 # 10-20, Medellin",
                "Jane Doe",
                LocalDate.now().minusYears(30)
        );
        when(registerIndividualClientUseCase.registerIndividualClient(
                anyString(), anyString(), anyString(), anyString(), anyString(), any(LocalDate.class)))
                .thenThrow(new BusinessException("Client with identificationId already exists"));

        // Act + Assert
        mockMvc.perform(post("/api/v1/clients/individual")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.error").value("BUSINESS_ERROR"));
    }
}
