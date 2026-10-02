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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// Controller REST: responde com dados (JSON), não páginas HTML
@RestController
// Todas as rotas aqui começam com /api/ocorrencias
@RequestMapping("/api/ocorrencias")
public class OcorrenciaController {

    // Repositório para salvar/buscar ocorrências no banco de dados
    private final OcorrenciaRepository ocorrenciaRepository;

    // Serviço de IA/NLP que classifica automaticamente a categoria da ocorrência a partir da descrição
    private final NLPClassificationService classificationService;

    // Serviço que calcula a prioridade (BAIXA, MEDIA, ALTA, CRITICA) de uma ocorrência
    private final PriorizacaoService priorizacaoService;

    // Serviço que conversa com o MinIO para salvar as fotos enviadas
    private final StorageService storageService;

    // Repositório de usuários (usado, por exemplo, para comparar IDs em "minhas ocorrências")
    private final UsuarioRepository usuarioRepository;

    // Construtor: o Spring injeta automaticamente todas essas dependências
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

    // Atende POST em /api/ocorrencias, mas especificamente requisições do tipo multipart/form-data
    // (necessário porque o cidadão pode enviar arquivos/fotos junto com os dados de texto)
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Ocorrencia> createOcorrencia(
            // Cada @RequestParam captura um campo individual enviado no formulário multipart
            @RequestParam("descricao") String descricao,
            @RequestParam(value = "endereco", required = false) String endereco,     // opcional
            @RequestParam(value = "latitude", required = false) Double latitude,     // opcional
            @RequestParam(value = "longitude", required = false) Double longitude,   // opcional
            // @RequestPart captura a(s) foto(s) enviada(s) — pode vir uma lista de arquivos, também opcional
            @RequestPart(value = "fotos", required = false) List<MultipartFile> fotos) {

        // Recupera o usuário atualmente autenticado (que o SecurityFilter já validou e "registrou"
        // no contexto de segurança da requisição, lá no início do fluxo)
        Usuario loggedUser = (Usuario) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        // Cria uma nova entidade Ocorrencia e preenche com os dados recebidos
        Ocorrencia ocorrencia = new Ocorrencia();
        ocorrencia.setDescricao(descricao);
        ocorrencia.setEndereco(endereco);
        ocorrencia.setLatitude(latitude);
        ocorrencia.setLongitude(longitude);
        ocorrencia.setDataCriacao(LocalDateTime.now());   // marca a data/hora atual de criação
        ocorrencia.setCidadao(loggedUser);                // vincula a ocorrência ao usuário logado (o "dono")

        // 1. Classificação Inteligente: usa NLP para definir automaticamente a categoria
        // (ex: ILUMINACAO_PUBLICA, LIMPEZA_PUBLICA...) a partir do TEXTO da descrição,
        // sem o usuário precisar escolher manualmente
        ocorrencia.setCategoria(classificationService.classify(descricao));

        // 2. Motor de Priorização: calcula automaticamente o nível de urgência da ocorrência
        Prioridade prioridade = priorizacaoService.calcularPrioridade(ocorrencia);
        ocorrencia.setPrioridade(prioridade);

        // 3. Tratamento do Upload de Arquivos
        if (fotos != null && !fotos.isEmpty()) {
            // Para cada foto enviada...
            for (MultipartFile file : fotos) {
                // ...envia o arquivo para o MinIO através do StorageService, recebendo de volta o caminho salvo
                String filePath = storageService.storeFile(file);
                if (filePath != null) {
                    // Cria a entidade Foto com esse caminho e associa à ocorrência
                    Foto foto = new Foto(filePath);
                    ocorrencia.addFoto(foto);
                }
            }
        }

        // 4. Salva a ocorrência (já com categoria, prioridade e fotos) no banco de dados
        Ocorrencia saved = ocorrenciaRepository.save(ocorrencia);

        // Retorna HTTP 201 (Created), junto com o objeto salvo (incluindo o ID gerado)
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

    // Atende PATCH em /api/ocorrencias/{id}/status — usado pelo gestor para mudar o status
    @PatchMapping("/{id}/status")
    public ResponseEntity<Ocorrencia> updateStatus(
            @PathVariable Long id,                              // captura o {id} da URL
            @RequestParam("status") StatusOcorrencia novoStatus) { // captura o novo status via query parameter

        // Busca a ocorrência pelo ID; Optional é usado porque ela pode não existir
        Optional<Ocorrencia> ocorrenciaOpt = ocorrenciaRepository.findById(id);
        
        // Se não encontrar, devolve HTTP 404 (Not Found)
        if (ocorrenciaOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        // Se encontrou, extrai o objeto de dentro do Optional
        Ocorrencia ocorrencia = ocorrenciaOpt.get();

        // Atualiza o status
        ocorrencia.setStatus(novoStatus);
        
        // Salva a alteração no banco (update, já que o objeto já tem ID)
        Ocorrencia saved = ocorrenciaRepository.save(ocorrencia);

        // Devolve HTTP 200 (OK) com a ocorrência já atualizada
        return ResponseEntity.ok(saved);
    }

    // Atende DELETE em /api/ocorrencias/{id} — usado pelo gestor para excluir
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOcorrencia(@PathVariable Long id) {
        // Verifica se a ocorrência existe antes de tentar excluir
        if (!ocorrenciaRepository.existsById(id)) {
            return ResponseEntity.notFound().build();  // 404 se não existir
        }

        // Exclui do banco de dados
        ocorrenciaRepository.deleteById(id);

        // Devolve HTTP 204 (No Content): sucesso, mas sem corpo de resposta
        // (padrão comum para operações de exclusão bem-sucedidas)
        return ResponseEntity.noContent().build();
    }

    // Atende GET em /api/ocorrencias — retorna TODAS as ocorrências (rota restrita a GESTOR, lá no SecurityConfig)
    @GetMapping
    public ResponseEntity<List<Ocorrencia>> getAllOcorrencias() {
        return ResponseEntity.ok(buscarEOrtenarOcorrencias());
    }

    // Atende GET em /api/ocorrencias/publicas — retorna só as ocorrências "aprovadas" (não pendentes)
    @GetMapping("/publicas")
    public ResponseEntity<List<Ocorrencia>> getOcorrenciasPublicas() {
        // Pega todas as ocorrências (já recalculadas e ordenadas)
        List<Ocorrencia> todas = buscarEOrtenarOcorrencias();

        // Filtra removendo as que ainda estão com status PENDENTE (não aprovadas)
        List<Ocorrencia> aprovadas = todas.stream()
            .filter(o -> o.getStatus() != StatusOcorrencia.PENDENTE)
            // Reordena de novo por prioridade (crítica primeiro), só para garantir
            .sorted((o1, o2) -> Integer.compare(
                getPesoPrioridade(o2.getPrioridade()),
                getPesoPrioridade(o1.getPrioridade())
            ))
            .toList();
            
        return ResponseEntity.ok(aprovadas);
    }

    // Atende GET em /api/ocorrencias/minhas — retorna só as ocorrências do usuário logado
    @GetMapping("/minhas")
    public ResponseEntity<List<Ocorrencia>> getMinhasOcorrencias() {
        // Recupera o usuário autenticado a partir do contexto de segurança
        Usuario loggedUser = (Usuario) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        List<Ocorrencia> todas = buscarEOrtenarOcorrencias();

        // Filtra apenas as ocorrências cujo "dono" (cidadao) tem o mesmo ID do usuário logado
        List<Ocorrencia> minhas = todas.stream()
            .filter(o -> o.getCidadao() != null && o.getCidadao().getId().equals(loggedUser.getId()))
            .sorted((o1, o2) -> Integer.compare(
                getPesoPrioridade(o2.getPrioridade()),
                getPesoPrioridade(o1.getPrioridade())
            ))
            .toList();

        return ResponseEntity.ok(minhas);
    }

    // Método auxiliar privado: busca TODAS as ocorrências do banco e já as recalcula/ordena
    // (usado internamente pelos 3 métodos GET acima, para não repetir essa lógica 3 vezes)
    private List<Ocorrencia> buscarEOrtenarOcorrencias() {
        // Busca todas as ocorrências do banco e coloca numa lista "mutável" (ArrayList),
        // já que o retorno de findAll() pode ser uma lista imutável
        List<Ocorrencia> lista = new ArrayList<>(ocorrenciaRepository.findAll());

        // Para cada ocorrência da lista...
        for (Ocorrencia o : lista) {
            try {
                // ...recalcula a prioridade em tempo real (ela pode mudar, por exemplo,
                // se a regra de priorização depender de fatores como tempo desde a criação)
                Prioridade p = priorizacaoService.calcularPrioridade(o);
                if (p != null) {
                    o.setPrioridade(p);
                }
            } catch (Exception e) {
                // Se der erro no cálculo (ex: dado antigo/incompleto no banco),
                // e a ocorrência ainda não tiver nenhuma prioridade definida,
                // assume BAIXA como valor de segurança (fallback), evitando que a lista quebre
                if (o.getPrioridade() == null) {
                    o.setPrioridade(Prioridade.BAIXA);
                }
            }
        }

        // Ordena a lista inteira por peso de prioridade, da mais urgente (CRITICA) para a menos (BAIXA)
        lista.sort((o1, o2) -> Integer.compare(
            getPesoPrioridade(o2.getPrioridade()),
            getPesoPrioridade(o1.getPrioridade())
        ));

        return lista;
    }

    // Converte o enum Prioridade em um número, para permitir a ordenação
    private int getPesoPrioridade(Prioridade prioridade) {
        if (prioridade == null) return 1;  // valor de segurança, caso venha nulo
        // "switch expression" (sintaxe moderna do Java): retorna um valor direto para cada caso
        return switch (prioridade) {
            case CRITICA -> 4;
            case ALTA -> 3;
            case MEDIA -> 2;
            case BAIXA -> 1;
        };
    }
}