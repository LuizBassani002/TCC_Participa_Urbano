package com.participaurbano.backend.domain.entity;

import com.participaurbano.backend.domain.enums.Categoria;
import com.participaurbano.backend.domain.enums.Prioridade;
import com.participaurbano.backend.domain.enums.StatusOcorrencia;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;

// Marca a classe como entidade JPA, representando a tabela do banco
@Entity
// Define o nome da tabela como "ocorrencias"
@Table(name = "ocorrencias")
public class Ocorrencia {

    // Chave primária, gerada automaticamente pelo banco (auto-incremento)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Coluna do tipo TEXT (permite textos longos), obrigatória
    @Column(columnDefinition = "TEXT", nullable = false)
    private String descricao;

    // Coordenadas geográficas — sem restrições especiais, podem ser nulas
    // (lembra que no front-end vimos que nem toda ocorrência tem coordenadas válidas)
    private Double latitude;
    
    private Double longitude;

    // Endereço em texto livre, também pode ser longo (TEXT) e pode ser nulo
    @Column(columnDefinition = "TEXT")
    private String endereco;

    // @Enumerated(EnumType.STRING) faz o Hibernate salvar o enum como TEXTO no banco
    // (ex: salva "PENDENTE", "RESOLVIDA"...) em vez de salvar como número (0, 1, 2...),
    // o que deixa o banco muito mais legível e seguro contra mudanças na ordem do enum
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "VARCHAR(50)")
    private StatusOcorrencia status;

    // Mesma lógica para a categoria (definida automaticamente pelo NLPClassificationService)
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "VARCHAR(50)")
    private Categoria categoria;

    // Mesma lógica para a prioridade (calculada pelo PriorizacaoService)
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "VARCHAR(50)")
    private Prioridade prioridade;

    // Data de criação: obrigatória, e "updatable = false" impede que essa coluna
    // seja alterada em um UPDATE posterior (a data de criação nunca deve mudar depois de definida)
    @Column(name = "data_criacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    // Relacionamento "um para muitos": UMA ocorrência pode ter VÁRIAS fotos
    // mappedBy = "ocorrencia" -> indica que quem "controla" esse relacionamento no banco
    //             é o campo "ocorrencia" lá na classe Foto (a chave estrangeira mora na tabela fotos)
    // cascade = CascadeType.ALL -> qualquer operação feita na Ocorrencia (salvar, deletar) 
    //             é propagada automaticamente para as Fotos associadas
    // orphanRemoval = true -> se uma Foto for removida da lista (fotos.remove(...)),
    //             ela é automaticamente excluída do banco também (não fica "órfã")
    @OneToMany(mappedBy = "ocorrencia", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Foto> fotos = new ArrayList<>();

    // Evita que o objeto Usuario completo (com senha, etc.) seja exposto no JSON de resposta
    @JsonIgnore
    // Relacionamento "muitos para um": VÁRIAS ocorrências pertencem a UM cidadão
    // FetchType.LAZY -> só busca os dados do Usuario quando realmente forem acessados
    @ManyToOne(fetch = FetchType.LAZY)
    // Coluna de chave estrangeira "cidadao_id"; aceita nulo, para não quebrar dados antigos
    // que talvez tenham sido criados antes desse relacionamento existir no sistema
    @JoinColumn(name = "cidadao_id", nullable = true) // nullable to support old data
    private Usuario cidadao;

    // Método executado AUTOMATICAMENTE pelo Hibernate, exatamente ANTES de salvar
    // uma nova Ocorrencia no banco pela primeira vez (INSERT)
    @PrePersist
    public void prePersist() {
        // Se ninguém definiu a data de criação manualmente, define agora como "agora"
        if (this.dataCriacao == null) {
            this.dataCriacao = LocalDateTime.now();
        }
        // Se ninguém definiu um status inicial, toda ocorrência nova começa como PENDENTE
        if (this.status == null) {
            this.status = StatusOcorrencia.PENDENTE;
        }
    }

    // Método auxiliar para adicionar uma foto à ocorrência, mantendo os DOIS lados
    // do relacionamento sincronizados (a lista de fotos aqui, E a referência de volta na Foto)
    public void addFoto(Foto foto) {
        fotos.add(foto);           // adiciona a foto na lista desta ocorrência
        foto.setOcorrencia(this);  // e também informa à Foto qual é a sua ocorrência "dona"
    }

    // Mesmo raciocínio, só que para remover
    public void removeFoto(Foto foto) {
        fotos.remove(foto);
        foto.setOcorrencia(null);
    }

    // Getters e Setters padrão — usados pelo JPA/Hibernate (para ler/gravar no banco)
    // e pelo Jackson (para converter de/para JSON nas respostas da API)

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public String getEndereco() { return endereco; }
    public void setEndereco(String endereco) { this.endereco = endereco; }

    public StatusOcorrencia getStatus() { return status; }
    public void setStatus(StatusOcorrencia status) { this.status = status; }

    public Categoria getCategoria() { return categoria; }
    public void setCategoria(Categoria categoria) { this.categoria = categoria; }

    public Prioridade getPrioridade() { return prioridade; }
    public void setPrioridade(Prioridade prioridade) { this.prioridade = prioridade; }

    public LocalDateTime getDataCriacao() { return dataCriacao; }
    public void setDataCriacao(LocalDateTime dataCriacao) { this.dataCriacao = dataCriacao; }

    public List<Foto> getFotos() { return fotos; }
    public void setFotos(List<Foto> fotos) { this.fotos = fotos; }

    public Usuario getCidadao() { return cidadao; }
    public void setCidadao(Usuario cidadao) { this.cidadao = cidadao; }
}