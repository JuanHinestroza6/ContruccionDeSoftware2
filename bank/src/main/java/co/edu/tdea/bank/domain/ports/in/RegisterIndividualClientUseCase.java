package co.edu.tdea.bank.domain.ports.in;

import co.edu.tdea.bank.domain.models.IndividualClient;

import java.time.LocalDate;

/**
 * Input port — registers a new individual (natural person) client.
 */
public interface RegisterIndividualClientUseCase {

    /**
     * Registers a new individual client after validating uniqueness of
     * identification number and email.
     *
     * @param identificationId national identification number (e.g. Cédula)
     * @param email            contact email address
     * @param phone            contact phone number
     * @param address          physical address
     * @param fullName         full legal name
     * @param birthDate        date of birth (must denote an age >= 18)
     * @return the persisted {@link IndividualClient}
     */
    IndividualClient registerIndividualClient(String identificationId,
                                              String email,
                                              String phone,
                                              String address,
                                              String fullName,
                                              LocalDate birthDate);
}
