public class Cliente extends Pessoa {
    private String empresa;

    public Cliente(String id, String nome, String email, String empresa) {
        super(id, nome, email);
        this.empresa = empresa;
    }

    public String getEmpresa() { return empresa; }
    public void setEmpresa(String empresa) { this.empresa = empresa; }

    @Override
    public void exibirDetalhes() {
        System.out.println("[Cliente] ID: " + getId() + " | Nome: " + getNome() + 
                           " | E-mail: " + getEmail() + " | Empresa: " + empresa);
    }
}

