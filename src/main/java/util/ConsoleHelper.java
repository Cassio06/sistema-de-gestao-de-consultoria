package util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

public class ConsoleHelper {
    private static final Scanner scanner = new Scanner(System.in);
    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static String lerTextoObrigatorio(String mensagem) {
        while (true) {
            System.out.print(mensagem);
            String entrada = scanner.nextLine().trim();
            if (!entrada.isEmpty()) {
                return entrada;
            }
            System.out.println("[ERRO] Este campo não pode ficar vazio. Tente novamente.");
        }
    }

    public static int lerInteiro(String mensagem, int min, int max) {
        while (true) {
            System.out.print(mensagem);
            String entrada = scanner.nextLine().trim();
            try {
                int valor = Integer.parseInt(entrada);
                if (valor >= min && valor <= max) {
                    return valor;
                }
                System.out.printf("[ERRO] Digite um número entre %d e %d.%n", min, max);
            } catch (NumberFormatException e) {
                System.out.println("[ERRO] Entrada inválida! Digite apenas números .");
            }
        }
    }

    public static double lerDoublePositivo(String mensagem) {
        while (true) {
            System.out.print(mensagem);
            String entrada = scanner.nextLine().trim().replace(",", ".");
            try {
                double valor = Double.parseDouble(entrada);
                if (valor > 0) {
                    return valor;
                }
                System.out.println("[ERRO] O valor deve ser maior que zero.");
            } catch (NumberFormatException e) {
                System.out.println("[ERRO] Entrada inválida! Digite um valor numérico (ex: 1500.00).");
            }
        }
    }

    public static LocalDate lerData(String mensagem) {
        while (true) {
            System.out.print(mensagem + " (dd/MM/aaaa): ");
            String entrada = scanner.nextLine().trim();
            try {
                return LocalDate.parse(entrada, FORMATO_DATA);
            } catch (DateTimeParseException e) {
                System.out.println("[ERRO] Data inválida! Use o formato dia/mês/ano (ex: 25/11/2026).");
            }
        }
    }

    public static void pausar() {
        System.out.print("\nPressione ENTER para continuar...");
        scanner.nextLine();
    }
}
