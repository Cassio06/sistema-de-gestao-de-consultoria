package util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class ConsoleHelper {
    private static final Scanner scanner = new Scanner(System.in);
    private static final DateTimeFormatter FORMATO_DATA_ESTRITO =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);

    private static String lerLinhaSegura() {
        try {
            if (!scanner.hasNextLine()) {
                System.out.println("\n[INFO] Entrada encerrada pelo usuário. Finalizando sistema...");
                System.exit(0);
            }
            return scanner.nextLine().trim();
        } catch (NoSuchElementException | IllegalStateException e) {
            System.out.println("\n[INFO] Entrada interrompida. Finalizando sistema...");
            System.exit(0);
            return "";
        }
    }

    public static String lerTextoObrigatorio(String prompt) {
        while (true) {
            System.out.print(prompt);
            String entrada = lerLinhaSegura();
            if (!entrada.isEmpty()) {
                return entrada;
            }
            System.out.println("[ERRO] Este campo é obrigatório e não pode ficar vazio.");
        }
    }

    public static int lerInteiro(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            String entrada = lerLinhaSegura();
            try {
                int valor = Integer.parseInt(entrada);
                if (valor < 0) {
                    System.out.println("[ERRO] Números negativos não são permitidos.");
                    continue;
                }
                if (valor >= min && valor <= max) {
                    return valor;
                }
                System.out.printf("[ERRO] Digite uma opção entre %d e %d.%n", min, max);
            } catch (NumberFormatException e) {
                System.out.println("[ERRO] Entrada inválida! Digite apenas números inteiros.");
            }
        }
    }

    public static double lerDecimalPositivo(String prompt) {
        while (true) {
            System.out.print(prompt);
            String entrada = lerLinhaSegura().replace(",", ".");
            try {
                double valor = Double.parseDouble(entrada);
                if (!Double.isFinite(valor) || valor <= 0) {
                    System.out.println("[ERRO] Digite um valor numérico válido e maior que zero.");
                    continue;
                }
                return valor;
            } catch (NumberFormatException e) {
                System.out.println("[ERRO] Valor inválido! Digite um número (ex: 1500.00).");
            }
        }
    }

    public static LocalDate lerData(String prompt) {
        while (true) {
            System.out.print(prompt + " (dd/MM/aaaa): ");
            String entrada = lerLinhaSegura();
            try {
                return LocalDate.parse(entrada, FORMATO_DATA_ESTRITO);
            } catch (DateTimeParseException e) {
                System.out.println("[ERRO] Data inexistente ou formato inválido! Use dd/MM/aaaa (ex: 28/02/2027).");
            }
        }
    }

    public static void pausar() {
        System.out.print("\nPressione ENTER para continuar...");
        lerLinhaSegura();
    }
}