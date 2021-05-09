package uno;

import org.jgroups.*;
import org.jgroups.util.Util;

import java.io.*;
import java.util.List;
import java.util.LinkedList;
import java.util.Collections;

import java.util.concurrent.Semaphore;

public class Jogador implements Receiver {
    JChannel channel;
    String user_name=System.getProperty("user.name", "n/a");
    final List<String> state=new LinkedList<>(); // TODO: change this to what feels best.

    private final String cores[] = {"Verde", "Vermelho", "Azul", "Amarelo"};
    private LinkedList<Carta> baralho;
    public  LinkedList<Carta> mao;
    public  LinkedList<Carta> descarte;

    public  int tamanhoInicialMao = 5;
    private int turno;
    private int idMeuTurno;
    private View view;
    private Boolean jogoRodando = true;

    private final String cmdComprarCarta        = "ComprarCarta";
    private final String cmdcomprarMaoInicial   = "comprarMaoInicial";
    private final String cmdJogar               = "Jogar";
    private final String cmdFimTurno            = "FimTurno";
    private final String cmdFimDeJogo           = "FimDeJogo";

    private Semaphore semTurno;

    private void start() throws Exception {
        baralho  = new LinkedList<Carta>();
        descarte = new LinkedList<Carta>();
        mao      = new LinkedList<Carta>();
        turno    = 0;

        semTurno = new Semaphore(0);

        channel = new JChannel().setReceiver(this);
        channel.connect("UnoParty");
        if (view != null)
            idMeuTurno = view.getMembers().size() - 1;
        channel.getState(null, 10000);

        // primeiro a se conectar cria o baralho
        if (Util.isCoordinator(channel)) {
            criarBaralho();
            prepararState();
            // primeiro turno precisa liberar o semaforo
            semTurno.release(1);
        }

        eventLoop();
        channel.close();
    }

    public void comprarMaoInicial() {
        for (int i=0; i<tamanhoInicialMao; i++) {
            mao.add(baralho.get(i));
        }
        String linha = cmdcomprarMaoInicial;
        sendMacro(linha);
    }

    public void comprarCarta() {
        if (baralho.size() == 0) {
            // pega a carta do topo do descarte
            Carta cartaTopo = descarte.pollLast();
            // passa o descarte pro baralho
            baralho.addAll(descarte);
            descarte.clear();
            // adiciona ao descarte a carta topo
            descarte.add(cartaTopo);
        }
        mao.add(baralho.getFirst());
        String linha = cmdComprarCarta;
        sendMacro(linha);
    }

    public void jogarCarta(int idMao) {
        Carta carta = mao.get(idMao);
        mao.remove(idMao);
        String linha = cmdJogar +" "+ carta.toString();
        sendMacro(linha);
    }

    public void acabarJogo() {
        String linha = cmdFimDeJogo;
        sendMacro(linha);
    }

    public void passarTurno() {
        String linha = cmdFimTurno;
        sendMacro(linha);
    }

    private void sendMacro(String linha) {
        try {
            Message msg=new ObjectMessage(null, linha);
            channel.send(msg);
            System.out.println("mandando "+ linha);
            prepararState();
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
        // adiciona a primeira do baralho como o topo do descarte
        descarte.add(baralho.pollFirst());
    }

    public Boolean possivelJogarCarta() {
        Carta cartaTopo = descarte.getLast();
        for (Carta c : mao) {
            if (cartaTopo.combina(c)) {
                return true;
            }
        }
        return false;
    }

    public Boolean cartaInvalida(int indice_carta) {
        Carta cartaTopo = descarte.getLast();
        if (indice_carta < 0 || indice_carta >= mao.size() ) {
            //todo maybe do it under
            return true;
        }
        Carta cartaJogada = mao.get(indice_carta);
        if (cartaTopo.combina(cartaJogada)) {
            return false;
        }
        return true;
    }

    public void viewAccepted(View new_view) {
        System.out.println("** OLD VIEW view: " + view);

        Boolean pessoa_saiu = (
            (view != null) &&
            new_view.getMembers().size() < view.getMembers().size()
        );
        if (pessoa_saiu) {
            Address my_own_address = view.getMembers().get(idMeuTurno);

            for (int i=0; i<new_view.getMembers().size(); i++) {
                Address this_member = new_view.getMembers().get(i);
                if (my_own_address.compareTo(this_member) == 0) {
                    idMeuTurno = i;
                }
            }

            for (int i=0; i<view.getMembers().size(); i++) {
                if (new_view.getMembers().size() <= i) {
                    turno = 0;
                    break;
                }
                // se der diferente antes quem saiu nao foi o ultimo,
                // o turno fica igual e encerramos a operação
                if (view.getMembers().get(i).compareTo(new_view.getMembers().get(i)) != 0) {
                    break;
                }
            }
        }

        System.out.println("** NEW VIEW view: " + new_view);
        view = new_view;
    }

    public Boolean isMeuTurno() {
        return turno == idMeuTurno;
    }

    public void receive(Message msg) {
        String conteudo = msg.getObject();
        String line=msg.getSrc() + ": " + conteudo;
        System.out.println(line);

        if (view != null) {
            int id = view.getMembers().indexOf(msg.getSrc());   // id da pessoa q mandou a msg
            if (id == turno) {
                // aceito a msg
                // System.out.println("turno");
                if (conteudo.contains(cmdcomprarMaoInicial)) {
                    for (int i=0; i<tamanhoInicialMao; i++) {
                        baralho.removeFirst();
                    }
                }
                else if (conteudo.contains(cmdComprarCarta)) {
                    if (baralho.size() == 0) {
                        // pega a carta do topo do descarte
                        Carta cartaTopo = descarte.pollLast();
                        // passa o descarte pro baralho
                        baralho.addAll(descarte);
                        descarte.clear();
                        // adiciona ao descarte a carta topo
                        descarte.add(cartaTopo);
                    }
                    baralho.removeFirst();
                    System.out.println("Comprou uma carta");
                }
                else if (conteudo.contains(cmdJogar)) {
                    // extrair carta da msg
                    String conteudoSplt[] = conteudo.split(" ");
                    String cartaStr = conteudoSplt[1] + " " + conteudoSplt[2];
                    Carta cartaJogada = new Carta(cartaStr);
                    descarte.add(cartaJogada);
                    System.out.println("Jogada "+cartaJogada.toString());
                }
                else if (conteudo.contains(cmdFimTurno)) {
                    turno = (turno + 1) % view.getMembers().size();
                    // try  {
                    //     channel.getState(null, 10000);
                    // } catch(Exception e) { }
                    System.out.println("Fim de turno, proximo turno: "+turno);
                    semTurno.release(1);
                    // System.out.println(idMeuTurno); //TODO colocar mensagem bonitinha com operador ternario
                }
                else if (conteudo.contains(cmdFimDeJogo)) {
                    System.out.println("O jogo acabou! E eu, " + msg.getSrc() + " venci! Muhahah");
                    jogoRodando = false;
                }
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
        // list.forEach(System.out::println);
        System.out.println("Baralho : "+list.get(0));
        System.out.println("Descarte: "+list.get(1));
        System.out.println("Turno   : "+list.get(2));
    }

    private void eventLoop() throws Exception {
        // BufferedReader in=new BufferedReader(new InputStreamReader(System.in));

        Console cnsl = System.console();

        while(jogoRodando) {
            semTurno.acquire(1);
            while (isMeuTurno()) {
                if (mao.size() == 0) {
                    comprarMaoInicial();
                }

                // print mao
                System.out.println("\nCarta Topo "+descarte.getLast().toString());
                System.out.print("Mao: ");
                mao.forEach(System.out::println);
                // print Possibilidades
                System.out.print("Possibilidades: ");
                System.out.print(possivelJogarCarta() ? "jogar " : " ");
                System.out.print("comprar ");
                System.out.println("fimturno");

                // Ler input
                // String line=in.readLine();
                String line = cnsl.readLine();
                if(line.startsWith("quit") || line.startsWith("exit")) { break; }


                if (line.contains("jogar") && possivelJogarCarta()) {
                    System.out.println("\nCarta Topo "+descarte.getLast().toString());
                    System.out.println("Mao:");
                    for (int i=0; i<mao.size(); i++) {
                        System.out.println(i+": "+mao.get(i).toString());
                    }
                    // pega o indice da carta da mao
                    int ind = -1;
                    while (cartaInvalida(ind)) {
                        // line=in.readLine();
                        System.out.println("Digite uma carta valida");
                        line = cnsl.readLine();
                        ind = Integer.parseInt(line);
                    }
                    jogarCarta(ind);
                }
                else if (line.contains("comprar")) {
                    comprarCarta();
                    System.out.println("Comprada "+mao.getLast().toString());
                }
                else if (line.contains("fimturno")) {
                    passarTurno();
                }

                Boolean jogoAcabou = (mao.size() == 0);
                if (jogoAcabou) {
                    acabarJogo();
                    break;
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        new Jogador().start();
    }

}
