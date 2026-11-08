# Herdeiros de Khan (INF1636, POO)

Implementação em Java 17 do jogo de tabuleiro Herdeiros de Khan, feita para o
trabalho de INF1636. O projeto é do Eclipse e segue MVC, com os padrões
Singleton, Façade e Observer do capítulo 17.

## Como rodar

No Eclipse: importe a pasta como projeto existente e rode `controller.Main`
(Run As > Java Application). Os testes rodam por `test/model/TodosOsTestes.java`
(Run As > JUnit Test).

Pelo terminal, com um JDK 17 ou mais novo (os scripts respeitam `JAVA_HOME`):

```bash
./scripts/run_game.sh                 # compila e abre o jogo
./scripts/run_tests.sh                # compila e roda os 212 testes
python3 scripts/package_delivery.py   # gera o ZIP da entrega em output/entrega/
```

## Como jogar

1. Na primeira janela, escolha de 2 a 5 jogadores, o nome de cada um e o
   herdeiro (não pode repetir). Se algo estiver errado, o aviso aparece na
   própria janela.
2. Cada jogador vê suas duas cartas secretas e fica com uma. Quem jogar com
   Jochi escolhe também a melhoria inicial da coluna branca.
3. No turno, ative uma coluna da cartela na aba Turno e clique no mapa para
   mover o peão e o Khan. Com Altani, clique no outro peão para trocar.
4. As outras abas têm o resto das ações: Ações (tributo, yurt, ataque e as
   habilidades de Ogedei e Chagatai), Melhorias (mercado e compra às cegas),
   Entregas (conselheiros e bônus) e Resultado.
5. Os botões Ver cartas e Ver cartela abrem janelas próprias com as imagens do
   jogo.

Clique que a regra não permite é ignorado, e o jogo não usa caixas de
diálogo: avisos e troca de turno aparecem nos próprios painéis.

## Arquitetura

- Model (`src/model`): regras, tabuleiro, cartas e pontuação. As classes
  têm visibilidade de pacote. As únicas públicas são `ModelAPI` (a fachada,
  com instância única) e as interfaces do Observer, `ObservadoIF` e
  `ObservadorIF`.
- View (`src/view`): janelas e painéis Swing. Tudo que é imagem do jogo
  (tabuleiro, cartas, cartelas, melhorias) é desenhado com
  `Graphics2D.drawImage()`. Os eventos usam classes internas que implementam
  `ActionListener` ou estendem `MouseAdapter`, sem lambdas.
- Controller (`src/controller`): `Controlador`, também Singleton, recebe
  os comandos da View, abre as janelas e diz em que etapa a partida está.

Quando um comando é aceito, a `ModelAPI` notifica os observadores e cada
painel consulta a fachada para se redesenhar. Os detalhes estão em
[docs/iteracao3.md](docs/iteracao3.md).

## Estrutura

```
src/model/        Model (só ModelAPI e as interfaces do Observer são públicas)
src/view/         Janelas, painéis e o catálogo de imagens
src/controller/   Controlador e Main
test/model/       Testes JUnit 4 do Model (mesmo pacote, pasta separada)
test/view/        Testes que clicam na interface
imagens/          tabuleiro.jpg e, em oficiais/, as fotos de cartas e cartelas do EAD
lib/              junit-4.13.2.jar e hamcrest-core-1.3.jar
scripts/          rodar o jogo, rodar os testes e gerar o ZIP
docs/             regras usadas, decisões e histórico de cada iteração
```

## Model

| Classe | Papel |
|---|---|
| `ModelAPI` | Fachada Singleton. Recebe e devolve só tipos simples (nomes de paradas, índices, listas). |
| `Partida`, `Turno`, `Fase`, `Movimento` | Preparação, sequência de turnos, gasto de cavalos e mãos, fim de jogo. |
| `Tabuleiro`, `Parada`, `Cidade`, `Provincia`, `Regiao` | O mapa: paradas ligadas por rotas, cidades com tesouros e províncias com tributos. |
| `MapaOficial` | Monta o `Tabuleiro` igual ao tabuleiro impresso. |
| `Jogador`, `Herdeiro`, `Peao`, `Coluna`, `Cor` | Jogadores, os 5 herdeiros com suas habilidades e as colunas da cartela. |
| `ComponentesKhan`, `Inventario`, `Melhoria` | Melhorias, tesouros, bônus e as escolhas pendentes do turno. |
| `Carta`, `Baralho`, `Objetivo`, `ObjetivoMapa`, `ObjetivoRecursos` | Os 14 conselheiros abertos e os 10 secretos. |
| `Conselheiro`, `TipoBonus`, `TipoTesouro`, `TipoTributo` | Entregas e recompensas. |
| `Pontuacao` | Influência por região, cartas secretas e desempates. |
| `PecasDoJogo`, `ConfiguracaoPecas` | Cadastro das peças e validação dos totais. |

O mapa tem 34 paradas: em cada região, 9 simples (3 em volta de cada cidade)
e 2 duplas de conselheiro, mais Karakorum no centro, ligado a 4 rotas. São 12
províncias, 4 de cada tributo, e 4 delas têm cabeça do Khan. A posição de cada
parada na imagem fica em `view.CoordenadasMapa`.

### Nomes da divisão original

O enunciado da 1ª iteração usava os termos do War. A tabela mostra o que
virou o quê:

| No enunciado | Neste projeto | Observação |
|---|---|---|
| `Territorio` | `Parada` | Não tem um dono só; guarda yurts de várias cores. |
| `HashMap<String, Territorio>` | `Tabuleiro.paradas` | |
| `HashMap<Territorio, Set<Territorio>>` | `Tabuleiro.rotas` | Rotas sempre nos dois sentidos. |
| `Continente` e bônus | `Regiao` | O "bônus" são os votos por maioria de yurts no fim. |
| `getTerritorio(nome)` | `getParada(nome)` | Lança `IllegalArgumentException` se não existir. |
| `saoVizinhos(a, b)` | `saoVizinhas(a, b)` | |
| `donoDe(territorio)` | `temYurtDe(parada, jogador)` | |
| `exercitosEm(territorio)` | `yurtsEm(parada)` | |
| `adicionarExercitos` / `removerExercitos` | `colocarYurt` / `removerYurt` | Mexem no estoque do jogador. |
| `territoriosDe(jogador)` | `paradasComYurtDe(jogador)` | |
| `dominaContinente(jogador, continente)` | `temMaioriaDeYurts(jogador, regiao)` | Empate segue o manual (p. 15). |

## Testes

São 212 testes JUnit 4. Os testes de regra trazem no Javadoc o objetivo, a
entrada e a saída esperada. Alguns dos principais:

- `TabuleiroTest` e `MapaOficialTest`: vizinhança, capacidade das paradas,
  maioria por região e as invariantes do mapa oficial.
- `JochiTest` e `ObjetivosTest`: as regras de herdeiros e objetivos que
  dependem do FAQ do manual.
- `PartidaCompletaInterfaceTest`: uma partida inteira só clicando nos botões e
  no mapa.
- `RegrasEmPartidasAleatoriasTest`: 40 partidas com sorteio fixo, conferindo
  as regras do manual depois de cada jogada.

## Documentação

- [docs/iteracao3.md](docs/iteracao3.md): padrões, imagens, regras
  interpretadas e um roteiro para testar o jogo na mão.
- [docs/regras-para-o-model.md](docs/regras-para-o-model.md): resumo das
  regras do manual usadas no Model.
- [docs/iteracao2-divisao.md](docs/iteracao2-divisao.md): divisão do trabalho
  na 2ª iteração.
- `docs/parte2-*`, `docs/parte3-*`, `docs/testes-parte*`: como cada parte da
  1ª iteração foi integrada e testada.

Salvar e recuperar uma partida fica para a última iteração.
