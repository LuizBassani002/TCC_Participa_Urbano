package com.participaurbano.backend.controller.dto;

// DTO que representa os dados recebidos no corpo (body) da requisição de login.
// O Spring converte automaticamente o JSON enviado pelo Flutter ({"email": "...", "senha": "..."})
// em um objeto desse tipo, preenchendo os campos abaixo:
public record LoginDTO(
    String email,  // e-mail digitado pelo usuário na tela de login
    String senha   // senha digitada pelo usuário na tela de login
) {
}