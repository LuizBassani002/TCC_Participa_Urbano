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

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;

    public AuthController(AuthenticationManager authenticationManager, UsuarioRepository usuarioRepository, TokenService tokenService, PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.usuarioRepository = usuarioRepository;
        this.tokenService = tokenService;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@RequestBody LoginDTO data){
        var usernamePassword = new UsernamePasswordAuthenticationToken(data.email(), data.senha());
        var auth = this.authenticationManager.authenticate(usernamePassword);

        var token = tokenService.generateToken((Usuario) auth.getPrincipal());

        return ResponseEntity.ok(new AuthResponseDTO(token));
    }

    @PostMapping("/registrar")
    public ResponseEntity<Void> registrar(@RequestBody RegisterDTO data){
        if(this.usuarioRepository.findByEmail(data.email()).isPresent()) return ResponseEntity.badRequest().build();

        String encryptedPassword = passwordEncoder.encode(data.senha());
        
        Role roleToAssign = Role.ROLE_CIDADAO;
        if(data.codigoPrefeitura() != null && data.codigoPrefeitura().equals("PREFEITURA_2026")) {
            roleToAssign = Role.ROLE_GESTOR;
        }

        Usuario newUsuario = new Usuario(data.nome(), data.email(), encryptedPassword, roleToAssign);

        this.usuarioRepository.save(newUsuario);

        return ResponseEntity.ok().build();
    }
}
