# Parte 2: cartas, peças e pontuação

> Atualização da revisão da 3ª iteração: o jogo usa `PecasDoJogo`, com cores
> e pedidos conferidos nas imagens do EAD e multiplicidades 15/15/14 assumidas.
> Jochi escolhe uma entre duas melhorias iniciais e pode ignorar a cor nas
> compras. O restante deste documento registra os contratos e cenários
> originais; o estado atual está em `docs/iteracao3.md`.


Implementação de Herdeiros de Khan baseada no `Regas.pdf` local, páginas impressas
2–4 e 11–20 (o arquivo contém duas páginas do manual em cada página do PDF).
Mantém Java 17, pacote `model`, classes com visibilidade de pacote e JUnit 4.
Não acrescenta Maven, Gradle ou dependências ao projeto Eclipse.

## Correspondência com a divisão do grupo

| Divisão original de War | Implementação de Herdeiros de Khan |
|---|---|
| Carta e Baralho | Conselheiros abertos/secretos; compra, distribuição e descarte sem reposição. |
| Objetivo e subclasses | `ObjetivoMapa` e `ObjetivoRecursos`, com um ou três patamares. |
| Trocas de cartas por exércitos | Não existem neste jogo; entregas de tesouros dão votos e o primeiro recebe bônus. |
| Reforços por territórios/continentes | Melhorias ampliam as ações; `Pontuacao` calcula influência por região. |
| Mínimo de três exércitos | Não se aplica às regras de Herdeiros de Khan. |

## Responsabilidades

| Classe | Responsabilidade |
|---|---|
| `Carta` | Nome e objetivo imutáveis; aberta ou secreta conforme os patamares. |
| `Baralho` | Lista embaralhada, pilha de descarte e `HashMap<Jogador, List<Carta>>`. A mesma peça não pode ser comprada ou descartada duas vezes. |
| `CartasOficiais` | Catálogo das 14 cartas abertas e 10 secretas das pp. 18–19. |
| `Objetivo` | Avaliação sem efeitos colaterais: condição cumprida e zero a três votos. |
| `ObjetivoMapa` | Yurts, conquistas, maior rede conectada, adjacências e regiões. |
| `ObjetivoRecursos` | Tributos, tesouros, bônus e padrões de melhorias. |
| `Inventario` | Tesouros físicos, quatro espaços por coluna, turno de aquisição e bônus usados/não usados. |
| `Melhoria` | Cor, símbolo e efeito; tesouros virtuais usam `TipoTesouro`. |
| `Conselheiro` | Parada, pedido de entrega e peça de bônus do primeiro jogador a entregar. |
| `ComponentesKhan` | Preparação e coordenação da parte 2 pelo contrato `ComponentesPartida`. |
| `Pontuacao` | Influência regional, desempate por tesouros restantes e distância, vitória compartilhada. |

## Ligação com a Partida

O construtor simples `Partida(tabuleiro, jogadores)` continua disponível para os
testes isolados das partes 1 e 3. Para incluir a parte 2, fornecer os componentes:

```java
// Dentro do pacote model. Listas configuradas com as peças do jogo.
ComponentesKhan componentes = new ComponentesKhan(
    pecasMelhoria, conselheiros, new Random());
Partida partida = new Partida(tabuleiro, jogadores, componentes, new Random());

// A futura interface apresenta as duas opções a cada jogador.
List<Carta> opcoes = componentes.cartasDe(jogador);
componentes.escolherCartaSecreta(jogador, opcoes.get(indiceEscolhido));
// Depois que todos escolherem:
partida.iniciar();
```

`validarPreparacao` é chamado antes das alterações em jogadores/cidades.
`preparar` distribui duas secretas por jogador, disponibiliza quatro abertas,
prepara os tesouros e o mercado. Jochi recebe uma melhoria aleatória na branca.
Cada instância de componentes pertence a uma única partida.

Exemplos executáveis de montagem e utilização estão em
`test/model/ComponentesKhanTest.java`. As listas pequenas desse teste são
cenários controlados, não a composição oficial das 44 peças.

### Operações durante o jogo

- `comprarMelhoria(indice)`: custa duas moedas, priorizando as virtuais.
  `-1` seleciona o topo da pilha; outros índices correspondem a `getMercado()`.
  O índice zero representa a peça mais à direita. Após retirar uma peça,
  as restantes ficam juntas à direita; a compra não repõe o mercado.
- `escolherMelhoriaGratuita(indice)`: preenche a branca ao completar uma linha
  das três cores. Jochi já ocupa o espaço da primeira linha.
- `partida.moverKhanParaMelhorias()`: entrega os tributos dos espaços
  descobertos e depois repõe até cinco melhorias. Da direita para a esquerda,
  os espaços são: sem tributo, espada, sem tributo, moeda, sem tributo (p. 9).
- `tesourosEm(cidade)` e `partida.atacarCidade(nome, tipo)`: permitem escolher
  uma peça disponível. A versão sem tipo seleciona a primeira peça.
  `pegarTesouroComMoeda(nome, tipo)` oferece a mesma escolha para Ogedei.
- `podeEntregarTesouros(nome, oferta)` consulta e `entregarTesouros(...)`
  executa uma entrega na parada visitada pelo peão ativo. A oferta é um
  `Map<TipoTesouro, Integer>` e deve corresponder exatamente ao pedido.
- `usarBonus(tipo)` aplica uma peça ainda não usada; o bônus de voto é
  aplicado automaticamente na primeira entrega. O bônus de tesouro abre
  `escolhasDeTesouro(jogador)` e é resolvido com `escolherTesouroDoBonus(tipo)`.
- `getVencedores()` retorna uma lista vazia antes do encerramento; depois,
  retorna o vencedor ou todos os jogadores que compartilham a vitória.

As cartas abertas são verificadas no turno do jogador da vez, inclusive na
ativação, antes de gastar os recursos virtuais. Uma carta cumprida sai de jogo
sem substituição. Cartas secretas e influência são aplicadas uma única vez ao
encerrar; esses votos não avançam a ficha neutra.

## Invariantes

- Compra, entrega ou escolha recusada preserva recursos e peças.
- Melhoria adquirida no turno atual não produz efeitos, mesmo se a compra
  acontecer antes de ativar a coluna.
- Tesouros virtuais existem em `Turno`, são consumidos antes dos físicos e
  expiram na troca de turno. Nunca entram no estoque físico nem no desempate.
- Tesouros físicos retirados das cidades são sincronizados com os contadores
  de `Cidade`. A reserva contém oito peças de cada um dos cinco tipos.
- Tesouros entregues e os dois recusados do bônus ficam no descarte.
- A pontuação regional exclui a branca e respeita os empates do `Tabuleiro`.
  Para 0/1/2/3/4 melhorias: primeiro recebe 1/1/2/2/3 votos; segundo, 0/1/1/2/2.
- Escolhas de tesouro ou melhoria gratuita impedem terminar o turno. Se não
  houver nenhuma melhoria disponível no mercado ou na pilha, uma recompensa
  sem peça não bloqueia a partida.
- Entregar tesouros fixa o peão escolhido por Altani, inclusive sem movimento.
- Os mutadores de `Inventario`, assim como os de `Tabuleiro` e `Jogador`, são
  operações internas de estado. A futura camada de controle deve usar as
  ações coordenadas de `Partida`/`ComponentesKhan` para respeitar custos e votos.

## Dados físicos e escolhas a conferir

O catálogo de objetivos e a tabela de influência foram conferidos no manual.
O PDF é monocromático e não enumera a cor de cada uma das 44 melhorias:
**a lista física completa de melhorias por cor ainda deve ser cadastrada**.
O construtor recebe essa lista explicitamente, sem inventar sua distribuição.
Os totais de símbolos do manual são: 8 moedas, 8 espadas, 8 yurts, 6 mãos,
9 cavalos e 1 de cada um dos 5 tesouros.

Os pedidos e bônus dos sete conselheiros também são configuração explícita.
O pedido vazio representa três tesouros quaisquer; nas outras paradas duplas
é um par de tipo específico; Karakorum exige três tipos distintos.
Os bônus devem ser embaralhados na montagem, e o pedido da capital depende
da face escolhida. O mapa aproximado existente continua sujeito à conferência
física já registrada no README. Não foi criada uma fábrica que se apresente
como configuração oficial completa sem esses dados.

`FERRO` e `PELES` são nomes usados no texto do manual; `GADO`, `TECIDO` e
`ESPECIARIAS` são rótulos adotados no código para os outros três desenhos.
Esses nomes podem ser ajustados sem mudar as regras de coleção e entrega.

Para o último desempate, a distância é o número de rotas até Karakorum, sem
descontar saltos por yurts; para Altani, usa-se o peão mais próximo. Para o
objetivo Viajante, contam-se yurts nas paradas conectadas (incluindo peças
duplas), sem ligar o centro de cidades às rotas. São interpretações dos casos
não detalhados no texto do manual e estão isoladas em `Pontuacao` e `ObjetivoMapa`.

## Eclipse e execução

1. Atualizar o projeto com **Refresh / F5**.
2. Confirmar um JDK 17 ou superior em **Java Build Path > Libraries**.
3. Executar `test/model/TodosOsTestes.java` em **Run As > JUnit Test**.

No terminal, o script agora respeita `JAVA_HOME`, sem precisar alterar o Java
padrão do computador:

```sh
JAVA_HOME="/caminho/do/jdk" ./scripts/run_tests.sh
```

A compilação usa `--release 17`. Nesta máquina, a validação foi executada com
o JDK 25 incluído no Eclipse, gerando classes compatíveis com Java 17.
