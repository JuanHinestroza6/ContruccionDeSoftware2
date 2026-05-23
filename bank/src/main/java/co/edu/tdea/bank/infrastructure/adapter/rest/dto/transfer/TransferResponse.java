package co.edu.tdea.bank.infrastructure.adapter.rest.dto.transfer;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Outbound representation of a {@code Transfer} aggregate.
 *
 * <p>Enum-valued attributes ({@code transferStatus}) are serialized as
 * their {@link Enum#name()} string to decouple the wire format from the
 * domain enum classes.</p>
 *
 * <p>{@code @JsonInclude(NON_NULL)} omits fields that are null for transfers
 * still in {@code PENDING_APPROVAL} (e.g. {@code approvalDateTime} and
 * {@code approvedByUserId} are null until the transfer is approved or
 * rejected).</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TransferResponse(

        Long transferId,
        String sourceAccountNumber,
        String destinationAccountNumber,
        BigDecimal amount,

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime creationDateTime,

        String transferStatus,
        UUID createdByUserId,

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime approvalDateTime,

        UUID approvedByUserId
) {
}
