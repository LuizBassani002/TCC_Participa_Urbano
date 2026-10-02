package com.participaurbano.backend.config;

import com.participaurbano.backend.repository.UsuarioRepository;
import com.participaurbano.backend.service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// Avisa ao Spring que essa classe é um componente gerenciado por ele (um "bean" automático),
// então ele pode ser injetado em outras classes, como vimos no SecurityConfig
@Component
// OncePerRequestFilter garante que esse filtro rode UMA ÚNICA VEZ por requisição
// (evita que o mesmo token seja validado várias vezes por engano na mesma chamada)
public class SecurityFilter extends OncePerRequestFilter {

    // Serviço responsável por validar/ler o token JWT
    private final TokenService tokenService;

    // Repositório usado para buscar o usuário no banco de dados a partir do e-mail (login)
    private final UsuarioRepository usuarioRepository;

    // Construtor: o Spring injeta automaticamente essas duas dependências
    public SecurityFilter(TokenService tokenService, UsuarioRepository usuarioRepository) {
        this.tokenService = tokenService;
        this.usuarioRepository = usuarioRepository;
    }

    // Método principal do filtro: executado automaticamente em TODA requisição que chega na API
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        
        // Tenta extrair o token JWT do header "Authorization" da requisição
        String token = recoverToken(request);

        // Só continua a validação se realmente veio um token na requisição
        if (token != null) {
            
            // Valida o token e extrai o "login" (e-mail) do usuário contido nele
            String login = tokenService.validateToken(token);

            // Se o token for válido, o login (e-mail) não virá vazio
            if (!login.isEmpty()) {
                
                // Busca no banco de dados o usuário correspondente a esse e-mail
                UserDetails user = usuarioRepository.findByEmail(login).orElse(null);
                
                // Se o usuário realmente existir no banco...
                if (user != null) {
                    
                    // Cria um objeto de autenticação do Spring Security, contendo o usuário
                    // e suas permissões/roles (user.getAuthorities())
                    UsernamePasswordAuthenticationToken authentication = 
                        new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                    
                    // "Registra" esse usuário como autenticado no contexto de segurança da requisição atual
                    // É isso que permite ao Spring, mais adiante, saber quem está fazendo a requisição
                    // e aplicar as regras de hasRole("GESTOR") / authenticated() do SecurityConfig
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        }
        
        // Deixa a requisição continuar seu caminho normal, para os próximos filtros/Controllers
        // (acontece independente de ter validado um usuário ou não — quem decide bloquear é o SecurityConfig)
        filterChain.doFilter(request, response);
    }

    // Método auxiliar que extrai o token puro do header "Authorization"
    private String recoverToken(HttpServletRequest request) {
        
        // Lê o header "Authorization" da requisição (ex: "Bearer eyJhbGciOiJIUzI1NiIs...")
        String authHeader = request.getHeader("Authorization");
        
        // Se o header não existir, ou não começar com "Bearer ", não há token válido a extrair
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return null;
        
        // Remove o prefixo "Bearer " e retorna só o token puro
        return authHeader.replace("Bearer ", "");
    }
}