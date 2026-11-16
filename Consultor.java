public class Consultor extends Pessoa {
    private String especialidade;

    public Consultor(String id, String nome, String email, String especialidade) {
        super(id, nome, email);
        this.especialidade = especialidade;
    }

    public String getEspecialidade() { return especialidade; }
    public void setEspecialidade(String especialidade) { this.especialidade = especialidade; }

    @Override
    public void exibirDetalhes() {
        System.out.println("[Consultor] ID: " + getId() + " | Nome: " + getNome() + 
                           " | E-mail: " + getEmail() + " | Especialidade: " + especialidade);
    }
}