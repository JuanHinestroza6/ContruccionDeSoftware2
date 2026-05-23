package co.edu.tdea.bank.domain.Exceptions;

import java.math.BigDecimal;

/**
 * Thrown when a withdrawal or transfer is attempted but the source account
 * does not have enough balance to cover the requested amount.
 */
public class InsufficientFundsException extends BusinessException {

    public InsufficientFundsException(String accountNumber, BigDecimal available, BigDecimal requested) {
        super("Insufficient funds in account '" + accountNumber
                + "': available " + available + ", requested " + requested);
    }

    public InsufficientFundsException(String message) {
        super(message);
    }
}
