package com.participaurbano.backend.service;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TextProcessingUtils {

    private static final List<String> STOP_WORDS = Arrays.asList(
            "o", "a", "os", "as", "um", "uma", "uns", "umas",
            "de", "do", "da", "dos", "das", "em", "no", "na",
            "nos", "nas", "por", "para", "com", "sem", "que",
            "e", "ou", "mas", "porem", "entao", "como", "quando",
            "onde", "porque", "se", "isso", "isto", "aquilo",
            "este", "esta", "esse", "essa"
    );

    /**
     * Normalizes text by removing accents, special characters, converting to lowercase
     * and removing common stop words.
     */
    public static List<String> processText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return List.of();
        }

        // Remove accents
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD);
        normalized = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");

        // Remove special characters, keep only letters and spaces, convert to lowercase
        normalized = normalized.replaceAll("[^a-zA-Z\\s]", " ").toLowerCase();

        // Split and remove stop words
        return Arrays.stream(normalized.split("\\s+"))
                .map(String::trim)
                .filter(word -> !word.isEmpty() && !STOP_WORDS.contains(word))
                .collect(Collectors.toList());
    }
}
