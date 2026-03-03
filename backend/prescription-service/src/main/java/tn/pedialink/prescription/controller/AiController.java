package tn.pedialink.prescription.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tn.pedialink.prescription.service.OcrService;
import tn.pedialink.prescription.service.TranslationService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/prescriptions/ai")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class AiController {

    private final OcrService ocrService;
    private final TranslationService translationService;

    /**
     * OCR endpoint - Extract prescription from image
     */
    @PostMapping("/ocr/extract")
    public ResponseEntity<?> extractPrescription(@RequestParam("image") MultipartFile image) {
        try {
            log.info("Received OCR request for image: {}", image != null ? image.getOriginalFilename() : "null");
            
            // Check if image is provided
            if (image == null || image.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "No image provided. Please upload an image."
                ));
            }
            
            // Validate image
            if (!ocrService.isValidImage(image)) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Invalid image format. Please upload JPEG or PNG (max 5MB)"
                ));
            }
            
            // Extract prescription data
            Map<String, Object> result = ocrService.extractPrescriptionFromImage(image);
            
            // Check if extraction was successful
            if (result.containsKey("success") && !(Boolean) result.get("success")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
            }
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Error processing OCR request", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "Error processing image: " + e.getMessage(),
                "error", e.getClass().getSimpleName()
            ));
        }
    }

    /**
     * Translation endpoint - Translate text
     */
    @PostMapping("/translate")
    public ResponseEntity<?> translateText(@RequestBody Map<String, String> request) {
        try {
            String text = request.get("text");
            String targetLanguage = request.get("targetLanguage");
            
            if (text == null || text.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Text is required"
                ));
            }
            
            if (targetLanguage == null || targetLanguage.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Target language is required"
                ));
            }
            
            String translatedText = translationService.translate(text, targetLanguage);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "originalText", text,
                "translatedText", translatedText,
                "targetLanguage", targetLanguage
            ));
            
        } catch (Exception e) {
            log.error("Error translating text", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "Error translating text: " + e.getMessage()
            ));
        }
    }

    /**
     * Translate to multiple languages
     */
    @PostMapping("/translate/multiple")
    public ResponseEntity<?> translateToMultiple(@RequestBody Map<String, String> request) {
        try {
            String text = request.get("text");
            
            if (text == null || text.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Text is required"
                ));
            }
            
            Map<String, String> translations = translationService.translateToMultipleLanguages(text);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "translations", translations
            ));
            
        } catch (Exception e) {
            log.error("Error translating to multiple languages", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "Error translating: " + e.getMessage()
            ));
        }
    }

    /**
     * Get supported languages
     */
    @GetMapping("/translate/languages")
    public ResponseEntity<?> getSupportedLanguages() {
        try {
            Map<String, String> languages = translationService.getSupportedLanguages();
            return ResponseEntity.ok(Map.of(
                "success", true,
                "languages", languages
            ));
        } catch (Exception e) {
            log.error("Error getting supported languages", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "Error getting languages: " + e.getMessage()
            ));
        }
    }

    /**
     * Translate prescription
     */
    @PostMapping("/translate/prescription")
    public ResponseEntity<?> translatePrescription(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> prescription = (Map<String, Object>) request.get("prescription");
            String targetLanguage = (String) request.get("targetLanguage");
            
            if (prescription == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Prescription data is required"
                ));
            }
            
            if (targetLanguage == null || targetLanguage.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Target language is required"
                ));
            }
            
            Map<String, Object> translated = translationService.translatePrescription(prescription, targetLanguage);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "prescription", translated,
                "targetLanguage", targetLanguage
            ));
            
        } catch (Exception e) {
            log.error("Error translating prescription", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "Error translating prescription: " + e.getMessage()
            ));
        }
    }
}
