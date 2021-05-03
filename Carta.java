package INE5418Uno;

public class Carta {
    private int numero;
    private String cor;

    public Carta(int numero, String cor) {
        this.numero = numero;
        this.cor = cor;
    }

    public int getNumero() { return this.numero; }
    public String getCor() { return this.cor; }

}
