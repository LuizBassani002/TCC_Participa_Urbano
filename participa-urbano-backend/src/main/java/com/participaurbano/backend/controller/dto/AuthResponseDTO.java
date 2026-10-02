package com.participaurbano.backend.controller.dto;

// "record" é um tipo especial de classe do Java, ideal para objetos simples que só carregam dados (DTOs),
// sem precisar escrever manualmente construtor, getters, equals/hashCode/toString
public record AuthResponseDTO(
    String token  // o único dado que essa resposta carrega: o token JWT gerado após o login
) {
}