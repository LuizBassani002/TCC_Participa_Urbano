package com.participaurbano.backend.service;

import com.participaurbano.backend.domain.enums.Categoria;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class NLPClassificationService {

    private final Map<Categoria, List<String>> categoryKeywords;

    public NLPClassificationService() {
        categoryKeywords = new HashMap<>();
        categoryKeywords.put(Categoria.LIMPEZA_PUBLICA, List.of("lixo", "entulho", "sujeira", "mato", "limpeza", "fedendo", "animais", "barata", "rato"));
        categoryKeywords.put(Categoria.ILUMINACAO_PUBLICA, List.of("poste", "luz", "escuro", "lampada", "apagado", "iluminacao", "queimada"));
        categoryKeywords.put(Categoria.INFRAESTRUTURA, List.of("buraco", "asfalto", "calcada", "esgoto", "vazamento", "agua", "cano", "inundacao", "alagamento"));
        categoryKeywords.put(Categoria.MEIO_AMBIENTE, List.of("arvore", "poda", "rio", "poluicao", "cheiro", "esgoto", "fumaça", "queimada"));
        categoryKeywords.put(Categoria.SEGURANCA, List.of("assalto", "roubo", "policiamento", "perigoso", "suspeito", "tiro", "briga"));
    }

    public Categoria classify(String description) {
        List<String> processedWords = TextProcessingUtils.processText(description);

        if (processedWords.isEmpty()) {
            return Categoria.OUTROS;
        }

        Map<Categoria, Integer> scores = new HashMap<>();
        for (Categoria cat : categoryKeywords.keySet()) {
            scores.put(cat, 0);
        }

        // Count keyword matches
        for (String word : processedWords) {
            for (Map.Entry<Categoria, List<String>> entry : categoryKeywords.entrySet()) {
                if (entry.getValue().contains(word)) {
                    scores.put(entry.getKey(), scores.get(entry.getKey()) + 1);
                }
            }
        }

        // Find highest score
        Categoria bestMatch = Categoria.OUTROS;
        int highestScore = 0;

        for (Map.Entry<Categoria, Integer> entry : scores.entrySet()) {
            if (entry.getValue() > highestScore) {
                highestScore = entry.getValue();
                bestMatch = entry.getKey();
            }
        }

        // If no keywords matched, return OUTROS
        if (highestScore == 0) {
            return Categoria.OUTROS;
        }

        return bestMatch;
    }
}
