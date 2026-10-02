package com.participaurbano.backend.service;

import com.participaurbano.backend.domain.enums.Categoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NLPClassificationServiceTest {

    private NLPClassificationService service;

    @BeforeEach
    void setUp() {
        service = new NLPClassificationService();
    }

    @Test
    @DisplayName("Avalia acurácia e gera Matriz de Confusão do PLN usando o dataset real da SP156")
    void testNLPWithSP156Dataset() throws Exception {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("dataset_sp156.csv");
        assertNotNull(inputStream, "O arquivo dataset_sp156.csv não foi encontrado na pasta src/test/resources!");

        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

        String line;
        boolean isHeader = true;

        int totalTestes = 0;
        int totalAcertos = 0;

        // Inicializa a Matriz de Confusão
        Map<Categoria, Map<Categoria, Integer>> matrizConfusao = new HashMap<>();
        for (Categoria catReal : Categoria.values()) {
            matrizConfusao.put(catReal, new HashMap<>());
            for (Categoria catPrevista : Categoria.values()) {
                matrizConfusao.get(catReal).put(catPrevista, 0);
            }
        }

        while ((line = reader.readLine()) != null) {
            if (line.trim().isEmpty()) continue;

            if (isHeader) {
                isHeader = false;
                continue;
            }

            // Suporta delimitador de vírgula ou ponto e vírgula
            String[] colunas = line.contains(";") ? line.split(";") : line.split(",");

            if (colunas.length < 5) continue;

            // Extração das colunas da SP156: Coluna D (Índice 3) = Tema | Coluna E (Índice 4) = Assunto
            String temaOriginal = sanitizar(colunas[3]);
            String assuntoOriginal = sanitizar(colunas[4]);
            String servicoOriginal = colunas.length > 5 ? sanitizar(colunas[5]) : "";

            // O texto de entrada para o classificador junta o assunto e o serviço
            String textoEntrada = assuntoOriginal + " " + servicoOriginal;

            // De/Para automático do Tema da SP156 para a Categoria do sistema
            Categoria categoriaGabarito = mapearParaCategoriaDomain(temaOriginal, assuntoOriginal);

            // Executa o seu algoritmo de PLN
            Categoria categoriaPrevista = service.classify(textoEntrada);

            totalTestes++;
            if (categoriaGabarito == categoriaPrevista) {
                totalAcertos++;
            }

            // Atualiza a Matriz de Confusão
            int contagemAtual = matrizConfusao.get(categoriaGabarito).get(categoriaPrevista);
            matrizConfusao.get(categoriaGabarito).put(categoriaPrevista, contagemAtual + 1);
        }

        reader.close();

        // Exibição dos resultados e métricas
        double acuracia = totalTestes > 0 ? ((double) totalAcertos / totalTestes) * 100.0 : 0.0;

        System.out.println("\n=================================================================");
        System.out.println("     RELATÓRIO DE DESEMPENHO DO CLASSIFICADOR PLN (SP156)        ");
        System.out.println("=================================================================");
        System.out.printf("Total de registros avaliados : %d\n", totalTestes);
        System.out.printf("Total de classificações corretas: %d\n", totalAcertos);
        System.out.printf("Acurácia Global do Algoritmo  : %.2f%%\n", acuracia);
        System.out.println("-----------------------------------------------------------------");
        System.out.println("MATRIZ DE CONFUSÃO (Gabarito Real vs. Previsto pelo Código):");

        for (Categoria real : Categoria.values()) {
            System.out.printf("\n[Categoria Real: %s]\n", real);
            for (Categoria prevista : Categoria.values()) {
                int qtd = matrizConfusao.get(real).get(prevista);
                if (qtd > 0) {
                    System.out.printf("   -> Classificado como %-20s: %d ocorrência(s)\n", prevista, qtd);
                }
            }
        }
        System.out.println("=================================================================\n");

        assertTrue(totalTestes > 0, "O dataset precisa conter ao menos uma linha válida.");
    }

    /**
     * Limpa aspas e espaços extras das células extraídas do CSV
     */
    private String sanitizar(String valor) {
        if (valor == null) return "";
        return valor.replace("\"", "").trim();
    }

    /**
     * Mapeamento Heurístico da SP156 para os Enums do Backend
     */
    private Categoria mapearParaCategoriaDomain(String tema, String assunto) {
        String combinado = (tema + " " + assunto).toLowerCase();

        if (combinado.contains("rua") || combinado.contains("asfalto") || combinado.contains("drenagem") 
                || combinado.contains("galeria") || combinado.contains("calçada") || combinado.contains("tapa-buraco")) {
            return Categoria.INFRAESTRUTURA;
        } 
        if (combinado.contains("iluminação") || combinado.contains("poste") || combinado.contains("luz") || combinado.contains("lâmpada")) {
            return Categoria.ILUMINACAO_PUBLICA;
        } 
        if (combinado.contains("animais") || combinado.contains("lixo") || combinado.contains("limpeza") || combinado.contains("entulho")) {
            return Categoria.LIMPEZA_PUBLICA;
        } 
        if (combinado.contains("árvore") || combinado.contains("poda") || combinado.contains("ambiente") || combinado.contains("praça")) {
            return Categoria.MEIO_AMBIENTE;
        } 
        if (combinado.contains("segurança") || combinado.contains("polícia") || combinado.contains("guarda")) {
            return Categoria.SEGURANCA;
        }

        return Categoria.OUTROS;
    }
}