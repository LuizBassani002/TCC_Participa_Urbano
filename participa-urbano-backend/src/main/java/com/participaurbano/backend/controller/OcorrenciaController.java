package com.participaurbano.backend.controller;

import com.participaurbano.backend.domain.entity.Foto;
import com.participaurbano.backend.domain.entity.Ocorrencia;
import com.participaurbano.backend.domain.enums.Prioridade;
import com.participaurbano.backend.repository.OcorrenciaRepository;
import com.participaurbano.backend.service.NLPClassificationService;
import com.participaurbano.backend.service.PriorizacaoService;
import com.participaurbano.backend.service.StorageService;
import com.participaurbano.backend.repository.UsuarioRepository;
import com.participaurbano.backend.domain.entity.Usuario;
import com.participaurbano.backend.domain.enums.StatusOcorrencia;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/ocorrencias")
public class OcorrenciaController {

    private final OcorrenciaRepository ocorrenciaRepository;
    private final NLPClassificationService classificationService;
    private final PriorizacaoService priorizacaoService;
    private final StorageService storageService;
    private final UsuarioRepository usuarioRepository;

    public OcorrenciaController(OcorrenciaRepository ocorrenciaRepository,
                                NLPClassificationService classificationService,
                                PriorizacaoService priorizacaoService,
                                StorageService storageService,
                                UsuarioRepository usuarioRepository) {
        this.ocorrenciaRepository = ocorrenciaRepository;
        this.classificationService = classificationService;
        this.priorizacaoService = priorizacaoService;
        this.storageService = storageService;
        this.usuarioRepository = usuarioRepository;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Ocorrencia> createOcorrencia(
            @RequestParam("descricao") String descricao,
            @RequestParam(value = "endereco", required = false) String endereco,
            @RequestParam(value = "latitude", required = false) Double latitude,
            @RequestParam(value = "longitude", required = false) Double longitude,
            @RequestPart(value = "fotos", required = false) List<MultipartFile> fotos) {

        // Get currently logged in user
        Usuario loggedUser = (Usuario) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Ocorrencia ocorrencia = new Ocorrencia();
        ocorrencia.setDescricao(descricao);
        ocorrencia.setEndereco(endereco);
        ocorrencia.setLatitude(latitude);
        ocorrencia.setLongitude(longitude);
        ocorrencia.setDataCriacao(LocalDateTime.now());
        ocorrencia.setCidadao(loggedUser); // Associating with the citizen

        // 1. Intelligent Classification
        ocorrencia.setCategoria(classificationService.classify(descricao));

        // 2. Prioritization Engine
        Prioridade prioridade = priorizacaoService.calcularPrioridade(ocorrencia);
        ocorrencia.setPrioridade(prioridade);

        // 3. Handle File Uploads
        if (fotos != null && !fotos.isEmpty()) {
            for (MultipartFile file : fotos) {
                String filePath = storageService.storeFile(file);
                if (filePath != null) {
                    Foto foto = new Foto(filePath);
                    ocorrencia.addFoto(foto);
                }
            }
        }

        // 4. Save to Database
        Ocorrencia saved = ocorrenciaRepository.save(ocorrencia);

        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

    // Endpoint available only for ROLE_FUNCIONARIO
    @PatchMapping("/{id}/status")
    public ResponseEntity<Ocorrencia> updateStatus(
            @PathVariable Long id,
            @RequestParam("status") StatusOcorrencia novoStatus) {

        Optional<Ocorrencia> ocorrenciaOpt = ocorrenciaRepository.findById(id);
        
        if (ocorrenciaOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Ocorrencia ocorrencia = ocorrenciaOpt.get();
        ocorrencia.setStatus(novoStatus);
        
        Ocorrencia saved = ocorrenciaRepository.save(ocorrencia);
        return ResponseEntity.ok(saved);
    }

    // Endpoint disponível apenas para ROLE_GESTOR (ver SecurityConfig)
    @GetMapping
    public ResponseEntity<List<Ocorrencia>> getAllOcorrencias() {
        return ResponseEntity.ok(ocorrenciaRepository.findAll());
    }

    // Endpoint disponível para qualquer usuário logado (cidadão ou gestor)
    // Mostra apenas ocorrências que o gestor já aprovou (status != PENDENTE)
    @GetMapping("/publicas")
    public ResponseEntity<List<Ocorrencia>> getOcorrenciasPublicas() {
        List<Ocorrencia> todas = ocorrenciaRepository.findAll();
        List<Ocorrencia> aprovadas = todas.stream()
            .filter(o -> o.getStatus() != StatusOcorrencia.PENDENTE)
            .toList();
        return ResponseEntity.ok(aprovadas);
    }

    // Endpoint disponível para qualquer usuário logado
    @GetMapping("/minhas")
    public ResponseEntity<List<Ocorrencia>> getMinhasOcorrencias() {
        Usuario loggedUser = (Usuario) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        List<Ocorrencia> todas = ocorrenciaRepository.findAll();
        List<Ocorrencia> minhas = todas.stream()
            .filter(o -> o.getCidadao() != null && o.getCidadao().getId().equals(loggedUser.getId()))
            .toList();

        return ResponseEntity.ok(minhas);
    }
}
