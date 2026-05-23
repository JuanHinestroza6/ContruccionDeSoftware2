package co.edu.tdea.bank.infrastructure.adapter.rest.dto.client;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Inbound payload for registering a new business (legal entity) client.
 *
 * <p>All fields are required.</p>
 */
public record RegisterBusinessClientRequest(

        @NotBlank(message = "identificationId must not be blank")
        String identificationId,

        @NotBlank(message = "email must not be blank")
        @Email(message = "email must be a valid email address")
        String email,

        @NotBlank(message = "phone must not be blank")
        String phone,

        @NotBlank(message = "address must not be blank")
        String address,

        @NotBlank(message = "companyName must not be blank")
        String companyName,

        @NotBlank(message = "legalRepresentative must not be blank")
        String legalRepresentative
) {
}
