package INE5418Uno;

public class Carta {
    private int numero;
    private String cor;

    public Carta(int numero, String cor) {
        this.numero = numero;
        this.cor = cor;
    }

    public Carta(String definicao) {
        String separados[] = definicao.split(" ");
        this.numero = Integer.parseInt(separados[0]);
        this.cor    = separados[1];
    }

    public int    getNumero() { return this.numero; }
    public String getCor()    { return this.cor; }

    public String toString() {
        return (this.numero + " " + this.cor);
    }

    public static void main(String[] args) {
        System.out.println("nada");
    }
}
