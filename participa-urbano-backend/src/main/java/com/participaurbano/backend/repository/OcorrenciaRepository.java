package com.participaurbano.backend.repository;

import com.participaurbano.backend.domain.entity.Ocorrencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OcorrenciaRepository extends JpaRepository<Ocorrencia, Long> {

    // Método para trazer todas as ocorrências ordenadas pela Prioridade mais alta
    List<Ocorrencia> findAllByOrderByPrioridadeDescDataCriacaoDesc();

    List<Ocorrencia> findByLatitudeBetweenAndLongitudeBetween(
            Double latStart, Double latEnd, Double lonStart, Double lonEnd);
            
    List<Ocorrencia> findByStatusNotAndDataCriacaoBefore(com.participaurbano.backend.domain.enums.StatusOcorrencia status, LocalDateTime date);
}