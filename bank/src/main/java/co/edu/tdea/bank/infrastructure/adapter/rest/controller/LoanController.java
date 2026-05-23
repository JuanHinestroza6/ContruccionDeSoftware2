package co.edu.tdea.bank.infrastructure.adapter.rest.controller;

import co.edu.tdea.bank.domain.models.Loan;
import co.edu.tdea.bank.domain.ports.in.ApproveLoanUseCase;
import co.edu.tdea.bank.domain.ports.in.DisburseLoanUseCase;
import co.edu.tdea.bank.domain.ports.in.FindLoanUseCase;
import co.edu.tdea.bank.domain.ports.in.RejectLoanUseCase;
import co.edu.tdea.bank.domain.ports.in.RequestLoanUseCase;
import co.edu.tdea.bank.infrastructure.adapter.rest.dto.loan.ApproveLoanRequest;
import co.edu.tdea.bank.infrastructure.adapter.rest.dto.loan.DisburseLoanRequest;
import co.edu.tdea.bank.infrastructure.adapter.rest.dto.loan.LoanResponse;
import co.edu.tdea.bank.infrastructure.adapter.rest.dto.loan.RejectLoanRequest;
import co.edu.tdea.bank.infrastructure.adapter.rest.dto.loan.RequestLoanRequest;
import co.edu.tdea.bank.infrastructure.adapter.rest.mapper.LoanDtoMapper;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST adapter for the {@code Loan} aggregate.
 *
 * <p>Pure delegating layer — every endpoint validates input, hands the call
 * off to the corresponding input port, and serializes the domain result via
 * {@link LoanDtoMapper}. No business decisions live in this class.</p>
 *
 * <p>Authorization (S3):
 * <ul>
 *   <li>Approve / reject / disburse are reserved for {@code INTERNAL_ANALYST}.</li>
 *   <li>Requesting a loan is open to clients and the commercial employee that
 *       on-boards them.</li>
 *   <li>Reads add ownership filtering via {@code @authz} so a client only sees
 *       its own loans; analysts bypass ownership.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/loans")
public class LoanController {

    private final RequestLoanUseCase requestLoanUseCase;
    private final ApproveLoanUseCase approveLoanUseCase;
    private final RejectLoanUseCase rejectLoanUseCase;
    private final DisburseLoanUseCase disburseLoanUseCase;
    private final FindLoanUseCase findLoanUseCase;

    public LoanController(RequestLoanUseCase requestLoanUseCase,
                          ApproveLoanUseCase approveLoanUseCase,
                          RejectLoanUseCase rejectLoanUseCase,
                          DisburseLoanUseCase disburseLoanUseCase,
                          FindLoanUseCase findLoanUseCase) {
        this.requestLoanUseCase  = requestLoanUseCase;
        this.approveLoanUseCase  = approveLoanUseCase;
        this.rejectLoanUseCase   = rejectLoanUseCase;
        this.disburseLoanUseCase = disburseLoanUseCase;
        this.findLoanUseCase     = findLoanUseCase;
    }

    /**
     * Submits a new loan request for an existing client. The loan is created
     * in {@code UNDER_REVIEW} status.
     */
    @PreAuthorize("hasAnyRole('INDIVIDUAL_CLIENT','BUSINESS_ADMIN','COMMERCIAL_EMPLOYEE')")
    @PostMapping
    public ResponseEntity<LoanResponse> requestLoan(
            @Valid @RequestBody RequestLoanRequest request) {

        Loan created = requestLoanUseCase.requestLoan(
                request.clientId(),
                request.loanType(),
                request.requestedAmount(),
                request.termInMonths()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(LoanDtoMapper.toResponse(created));
    }

    /**
     * Retrieves a loan by its technical identifier.
     */
    @PreAuthorize("hasRole('INTERNAL_ANALYST') or @authz.hasAccessToLoan(authentication, #loanId)")
    @GetMapping("/{loanId}")
    public ResponseEntity<LoanResponse> findById(@PathVariable Long loanId) {
        Loan loan = findLoanUseCase.findById(loanId);
        return ResponseEntity.ok(LoanDtoMapper.toResponse(loan));
    }

    /**
     * Lists every loan belonging to the given client. Returns an empty list
     * (never 404) when the client has no loans.
     */
    @PreAuthorize("hasRole('INTERNAL_ANALYST') or @authz.hasAccessToClient(authentication, #clientId)")
    @GetMapping("/by-client/{clientId}")
    public ResponseEntity<List<LoanResponse>> findByClientId(@PathVariable UUID clientId) {
        List<LoanResponse> loans = findLoanUseCase.findByClientId(clientId)
                .stream()
                .map(LoanDtoMapper::toResponse)
                .toList();
        return ResponseEntity.ok(loans);
    }

    /**
     * Approves a loan currently in {@code UNDER_REVIEW}, applying the
     * commercial terms supplied by an INTERNAL_ANALYST.
     */
    @PreAuthorize("hasRole('INTERNAL_ANALYST')")
    @PatchMapping("/{loanId}/approve")
    public ResponseEntity<LoanResponse> approveLoan(
            @PathVariable Long loanId,
            @Valid @RequestBody ApproveLoanRequest request) {

        Loan approved = approveLoanUseCase.approveLoan(
                loanId,
                request.approverId(),
                request.approvedAmount(),
                request.interestRate(),
                request.approvalDate()
        );
        return ResponseEntity.ok(LoanDtoMapper.toResponse(approved));
    }

    /**
     * Rejects a loan currently in {@code UNDER_REVIEW}.
     */
    @PreAuthorize("hasRole('INTERNAL_ANALYST')")
    @PatchMapping("/{loanId}/reject")
    public ResponseEntity<LoanResponse> rejectLoan(
            @PathVariable Long loanId,
            @Valid @RequestBody RejectLoanRequest request) {

        Loan rejected = rejectLoanUseCase.rejectLoan(loanId, request.approverId());
        return ResponseEntity.ok(LoanDtoMapper.toResponse(rejected));
    }

    /**
     * Disburses an approved loan by crediting the approved amount into the
     * destination account.
     */
    @PreAuthorize("hasRole('INTERNAL_ANALYST')")
    @PatchMapping("/{loanId}/disburse")
    public ResponseEntity<LoanResponse> disburseLoan(
            @PathVariable Long loanId,
            @Valid @RequestBody DisburseLoanRequest request) {

        Loan disbursed = disburseLoanUseCase.disburseLoan(
                loanId,
                request.destinationAccountNumber(),
                request.disbursementDate(),
                request.analystUserId()
        );
        return ResponseEntity.ok(LoanDtoMapper.toResponse(disbursed));
    }
}
