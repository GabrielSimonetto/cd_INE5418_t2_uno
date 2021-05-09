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
    public int turno;

    private View view;

    private final String cmdComprar  = "Comprar";
    private final String cmdJogar    = "Jogar";
    private final String cmdFimTurno = "FimTurno";

    // TODO Implement this (allow player to enter only in the first phase)
    // private Boolean aptoJogar = false;

    private void start() throws Exception {
        baralho  = new LinkedList<Carta>();
        descarte = new LinkedList<Carta>();
        turno = 0;

        channel=new JChannel().setReceiver(this);
        channel.connect("UnoParty");
        channel.getState(null, 10000);

        if (Util.isCoordinator(channel)) {
            criarBaralho();
            prepararState();
        }

        jogarCarta(baralho.get(0));

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

    public void jogarCarta(Carta carta) {
        try {
            String linha = cmdJogar + carta.toString();
            Message msg=new ObjectMessage(null, linha);
            channel.send(msg);
        } catch(Exception e) { }
    }

    private void criarBaralho() {
        baralho.clear();
        for (String cor : cores) {
            for (int i=0; i<=9; i++) {
                baralho.add(new Carta(i, cor));
                if (i != 0) {   // duas cartas com msm cor e numero, caso seja diferente de 0
                    baralho.add(new Carta(i, cor));
                }
            }
        }
        Collections.shuffle(baralho);
    }

    public void viewAccepted(View new_view) {
        System.out.println("** view: " + new_view);
        view = new_view;
    }

    public void receive(Message msg) {
        String conteudo = msg.getObject();
        String line=msg.getSrc() + ": " + conteudo;

        if (view != null) {
            // acho o id da pessoa q mandou a msg
            int id = view.getMembers().indexOf(msg.getSrc());
            if (id == turno) {
                // aceito a msg
                if (conteudo.contains(cmdJogar)) {
                    // extrair carta da msg
                    
                    descarte.add()
                }
                System.out.println(line);
            }
        }

    }

    public void prepararState() throws Exception {
        // baralho
        String baralhoStr = "";
        for (Carta carta : baralho) {
            baralhoStr += carta.toString() + ",";
        }
        // descarte
        String descarteStr = "";
        for (Carta carta : descarte) {
            descarteStr += carta.toString() + ",";
        }
        // turno
        String turnoStr = Integer.toString(turno);

        synchronized(state) {
            state.clear();
            state.add(baralhoStr);
            state.add(descarteStr);
            state.add(turnoStr);
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

        // baralho
        baralho.clear();
        String baralhoStr = state.get(0);
        if (baralhoStr.length() > 1) {
            String baralhoStrSeparado[] = baralhoStr.split(",");
            for (String cartaStr : baralhoStrSeparado) {
                baralho.add(new Carta(cartaStr));
            }
        }
        // descarte
        descarte.clear();
        String descarteStr = state.get(1);
        if (descarteStr.length() > 1) {
            String descarteStrSeparado[] = descarteStr.split(",");
            for (String cartaStr : descarteStrSeparado) {
                descarte.add(new Carta(cartaStr));
            }
        }
        // Turno
        String turnoStr = state.get(2);
        if (turnoStr.length() > 0) {
            turno = Integer.parseInt(turnoStr);
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
