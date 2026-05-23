package co.edu.tdea.bank.infrastructure.adapter.rest.dto.client;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;

import java.time.LocalDate;

/**
 * Inbound payload for registering a new individual (natural person) client.
 *
 * <p>All fields are required. {@code birthDate} must be a date strictly in
 * the past; the minimum-age rule (>= 18) is enforced by the domain layer.</p>
 */
public record RegisterIndividualClientRequest(

        @NotBlank(message = "identificationId must not be blank")
        String identificationId,

        @NotBlank(message = "email must not be blank")
        @Email(message = "email must be a valid email address")
        String email,

        @NotBlank(message = "phone must not be blank")
        String phone,

        @NotBlank(message = "address must not be blank")
        String address,

        @NotBlank(message = "fullName must not be blank")
        String fullName,

        @NotNull(message = "birthDate must not be null")
        @Past(message = "birthDate must be a past date")
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate birthDate
) {
}
