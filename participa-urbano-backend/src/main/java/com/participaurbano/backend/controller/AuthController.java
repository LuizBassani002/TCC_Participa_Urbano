package com.participaurbano.backend.controller;

import com.participaurbano.backend.controller.dto.AuthResponseDTO;
import com.participaurbano.backend.controller.dto.LoginDTO;
import com.participaurbano.backend.controller.dto.RegisterDTO;
import com.participaurbano.backend.domain.entity.Usuario;
import com.participaurbano.backend.domain.enums.Role;
import com.participaurbano.backend.repository.UsuarioRepository;
import com.participaurbano.backend.service.TokenService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller REST responsável pelo fluxo de autenticação e registro de usuários.
 * Expõe os endpoints públicos de Login e Cadastro.
 */
@RestController
@RequestMapping("/api/auth") // Mapeia a rota base para todos os endpoints de autenticação
public class AuthController {

    // Gerenciador nativo do Spring Security para efetuar a validação de credenciais
    private final AuthenticationManager authenticationManager;

    // Repositório para consulta e persistência de usuários no banco
    private final UsuarioRepository usuarioRepository;

    // Serviço para emissão do Token JWT após autenticação bem-sucedida
    private final TokenService tokenService;

    // Utilitário BCrypt para hash seguro de senhas
    private final PasswordEncoder passwordEncoder;

    // Injeção de dependências via construtor (gerida pelo Spring IoC)
    public AuthController(AuthenticationManager authenticationManager, 
                          UsuarioRepository usuarioRepository, 
                          TokenService tokenService, 
                          PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.usuarioRepository = usuarioRepository;
        this.tokenService = tokenService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Endpoint de login da aplicação.
     * 
     * @param data DTO contendo e-mail e senha enviados no corpo do JSON.
     * @return ResponseEntity contendo o token JWT em caso de sucesso.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@RequestBody LoginDTO data) {
        // Encapsula e-mail e senha em um token de autenticação não validado
        var usernamePassword = new UsernamePasswordAuthenticationToken(data.email(), data.senha());

        // O AuthenticationManager chama o UserDetailsService e PasswordEncoder para validar a senha.
        // Lança exceção de autenticação se as credenciais forem inválidas.
        var auth = this.authenticationManager.authenticate(usernamePassword);

        // Recupera o usuário autenticado e gera o token JWT assinado
        var token = tokenService.generateToken((Usuario) auth.getPrincipal());

        // Retorna HTTP 200 OK com o token envelopado no DTO
        return ResponseEntity.ok(new AuthResponseDTO(token));
    }

    /**
     * Endpoint para registro de novos usuários (Cidadão ou Gestor público).
     * 
     * @param data DTO contendo nome, e-mail, senha e código opcional da prefeitura.
     * @return ResponseEntity sem corpo (Void) indicando status da operação.
     */
    @PostMapping("/registrar")
    public ResponseEntity<Void> registrar(@RequestBody RegisterDTO data) {
        // Validação de e-mail duplicado: interrompe e retorna 400 Bad Request se já existir
        if (this.usuarioRepository.findByEmail(data.email()).isPresent()) {
            return ResponseEntity.badRequest().build();
        }

        // Gera o hash unidirecional da senha usando BCrypt antes de salvar
        String encryptedPassword = passwordEncoder.encode(data.senha());

        // Atribui o nível de acesso padrão como CIDADÃO
        Role roleToAssign = Role.ROLE_CIDADAO;

        // Regra de Negócio: Promove para GESTOR apenas se informar a chave secreta válida
        if (data.codigoPrefeitura() != null && data.codigoPrefeitura().equals("PREFEITURA_2026")) {
            roleToAssign = Role.ROLE_GESTOR;
        }

        // Instancia a nova entidade com a senha criptografada e a role definida
        Usuario newUsuario = new Usuario(data.nome(), data.email(), encryptedPassword, roleToAssign);

        // Salva a nova conta no banco de dados
        this.usuarioRepository.save(newUsuario);

        // Retorna HTTP 200 OK confirmando a criação
        return ResponseEntity.ok().build();
    }
}