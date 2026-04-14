package tn.pedialink.auth.h2.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;
import tn.pedialink.auth.h2.entity.LoginHistoryH2;
import tn.pedialink.auth.h2.entity.OtpH2;
import tn.pedialink.auth.h2.entity.UserH2;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the H2 JPA repositories.
 * Uses an in-memory H2 database (Spring Boot Test replaces the file-based URL).
 * MongoDB is NOT involved – this is a pure JPA slice test.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false"
})
class H2RepositoriesIntegrationTest {

    @Autowired UserH2Repository userRepo;
    @Autowired LoginHistoryH2Repository loginHistoryRepo;
    @Autowired OtpH2Repository otpRepo;

    // ─────────────────────── UserH2Repository ──────────────────────────────────

    @Test
    @DisplayName("save and findByMongoId returns correct user")
    void saveAndFindUser() {
        userRepo.save(UserH2.builder()
                .mongoId("mongo-u-1")
                .fullName("Alice Doctor")
                .email("alice@pedialink.tn")
                .role("DOCTOR")
                .status("ACTIVE")
                .emailVerified(true)
                .createdAt(Instant.now())
                .build());

        Optional<UserH2> found = userRepo.findByMongoId("mongo-u-1");
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("alice@pedialink.tn");
        assertThat(found.get().getRole()).isEqualTo("DOCTOR");
        assertThat(found.get().isEmailVerified()).isTrue();
    }

    @Test
    @DisplayName("findByEmail returns user when email matches")
    void findByEmail() {
        userRepo.save(UserH2.builder()
                .mongoId("mongo-u-2")
                .email("bob@pedialink.tn")
                .role("PARENT")
                .status("ACTIVE")
                .build());

        Optional<UserH2> found = userRepo.findByEmail("bob@pedialink.tn");
        assertThat(found).isPresent();
        assertThat(found.get().getMongoId()).isEqualTo("mongo-u-2");
    }

    @Test
    @DisplayName("existsByEmail returns true for existing email")
    void existsByEmail() {
        userRepo.save(UserH2.builder()
                .mongoId("mongo-u-3")
                .email("carol@pedialink.tn")
                .role("INFIRMIER")
                .status("ACTIVE")
                .build());

        assertThat(userRepo.existsByEmail("carol@pedialink.tn")).isTrue();
        assertThat(userRepo.existsByEmail("notexist@x.com")).isFalse();
    }

    @Test
    @DisplayName("deleting a user removes it from H2")
    void deleteUser() {
        userRepo.save(UserH2.builder()
                .mongoId("mongo-u-4")
                .email("del@pedialink.tn")
                .role("PARENT")
                .status("ACTIVE")
                .build());

        UserH2 found = userRepo.findByMongoId("mongo-u-4").orElseThrow();
        userRepo.delete(found);

        assertThat(userRepo.findByMongoId("mongo-u-4")).isEmpty();
    }

    // ────────────────────── LoginHistoryH2Repository ───────────────────────────

    @Test
    @DisplayName("save login history and find by userId")
    void saveAndFindLoginHistory() {
        loginHistoryRepo.save(LoginHistoryH2.builder()
                .mongoId("mongo-lh-1")
                .userId("user-1")
                .email("a@b.com")
                .ipAddress("10.0.0.1")
                .success(true)
                .role("DOCTOR")
                .timestamp(Instant.now())
                .build());

        List<LoginHistoryH2> list = loginHistoryRepo.findByUserIdOrderByTimestampDesc("user-1");
        assertThat(list).hasSize(1);
        assertThat(list.get(0).getMongoId()).isEqualTo("mongo-lh-1");
        assertThat(list.get(0).isSuccess()).isTrue();
    }

    @Test
    @DisplayName("countByUserId returns correct count")
    void countLoginHistory() {
        for (int i = 0; i < 3; i++) {
            loginHistoryRepo.save(LoginHistoryH2.builder()
                    .mongoId("mongo-lh-cnt-" + i)
                    .userId("user-cnt")
                    .email("cnt@x.com")
                    .success(i % 2 == 0)
                    .timestamp(Instant.now())
                    .build());
        }

        assertThat(loginHistoryRepo.countByUserId("user-cnt")).isEqualTo(3);
    }

    // ──────────────────────── OtpH2Repository ──────────────────────────────────

    @Test
    @DisplayName("save OTP and find by mongoId")
    void saveAndFindOtp() {
        Instant expires = Instant.now().plusSeconds(600);
        otpRepo.save(OtpH2.builder()
                .mongoId("mongo-otp-1")
                .email("otp@test.com")
                .code("654321")
                .purpose("VERIFY_EMAIL")
                .expiresAt(expires)
                .used(false)
                .build());

        Optional<OtpH2> found = otpRepo.findByMongoId("mongo-otp-1");
        assertThat(found).isPresent();
        assertThat(found.get().getCode()).isEqualTo("654321");
        assertThat(found.get().isUsed()).isFalse();
        assertThat(found.get().getExpiresAt()).isEqualTo(expires);
    }

    @Test
    @DisplayName("findByEmailOrderByExpiresAtDesc returns all OTPs for email")
    void findOtpsByEmail() {
        Instant now = Instant.now();
        for (int i = 0; i < 2; i++) {
            otpRepo.save(OtpH2.builder()
                    .mongoId("mongo-otp-list-" + i)
                    .email("multi@test.com")
                    .code("00000" + i)
                    .purpose("RESET_PASSWORD")
                    .expiresAt(now.plusSeconds(i * 100L))
                    .used(false)
                    .build());
        }

        List<OtpH2> otps = otpRepo.findByEmailOrderByExpiresAtDesc("multi@test.com");
        assertThat(otps).hasSize(2);
        // verify ordering: later expiry first
        assertThat(otps.get(0).getExpiresAt()).isAfterOrEqualTo(otps.get(1).getExpiresAt());
    }
}
