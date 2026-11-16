package br.com.planilhacerta.service;

import br.com.planilhacerta.domain.enums.Especializacao;
import br.com.planilhacerta.domain.model.Consultor;
import br.com.planilhacerta.domain.model.Contrato;

import java.util.List;
import java.util.Optional;

public class AlocacaoService {
    public Optional<Consultor> selecionarConsultor(Especializacao exigida, List<Consultor> consultores) {
        // TODO Cássio: considerar apenas a especialização exigida e quem tem vaga.
        // Escolher o menor número de projetos ativos; retornar Optional.empty() sem candidato.
        // O PDF não define desempate: combine uma regra com a equipe e documente.
        throw new UnsupportedOperationException("Cássio: implementar a seleção por especialização e carga.");
    }

    public List<Contrato> ordenarPorPrioridade(List<Contrato> contratosPendentes) {
        // TODO Cássio: devolver uma nova lista, atendendo VIP antes de Regular.
        // Usar contrato.getCliente().temPrioridadeNaAlocacao() para aplicar polimorfismo.
        // Preservar a ordem de chegada entre clientes de mesma prioridade é uma opção de desempate.
        throw new UnsupportedOperationException("Cássio: implementar a ordem de atendimento.");
    }

    public Optional<Consultor> alocar(Contrato contrato, List<Consultor> consultores) {
        // TODO Cássio: selecionar o consultor e registrar o projeto como ativo nele.
        // Sem candidato, retornar Optional.empty() para o chamador manter o contrato pendente.
        // Impedir que o mesmo projeto seja alocado novamente, inclusive a outro consultor.
        // A fila deve chamar este método na ordem retornada por ordenarPorPrioridade.
        // O consultor retornado será vinculado ao Projeto na integração com Arthur.
        throw new UnsupportedOperationException("Cássio: implementar a alocação do projeto.");
    }
}
