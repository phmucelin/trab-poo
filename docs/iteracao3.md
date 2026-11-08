# 3ª iteração

Nesta iteração a partida inteira passou a ser jogada pela interface, da
escolha dos herdeiros até o resultado. Este arquivo explica como os padrões
pedidos foram usados, as regras que tivemos de interpretar e um roteiro para
testar o jogo na mão.

## Padrões

### Singleton e Façade

`ModelAPI` é a fachada do Model e tem uma instância
só (`ModelAPI.getInstance()`). Fora ela e as duas interfaces do Observer,
nenhuma classe do pacote `model` é pública, então View e Controller não têm
como mexer no Model por outro caminho. A fachada só recebe e devolve
`String`, `int`, `boolean`, listas e mapas desses tipos.

### Observer

Segue o capítulo 17: `ObservadoIF` (`add`, `remove`) é
implementada pela `ModelAPI`, e `ObservadorIF` (`notify`) pelos painéis e
janelas. Depois de cada comando aceito, a fachada chama `notify(this)` em todos
os observadores, e cada um consulta o que precisa para se redesenhar. Comando
recusado não notifica ninguém.

O enunciado cita a abertura da janela da cartela e da janela de cartas como
mudanças de estado. Por isso o estado "janela aberta" fica na `ModelAPI`:
o botão pede ao Controlador para abrir, o Model muda o estado e notifica, e a
janela fica visível ao receber o `notify`. O mesmo vale para fechar, inclusive
pelo X da janela.

### Controller

`Controlador` também é Singleton. A View chama só os métodos
dele para agir. As regras ficam no Model (o Controlador não decide se uma
jogada vale). O que ele organiza é a sequência da partida:

- abre a `JanelaInicial`, cria a partida e abre a `JanelaJogo`;
- cria e reaproveita as janelas de cartas e de cartela;
- `getEtapaPartida()` diz em que etapa a partida está (preparação, jogo,
  escolha obrigatória de tesouro ou de melhoria, resultado), e a `JanelaJogo`
  usa isso para trocar de tela ou de aba quando recebe o `notify`.

## Imagens

Cartas, cartelas e melhorias usam as fotos do EAD, em `imagens/oficiais/`, e
são desenhadas com `Graphics2D.drawImage()`. Nenhum JLabel ou botão carrega
imagem.

O Model não sabe nada de arquivos. Ele expõe ids como `SECRETA_VIAJANTE` ou
`MELHORIA_VERDE_TECIDO`, e o `CatalogoImagens` (na View) liga cada id ao seu
arquivo por um `HashMap`. São 49 imagens: 24 cartas, 5 cartelas e 20 tipos de
melhoria. As cartelas e uma das cartas foram reduzidas para 2000 px, porque
as fotos originais tinham quase 6000 px e deixavam o ZIP com mais de 50 MB.

- `PainelCartas`: carta secreta (escondida até o jogador clicar em Revelar) e
  as quatro cartas abertas, com o progresso de cada objetivo.
- `PainelCartela`: a cartela do herdeiro com as melhorias desenhadas por cima
  dos espaços de cada coluna, de baixo para cima.
- `PainelImagem`: as opções da preparação e o mercado de melhorias.

## Dados do jogo

`PecasDoJogo` guarda o cadastro das peças, e a partida completa é o que abre
por padrão. Os pedidos dos conselheiros foram conferidos no tabuleiro:

| Parada | Pedido |
|---|---|
| Conselheiro Norte | TECIDO:2 |
| Conselheiro Nordeste | FERRO:2 |
| Conselheiro Oeste | PELES:2 |
| Conselheiro Leste | GADO:2 |
| Conselheiro Sudoeste | ESPECIARIAS:2 |
| Conselheiro Sul | três tesouros quaisquer |
| Karakorum | FERRO:1, PELES:1, GADO:1 (face impressa no tabuleiro) |

Nas melhorias, os tesouros têm cor fixa: gado e especiarias são amarelas,
ferro é vermelha, peles e tecido são verdes. Moeda, espada, yurt, mão e
cavalos aparecem nas três cores.

O manual só diz que são 44 peças de melhoria, sem dizer quantas de cada cor.
Usamos 15 amarelas, 15 vermelhas e 14 verdes, mantendo os totais por símbolo
(8 moedas, 8 espadas, 8 yurts, 6 mãos, 9 cavalos e 1 de cada tesouro). Essa
divisão por cor é suposição nossa.

## Regras que precisaram de interpretação

### Jochi

A cartela diz para pegar 2 melhorias ao acaso e pôr uma na coluna
branca. Na preparação, Jochi escolhe entre as duas. A que sobra volta para o
fundo da pilha; a cartela não diz o que fazer com ela, então essa parte é
decisão nossa. Nas compras, Jochi não precisa seguir a cor da peça, mas só
escolhe entre as três colunas coloridas: a branca só recebe as melhorias
grátis das linhas completas (manual, p. 11 e 12). Como a melhoria inicial já ocupa
o espaço da primeira linha, completar essa linha não dá prêmio (FAQ, p. 20).

### Tolui

O yurt que Tolui começa em Karakorum ajuda no movimento e conta
nos objetivos abertos de 6 e 10 yurts no mapa. Pelo FAQ (p. 20) ele não conta
para mais nada: não liga a rede do Viajante e não vale como yurt vizinho de
cidade ou de província do Khan.

### Parada dupla

Cabem 2 peões (p. 7), contando os dois de Altani.

## Janela

A `JanelaJogo` não passa de 1400 × 1050 e também não passa da área útil da
tela (sem a barra de menus e o Dock). Num notebook de 1512 × 982 a janela
encolhe e o tabuleiro é desenhado menor, para que a área de melhorias, no
rodapé do mapa, continue ao alcance do mouse.

## Testes

São 212 testes JUnit 4 (`./scripts/run_tests.sh` ou `TodosOsTestes` no
Eclipse). Além dos testes de cada regra, há dois testes maiores:

- `PartidaCompletaInterfaceTest` joga uma partida inteira só clicando nos
  botões e no mapa.
- `RegrasEmPartidasAleatoriasTest` joga 40 partidas com sorteio fixo, de 2 a
  5 jogadores, e confere as regras do manual depois de cada jogada.

## Roteiro para testar na mão

1. Comece uma partida com 2 a 5 jogadores. Cada um revela e escolhe a carta
   secreta. Se tiver Jochi, ele escolhe também a melhoria inicial.
2. Na aba Turno, ative uma coluna e mova o peão e o Khan clicando no mapa.
   Com Altani, clique no outro peão para trocar.
3. Na aba Ações, pegue tributo, construa yurt, ataque uma cidade e use as
   habilidades de Ogedei e Chagatai.
4. Clique em Ver cartas, revele e esconda a carta secreta e feche a janela
   (pelo botão e pelo X). Ao terminar o turno, a carta tem que sumir.
5. Clique em Ver cartela, troque o jogador no seletor e, com a janela aberta,
   compre uma melhoria na aba Melhorias para ver a cartela atualizar.
6. Complete uma linha de melhorias e pegue a grátis para a branca. Leve o
   Khan até a área de melhorias para repor o mercado.
7. Na aba Entregas, entregue tesouros num conselheiro visitado e use o bônus.
8. Use Traçar rota no mapa para escolher se salta ou visita um yurt próprio no
   caminho; clique de novo no destino para mover.
9. Jogue até o fim e confira votos, Kurultai e desempates na aba Resultado.

## Entrega

`python3 scripts/package_delivery.py` gera
`output/entrega/HerdeirosDeKhan-iteracao3.zip` com o projeto Eclipse (fontes,
testes, imagens, bibliotecas, scripts e documentação), sem `.git` e sem
classes compiladas. Salvar e recuperar partida fica para a última iteração.
