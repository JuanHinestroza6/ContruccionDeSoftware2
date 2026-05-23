package co.edu.tdea.bank.domain.models;

import co.edu.tdea.bank.domain.enums.SystemRole;
import co.edu.tdea.bank.domain.enums.UserStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {

    private static User.Builder validBuilder() {
        return User.builder()
                .fullName("Jane Doe")
                .identificationId("U-12345")
                .email("jane.doe@example.com")
                .phone("+57 300 1234567")
                .address("Cl 1 # 2-3, Medellin")
                .systemRole(SystemRole.COMMERCIAL_EMPLOYEE);
    }

    @Test
    void should_build_user_when_all_fields_valid() {
        // Arrange
        UUID id = UUID.randomUUID();
        LocalDate birth = LocalDate.of(1990, 5, 12);

        // Act
        User user = User.builder()
                .userId(id)
                .fullName("  Jane Doe  ")
                .identificationId("U-12345")
                .email("Jane.Doe@Example.COM")
                .phone("+57 300 1234567")
                .birthDate(birth)
                .address("Cl 1 # 2-3, Medellin")
                .systemRole(SystemRole.COMMERCIAL_EMPLOYEE)
                .userStatus(UserStatus.ACTIVE)
                .build();

        // Assert
        assertThat(user.getUserId()).isEqualTo(id);
        assertThat(user.getFullName()).isEqualTo("Jane Doe");
        assertThat(user.getEmail()).isEqualTo("jane.doe@example.com"); // normalized
        assertThat(user.getBirthDate()).isEqualTo(birth);
        assertThat(user.getSystemRole()).isEqualTo(SystemRole.COMMERCIAL_EMPLOYEE);
        assertThat(user.getUserStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void should_allow_null_birthDate() {
        // Internal-employee users may have no birthDate (per builder contract)
        User user = validBuilder().build(); // birthDate not set

        assertThat(user.getBirthDate()).isNull();
        assertThat(user.getUserId()).isNotNull();
    }

    @Test
    void should_throw_when_birthDate_is_future() {
        assertThatThrownBy(() -> validBuilder().birthDate(LocalDate.now().plusDays(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("past date");
    }

    @Test
    void should_throw_when_required_field_is_null() {
        // Missing fullName
        assertThatThrownBy(() -> User.builder()
                .identificationId("U-12345")
                .email("jane.doe@example.com")
                .phone("+57 300 1234567")
                .address("Cl 1 # 2-3")
                .systemRole(SystemRole.COMMERCIAL_EMPLOYEE)
                .build())
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("fullName");
    }

    @Test
    void should_throw_when_systemRole_is_missing() {
        assertThatThrownBy(() -> User.builder()
                .fullName("Jane Doe")
                .identificationId("U-12345")
                .email("jane.doe@example.com")
                .phone("+57 300 1234567")
                .address("Cl 1 # 2-3")
                .build())
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("systemRole");
    }

    @Test
    void should_throw_when_email_is_invalid() {
        assertThatThrownBy(() -> validBuilder().email("not-an-email"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("email");
    }

    @Test
    void should_throw_when_fullName_is_blank() {
        assertThatThrownBy(() -> validBuilder().fullName("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fullName");
    }

    @Test
    void should_activate_user() {
        User user = validBuilder().userStatus(UserStatus.INACTIVE).build();
        user.activate();
        assertThat(user.getUserStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void should_block_active_user() {
        User user = validBuilder().build(); // ACTIVE by default
        user.block();
        assertThat(user.getUserStatus()).isEqualTo(UserStatus.BLOCKED);
    }

    @Test
    void should_throw_when_blocking_non_active_user() {
        User user = validBuilder().userStatus(UserStatus.INACTIVE).build();
        assertThatThrownBy(user::block)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ACTIVE");
    }

    @Test
    void should_change_role() {
        User user = validBuilder().build();
        user.changeRole(SystemRole.INTERNAL_ANALYST);
        assertThat(user.getSystemRole()).isEqualTo(SystemRole.INTERNAL_ANALYST);
    }

    @Test
    void should_throw_when_changing_to_null_role() {
        User user = validBuilder().build();
        assertThatThrownBy(() -> user.changeRole(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("role");
    }
}
