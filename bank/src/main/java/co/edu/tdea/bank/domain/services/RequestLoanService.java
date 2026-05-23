package co.edu.tdea.bank.domain.services;

import co.edu.tdea.bank.domain.ports.in.RequestLoanUseCase;
import co.edu.tdea.bank.domain.ports.out.AuditLogPort;
import co.edu.tdea.bank.domain.ports.out.ClientRepositoryPort;
import co.edu.tdea.bank.domain.ports.out.LoanRepositoryPort;
import co.edu.tdea.bank.domain.ports.out.UserRepositoryPort;
import co.edu.tdea.bank.domain.enums.LoanType;
import co.edu.tdea.bank.domain.enums.UserStatus;
import co.edu.tdea.bank.domain.models.Client;
import co.edu.tdea.bank.domain.models.Loan;
import co.edu.tdea.bank.domain.models.User;
import co.edu.tdea.bank.domain.Exceptions.ResourceNotFoundException;
import co.edu.tdea.bank.domain.Exceptions.UnauthorizedOperationException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;

/**
 * Application service â€” orchestrates the submission of a new loan request.
 *
 * <p>Business rules enforced:
 * <ol>
 *   <li>The client must exist in the system.</li>
 *   <li>A system user linked to that client must exist.</li>
 *   <li>The linked user must not be {@code INACTIVE} or {@code BLOCKED}.</li>
 *   <li>The loan is created in {@code UNDER_REVIEW}, persisted, and audited.</li>
 * </ol>
 *
 * <p>The loan identifier is assigned by the persistence layer after saving;
 * the domain factory {@link Loan#request} is called with the ID returned by the repository.
 */
@Service
public class RequestLoanService implements RequestLoanUseCase {

    private final ClientRepositoryPort clientRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;
    private final LoanRepositoryPort loanRepositoryPort;
    private final AuditLogPort auditLogPort;

    public RequestLoanService(ClientRepositoryPort clientRepositoryPort,
                              UserRepositoryPort userRepositoryPort,
                              LoanRepositoryPort loanRepositoryPort,
                              AuditLogPort auditLogPort) {
        this.clientRepositoryPort = clientRepositoryPort;
        this.userRepositoryPort   = userRepositoryPort;
        this.loanRepositoryPort   = loanRepositoryPort;
        this.auditLogPort         = auditLogPort;
    }

    @Override
    public Loan requestLoan(UUID clientId,
                            LoanType loanType,
                            BigDecimal requestedAmount,
                            Integer termInMonths) {

        // 1. Client must exist
        Client client = clientRepositoryPort.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", clientId));

        // 2. A system user linked to this client must exist
        User relatedUser = userRepositoryPort.findByRelatedClientId(clientId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "System user linked to client", clientId));

        // 3. User must not be INACTIVE
        if (relatedUser.getUserStatus() == UserStatus.INACTIVE) {
            throw new UnauthorizedOperationException(
                    "Cannot request loan: linked user " + relatedUser.getUserId()
                    + " is INACTIVE.");
        }

        // 4. User must not be BLOCKED
        if (relatedUser.getUserStatus() == UserStatus.BLOCKED) {
            throw new UnauthorizedOperationException(
                    "Cannot request loan: linked user " + relatedUser.getUserId()
                    + " is BLOCKED.");
        }

        // 5. Build the loan aggregate â€” loanId is a placeholder; the real ID is
        //    assigned by the persistence layer. The saved instance carries the final ID.
        Loan loan = Loan.request(loanType, client, requestedAmount, termInMonths);

        // 6. Persist â€” the repository assigns and returns the definitive loanId
        Loan savedLoan = loanRepositoryPort.save(loan);

        // 7. Audit
        auditLogPort.save(
                "Loan",
                String.valueOf(savedLoan.getLoanId()),
                "LOAN_REQUESTED",
                relatedUser.getUserId().toString(),
                LocalDateTime.now(),
                "clientId=" + clientId
                + ", loanType=" + loanType
                + ", requestedAmount=" + requestedAmount
                + ", termInMonths=" + termInMonths
        );

        return savedLoan;
    }
}
