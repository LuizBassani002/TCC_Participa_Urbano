package com.participaurbano.backend.service;

// Importações do SDK Oficial do MinIO
import io.minio.*;
import io.minio.http.Method;
import io.minio.messages.Bucket;

// Importações de anotações e utilitários do Spring Boot
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.util.UUID;

@Service // Registra a classe como um serviço do Spring no container de IoC
public class StorageService {

    // Injeta a URL base do servidor MinIO configurada no application.properties
    @Value("${minio.url}")
    private String minioUrl;

    // Injeta a chave de acesso (usuário) do MinIO
    @Value("${minio.access-key}")
    private String accessKey;

    // Injeta a chave secreta (senha) do MinIO
    @Value("${minio.secret-key}")
    private String secretKey;

    // Injeta o nome do bucket onde os arquivos das ocorrências serão armazenados
    @Value("${minio.bucket-name}")
    private String bucketName;

    // Cliente interno do SDK para disparar comandos HTTP/S3 para o servidor MinIO
    private MinioClient minioClient;

    /**
     * Método executado automaticamente pelo Spring LOGO APÓS a criação do Bean.
     * Prepara o cliente S3, verifica a existência do bucket e aplica políticas de acesso.
     */
    @PostConstruct
    public void init() {
        // Constrói o cliente de conexão apontando para o endpoint e credenciais configuradas
        minioClient = MinioClient.builder()
                .endpoint(minioUrl)
                .credentials(accessKey, secretKey)
                .build();

        try {
            // Checa no servidor remoto se o bucket configurado já foi criado
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucketName).build()
            );

            if (!exists) {
                // Cria o bucket caso ele ainda não exista no servidor
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(bucketName).build()
                );

                // Define uma política em formato JSON permitindo leitura pública direta (s3:GetObject)
                String policy = "{"
                        + "\"Version\":\"2012-10-17\","
                        + "\"Statement\":[{"
                        + "\"Effect\":\"Allow\","
                        + "\"Principal\":{\"AWS\":[\"*\"]},"
                        + "\"Action\":[\"s3:GetObject\"],"
                        + "\"Resource\":[\"arn:aws:s3:::" + bucketName + "/*\"]"
                        + "}]}";

                // Aplica a política de leitura pública ao bucket recém-criado
                minioClient.setBucketPolicy(
                        SetBucketPolicyArgs.builder()
                                .bucket(bucketName)
                                .config(policy)
                                .build()
                );
                System.out.println("Bucket '" + bucketName + "' criado com política pública de leitura.");
            } else {
                System.out.println("Bucket '" + bucketName + "' já existe.");
            }
        } catch (Exception e) {
            // Lança uma exceção de runtime interrompendo a subida da aplicação se o MinIO falhar
            throw new RuntimeException("Erro ao inicializar conexão com MinIO: " + e.getMessage(), e);
        }
    }

    /**
     * Recebe um arquivo via Multipart, gera um UUID único e faz o upload para o MinIO.
     */
    public String storeFile(MultipartFile file) {
        // Validação preventiva: ignora chamadas vazias ou sem arquivo
        if (file == null || file.isEmpty()) {
            return null;
        }

        try {
            // Extrai o nome original do arquivo enviado pelo usuário
            String originalName = file.getOriginalFilename();
            String extension = "";

            // Preserva a extensão (.jpg, .png) do arquivo original
            if (originalName != null && originalName.contains(".")) {
                extension = originalName.substring(originalName.lastIndexOf("."));
            }

            // Gera um UUID único evitando sobrescrever arquivos com o mesmo nome no bucket
            String objectName = UUID.randomUUID().toString() + extension;

            // Envia o stream do arquivo para o bucket do MinIO
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            // Retorna o identificador único (nome do objeto) gerado para salvar no banco
            return objectName;

        } catch (Exception e) {
            throw new RuntimeException("Erro ao fazer upload para MinIO: " + e.getMessage(), e);
        }
    }

    /**
     * Busca os dados binários do arquivo no MinIO para permitir streaming ao cliente.
     */
    public GetObjectResponse getObject(String objectName) throws Exception {
        // Higieniza o identificador caso tenha recebido o caminho completo
        if (objectName.contains("/")) {
            objectName = objectName.substring(objectName.lastIndexOf("/") + 1);
        }
        // Faz o download do fluxo de dados do objeto do bucket
        return minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .build()
        );
    }

    /**
     * Recupera os metadados do arquivo (Tamanho, Content-Type, ETag) salvos no MinIO.
     */
    public StatObjectResponse statObject(String objectName) throws Exception {
        // Higieniza a string tratando caminhos absolutos
        if (objectName.contains("/")) {
            objectName = objectName.substring(objectName.lastIndexOf("/") + 1);
        }
        // Consulta apenas os cabeçalhos/metadados do objeto no servidor S3
        return minioClient.statObject(
                StatObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .build()
        );
    }
}