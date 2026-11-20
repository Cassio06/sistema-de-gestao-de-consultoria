package br.com.planilhacerta.service;

import br.com.planilhacerta.domain.enums.Especializacao;
import br.com.planilhacerta.domain.enums.ModalidadePagamento;
import br.com.planilhacerta.domain.model.Cliente;
import br.com.planilhacerta.domain.model.ClienteRegular;
import br.com.planilhacerta.domain.model.ClienteVIP;
import br.com.planilhacerta.domain.model.Consultor;
import br.com.planilhacerta.domain.model.Contrato;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static br.com.planilhacerta.domain.enums.Especializacao.FINANCEIRO;
import static br.com.planilhacerta.domain.enums.Especializacao.TI;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoreConsultoriaTest {
    private static final LocalDate INICIO = LocalDate.of(2026, 11, 20);

    @Test
    void cadastraContratoEAtualizaSomenteOHistoricoDoTitular() {
        Cliente titular = regular("CLI-1");
        Cliente outro = regular("CLI-2");
        Contrato contrato = contrato("CTR-1", titular, "PRJ-1", FINANCEIRO);
        ContratoService contratos = new ContratoService();

        assertThrows(IllegalArgumentException.class, () -> outro.registrarContrato(contrato));
        assertTrue(outro.getHistoricoContratos().isEmpty());
        contratos.cadastrar(contrato);
        assertSame(contrato, contratos.buscarPorId("CTR-1").orElseThrow());
        assertEquals(List.of(contrato), titular.getHistoricoContratos());
        assertThrows(UnsupportedOperationException.class, () -> titular.getHistoricoContratos().clear());
    }

    @Test
    void cadastroRecusadoPreservaServicoEHistorico() {
        Cliente titular = regular("CLI-1");
        Cliente outro = regular("CLI-2");
        Contrato primeiro = contrato("CTR-1", titular, "PRJ-1", FINANCEIRO);
        ContratoService contratos = new ContratoService();
        contratos.cadastrar(primeiro);

        assertThrows(IllegalArgumentException.class, () -> contratos.cadastrar(
                contrato("CTR-1", outro, "PRJ-2", FINANCEIRO)));
        assertTrue(outro.getHistoricoContratos().isEmpty());
        assertEquals(List.of(primeiro), contratos.listar());

        ContratoService outroServico = new ContratoService();
        assertThrows(IllegalArgumentException.class, () -> outroServico.cadastrar(primeiro));
        assertTrue(outroServico.listar().isEmpty());
        assertEquals(List.of(primeiro), titular.getHistoricoContratos());
    }

    @Test
    void validaOsDadosObrigatoriosDoContrato() {
        Cliente titular = regular("CLI-1");
        assertThrows(NullPointerException.class, () -> contrato("CTR-1", null, "PRJ-1", TI));
        assertThrows(IllegalArgumentException.class, () -> new Contrato(
                "CTR-1", titular, "PRJ-1", TI, INICIO, INICIO.minusDays(1), BigDecimal.TEN,
                ModalidadePagamento.POR_ETAPA));
        assertThrows(IllegalArgumentException.class, () -> new Contrato(
                "CTR-1", titular, "PRJ-1", TI, INICIO, INICIO, BigDecimal.ZERO,
                ModalidadePagamento.POR_ETAPA));
    }

    @Test
    void aplicaLimiteDeTresVagasEPermiteReusarUmaVagaLiberada() {
        Consultor consultor = consultor("CON-1", FINANCEIRO);
        consultor.adicionarProjeto(" PRJ-1 ");
        assertThrows(IllegalArgumentException.class, () -> consultor.adicionarProjeto("PRJ-1"));
        consultor.adicionarProjeto("PRJ-2");
        consultor.adicionarProjeto("PRJ-3");
        assertFalse(consultor.temDisponibilidade());
        assertThrows(IllegalStateException.class, () -> consultor.adicionarProjeto("PRJ-4"));
        assertThrows(IllegalArgumentException.class, () -> consultor.removerProjeto("PRJ-9"));
        assertEquals(List.of("PRJ-1", "PRJ-2", "PRJ-3"), consultor.getProjetosAtivos());

        consultor.removerProjeto(" PRJ-2 ");
        assertTrue(consultor.temDisponibilidade());
        consultor.adicionarProjeto("PRJ-4");
        assertEquals(List.of("PRJ-1", "PRJ-3", "PRJ-4"), consultor.getProjetosAtivos());
    }

    @Test
    void selecionaPorEspecializacaoCargaEIdentificador() {
        AlocacaoService alocacao = new AlocacaoService();
        Consultor outraArea = consultor("CON-1", TI);
        Consultor ocupado = consultor("CON-2", FINANCEIRO);
        ocupado.adicionarProjeto("PRJ-1");
        Consultor desempate = consultor("CON-3", FINANCEIRO);
        Consultor escolhido = consultor("CON-4", FINANCEIRO);

        assertSame(desempate, alocacao.selecionarConsultor(FINANCEIRO,
                List.of(outraArea, ocupado, escolhido, desempate)).orElseThrow());
        assertTrue(alocacao.selecionarConsultor(FINANCEIRO, List.of(outraArea)).isEmpty());
    }

    @Test
    void atendeVipAntesDeRegularEPreservaAOrdemDeChegadaNaCategoria() {
        AlocacaoService alocacao = new AlocacaoService();
        Cliente regular = regular("CLI-1");
        Cliente vip = new ClienteVIP("CLI-2", "Cliente VIP", "vip@example.test");
        Contrato contratoRegular = contrato("CTR-1", regular, "PRJ-1", FINANCEIRO);
        Contrato primeiroVip = contrato("CTR-2", vip, "PRJ-2", FINANCEIRO);
        Contrato segundoVip = contrato("CTR-3", vip, "PRJ-3", FINANCEIRO);
        List<Contrato> entrada = new ArrayList<>(List.of(contratoRegular, primeiroVip, segundoVip));
        Consultor consultor = consultor("CON-1", FINANCEIRO);
        consultor.adicionarProjeto("EM-EXECUCAO-1");
        consultor.adicionarProjeto("EM-EXECUCAO-2");

        List<Contrato> fila = alocacao.ordenarPorPrioridade(entrada);
        assertEquals(List.of(primeiroVip, segundoVip, contratoRegular), fila);
        assertEquals(List.of(contratoRegular, primeiroVip, segundoVip), entrada);
        assertSame(consultor, alocacao.alocar(fila.get(0), List.of(consultor)).orElseThrow());
        assertTrue(alocacao.alocar(contratoRegular, List.of(consultor)).isEmpty());
        consultor.removerProjeto(primeiroVip.getProjetoId());
        assertSame(consultor, alocacao.alocar(segundoVip, List.of(consultor)).orElseThrow());
    }

    @Test
    void repetirAlocacaoReaproveitaOVinculoExistente() {
        AlocacaoService alocacao = new AlocacaoService();
        Consultor original = consultor("CON-1", FINANCEIRO);
        Consultor outro = consultor("CON-2", FINANCEIRO);
        Contrato contrato = contrato("CTR-1", regular("CLI-1"), "PRJ-1", FINANCEIRO);

        assertSame(original, alocacao.alocar(contrato, List.of(original, outro)).orElseThrow());
        assertSame(original, alocacao.alocar(contrato, List.of(original, outro)).orElseThrow());
        assertEquals(List.of("PRJ-1"), original.getProjetosAtivos());
        assertTrue(outro.getProjetosAtivos().isEmpty());
    }

    @Test
    void mantemVinculoAoTentarAlocarOMesmoProjetoComOutraLista() {
        AlocacaoService alocacao = new AlocacaoService();
        Consultor original = consultor("CON-1", FINANCEIRO);
        Consultor outro = consultor("CON-2", FINANCEIRO);
        Contrato contrato = contrato("CTR-1", regular("CLI-1"), "PRJ-1", FINANCEIRO);

        assertSame(original, alocacao.alocar(contrato, List.of(original)).orElseThrow());
        assertSame(original, alocacao.alocar(contrato, List.of(outro)).orElseThrow());
        assertTrue(outro.getProjetosAtivos().isEmpty());

        original.removerProjeto(contrato.getProjetoId());
        assertSame(outro, alocacao.alocar(contrato, List.of(outro)).orElseThrow());
    }

    @Test
    void chamadasConcorrentesNaoAlocamOMesmoProjetoDuasVezes() throws Exception {
        Consultor primeiro = consultor("CON-1", FINANCEIRO);
        Consultor segundo = consultor("CON-2", FINANCEIRO);
        List<Consultor> consultores = List.of(primeiro, segundo);
        Contrato contrato = contrato("CTR-1", regular("CLI-1"), "PRJ-1", FINANCEIRO);
        var inicio = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var uma = executor.submit(() -> {
                inicio.await();
                return new AlocacaoService().alocar(contrato, consultores).orElseThrow();
            });
            var outra = executor.submit(() -> {
                inicio.await();
                return new AlocacaoService().alocar(contrato, consultores).orElseThrow();
            });
            inicio.countDown();
            assertSame(uma.get(5, TimeUnit.SECONDS), outra.get(5, TimeUnit.SECONDS));
            assertEquals(1, primeiro.getQuantidadeProjetosAtivos() + segundo.getQuantidadeProjetosAtivos());
        } finally {
            executor.shutdownNow();
        }
    }

    private static Cliente regular(String id) {
        return new ClienteRegular(id, "Cliente Regular", "regular@example.test");
    }

    private static Consultor consultor(String id, Especializacao area) {
        return new Consultor(id, "Consultor", "consultor@example.test", area);
    }

    private static Contrato contrato(String id, Cliente cliente, String projeto, Especializacao area) {
        return new Contrato(id, cliente, projeto, area, INICIO, INICIO.plusDays(7), BigDecimal.TEN,
                ModalidadePagamento.POR_ETAPA);
    }
}
