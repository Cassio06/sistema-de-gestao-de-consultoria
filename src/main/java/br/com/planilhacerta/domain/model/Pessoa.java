package br.com.planilhacerta.domain.model;

public abstract class Pessoa {
    private final String id;
    private String nome;
    private String email;

    protected Pessoa(String id, String nome, String email) {
        this.id = textoObrigatorio(id, "Identificador");
        this.nome = textoObrigatorio(nome, "Nome");
        this.email = textoObrigatorio(email, "E-mail");
    }

    public String getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = textoObrigatorio(nome, "Nome");
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = textoObrigatorio(email, "E-mail");
    }

    protected static String textoObrigatorio(String texto, String campo) {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException(campo + " deve ser informado.");
        }
        return texto.trim();
    }
}
