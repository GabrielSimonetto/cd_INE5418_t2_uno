# O INCRIVEL UNO

## Para rodar:

Se voce quiser recompilar use `make all`
Se voce quiser rodar algo ja compilado, use `make run`


## TODO:

- [ ] Programar cartas 1 a 9 com 4 naipes

    - [ ] pode ser umas tupla doida eu sei la
    - [ ] nao precisa se preocupar com carta duplicada no random eu acho
    - [ ] Definir tamanho da mão em algum lugar global (5)
    - [ ] Se vc nao tem carta pra jogar na tua vez tu pesca uma vez

- [ ] Ter uma etapa de receber gente
    - [ ] o dono da sala fala se eh pra começar
    - [ ] se ele nao falar nada da pra entrar mais gente

- [ ] Ai dps dessa etapa entra em while loop de jogatinas até uma pessoa fica sem cartas
    - [ ] ou até o dono dizer "sair" eu acho sei la como eu sei quem eh o dono nao sei deixa quieto

- [ ] Se uma pessoa cair tem que tirar ela do iterador que roda entre os jogadores
    - [ ] o que eh um bom ponto, como diabos gira entre os jogadores


## problemas

[x] - o primeiro jogador nao ve o topo do discarte.
- checagem de carta a ser jogada no monte
    [x] - nao deixar entrar na mao se nao tiver nada
    [x] - se entrar na mao soh deixar jogar as cartas validas.
    [x] - topo era 8 verde, nao identificava que 2 verde era uma entrada valida.
        R: comparação bosta do java.

[x] - Consertar se a pessoa que tem o turno sair. (crasha)
[x] - Se dono sai e ta na segunda pessoa ela fica com o turno indefinidamente

[ ] - ocultar o baralho quando uma pessoa entra. -> line 288
[x] - ver como o jogo termina
[x] - terminar o jogo (mostra VENCEDOR e reinicia o drawing de mao)
[ ] - TODO busy waiting

[ ] - tirar o simple chat do trabalho e remover comentarios

## ideias

- toda vez que rodar o turno, junto com a mão, mostrar o topo do discarte

- passar turno apos uma unica ação 
    (tirar fimturno 
        (mas deixar ele vivo internamente pra debuggar coisas talvez)
    )