package tn.pedialink.auth.h2.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.pedialink.auth.entity.*;
import tn.pedialink.auth.h2.entity.LoginHistoryH2;
import tn.pedialink.auth.h2.entity.OtpH2;
import tn.pedialink.auth.h2.entity.UserH2;
import tn.pedialink.auth.h2.repository.LoginHistoryH2Repository;
import tn.pedialink.auth.h2.repository.OtpH2Repository;
import tn.pedialink.auth.h2.repository.UserH2Repository;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AuthH2BackupService}.
 * No Spring context – pure Mockito.
 */
@ExtendWith(MockitoExtension.class)
class AuthH2BackupServiceTest {

    @Mock UserH2Repository userH2Repo;
    @Mock LoginHistoryH2Repository loginHistoryH2Repo;
    @Mock OtpH2Repository otpH2Repo;

    @InjectMocks AuthH2BackupService backupService;

    // ────────────────────────────────── backupUser ─────────────────────────────

    @Nested
    @DisplayName("backupUser()")
    class BackupUser {

        @Test
        @DisplayName("creates new H2 user when mongoId not present")
        void createsNewUser() {
            User user = buildUser("mongo-1");
            when(userH2Repo.findByMongoId("mongo-1")).thenReturn(Optional.empty());
            when(userH2Repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            backupService.backupUser(user);

            ArgumentCaptor<UserH2> captor = ArgumentCaptor.forClass(UserH2.class);
            verify(userH2Repo).save(captor.capture());
            UserH2 saved = captor.getValue();

            assertThat(saved.getMongoId()).isEqualTo("mongo-1");
            assertThat(saved.getEmail()).isEqualTo("test@pedialink.tn");
            assertThat(saved.getFullName()).isEqualTo("Dr. Test");
            assertThat(saved.getRole()).isEqualTo("DOCTOR");
            assertThat(saved.getStatus()).isEqualTo("ACTIVE");
            assertThat(saved.isEmailVerified()).isTrue();
            assertThat(saved.getSpecialization()).isEqualTo("Pediatrics");
        }

        @Test
        @DisplayName("updates existing H2 user when mongoId already exists")
        void updatesExistingUser() {
            User user = buildUser("mongo-2");
            UserH2 existing = UserH2.builder().mongoId("mongo-2").email("old@test.com").build();
            when(userH2Repo.findByMongoId("mongo-2")).thenReturn(Optional.of(existing));
            when(userH2Repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            backupService.backupUser(user);

            verify(userH2Repo).save(existing);
            assertThat(existing.getEmail()).isEqualTo("test@pedialink.tn");
            assertThat(existing.getFullName()).isEqualTo("Dr. Test");
        }

        @Test
        @DisplayName("logs error gracefully when repository throws")
        void handlesException() {
            User user = buildUser("mongo-3");
            when(userH2Repo.findByMongoId(any())).thenThrow(new RuntimeException("DB error"));

            // should not throw
            backupService.backupUser(user);

            verify(userH2Repo, never()).save(any());
        }
    }

    // ────────────────────────────────── deleteUser ─────────────────────────────

    @Nested
    @DisplayName("deleteUser()")
    class DeleteUser {

        @Test
        @DisplayName("deletes user when found in H2")
        void deletesUser() {
            UserH2 h2 = UserH2.builder().mongoId("mongo-1").build();
            when(userH2Repo.findByMongoId("mongo-1")).thenReturn(Optional.of(h2));

            backupService.deleteUser("mongo-1");

            verify(userH2Repo).delete(h2);
        }

        @Test
        @DisplayName("does nothing when user not in H2")
        void silentWhenAbsent() {
            when(userH2Repo.findByMongoId("mongo-X")).thenReturn(Optional.empty());

            backupService.deleteUser("mongo-X");

            verify(userH2Repo, never()).delete(any());
        }
    }

    // ─────────────────────────────── backupLoginHistory ───────────────────────

    @Nested
    @DisplayName("backupLoginHistory()")
    class BackupLoginHistory {

        @Test
        @DisplayName("maps all fields from LoginHistory to LoginHistoryH2")
        void mapsAllFields() {
            LoginHistory lh = LoginHistory.builder()
                    .id("lh-1")
                    .userId("user-1")
                    .email("a@b.com")
                    .ipAddress("127.0.0.1")
                    .userAgent("JUnit/5")
                    .success(false)
                    .failureReason("Bad password")
                    .role(Role.PARENT)
                    .timestamp(Instant.parse("2026-01-01T00:00:00Z"))
                    .build();

            when(loginHistoryH2Repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            backupService.backupLoginHistory(lh);

            ArgumentCaptor<LoginHistoryH2> captor = ArgumentCaptor.forClass(LoginHistoryH2.class);
            verify(loginHistoryH2Repo).save(captor.capture());
            LoginHistoryH2 saved = captor.getValue();

            assertThat(saved.getMongoId()).isEqualTo("lh-1");
            assertThat(saved.getUserId()).isEqualTo("user-1");
            assertThat(saved.getEmail()).isEqualTo("a@b.com");
            assertThat(saved.getIpAddress()).isEqualTo("127.0.0.1");
            assertThat(saved.isSuccess()).isFalse();
            assertThat(saved.getFailureReason()).isEqualTo("Bad password");
            assertThat(saved.getRole()).isEqualTo("PARENT");
        }
    }

    // ────────────────────────────────── backupOtp ──────────────────────────────

    @Nested
    @DisplayName("backupOtp()")
    class BackupOtp {

        @Test
        @DisplayName("creates new OtpH2 record when not present")
        void createsNewOtp() {
            Instant expires = Instant.now().plusSeconds(600);
            Otp otp = Otp.builder()
                    .id("otp-1")
                    .email("x@y.com")
                    .code("123456")
                    .purpose(OtpPurpose.VERIFY_EMAIL)
                    .expiresAt(expires)
                    .used(false)
                    .build();

            when(otpH2Repo.findByMongoId("otp-1")).thenReturn(Optional.empty());
            when(otpH2Repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            backupService.backupOtp(otp);

            ArgumentCaptor<OtpH2> captor = ArgumentCaptor.forClass(OtpH2.class);
            verify(otpH2Repo).save(captor.capture());
            OtpH2 saved = captor.getValue();

            assertThat(saved.getMongoId()).isEqualTo("otp-1");
            assertThat(saved.getCode()).isEqualTo("123456");
            assertThat(saved.getPurpose()).isEqualTo("VERIFY_EMAIL");
            assertThat(saved.getExpiresAt()).isEqualTo(expires);
            assertThat(saved.isUsed()).isFalse();
        }
    }

    // ──────────────────────────────── helpers ──────────────────────────────────

    private User buildUser(String id) {
        return User.builder()
                .id(id)
                .fullName("Dr. Test")
                .email("test@pedialink.tn")
                .passwordHash("$2a$bcrypt")
                .role(Role.DOCTOR)
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .phone("22334455")
                .specialization("Pediatrics")
                .licenseNumber("LIC-001")
                .clinicName("PediaClinic")
                .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                .updatedAt(Instant.parse("2026-01-02T00:00:00Z"))
                .build();
    }
}
