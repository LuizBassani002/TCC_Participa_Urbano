package com.participaurbano.backend.config;

// Importações dos módulos do Spring Framework e Spring Security
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration // Indica ao Spring que esta classe contém configurações de beans da aplicação
@EnableWebSecurity // Habilita a segurança web do Spring Security na aplicação
public class SecurityConfig {

    // Injeção do filtro customizado que intercepta e valida os tokens JWT
    private final SecurityFilter securityFilter;

    // Construtor para injeção de dependência do SecurityFilter
    public SecurityConfig(SecurityFilter securityFilter) {
        this.securityFilter = securityFilter;
    }

    @Bean // Define a cadeia de filtros de segurança (Security Filter Chain)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // Aplica a configuração global de CORS definida no bean corsConfigurationSource()
                .cors(Customizer.withDefaults())
                
                // Desativa a proteção CSRF, pois APIs REST stateless baseadas em JWT não usam cookies de sessão
                .csrf(csrf -> csrf.disable())
                
                // Configura a gestão de sessão para STATELESS (sem estado em memória/sem uso de HTTP Session)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                
                // Início do mapeamento de permissões de acesso às rotas
                .authorizeHttpRequests(authorize -> authorize
                        // Libera requisições preflight (OPTIONS) enviadas pelos navegadores
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        
                        // Rotas públicas de autenticação (Login e Cadastro)
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/registrar").permitAll()
                        
                        // Rota pública para visualização/carregamento de imagens
                        .requestMatchers(HttpMethod.GET, "/api/imagens/**").permitAll()
                        
                        // --- REGRAS DE GESTÃO (RESTRITAS A USUÁRIOS COM ROLE 'GESTOR') ---
                        // Apenas Gestor pode alterar status de ocorrências via PATCH
                        .requestMatchers(HttpMethod.PATCH, "/api/ocorrencias/*/status").hasRole("GESTOR")
                        
                        // Apenas Gestor pode deletar ocorrências via DELETE
                        .requestMatchers(HttpMethod.DELETE, "/api/ocorrencias/*").hasRole("GESTOR")
                        
                        // Apenas Gestor pode listar todas as ocorrências do sistema via GET
                        .requestMatchers(HttpMethod.GET, "/api/ocorrencias").hasRole("GESTOR")
                        
                        // --- REGRAS PÚBLICAS OU DO CIDADÃO AUTENTICADO ---
                        // Exige apenas que o usuário esteja autenticado para ver ocorrências públicas
                        .requestMatchers(HttpMethod.GET, "/api/ocorrencias/publicas").authenticated()
                        
                        // Exige autenticação para que o cidadão veja suas próprias ocorrências
                        .requestMatchers(HttpMethod.GET, "/api/ocorrencias/minhas").authenticated()
                        
                        // Qualquer outra rota não mapeada explicitamente exige autenticação
                        .anyRequest().authenticated()
                )
                
                // Adiciona o filtro de JWT antes do filtro padrão de autenticação por usuário e senha
                .addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class)
                
                // Constrói e retorna a cadeia de segurança configurada
                .build();
    }

    @Bean // Configura as regras de permissão para CORS (Cross-Origin Resource Sharing)
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Libera chamadas de qualquer origem (útil para desenvolvimento Flutter Web/Mobile)
        configuration.setAllowedOriginPatterns(List.of("*"));
        
        // Define quais métodos HTTP são permitidos nas requisições
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        
        // Libera todos os cabeçalhos (headers), incluindo o Authorization para envio do JWT
        configuration.setAllowedHeaders(List.of("*"));
        
        // Permite envio de credenciais/tokens nas requisições
        configuration.setAllowCredentials(true);

        // Registra as regras de CORS para todas as rotas do backend ("/**")
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean // Expõe o Gerenciador de Autenticação do Spring Security para ser usado no AuthController
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean // Define o algoritmo BCrypt para criptografar senhas antes de salvar no banco
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}