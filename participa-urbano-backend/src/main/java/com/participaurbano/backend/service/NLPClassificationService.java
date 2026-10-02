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

        // LIMPEZA PÚBLICA: Termos específicos de acúmulo, resíduos e manutenção higiênica
        categoryKeywords.put(Categoria.LIMPEZA_PUBLICA, List.of(
            "lixo", "entulho", "sujeira", "mato", "limpeza", "fedendo", "animais", 
            "barata", "rato", "varricao", "capina", "descarte", "boca", "lobo", "vespas", "abelhas"
        ));

        // ILUMINAÇÃO PÚBLICA: Elementos de rede elétrica e iluminação de vias
        categoryKeywords.put(Categoria.ILUMINACAO_PUBLICA, List.of(
            "poste", "luz", "escuro", "lampada", "apagado", "iluminacao", "queimada", "luminaria"
        ));

        // INFRAESTRUTURA: Removidas palavras ambíguas de local ("calcada", "esgoto") 
        // Mantidos apenas termos focados em patologias da via e obras
        categoryKeywords.put(Categoria.INFRAESTRUTURA, List.of(
            "buraco", "asfalto", "vazamento", "cano", "inundacao", "alagamento", 
            "tapa", "cratera", "recapeamento", "sarjeta", "sarjetao", "galeria", "drenagem", "guias"
        ));

        // MEIO AMBIENTE: Poda, preservação e vegetação urbana
        categoryKeywords.put(Categoria.MEIO_AMBIENTE, List.of(
            "arvore", "poda", "rio", "poluicao", "cheiro", "fumaca", "praca", "parque", "raiz"
        ));

        // SEGURANÇA: Ocorrências de ordem pública
        categoryKeywords.put(Categoria.SEGURANCA, List.of(
            "assalto", "roubo", "policiamento", "perigoso", "suspeito", "tiro", "briga", "vandalismo"
        ));
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

        // Pontuação por frequência de termos das palavras processadas
        for (String word : processedWords) {
            for (Map.Entry<Categoria, List<String>> entry : categoryKeywords.entrySet()) {
                if (entry.getValue().contains(word)) {
                    scores.put(entry.getKey(), scores.get(entry.getKey()) + 1);
                }
            }
        }

        Categoria bestMatch = Categoria.OUTROS;
        int highestScore = 0;

        for (Map.Entry<Categoria, Integer> entry : scores.entrySet()) {
            if (entry.getValue() > highestScore) {
                highestScore = entry.getValue();
                bestMatch = entry.getKey();
            }
        }

        if (highestScore == 0) {
            return Categoria.OUTROS;
        }

        return bestMatch;
    }
}