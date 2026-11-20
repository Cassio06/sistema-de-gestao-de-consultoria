package br.com.planilhacerta.view;

import br.com.planilhacerta.model.Cliente;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.function.Predicate;

public class GerenciadorClientes {
    private final List<Cliente> clientes = new ArrayList<>();
    private final Set<String> idsUtilizados = new HashSet<>();
    private final Scanner scanner;
    private Predicate<Cliente> possuiContratoAtivo = cliente -> false;

    public GerenciadorClientes() {
        this(new Scanner(System.in));
    }

    public GerenciadorClientes(Scanner scanner) {
        this.scanner = scanner;
    }

    public void cadastrar() {
        System.out.print("ID do Cliente: ");
        String entrada = lerLinhaCadastro();
        if (entrada == null) return;
        String id = entrada.trim();
        if (id.isEmpty() || idsUtilizados.contains(id)) {
            System.out.println("ID vazio ou já utilizado.");
            return;
        }
        System.out.print("Nome: ");
        String nome = lerLinhaCadastro();
        if (nome == null) return;
        System.out.print("E-mail: ");
        String email = lerLinhaCadastro();
        if (email == null) return;
        System.out.print("Empresa: ");
        String empresa = lerLinhaCadastro();
        if (empresa == null) return;
        System.out.print("Cliente VIP? (s/n): ");
        String respostaVip = lerLinhaCadastro();
        if (respostaVip == null) return;
        boolean vip = respostaVip.trim().equalsIgnoreCase("s");

        clientes.add(new Cliente(id, nome, email, empresa, vip));
        idsUtilizados.add(id);
        System.out.println("-> Cliente cadastrado com sucesso!");
    }

    public void listar() {
        if (clientes.isEmpty()) {
            System.out.println("Nenhum cliente cadastrado.");
            return;
        }
        System.out.println("\n--- LISTA DE CLIENTES ---");
        for (Cliente c : clientes) {
            c.exibirDetalhes();
        }
    }

    public void editar() {
        System.out.print("Informe o ID do cliente que deseja editar: ");
        String id = scanner.nextLine();
        Cliente c = buscarPorId(id);

        if (c != null) {
            System.out.print("Novo nome (" + c.getNome() + "): ");
            String nome = scanner.nextLine();
            if (!nome.isEmpty()) c.setNome(nome);

            System.out.print("Novo e-mail (" + c.getEmail() + "): ");
            String email = scanner.nextLine();
            if (!email.isEmpty()) c.setEmail(email);

            System.out.print("Nova empresa (" + c.getEmpresa() + "): ");
            String empresa = scanner.nextLine();
            if (!empresa.isEmpty()) c.setEmpresa(empresa);

            System.out.println("-> Cliente atualizado com sucesso!");
        } else {
            System.out.println("-> Cliente não encontrado.");
        }
    }

    public void remover() {
        System.out.print("Informe o ID do cliente que deseja remover: ");
        String id = scanner.nextLine();
        Cliente c = buscarPorId(id);
        if (c != null) {
            if (possuiContratoAtivo.test(c)) {
                System.out.println("-> Cliente n\u00e3o pode ser removido: possui contrato ativo ou fatura pendente.");
                return;
            }
            clientes.remove(c);
            System.out.println("-> Cliente removido com sucesso!");
        } else {
            System.out.println("-> Cliente não encontrado.");
        }
    }

    void definirVerificadorDeContratoAtivo(Predicate<Cliente> verificador) {
        this.possuiContratoAtivo = java.util.Objects.requireNonNull(verificador, "verificador");
    }

    public Cliente buscarPorId(String id) {
        for (Cliente c : clientes) {
            if (c.getId().equals(id)) return c;
        }
        return null;
    }

    public List<Cliente> getClientes() {
        return List.copyOf(clientes);
    }

    private String lerLinhaCadastro() {
        try {
            return scanner.nextLine();
        } catch (java.util.NoSuchElementException erro) {
            System.out.println("-> Cadastro cancelado: entrada encerrada antes de concluir todos os campos.");
            return null;
        }
    }
}



