package com.participaurbano.backend.domain.entity;

import com.participaurbano.backend.domain.enums.Role;
import jakarta.persistence.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

// Marca a classe como entidade JPA (representa a tabela "usuarios" no banco)
@Entity
@Table(name = "usuarios")
// "implements UserDetails" é a parte mais importante dessa classe:
// isso faz o Usuario também servir como o objeto de autenticação do Spring Security,
// permitindo que o próprio Spring entenda "quem é" esse usuário durante o login
public class Usuario implements UserDetails {

    // Chave primária, auto-incremento
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Nome completo, obrigatório
    @Column(nullable = false)
    private String nome;

    // E-mail obrigatório e ÚNICO (não pode haver dois usuários com o mesmo e-mail no banco)
    // é essa restrição "unique" que garante, no nível do banco, a regra que o AuthController
    // também verifica manualmente antes de cadastrar
    @Column(nullable = false, unique = true)
    private String email;

    // Senha (já criptografada em BCrypt antes de chegar aqui, vinda do AuthController)
    @Column(nullable = false)
    private String senha;

    // Papel do usuário no sistema (ROLE_CIDADAO ou ROLE_GESTOR), salvo como texto no banco
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(50)")
    private Role role;

    // Construtor vazio, exigido pelo JPA/Hibernate
    public Usuario() {}

    // Construtor de conveniência, usado no AuthController ao criar um novo cadastro:
    // new Usuario(data.nome(), data.email(), encryptedPassword, roleToAssign)
    public Usuario(String nome, String email, String senha, Role role) {
        this.nome = nome;
        this.email = email;
        this.senha = senha;
        this.role = role;
    }

    // Getters e Setters padrão dos campos da entidade
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }
    
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    // ---------------------------------------------------------
    // A PARTIR DAQUI: métodos EXIGIDOS pela interface UserDetails
    // (o Spring Security chama esses métodos automaticamente durante login/autorização)
    // ---------------------------------------------------------

    // Retorna a lista de "permissões"/papéis desse usuário.
    // É AQUI que a Role vira algo que o Spring Security entende e usa
    // nas regras hasRole("GESTOR") que vimos no SecurityConfig
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Converte o enum Role (ex: ROLE_GESTOR) num objeto GrantedAuthority,
        // que é o "formato" que o Spring Security entende como permissão
        return List.of(new SimpleGrantedAuthority(role.name()));
    }

    // O Spring Security chama esse método para saber qual é a senha (hash) a comparar no login
    @Override
    public String getPassword() {
        return this.senha;
    }

    // O Spring Security trata "username" como o identificador único de login;
    // no seu sistema, esse identificador é o e-mail
    @Override
    public String getUsername() {
        return this.email;
    }

    // Os 4 métodos abaixo são "flags" de status da conta, exigidos pela interface UserDetails.
    // Como o sistema não implementa expiração de conta, bloqueio ou expiração de senha,
    // todos retornam "true" fixo — ou seja, toda conta é sempre considerada válida/ativa
    @Override
    public boolean isAccountNonExpired() { return true; }   // conta nunca expira

    @Override
    public boolean isAccountNonLocked() { return true; }    // conta nunca fica bloqueada

    @Override
    public boolean isCredentialsNonExpired() { return true; } // senha nunca expira

    @Override
    public boolean isEnabled() { return true; }              // conta está sempre ativa/habilitada
}