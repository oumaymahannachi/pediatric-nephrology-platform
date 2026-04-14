package tn.pedialink.treatment.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class AIGeneratedPlanRequest {
    private String childId;
    private LocalDate dateOfBirth;
    private String gender;
    private Double weight;  // Fallback si pas de mesure récente
    private Double height;  // Fallback si pas de mesure récente
}
