package tn.pedialink.treatment.h2.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.pedialink.treatment.entity.*;
import tn.pedialink.treatment.h2.entity.*;
import tn.pedialink.treatment.h2.repository.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link TreatmentH2BackupService}.
 * No Spring context – pure Mockito.
 */
@ExtendWith(MockitoExtension.class)
class TreatmentH2BackupServiceTest {

    @Mock ChildH2Repository childH2Repo;
    @Mock GrowthMeasurementH2Repository growthH2Repo;
    @Mock AppointmentH2Repository appointmentH2Repo;
    @Mock NutritionalPlanH2Repository planH2Repo;
    @Mock DietaryRestrictionH2Repository dietaryH2Repo;

    @InjectMocks TreatmentH2BackupService backupService;

    // ─────────────────────────────── backupChild ───────────────────────────────

    @Nested
    @DisplayName("backupChild()")
    class BackupChild {

        @Test
        @DisplayName("creates new ChildH2 when mongoId not present")
        void createsNewChild() {
            Child child = buildChild("child-1");
            when(childH2Repo.findByMongoId("child-1")).thenReturn(Optional.empty());
            when(childH2Repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            backupService.backupChild(child);

            ArgumentCaptor<ChildH2> captor = ArgumentCaptor.forClass(ChildH2.class);
            verify(childH2Repo).save(captor.capture());
            ChildH2 saved = captor.getValue();

            assertThat(saved.getMongoId()).isEqualTo("child-1");
            assertThat(saved.getFullName()).isEqualTo("Tommy Test");
            assertThat(saved.getGender()).isEqualTo("M");
            assertThat(saved.getParentId()).isEqualTo("parent-1");
            assertThat(saved.getDoctorIds()).containsExactly("doc-1", "doc-2");
        }

        @Test
        @DisplayName("updates existing ChildH2")
        void updatesExistingChild() {
            Child child = buildChild("child-2");
            ChildH2 existing = ChildH2.builder().mongoId("child-2").fullName("Old Name").build();
            when(childH2Repo.findByMongoId("child-2")).thenReturn(Optional.of(existing));
            when(childH2Repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            backupService.backupChild(child);

            verify(childH2Repo).save(existing);
            assertThat(existing.getFullName()).isEqualTo("Tommy Test");
        }

        @Test
        @DisplayName("handles exception gracefully")
        void handlesException() {
            Child child = buildChild("child-err");
            when(childH2Repo.findByMongoId(any())).thenThrow(new RuntimeException("H2 error"));

            backupService.backupChild(child);  // must not throw

            verify(childH2Repo, never()).save(any());
        }
    }

    // ─────────────────────────── deleteChild ───────────────────────────────────

    @Nested
    @DisplayName("deleteChild()")
    class DeleteChild {

        @Test
        @DisplayName("deletes child when found")
        void deletesChild() {
            ChildH2 h2 = ChildH2.builder().mongoId("child-del").build();
            when(childH2Repo.findByMongoId("child-del")).thenReturn(Optional.of(h2));

            backupService.deleteChild("child-del");

            verify(childH2Repo).delete(h2);
        }

        @Test
        @DisplayName("does nothing when child not found")
        void silentWhenAbsent() {
            when(childH2Repo.findByMongoId("x")).thenReturn(Optional.empty());

            backupService.deleteChild("x");

            verify(childH2Repo, never()).delete(any());
        }
    }

    // ──────────────────────── backupGrowthMeasurement ──────────────────────────

    @Nested
    @DisplayName("backupGrowthMeasurement()")
    class BackupGrowthMeasurement {

        @Test
        @DisplayName("maps all measurement fields correctly")
        void mapsAllFields() {
            GrowthMeasurement gm = GrowthMeasurement.builder()
                    .id("gm-1")
                    .childId("child-1")
                    .date(LocalDate.of(2026, 1, 15))
                    .weight(12.5)
                    .height(85.0)
                    .bmi(17.3)
                    .headCircumference(48.0)
                    .notes("Normal growth")
                    .recordedBy("doc-1")
                    .createdAt(Instant.now())
                    .build();

            when(growthH2Repo.findByMongoId("gm-1")).thenReturn(Optional.empty());
            when(growthH2Repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            backupService.backupGrowthMeasurement(gm);

            ArgumentCaptor<GrowthMeasurementH2> captor = ArgumentCaptor.forClass(GrowthMeasurementH2.class);
            verify(growthH2Repo).save(captor.capture());
            GrowthMeasurementH2 saved = captor.getValue();

            assertThat(saved.getMongoId()).isEqualTo("gm-1");
            assertThat(saved.getWeight()).isEqualTo(12.5);
            assertThat(saved.getHeight()).isEqualTo(85.0);
            assertThat(saved.getBmi()).isEqualTo(17.3);
            assertThat(saved.getDate()).isEqualTo(LocalDate.of(2026, 1, 15));
        }
    }

    // ─────────────────────────── backupAppointment ─────────────────────────────

    @Nested
    @DisplayName("backupAppointment()")
    class BackupAppointment {

        @Test
        @DisplayName("maps status enum to string")
        void mapsStatusEnum() {
            Appointment apt = Appointment.builder()
                    .id("apt-1")
                    .childId("child-1")
                    .doctorId("doc-1")
                    .parentId("parent-1")
                    .status(AppointmentStatus.ACCEPTED)
                    .reason("Routine checkup")
                    .createdAt(Instant.now())
                    .build();

            when(appointmentH2Repo.findByMongoId("apt-1")).thenReturn(Optional.empty());
            when(appointmentH2Repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            backupService.backupAppointment(apt);

            ArgumentCaptor<AppointmentH2> captor = ArgumentCaptor.forClass(AppointmentH2.class);
            verify(appointmentH2Repo).save(captor.capture());
            AppointmentH2 saved = captor.getValue();

            assertThat(saved.getStatus()).isEqualTo("ACCEPTED");
            assertThat(saved.getReason()).isEqualTo("Routine checkup");
        }
    }

    // ──────────────────────── backupNutritionalPlan ────────────────────────────

    @Nested
    @DisplayName("backupNutritionalPlan()")
    class BackupNutritionalPlan {

        @Test
        @DisplayName("maps meals list from MongoDB entity")
        void mapsMeals() {
            Meal meal = new Meal("Breakfast", "08:00", "Oatmeal", "350", null);
            NutritionalPlan plan = NutritionalPlan.builder()
                    .id("plan-1")
                    .childId("child-1")
                    .doctorId("doc-1")
                    .title("Weight plan")
                    .status("ACTIVE")
                    .meals(new ArrayList<>(List.of(meal)))
                    .createdAt(Instant.now())
                    .build();

            when(planH2Repo.findByMongoId("plan-1")).thenReturn(Optional.empty());
            when(planH2Repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            backupService.backupNutritionalPlan(plan);

            ArgumentCaptor<NutritionalPlanH2> captor = ArgumentCaptor.forClass(NutritionalPlanH2.class);
            verify(planH2Repo).save(captor.capture());
            NutritionalPlanH2 saved = captor.getValue();

            assertThat(saved.getMeals()).hasSize(1);
            assertThat(saved.getMeals().get(0).getName()).isEqualTo("Breakfast");
            assertThat(saved.getTitle()).isEqualTo("Weight plan");
        }
    }

    // ─────────────────────── backupDietaryRestriction ─────────────────────────

    @Nested
    @DisplayName("backupDietaryRestriction()")
    class BackupDietaryRestriction {

        @Test
        @DisplayName("maps all dietary restriction fields")
        void mapsAllFields() {
            DietaryRestriction dr = DietaryRestriction.builder()
                    .id("dr-1")
                    .childId("child-1")
                    .type("ALLERGY")
                    .allergen("Peanuts")
                    .severity("SEVERE")
                    .description("Anaphylaxis risk")
                    .createdAt(Instant.now())
                    .build();

            when(dietaryH2Repo.findByMongoId("dr-1")).thenReturn(Optional.empty());
            when(dietaryH2Repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            backupService.backupDietaryRestriction(dr);

            ArgumentCaptor<DietaryRestrictionH2> captor = ArgumentCaptor.forClass(DietaryRestrictionH2.class);
            verify(dietaryH2Repo).save(captor.capture());
            DietaryRestrictionH2 saved = captor.getValue();

            assertThat(saved.getMongoId()).isEqualTo("dr-1");
            assertThat(saved.getAllergen()).isEqualTo("Peanuts");
            assertThat(saved.getSeverity()).isEqualTo("SEVERE");
        }
    }

    // ──────────────────────────────── helpers ──────────────────────────────────

    private Child buildChild(String id) {
        return Child.builder()
                .id(id)
                .fullName("Tommy Test")
                .dateOfBirth("2020-03-15")
                .gender("M")
                .parentId("parent-1")
                .doctorIds(new ArrayList<>(List.of("doc-1", "doc-2")))
                .notes("Healthy child")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
}
