package tn.pedialink.treatment.h2.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;
import tn.pedialink.treatment.h2.entity.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the treatment H2 JPA repositories.
 * Uses an in-memory H2 database – MongoDB is NOT involved.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false"
})
class TreatmentH2RepositoriesIntegrationTest {

    @Autowired ChildH2Repository childRepo;
    @Autowired GrowthMeasurementH2Repository growthRepo;
    @Autowired AppointmentH2Repository appointmentRepo;
    @Autowired NutritionalPlanH2Repository planRepo;
    @Autowired DietaryRestrictionH2Repository dietaryRepo;

    // ─────────────────────────── ChildH2Repository ─────────────────────────────

    @Test
    @DisplayName("save child and find by mongoId")
    void saveAndFindChild() {
        childRepo.save(ChildH2.builder()
                .mongoId("c-1")
                .fullName("Emma Dupont")
                .dateOfBirth("2019-05-10")
                .gender("F")
                .parentId("p-1")
                .createdAt(Instant.now())
                .build());

        Optional<ChildH2> found = childRepo.findByMongoId("c-1");
        assertThat(found).isPresent();
        assertThat(found.get().getFullName()).isEqualTo("Emma Dupont");
        assertThat(found.get().getGender()).isEqualTo("F");
    }

    @Test
    @DisplayName("findByParentId returns all children for parent")
    void findChildrenByParent() {
        for (int i = 0; i < 3; i++) {
            childRepo.save(ChildH2.builder()
                    .mongoId("c-p-" + i)
                    .fullName("Child " + i)
                    .parentId("parent-X")
                    .build());
        }
        childRepo.save(ChildH2.builder().mongoId("c-other").parentId("parent-Y").build());

        List<ChildH2> children = childRepo.findByParentId("parent-X");
        assertThat(children).hasSize(3);
    }

    @Test
    @DisplayName("delete child removes record")
    void deleteChild() {
        childRepo.save(ChildH2.builder().mongoId("c-del").parentId("p-del").build());

        ChildH2 found = childRepo.findByMongoId("c-del").orElseThrow();
        childRepo.delete(found);

        assertThat(childRepo.findByMongoId("c-del")).isEmpty();
    }

    // ─────────────────────── GrowthMeasurementH2Repository ─────────────────────

    @Test
    @DisplayName("save growth measurement and find by childId ordered by date desc")
    void saveAndFindGrowthMeasurement() {
        LocalDate today = LocalDate.now();
        growthRepo.save(GrowthMeasurementH2.builder()
                .mongoId("gm-1").childId("child-gm").date(today.minusDays(10))
                .weight(11.0).height(80.0).bmi(17.2).createdAt(Instant.now()).build());
        growthRepo.save(GrowthMeasurementH2.builder()
                .mongoId("gm-2").childId("child-gm").date(today)
                .weight(11.5).height(81.0).bmi(17.5).createdAt(Instant.now()).build());

        List<GrowthMeasurementH2> results = growthRepo.findByChildIdOrderByDateDesc("child-gm");
        assertThat(results).hasSize(2);
        // most recent first
        assertThat(results.get(0).getMongoId()).isEqualTo("gm-2");
        assertThat(results.get(1).getMongoId()).isEqualTo("gm-1");
    }

    // ─────────────────────── AppointmentH2Repository ───────────────────────────

    @Test
    @DisplayName("save appointment and find by doctorId")
    void saveAndFindAppointment() {
        appointmentRepo.save(AppointmentH2.builder()
                .mongoId("apt-1")
                .doctorId("doc-1")
                .parentId("p-1")
                .childId("c-1")
                .status("PENDING")
                .reason("Checkup")
                .createdAt(Instant.now())
                .build());

        List<AppointmentH2> found = appointmentRepo.findByDoctorId("doc-1");
        assertThat(found).hasSize(1);
        assertThat(found.get(0).getStatus()).isEqualTo("PENDING");
        assertThat(found.get(0).getReason()).isEqualTo("Checkup");
    }

    @Test
    @DisplayName("findByStatus returns only matching appointments")
    void findByStatus() {
        appointmentRepo.save(AppointmentH2.builder().mongoId("apt-s-1").doctorId("d").parentId("p").childId("c").status("ACCEPTED").createdAt(Instant.now()).build());
        appointmentRepo.save(AppointmentH2.builder().mongoId("apt-s-2").doctorId("d").parentId("p").childId("c").status("PENDING").createdAt(Instant.now()).build());
        appointmentRepo.save(AppointmentH2.builder().mongoId("apt-s-3").doctorId("d").parentId("p").childId("c").status("ACCEPTED").createdAt(Instant.now()).build());

        assertThat(appointmentRepo.findByStatus("ACCEPTED")).hasSize(2);
        assertThat(appointmentRepo.findByStatus("PENDING")).hasSize(1);
    }

    // ──────────────────────── NutritionalPlanH2Repository ──────────────────────

    @Test
    @DisplayName("save nutritional plan and find by childId")
    void saveAndFindNutritionalPlan() {
        planRepo.save(NutritionalPlanH2.builder()
                .mongoId("plan-1")
                .childId("child-p")
                .doctorId("doc-p")
                .title("High protein plan")
                .status("ACTIVE")
                .createdAt(Instant.now())
                .build());

        List<NutritionalPlanH2> found = planRepo.findByChildId("child-p");
        assertThat(found).hasSize(1);
        assertThat(found.get(0).getTitle()).isEqualTo("High protein plan");
        assertThat(found.get(0).getStatus()).isEqualTo("ACTIVE");
    }

    // ─────────────────────── DietaryRestrictionH2Repository ────────────────────

    @Test
    @DisplayName("save dietary restriction and find by childId")
    void saveAndFindDietaryRestriction() {
        dietaryRepo.save(DietaryRestrictionH2.builder()
                .mongoId("dr-1")
                .childId("child-dr")
                .type("ALLERGY")
                .allergen("Gluten")
                .severity("MILD")
                .createdAt(Instant.now())
                .build());

        List<DietaryRestrictionH2> found = dietaryRepo.findByChildId("child-dr");
        assertThat(found).hasSize(1);
        assertThat(found.get(0).getAllergen()).isEqualTo("Gluten");
        assertThat(found.get(0).getSeverity()).isEqualTo("MILD");
    }

    @Test
    @DisplayName("findBySeverity returns matching restrictions")
    void findBySeverity() {
        dietaryRepo.save(DietaryRestrictionH2.builder().mongoId("dr-s-1").childId("c").type("A").allergen("X").severity("SEVERE").createdAt(Instant.now()).build());
        dietaryRepo.save(DietaryRestrictionH2.builder().mongoId("dr-s-2").childId("c").type("A").allergen("Y").severity("MILD").createdAt(Instant.now()).build());
        dietaryRepo.save(DietaryRestrictionH2.builder().mongoId("dr-s-3").childId("c").type("A").allergen("Z").severity("SEVERE").createdAt(Instant.now()).build());

        assertThat(dietaryRepo.findBySeverity("SEVERE")).hasSize(2);
        assertThat(dietaryRepo.findBySeverity("MILD")).hasSize(1);
    }
}
