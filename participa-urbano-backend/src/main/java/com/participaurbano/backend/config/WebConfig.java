package com.participaurbano.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// WebConfig agora é vazio pois os arquivos são servidos pelo MinIO diretamente.
// A configuração de resource handler local foi removida.
@Configuration
public class WebConfig implements WebMvcConfigurer {
}
