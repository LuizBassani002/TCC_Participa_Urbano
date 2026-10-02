package com.participaurbano.backend.domain.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

// Marca essa classe como uma Entidade JPA: ela representa uma tabela no banco de dados
@Entity
// Define o nome exato da tabela no banco como "fotos"
@Table(name = "fotos")
public class Foto {

    // Marca esse campo como a CHAVE PRIMÁRIA da tabela
    @Id
    // Define que o valor do ID é gerado automaticamente pelo próprio banco de dados
    // (estratégia IDENTITY = auto-incremento, comum em bancos como MySQL/PostgreSQL)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Define uma coluna chamada "caminho_url" no banco, que não pode ser nula,
    // com tamanho máximo de 1000 caracteres (espaço suficiente para uma URL/caminho longo)
    @Column(name = "caminho_url", nullable = false, length = 1000)
    private String caminhoUrl;

    // Impede que esse campo apareça quando o objeto Foto for convertido para JSON
    // (evita loop infinito: Foto -> Ocorrencia -> lista de Fotos -> Ocorrencia -> ...)
    @JsonIgnore
    // Define um relacionamento "muitos para um": MUITAS fotos pertencem a UMA ocorrência
    // FetchType.LAZY = só busca os dados da Ocorrencia relacionada quando forem realmente acessados
    // (evita carregar dados desnecessários do banco o tempo todo, otimizando performance)
    @ManyToOne(fetch = FetchType.LAZY)
    // Define a coluna de chave estrangeira no banco: "ocorrencia_id", que não pode ser nula
    // (toda foto OBRIGATORIAMENTE pertence a uma ocorrência)
    @JoinColumn(name = "ocorrencia_id", nullable = false)
    private Ocorrencia ocorrencia;

    // Construtor vazio: EXIGIDO pelo JPA/Hibernate para conseguir instanciar o objeto
    // internamente (por reflexão) ao buscar dados do banco
    public Foto() {}

    // Construtor "de conveniência": permite criar uma Foto passando só o caminho,
    // usado no OcorrenciaController quando uma foto é salva no MinIO (new Foto(filePath))
    public Foto(String caminhoUrl) {
        this.caminhoUrl = caminhoUrl;
    }

    // Getters e Setters: métodos padrão para ler e alterar os campos privados da classe
    // (o JPA/Hibernate e o Jackson (conversor de JSON) usam esses métodos nos bastidores)

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCaminhoUrl() {
        return caminhoUrl;
    }

    public void setCaminhoUrl(String caminhoUrl) {
        this.caminhoUrl = caminhoUrl;
    }

    public Ocorrencia getOcorrencia() {
        return ocorrencia;
    }

    public void setOcorrencia(Ocorrencia ocorrencia) {
        this.ocorrencia = ocorrencia;
    }
}