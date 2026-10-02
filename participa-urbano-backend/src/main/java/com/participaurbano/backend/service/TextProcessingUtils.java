package com.participaurbano.backend.service;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Classe utilitária responsável pela pré-processamento de texto (NLP pipeline).
 * Higieniza as descrições dos usuários para alimentar o modelo de classificação.
 */
public class TextProcessingUtils {

    // Lista estática de "Stop Words" em português: palavras muito comuns que 
    // não agregam significado relevante para a classificação semântica do texto.
    private static final List<String> STOP_WORDS = Arrays.asList(
            "o", "a", "os", "as", "um", "uma", "uns", "umas",
            "de", "do", "da", "dos", "das", "em", "no", "na",
            "nos", "nas", "por", "para", "com", "sem", "que",
            "e", "ou", "mas", "porem", "entao", "como", "quando",
            "onde", "porque", "se", "isso", "isto", "aquilo",
            "este", "esta", "esse", "essa"
    );

    /**
     * Normaliza um texto: remove acentos, caracteres especiais, pontuações,
     * converte para minúsculas e filtra as stop words.
     *
     * @param text O texto original enviado na descrição da ocorrência.
     * @return Lista de palavras ("tokens") limpas e prontas para classificação.
     */
    public static List<String> processText(String text) {
        // Validação preventiva: se o texto for nulo ou vazio, retorna uma lista vazia
        if (text == null || text.trim().isEmpty()) {
            return List.of();
        }

        // 1. Desmembro os caracteres acentuados (ex: "é" vira "e" + acento agudo)
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD);
        
        // 2. Remove todos os diacríticos (acentos e til) restantes via regex
        normalized = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");

        // 3. Remove pontuações/números (mantém só letras e espaços) e converte tudo para minúsculas
        normalized = normalized.replaceAll("[^a-zA-Z\\s]", " ").toLowerCase();

        // 4. Tokenização e Filtragem via Java Streams:
        return Arrays.stream(normalized.split("\\s+")) // Quebra o texto em palavras a cada espaço
                .map(String::trim)                       // Remove espaços sobressalentes nas pontas
                .filter(word -> !word.isEmpty() && !STOP_WORDS.contains(word)) // Remove vazios e stop words
                .collect(Collectors.toList());           // Agrupa o resultado em uma lista de Strings
    }
}