// package INE5418Uno;
// import INE5418Uno.Carta.*;

import org.jgroups.*;
import org.jgroups.util.Util;

import java.io.*;
import java.util.List;
import java.util.LinkedList;

import java.util.Collections;
// import javafx.util.Pair;


public class Jogador implements Receiver {
    // Fix
    private class Carta {
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
    }


    JChannel channel;
    String user_name=System.getProperty("user.name", "n/a");
    final List<String> state=new LinkedList<>(); // TODO: change this to what feels best.
    // key == baralho, value == IDJogador atual
    // final Pair<List<Carta>, String> state = new Pair<>();
    // Integer key = pair.getKey();
    // String value = pair.getValue();

    private final String cores[] = {"Verde", "Vermelho", "Azul", "Amarelo"};
    private List<Carta> mao;
    public List<Carta> baralho;
    public List<Carta> descarte;

    // TODO Implement this (allow player to enter only in the first phase)
    // private Boolean aptoJogar = false;

    private void start() throws Exception {
        channel=new JChannel().setReceiver(this);
        channel.connect("UnoParty");
        channel.getState(null, 10000);

        if (Util.isCoordinator(channel)) {
            baralho  = new LinkedList<Carta>();
            descarte = new LinkedList<Carta>();
            criarBaralho();
            prepararState();
            // O lider recebe estado null e cria o baralho
            // setState(baralho)
        }


        mao = new LinkedList<Carta>();
        // mao = inicializarMao();
        inicializarMao(); // changes mao inplace.

        eventLoop();
        channel.close();
    }

    // private LinkedList<Carta> inicializarMao() {
    private void inicializarMao() {
        int tamanhoMao = 5;

        for (int i=0; i<tamanhoMao; i++) {
            this.mao.add(drawNewCard());
        }
    }

    // private Carta drawNewCard() {
    //     return this.baralho.pop()
    // }

    private Carta drawNewCard() {
        String command = "drawCard";
        Message msg=new ObjectMessage(null, command);
        // channel.send(msg); // aviso pra geral descartar uma carta

        // return // eu puxo do meu proprio baralho
        return new Carta("1 a");
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

        // if "drawCard" && IamLeaderIownTheBaralho{
        //     return drawCard
        // }
    }

    public void getState(OutputStream output) throws Exception {
        synchronized(state) {
            Util.objectToStream(state, new DataOutputStream(output));
        }
    }

    public void prepararState() throws Exception {
        String baralhoStr = "";
        for (Carta carta : baralho) {
            baralhoStr += carta.toString() + ",";
        }
        String descarteStr = "";
        for (Carta carta : descarte) {
            descarteStr += carta.toString() + ",";
        }
        int turno = 1;
        String turnoStr = Integer.toString(turno);

        synchronized(state) {
            state.clear();
            state.add(baralhoStr);
            state.add(descarteStr);
            state.add(turnoStr);
        }
    }

    public void setState(InputStream input) throws Exception {
        // Tem um historico de mensagens
        // e a gente quer passar: baralho - de quem eh o turno.
        // List<Carta> + TypeID -- state

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
        // tarefas:
        // começar o jogo --> semaforo?
        // circular entre jogadores
        //     jogador joga carta OU pesca uma carta
        //     checa se jogador nao tem cartas e encerra

        while(true) {
            try {
                System.out.print("> ");
                System.out.flush();
                // Fluxo de enviar mensagem
                String line=in.readLine().toLowerCase();
                if(line.startsWith("quit") || line.startsWith("exit")) {
                    break;
                }
                line="[" + user_name + "] " + line;

                // cria a mensagem e envia pro canal todo
                Message msg=new ObjectMessage(null, line);
                channel.send(msg);
            }
            catch(Exception e) {
            }
        }
    }

    // TODO: aptoJogar shenanigans esperando roles.
    // private void esperaInicioLoop() {
    //     BufferedReader in=new BufferedReader(new InputStreamReader(System.in));
    //     while(true) {
    //         try {
    //             synchronized(state) {


    //             }
    //         }
    //     }
    // }


    public static void main(String[] args) throws Exception {
        new Jogador().start();
    }

}
