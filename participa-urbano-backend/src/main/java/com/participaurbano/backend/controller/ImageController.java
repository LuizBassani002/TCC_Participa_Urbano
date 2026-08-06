package com.participaurbano.backend.controller;

import com.participaurbano.backend.service.StorageService;
import io.minio.GetObjectResponse;
import io.minio.StatObjectResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/imagens")
public class ImageController {

    private final StorageService storageService;

    public ImageController(StorageService storageService) {
        this.storageService = storageService;
    }

    @GetMapping("/{objectName}")
    public ResponseEntity<byte[]> getImage(@PathVariable String objectName) {
        try {
            StatObjectResponse stat = storageService.statObject(objectName);
            String contentType = stat.contentType();

            GetObjectResponse objectResponse = storageService.getObject(objectName);
            byte[] data = objectResponse.readAllBytes();
            objectResponse.close();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType != null ? contentType : "image/jpeg"));
            headers.setContentLength(data.length);
            // Permite que o browser (Flutter Web) carregue a imagem sem bloqueio CORS
            //headers.add(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*");

            return new ResponseEntity<>(data, headers, HttpStatus.OK);

        } catch (Exception e) {
            System.err.println("Erro ao buscar imagem '" + objectName + "' do MinIO: " + e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
}
