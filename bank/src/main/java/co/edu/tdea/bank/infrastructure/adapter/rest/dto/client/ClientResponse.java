package co.edu.tdea.bank.infrastructure.adapter.rest.dto.client;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Outbound representation of a {@code Client}, polymorphic for both
 * {@code IndividualClient} and {@code BusinessClient}.
 *
 * <p>Type-specific attributes are populated only for the matching subtype
 * (i.e. {@code fullName} / {@code birthDate} for individuals, {@code companyName}
 * / {@code legalRepresentative} for businesses) and are {@code null} otherwise.
 * Nulls are omitted from the serialized JSON via
 * {@link JsonInclude.Include#NON_NULL}.</p>
 *
 * <p><b>Note:</b> {@code clientStatus} is intentionally not exposed yet — the
 * current domain model has no {@code ClientStatus} field. The attribute can
 * be added once the domain layer introduces it.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ClientResponse(

        UUID clientId,
        String identificationId,
        String email,
        String phone,
        String address,
        String clientType,

        // --- Individual-only fields ---
        String fullName,

        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate birthDate,

        // --- Business-only fields ---
        String companyName,
        String legalRepresentative
) {
}
