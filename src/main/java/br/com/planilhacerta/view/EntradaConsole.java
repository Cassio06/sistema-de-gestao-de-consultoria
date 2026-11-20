package br.com.planilhacerta.view;

import java.util.NoSuchElementException;
import java.util.Scanner;

final class EntradaConsole {
    private EntradaConsole() {
    }

    static String lerLinha(Scanner scanner) {
        if (!scanner.hasNextLine()) {
            throw new Encerrada();
        }
        return scanner.nextLine();
    }

    static final class Encerrada extends NoSuchElementException {
        private static final long serialVersionUID = 1L;
    }
}
