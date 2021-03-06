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
                // duas cartas com msm cor e numero, caso seja diferente de 0
                if (i != 0) {
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
        if (indice_carta < 0 || indice_carta >= mao.size() ) {
            return true;
        }

        Carta cartaTopo = descarte.getLast();
        Carta cartaJogada = mao.get(indice_carta);
        if (cartaTopo.combina(cartaJogada)) {
            return false;
        }
        return true;
    }

    public void viewAccepted(View new_view) {
        Boolean pessoa_saiu = (
            (view != null) &&
            new_view.getMembers().size() < view.getMembers().size()
        );

        if (pessoa_saiu) {
            // pego o endereco da antiga view
            Address my_own_address = view.getMembers().get(idMeuTurno);
            // atualizo idMeuTurno com a nova view
            for (int i=0; i<new_view.getMembers().size(); i++) {
                Address this_member = new_view.getMembers().get(i);
                if (my_own_address.compareTo(this_member) == 0) {
                    idMeuTurno = i;
                }
            }
            // turno de alguem que saiu, e esta inalcancavel agora eh zerado
            if (turno >= new_view.getMembers().size()) {
                turno = 0;
                System.out.println("Jogador saiu, turno passado para "+turno);
            }
            // se for meu turno
            if (isMeuTurno()) {
                semTurno.release(1);
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
            // id da pessoa q mandou a msg
            int id = view.getMembers().indexOf(msg.getSrc());
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
                    System.out.println("Fim de turno, proximo turno: "+turno);
                    semTurno.release(1);
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
    }

    private void eventLoop() throws Exception {
        Console cnsl = System.console();

        while(jogoRodando) {
            semTurno.acquire(1);
            while (isMeuTurno()) {
                // print mao
                System.out.println("\nCarta Topo "+descarte.getLast().toString());
                System.out.print("Mao: ");
                mao.forEach(System.out::println);
                // print Possibilidades
                System.out.print("Possibilidades: ");
                if (mao.size() == 0) {
                    System.out.println("comprarMao");
                } else {
                    System.out.print(possivelJogarCarta() ? "jogar " : " ");
                    System.out.print("comprar ");
                    System.out.println("fimturno");
                }

                String line = cnsl.readLine();
                if(line.startsWith("sair") || line.startsWith("exit")) {
                    jogoRodando = false;
                    break;
                }
                if (line.contains("comprarMao") && mao.size() == 0) {
                    comprarMaoInicial();
                }
                else {
                    if (line.contains("jogar") && possivelJogarCarta()) {
                        System.out.println("\nCarta Topo "+descarte.getLast().toString());
                        System.out.println("Mao:");
                        for (int i=0; i<mao.size(); i++) {
                            System.out.println(i+": "+mao.get(i).toString());
                        }
                        // pega o indice da carta da mao
                        int ind = -1;
                        while (cartaInvalida(ind)) {
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
                }
                // jogo acabou
                if (mao.size() == 0) {
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
