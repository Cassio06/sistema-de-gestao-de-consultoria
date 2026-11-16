package br.com.planilhacerta.view;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            GerenciadorClientes gerClientes = new GerenciadorClientes(scanner);
            GerenciadorConsultores gerConsultores = new GerenciadorConsultores(scanner);
        
            int opcao;

            do {
                System.out.println("\n--- PLANILHA CERTA ---");
                System.out.println("1. Cadastrar Cliente");
                System.out.println("2. Listar Clientes");
                System.out.println("3. Editar Cliente");
                System.out.println("4. Remover Cliente");
                System.out.println("-----------------------------------");
                System.out.println("5. Cadastrar Consultor");
                System.out.println("6. Listar Consultores");
                System.out.println("7. Editar Consultor");
                System.out.println("8. Remover Consultor");
                System.out.println("-----------------------------------");
                System.out.println("0. Sair");
                System.out.print("Escolha uma opção: ");

                try {
                    opcao = Integer.parseInt(scanner.nextLine());
                } catch (NumberFormatException e) {
                    opcao = -1;
                }

                switch (opcao) {
                    case 1 -> gerClientes.cadastrar();
                    case 2 -> gerClientes.listar();
                    case 3 -> gerClientes.editar();
                    case 4 -> gerClientes.remover();
                    case 5 -> gerConsultores.cadastrar();
                    case 6 -> gerConsultores.listar();
                    case 7 -> gerConsultores.editar();
                    case 8 -> gerConsultores.remover();
                    case 0 -> System.out.println("Encerrando testes...");
                    default -> System.out.println("Opção inválida! Tente novamente.");
                }
            } while (opcao != 0);
        }
    }
}