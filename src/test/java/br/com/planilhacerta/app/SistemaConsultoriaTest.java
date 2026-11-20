package br.com.planilhacerta.app;

import br.com.planilhacerta.domain.enums.Especializacao;
import br.com.planilhacerta.domain.enums.ModalidadePagamento;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class SistemaConsultoriaTest {
    @TempDir Path base;

    private SistemaConsultoria sistema() {
        return new SistemaConsultoria(base.resolve("dados"), base.resolve("pdf"));
    }

    @Test
    void vipAguardaERecebeVagaLiberadaMesmoAposReabrir() throws Exception {
        SistemaConsultoria app = sistema();
        app.cadastrarCliente("REG", "Regular", "reg@example.test", false);
        app.cadastrarCliente("VIP", "Vip", "vip@example.test", true);
        app.cadastrarConsultor("CON", "Consultor", "con@example.test", Especializacao.TI);
        for (int i = 1; i <= 4; i++) criar(app, "REG", i, ModalidadePagamento.POR_ETAPA, "150");
        criar(app, "VIP", 5, ModalidadePagamento.POR_ETAPA, "150");
        app.salvar();

        SistemaConsultoria retomado = sistema();
        assertNull(retomado.projetos().get(3).getIdConsultor());
        assertNull(retomado.projetos().get(4).getIdConsultor());
        assertThrows(IllegalArgumentException.class,
                () -> retomado.concluirEtapa("P1", "E1", LocalDate.now().plusDays(1)));
        retomado.concluirEtapa("P1", "E1", LocalDate.now());
        assertEquals("CON", retomado.projetos().get(4).getIdConsultor());
        assertNull(retomado.projetos().get(3).getIdConsultor());
        assertEquals(3, retomado.consultores().get(0).getQuantidadeProjetosAtivos());
        assertEquals(0, retomado.pontos("REG"));
    }

    @Test
    void pagamentoFaturaERecomendacoesSobrevivemAoReinicio() throws Exception {
        SistemaConsultoria app = sistema();
        app.cadastrarCliente("VIP", "Vip", "vip@example.test", true);
        app.cadastrarConsultor("CON", "Consultor", "con@example.test", Especializacao.TI);
        criar(app, "VIP", 1, ModalidadePagamento.POR_ETAPA, "120");
        assertThrows(IllegalStateException.class, () -> app.emitirFatura("C1", "E1", LocalDate.now()));
        app.concluirEtapa("P1", "E1", LocalDate.now());
        assertEquals(0, app.pontos("VIP"));
        var fatura = app.emitirFatura("C1", "E1", LocalDate.now());
        assertEquals(0, new BigDecimal("108").compareTo(fatura.getValorLiquido()));
        assertThrows(IllegalArgumentException.class,
                () -> app.pagar(fatura.getId(), fatura.getValorLiquido(), LocalDate.now().minusDays(1)));
        app.pagar(fatura.getId(), fatura.getValorLiquido(), LocalDate.now());
        assertEquals(1, app.pontos("VIP"));
        app.salvar();

        SistemaConsultoria retomado = sistema();
        assertEquals(1, retomado.pagamentos().size());
        assertEquals(1, retomado.pontos("VIP"));
        assertEquals(fatura.getId(), retomado.emitirFatura("C1", "E1", LocalDate.now()).getId());
        assertThrows(IllegalStateException.class,
                () -> retomado.pagar(fatura.getId(), fatura.getValorLiquido(), LocalDate.now()));
        assertEquals(List.of(br.com.planilhacerta.model.Especializacao.FINANCEIRO,
                br.com.planilhacerta.model.Especializacao.GESTAO), retomado.recomendar("VIP"));
        System.setProperty("java.awt.headless", "true");
        retomado.emitirRelatorio("P1");
        retomado.exportarCSV();
        assertTrue(Files.size(base.resolve("pdf/Relatorio_P1_Vip.pdf")) > 0);
        assertTrue(Files.size(base.resolve("dados/relatorio_planilha_certa.csv")) > 0);
    }

    @Test
    void descontoDoProximoContratoEEntregaFinalSobrevivemAoReinicio() throws Exception {
        SistemaConsultoria app = sistema();
        app.cadastrarCliente("REG", "Regular", "reg@example.test", false);
        app.cadastrarConsultor("CON", "Consultor", "con@example.test", Especializacao.TI);
        criar(app, "REG", 1, ModalidadePagamento.POR_ETAPA, "10000");
        app.concluirEtapa("P1", "E1", LocalDate.now());
        var primeira = app.emitirFatura("C1", "E1", LocalDate.now());
        app.pagar(primeira.getId(), primeira.getValorLiquido(), LocalDate.now());
        assertEquals(100, app.pontos("REG"));

        criar(app, "REG", 2, ModalidadePagamento.ENTREGA_FINAL, "200");
        assertEquals(0, app.pontos("REG"));
        app.salvar();
        SistemaConsultoria retomado = sistema();
        assertThrows(IllegalStateException.class, () -> retomado.emitirFatura("C2", null, LocalDate.now()));
        retomado.concluirEtapa("P2", "E1", LocalDate.now());
        var fatura = retomado.emitirFatura("C2", null, LocalDate.now());
        assertEquals(0, new BigDecimal("10").compareTo(fatura.getDescontoFidelidade()));
        assertEquals(0, new BigDecimal("190").compareTo(fatura.getValorLiquido()));
        retomado.salvar();
        assertEquals(fatura.getId(), sistema().emitirFatura("C2", null, LocalDate.now()).getId());
    }

    @Test
    void recuperaBackupQuandoFaturaDoPrincipalFoiAlterada() throws Exception {
        SistemaConsultoria app = sistema();
        app.carregarDemonstracao();
        app.emitirFatura("CTR-DEMO", "ETP-1", LocalDate.now());
        app.salvar();
        app.cadastrarCliente("NOVO", "Novo", "novo@example.test", false);
        app.salvar();
        Path principal = base.resolve("dados/consultoria.json");
        Path backup = base.resolve("dados/consultoria.bak.json");
        byte[] anterior = Files.readAllBytes(backup);
        var json = com.google.gson.JsonParser.parseString(Files.readString(principal)).getAsJsonObject();
        json.getAsJsonObject("estado").getAsJsonObject("financeiro").getAsJsonArray("cobrancas")
                .get(0).getAsJsonObject().getAsJsonArray("itens").get(0).getAsJsonObject()
                .addProperty("valor", new BigDecimal("2001"));
        Files.writeString(principal, json.toString());
        SistemaConsultoria restaurado = sistema();
        assertEquals(1, restaurado.clientes().size());
        assertArrayEquals(anterior, Files.readAllBytes(principal));
        assertArrayEquals(anterior, Files.readAllBytes(backup));
    }

    private static void criar(SistemaConsultoria app, String cliente, int numero,
                              ModalidadePagamento forma, String valor) {
        LocalDate hoje = LocalDate.now();
        app.criarContrato("C" + numero, cliente, "P" + numero, "Projeto " + numero,
                Especializacao.TI, hoje, hoje.plusDays(10), forma,
                List.of(new SistemaConsultoria.DadosEtapa("E1", "Análise", hoje.plusDays(2), new BigDecimal(valor))));
    }
}
