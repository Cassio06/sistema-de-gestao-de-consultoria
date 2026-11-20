package br.com.planilhacerta.view;

import br.com.planilhacerta.model.Consultor;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static br.com.planilhacerta.view.EntradaConsole.lerLinha;

public class GerenciadorConsultores {
    private final List<Consultor> consultores = new ArrayList<>();
    private final Scanner scanner;

    public GerenciadorConsultores() {
        this(new Scanner(System.in));
    }

    public GerenciadorConsultores(Scanner scanner) {
        this.scanner = scanner;
    }

    public void cadastrar() {
        System.out.print("ID do Consultor: ");
        String id = lerLinha(scanner);
        System.out.print("Nome: ");
        String nome = lerLinha(scanner);
        System.out.print("E-mail: ");
        String email = lerLinha(scanner);
        System.out.print("Especialidade: ");
        String especialidade = lerLinha(scanner);

        consultores.add(new Consultor(id, nome, email, especialidade));
        System.out.println("-> Consultor cadastrado com sucesso!");
    }

    public void listar() {
        if (consultores.isEmpty()) {
            System.out.println("Nenhum consultor cadastrado.");
            return;
        }
        System.out.println("\n--- LISTA DE CONSULTORES ---");
        for (Consultor con : consultores) {
            con.exibirDetalhes();
        }
    }

    public void editar() {
        System.out.print("Informe o ID do consultor que deseja editar: ");
        String id = lerLinha(scanner);
        Consultor con = buscarPorId(id);

        if (con != null) {
            System.out.print("Novo nome (" + con.getNome() + "): ");
            String nome = lerLinha(scanner);

            System.out.print("Novo e-mail (" + con.getEmail() + "): ");
            String email = lerLinha(scanner);

            System.out.print("Nova especialidade (" + con.getEspecialidade() + "): ");
            String especialidade = lerLinha(scanner);

            if (!nome.isEmpty()) con.setNome(nome);
            if (!email.isEmpty()) con.setEmail(email);
            if (!especialidade.isEmpty()) con.setEspecialidade(especialidade);

            System.out.println("-> Consultor atualizado com sucesso!");
        } else {
            System.out.println("-> Consultor não encontrado.");
        }
    }

    public void remover() {
        System.out.print("Informe o ID do consultor que deseja remover: ");
        String id = lerLinha(scanner);
        Consultor con = buscarPorId(id);
        if (con != null) {
            consultores.remove(con);
            System.out.println("-> Consultor removido com sucesso!");
        } else {
            System.out.println("-> Consultor não encontrado.");
        }
    }

    public Consultor buscarPorId(String id) {
        for (Consultor con : consultores) {
            if (con.getId().equals(id)) return con;
        }
        return null;
    }
}
