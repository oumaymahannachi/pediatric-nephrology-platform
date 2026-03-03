package tn.pedialink.prescription.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class TranslationService {

    @Value("${google.translate.api.key:}")
    private String googleTranslateApiKey;

    /**
     * Translate text to target language
     * This is a simplified implementation. In production, integrate with Google Translate API or DeepL
     */
    public String translate(String text, String targetLanguage) {
        log.info("Translating text to: {}", targetLanguage);
        
        // In production, call Google Translate API or DeepL here
        // For now, return simulated translations
        return simulateTranslation(text, targetLanguage);
    }

    /**
     * Translate prescription instructions to multiple languages
     */
    public Map<String, String> translateToMultipleLanguages(String text) {
        Map<String, String> translations = new HashMap<>();
        
        translations.put("fr", text); // Original French
        translations.put("ar", translate(text, "ar")); // Arabic
        translations.put("en", translate(text, "en")); // English
        
        return translations;
    }

    /**
     * Simulate translation (replace with actual API call in production)
     */
    private String simulateTranslation(String text, String targetLanguage) {
        // This is a placeholder. In production, you would:
        // 1. Call Google Translate API or DeepL API
        // 2. Return the translated text
        
        log.info("Simulating translation to: {}", targetLanguage);
        
        // Sample translations for common medical terms
        Map<String, Map<String, String>> translations = new HashMap<>();
        
        // French to Arabic
        Map<String, String> frToAr = new HashMap<>();
        frToAr.put("Prendre", "خذ");
        frToAr.put("comprimé", "قرص");
        frToAr.put("fois par jour", "مرات في اليوم");
        frToAr.put("avant les repas", "قبل الوجبات");
        frToAr.put("après les repas", "بعد الوجبات");
        frToAr.put("avec de l'eau", "مع الماء");
        frToAr.put("Ne pas dépasser", "لا تتجاوز");
        frToAr.put("jours", "أيام");
        frToAr.put("matin", "صباح");
        frToAr.put("midi", "ظهر");
        frToAr.put("soir", "مساء");
        
        // French to English
        Map<String, String> frToEn = new HashMap<>();
        frToEn.put("Prendre", "Take");
        frToEn.put("comprimé", "tablet");
        frToEn.put("fois par jour", "times per day");
        frToEn.put("avant les repas", "before meals");
        frToEn.put("après les repas", "after meals");
        frToEn.put("avec de l'eau", "with water");
        frToEn.put("Ne pas dépasser", "Do not exceed");
        frToEn.put("jours", "days");
        frToEn.put("matin", "morning");
        frToEn.put("midi", "noon");
        frToEn.put("soir", "evening");
        
        translations.put("ar", frToAr);
        translations.put("en", frToEn);
        
        if (targetLanguage.equals("fr")) {
            return text;
        }
        
        String translatedText = text;
        Map<String, String> targetTranslations = translations.get(targetLanguage);
        
        if (targetTranslations != null) {
            for (Map.Entry<String, String> entry : targetTranslations.entrySet()) {
                translatedText = translatedText.replace(entry.getKey(), entry.getValue());
            }
        }
        
        return translatedText;
    }

    /**
     * Get supported languages
     */
    public Map<String, String> getSupportedLanguages() {
        Map<String, String> languages = new HashMap<>();
        languages.put("fr", "Français");
        languages.put("ar", "العربية");
        languages.put("en", "English");
        return languages;
    }

    /**
     * Translate prescription object
     */
    public Map<String, Object> translatePrescription(Map<String, Object> prescription, String targetLanguage) {
        Map<String, Object> translated = new HashMap<>(prescription);
        
        // Translate diagnostic
        if (prescription.containsKey("diagnostic")) {
            translated.put("diagnostic", translate((String) prescription.get("diagnostic"), targetLanguage));
        }
        
        // Translate notes
        if (prescription.containsKey("notes")) {
            translated.put("notes", translate((String) prescription.get("notes"), targetLanguage));
        }
        
        // Translate medication instructions
        if (prescription.containsKey("medicaments")) {
            // Handle medication translation
            translated.put("medicaments", prescription.get("medicaments"));
        }
        
        return translated;
    }
}
