package com.participaurbano.backend.controller.dto;

// DTO que representa os dados recebidos no corpo (body) da requisição de cadastro.
// O Spring converte automaticamente o JSON enviado pelo Flutter em um objeto desse tipo:
public record RegisterDTO(
    String nome,             // nome completo do usuário
    String email,            // e-mail do usuário (usado também como "login")
    String senha,            // senha em texto puro digitada pelo usuário (será criptografada no back-end antes de salvar)
    String codigoPrefeitura  // código de autorização, obrigatório apenas para cadastro de GESTOR; vem vazio ("") para cidadãos
) {
}