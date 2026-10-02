package com.participaurbano.backend.controller;

import com.participaurbano.backend.service.StorageService;
import io.minio.GetObjectResponse;
import io.minio.StatObjectResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// Marca a classe como Controller REST: responde diretamente com dados, não páginas HTML
@RestController
// Todas as rotas desse Controller começam com /api/imagens
// (bate exatamente com a URL que o Flutter monta: '${ApiService.baseUrl}/imagens/$fileName')
@RequestMapping("/api/imagens")
public class ImageController {

    // Serviço responsável por conversar com o MinIO (buscar/enviar arquivos)
    private final StorageService storageService;

    // Construtor: o Spring injeta automaticamente o StorageService configurado
    public ImageController(StorageService storageService) {
        this.storageService = storageService;
    }

    // Atende requisições GET em /api/imagens/{objectName}
    // {objectName} é um parâmetro dinâmico na URL — por exemplo: /api/imagens/foto123.jpg
    @GetMapping("/{objectName}")
    public ResponseEntity<byte[]> getImage(@PathVariable String objectName) {
        // @PathVariable captura o valor que veio na URL (ex: "foto123.jpg") e joga na variável objectName
        try {
            // Pergunta ao MinIO os METADADOS do arquivo (sem baixar o conteúdo ainda),
            // principalmente para descobrir qual é o tipo do arquivo (ex: image/png, image/jpeg)
            StatObjectResponse stat = storageService.statObject(objectName);
            String contentType = stat.contentType();

            // Agora sim, busca o CONTEÚDO real do arquivo (os bytes da imagem) no MinIO
            GetObjectResponse objectResponse = storageService.getObject(objectName);

            // Lê todos os bytes da imagem para um array, carregando o conteúdo na memória
            byte[] data = objectResponse.readAllBytes();

            // Fecha a conexão/stream com o MinIO, liberando recursos (boa prática, evita vazamento de conexão)
            objectResponse.close();

            // Monta os cabeçalhos HTTP da resposta que será devolvida ao Flutter
            HttpHeaders headers = new HttpHeaders();

            // Define o Content-Type correto (ex: image/jpeg), para o navegador/app saber que é uma imagem
            // e exibi-la corretamente. Se o MinIO não informar o tipo, usa "image/jpeg" como padrão de segurança
            headers.setContentType(MediaType.parseMediaType(contentType != null ? contentType : "image/jpeg"));

            // Informa o tamanho exato do arquivo em bytes (ajuda o cliente a saber quando o download terminou)
            headers.setContentLength(data.length);

            // (Comentado/desativado) Esse header, se ativado, liberaria explicitamente o CORS pra "qualquer origem".
            // Está desnecessário aqui porque a rota já foi liberada de forma mais ampla lá no SecurityConfig
            // (permitAll para GET /api/imagens/**) e a configuração global de CORS já cobre isso.
            //headers.add(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*");

            // Devolve HTTP 200 (OK), com os bytes da imagem no corpo da resposta e os headers configurados
            return new ResponseEntity<>(data, headers, HttpStatus.OK);

        } catch (Exception e) {
            // Se der qualquer erro (arquivo não existe no MinIO, MinIO fora do ar, nome inválido, etc.),
            // registra o erro no log do servidor...
            System.err.println("Erro ao buscar imagem '" + objectName + "' do MinIO: " + e.getMessage());

            // ...e devolve HTTP 404 (Not Found) para o cliente, indicando que a imagem não foi encontrada
            return ResponseEntity.notFound().build();
        }
    }
}