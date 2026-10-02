package com.participaurbano.backend.service;

import com.participaurbano.backend.domain.entity.Ocorrencia;
import com.participaurbano.backend.domain.enums.Categoria;
import com.participaurbano.backend.domain.enums.Prioridade;
import com.participaurbano.backend.repository.OcorrenciaRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class PriorizacaoService {

    private final OcorrenciaRepository ocorrenciaRepository;

    // Approximations for ~1km in latitude and longitude
    private static final double LAT_KM = 0.009;
    private static final double LON_KM = 0.010; // Highly dependent on latitude, but good enough for naive approach

    public PriorizacaoService(OcorrenciaRepository ocorrenciaRepository) {
        this.ocorrenciaRepository = ocorrenciaRepository;
    }

    public Prioridade calcularPrioridade(Ocorrencia ocorrencia) {
        int score = 0;

        // 1. Categoria (Gravidade do problema)
        score += getCategoryWeight(ocorrencia.getCategoria());

        // 2. Recorrência (Volume na mesma região - aprox. 1km raio)
        // Monta uma "caixa" (área retangular) de aproximadamente 1km ao redor
        // do ponto da ocorrência: pega a coordenada e soma/subtrai a margem de 1km
        if (ocorrencia.getLatitude() != null && ocorrencia.getLongitude() != null) {    
            Double latStart = ocorrencia.getLatitude() - LAT_KM;
            Double latEnd = ocorrencia.getLatitude() + LAT_KM;
            Double lonStart = ocorrencia.getLongitude() - LON_KM;
            Double lonEnd = ocorrencia.getLongitude() + LON_KM;

            List<Ocorrencia> pRoximas = ocorrenciaRepository.findByLatitudeBetweenAndLongitudeBetween(latStart, latEnd, lonStart, lonEnd);
            
            // Score aumenta com base no número de ocorrências próximas não resolvidas
            long naoResolvidasProximas = pRoximas.stream()
                .filter(o -> o.getStatus() != com.participaurbano.backend.domain.enums.StatusOcorrencia.RESOLVIDA)
                .count();
                
            score += Math.min(naoResolvidasProximas * 2, 30); // Cap in 20 points
        }

        // 3. Tempo decorrido (Para testes unitários, podemos passar ocorrências antigas e ver a mudança)
        if (ocorrencia.getDataCriacao() != null) {
            long daysOld = ChronoUnit.DAYS.between(ocorrencia.getDataCriacao(), LocalDateTime.now());
            score += daysOld * 2; // +2 points per day open
        }

        return scoreToPrioridade(score);
    }

    private int getCategoryWeight(Categoria categoria) {
        if (categoria == null) return 5;
        
        return switch (categoria) {
            case SEGURANCA -> 25; 
            case INFRAESTRUTURA -> 20;
            case MEIO_AMBIENTE -> 15;
            case ILUMINACAO_PUBLICA, LIMPEZA_PUBLICA -> 10;
            case OUTROS -> 5;
        };
    }

    private Prioridade scoreToPrioridade(int score) {
        if (score < 15) return Prioridade.BAIXA;
        if (score < 30) return Prioridade.MEDIA;
        if (score < 50) return Prioridade.ALTA;
        return Prioridade.CRITICA;
    }
}
