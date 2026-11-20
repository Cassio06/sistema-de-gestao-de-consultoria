package br.com.planilhacerta.app;

import br.com.planilhacerta.domain.enums.CategoriaCliente;
import br.com.planilhacerta.projetos.Projeto;
import java.nio.file.Path;
import java.util.List;
import model.ModelosSistema;
import persistence.PersistenciaService;
import report.RelatorioService;

final class RelatoriosIntegrados {
    private final RelatorioService pdf;
    private final PersistenciaService csv;

    RelatoriosIntegrados(Path dados, Path pdfs) {
        pdf = new RelatorioService(pdfs);
        csv = new PersistenciaService(dados);
    }

    void emitir(Projeto projeto, br.com.planilhacerta.domain.model.Contrato contrato,
                br.com.planilhacerta.domain.model.Consultor consultor, int pontos,
                List<br.com.planilhacerta.model.Especializacao> recomendacoes) {
        var cliente = cliente(contrato.getCliente());
        cliente.adicionarPontos(pontos);
        var projetoApresentacao = projeto(projeto, contrato);
        var consultorApresentacao = consultor == null ? null : consultor(consultor);
        var areas = recomendacoes.stream().map(area -> ModelosSistema.Especializacao.valueOf(area.name())).toList();
        pdf.exibirRelatorioConsole(projetoApresentacao, cliente, consultorApresentacao, areas);
        pdf.gerarRelatorioPDF(projetoApresentacao, cliente, consultorApresentacao, areas);
    }

    void exportar(List<Projeto> projetos, List<br.com.planilhacerta.domain.model.Contrato> contratos,
                  List<br.com.planilhacerta.domain.model.Cliente> clientes,
                  List<br.com.planilhacerta.domain.model.Consultor> consultores) {
        var dados = new ModelosSistema.BancoDados();
        clientes.forEach(c -> dados.clientes.add(cliente(c)));
        consultores.forEach(c -> dados.consultores.add(consultor(c)));
        var contratosPorProjeto = new java.util.HashMap<String, br.com.planilhacerta.domain.model.Contrato>();
        contratos.forEach(c -> contratosPorProjeto.put(c.getProjetoId(), c));
        projetos.forEach(p -> dados.projetos.add(projeto(p, contratosPorProjeto.get(p.getId()))));
        csv.exportarCSV(dados);
    }

    private static ModelosSistema.Cliente cliente(br.com.planilhacerta.domain.model.Cliente cliente) {
        return new ModelosSistema.Cliente(cliente.getId(), cliente.getNome(), cliente.getEmail(), "",
                cliente.getCategoria() == CategoriaCliente.VIP);
    }

    private static ModelosSistema.Consultor consultor(br.com.planilhacerta.domain.model.Consultor consultor) {
        return new ModelosSistema.Consultor(consultor.getId(), consultor.getNome(), consultor.getEmail(), "",
                ModelosSistema.Especializacao.valueOf(consultor.getEspecializacao().name()));
    }

    private static ModelosSistema.Projeto projeto(Projeto projeto, br.com.planilhacerta.domain.model.Contrato contrato) {
        var saida = new ModelosSistema.Projeto(projeto.getId(), projeto.getNome(), projeto.getIdCliente(),
                projeto.getIdConsultor(), ModelosSistema.Especializacao.valueOf(contrato.getEspecializacao().name()));
        for (var etapa : projeto.getEtapas()) {
            var copia = new ModelosSistema.Etapa(etapa.getNome(), etapa.getPrazo(), etapa.getValor().doubleValue());
            copia.setStatus(ModelosSistema.StatusEtapa.valueOf(etapa.getStatusAtual().name()));
            saida.getEtapas().add(copia);
        }
        return saida;
    }
}
