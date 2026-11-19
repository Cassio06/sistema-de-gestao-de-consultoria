package br.com.planilhacerta.view;

import br.com.planilhacerta.model.Cliente;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class GerenciadorClientes {
    private final List<Cliente> clientes = new ArrayList<>();
    private final Scanner scanner;

    public GerenciadorClientes() {
        this(new Scanner(System.in));
    }

    public GerenciadorClientes(Scanner scanner) {
        this.scanner = scanner;
    }

    public void cadastrar() {
        System.out.print("ID do Cliente: ");
        String id = scanner.nextLine();
        System.out.print("Nome: ");
        String nome = scanner.nextLine();
        System.out.print("E-mail: ");
        String email = scanner.nextLine();
        System.out.print("Empresa: ");
        String empresa = scanner.nextLine();
        System.out.print("Cliente VIP? (s/n): ");
        boolean vip = scanner.nextLine().trim().equalsIgnoreCase("s");

        clientes.add(new Cliente(id, nome, email, empresa, vip));
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
            clientes.remove(c);
            System.out.println("-> Cliente removido com sucesso!");
        } else {
            System.out.println("-> Cliente não encontrado.");
        }
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
}



