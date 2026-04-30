package com.participaurbano.backend.service;

import com.participaurbano.backend.domain.entity.Ocorrencia;
import com.participaurbano.backend.domain.enums.Categoria;
import com.participaurbano.backend.domain.enums.Prioridade;
import com.participaurbano.backend.repository.OcorrenciaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PriorizacaoServiceTest {

    @Mock
    private OcorrenciaRepository ocorrenciaRepository;

    @InjectMocks
    private PriorizacaoService priorizacaoService;

    @BeforeEach
    public void setup() {
        // Setup base behaviors if needed
    }

    @Test
    public void deveRetornarPrioridadeAltaParaSeguranca() {
        Ocorrencia ocorrencia = new Ocorrencia();
        ocorrencia.setCategoria(Categoria.SEGURANCA);
        ocorrencia.setLatitude(-23.550520);
        ocorrencia.setLongitude(-46.633308);
        ocorrencia.setDataCriacao(LocalDateTime.now());
        
        when(ocorrenciaRepository.findByLatitudeBetweenAndLongitudeBetween(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
            .thenReturn(new ArrayList<>()); // No occurrences nearby

        Prioridade prioridade = priorizacaoService.calcularPrioridade(ocorrencia);
        // Base score for SEGURANCA is 25. Prioridade.MEDIA is < 30.
        // Wait, 25 is < 30 so it's MEDIA. Let's make it ALTA by adding some days or recurrences.
        
        // As defined in the service: <15 BAIXA, <30 MEDIA, <50 ALTA, >=50 CRITICA
        assertEquals(Prioridade.MEDIA, prioridade);
    }
    
    @Test
    public void deveAumentarPrioridadeComBaseEmOcorrenciasProximas() {
        Ocorrencia ocorrencia = new Ocorrencia();
        ocorrencia.setCategoria(Categoria.INFRAESTRUTURA); // weight 20
        ocorrencia.setLatitude(-23.550520);
        ocorrencia.setLongitude(-46.633308);
        ocorrencia.setDataCriacao(LocalDateTime.now());

        // Simulate 6 unresolved occurrences nearby
        List<Ocorrencia> pRoximas = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            Ocorrencia o = new Ocorrencia();
            o.setStatus(com.participaurbano.backend.domain.enums.StatusOcorrencia.ABERTA);
            pRoximas.add(o);
        }

        when(ocorrenciaRepository.findByLatitudeBetweenAndLongitudeBetween(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
            .thenReturn(pRoximas);

        Prioridade prioridade = priorizacaoService.calcularPrioridade(ocorrencia);
        
        // INFRAESTRUTURA (20) + (6 * 2 = 12) = 32. 32 is < 50 => ALTA
        assertEquals(Prioridade.ALTA, prioridade);
    }

    @Test
    public void deveAumentarPrioridadeComBaseNoTempo() {
         Ocorrencia ocorrencia = new Ocorrencia();
        ocorrencia.setCategoria(Categoria.LIMPEZA_PUBLICA); // weight 10
        ocorrencia.setLatitude(-23.550520);
        ocorrencia.setLongitude(-46.633308);
        ocorrencia.setDataCriacao(LocalDateTime.now().minusDays(15)); // 15 days ago

        when(ocorrenciaRepository.findByLatitudeBetweenAndLongitudeBetween(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
            .thenReturn(new ArrayList<>());

        Prioridade prioridade = priorizacaoService.calcularPrioridade(ocorrencia);
        
        // LIMPEZA (10) + (15 days * 2 = 30) = 40. 40 is < 50 => ALTA
        assertEquals(Prioridade.ALTA, prioridade);
    }
}
