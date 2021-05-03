// package INE5418Uno;
// import INE5418Uno.Carta.*;

import org.jgroups.*;
import org.jgroups.util.Util;

import java.io.*;
import java.util.List;
import java.util.LinkedList;

import java.util.Collections;

public class Jogador implements Receiver {
    // Fix
    private class Carta {
        private int numero;
        private String cor;

        public Carta(int numero, String cor) {
            this.numero = numero;
            this.cor = cor;
        }

        public int getNumero() { return this.numero; }
        public String getCor() { return this.cor; }

        public String toString() {
            return (this.numero + " " + this.cor);
        }
    }


    JChannel channel;
    String user_name=System.getProperty("user.name", "n/a");
    final List<String> state=new LinkedList<>();

    private final String cores[] = {"Verde", "Vermelho", "Azul", "Amarelo"};
    private List<Carta> mao;
    public List<Carta> baralho;

    private Boolean aptoJogar = false;

    private void start() throws Exception {
        mao = new LinkedList<Carta>();
        baralho = new LinkedList<Carta>();
        criarBaralho();
        // for (Carta c : baralho) { System.out.println(c.toString()); }


        channel=new JChannel().setReceiver(this);
        channel.connect("UnoParty");
        channel.getState(null, 10000);
        eventLoop();
        channel.close();
    }

    private void criarBaralho() {
        baralho.clear();
        for (String cor : cores) {
            for (int i=0; i<=9; i++) {
                baralho.add(new Carta(i, cor));
                if (i != 0) {
                    baralho.add(new Carta(i, cor));
                }
            }
        }
        Collections.shuffle(baralho);
    }

    public void viewAccepted(View new_view) {
        System.out.println("** view: " + new_view);
    }

    public void receive(Message msg) {
        String line=msg.getSrc() + ": " + msg.getObject();
        System.out.println(line);
        synchronized(state) {
            state.add(line);
        }
    }

    public void getState(OutputStream output) throws Exception {
        synchronized(state) {
            Util.objectToStream(state, new DataOutputStream(output));
        }
    }

    public void setState(InputStream input) throws Exception {
        List<String> list=Util.objectFromStream(new DataInputStream(input));
        synchronized(state) {
            state.clear();
            state.addAll(list);
        }
        System.out.println("received state (" + list.size() + " messages in chat history):");
        list.forEach(System.out::println);
    }

    private void eventLoop() {
        BufferedReader in=new BufferedReader(new InputStreamReader(System.in));
        while(true) {
            try {
                System.out.print("> "); System.out.flush();
                String line=in.readLine().toLowerCase();
                if(line.startsWith("quit") || line.startsWith("exit")) {
                    break;
                }
                line="[" + user_name + "] " + line;
                Message msg=new ObjectMessage(null, line);
                channel.send(msg);
            }
            catch(Exception e) {
            }
        }
    }

    private void esperaInicioLoop() {
        BufferedReader in=new BufferedReader(new InputStreamReader(System.in));
        while(true) {
            try {
                synchronized(state) {


                }
            }
        }
    }


    public static void main(String[] args) throws Exception {
        new Jogador().start();
    }

}
