# Herdeiros de Khan: Trabalho de INF1636 (POO)

Projeto Eclipse em Java 17, organizado em MVC. A 3ª iteração integra a partida
completa pela interface e incorpora as correções da revisão do PR #8.

## Executar

No Eclipse, importe o projeto existente e execute `controller.Main`.
Pelo terminal, com JDK 17 ou superior:

```bash
./scripts/run_game.sh
./scripts/run_tests.sh
python3 scripts/package_delivery.py
```

Os scripts Java respeitam `JAVA_HOME`. A tela inicial pede os jogadores e os
herdeiros e prepara automaticamente o jogo completo com `model.PecasDoJogo`.
Cada jogador escolhe sua carta secreta. Jochi também escolhe uma entre duas
melhorias para a branca antes de iniciar os turnos.

## Interface da 3ª iteração

- **Turno:** ativar colunas, acompanhar Kurultai e terminar o turno. Os botões
  **Ver cartas** e **Ver cartela** abrem as janelas próprias.
- **Ações:** coletar tributos, construir yurts, atacar cidades escolhendo
  tesouros e usar as habilidades de Ogedei e Chagatai.
- **Melhorias:** comprar no mercado ou às cegas da pilha e resolver recompensas
  para a branca. Jochi escolhe a coluna da compra, respeitando os quatro espaços.
- **Entregas:** selecionar tesouros, entregar aos conselheiros e usar os bônus.
  Escolhas obrigatórias são apresentadas antes de permitir encerrar o turno.
- **Cartas e Resultado:** consultar objetivos na janela de cartas e apresentar a pontuação final,
  influência, cartas secretas e critérios de desempate.

As janelas de cartas e cartela desenham as imagens oficiais do EAD com
`Graphics2D.drawImage()`. A cartela mostra a imagem do herdeiro e as melhorias
sobre cada coluna. Os identificadores do Model são ligados às fotos por um
`HashMap` da View. Abertura, fechamento, seleção de cartela, compras e troca de
turno atualizam as janelas pelo Observer. A carta secreta volta a ficar oculta
na troca de turno e ao fechar sua janela.

Peões e Khan se movem por clique no mapa. A opção **Traçar rota no mapa**
permite definir cada parada, escolher saltar ou visitar yurts próprios e
executar clicando novamente no destino.

## Dados e entrega

Os pedidos dos conselheiros e as combinações de cor e símbolo das melhorias
foram conferidos na revisão contra as imagens oficiais. As **quantidades por
cor são uma hipótese documentada**: 15 amarelas, 15 vermelhas e 14 verdes,
respeitando os 44 componentes e os totais por símbolo. Essa multiplicidade
não é comprovada pelas fotos dos tipos de peça.

A suíte atual tem **205 testes**, incluindo Jochi, dados, imagens reais,
Observer, escolhas e uma partida completa pelos controles Swing. O pacote
fica em `output/entrega/HerdeirosDeKhan-iteracao3.zip`.

Roteiro de conferência: [docs/entrega-iteracao3.md](docs/entrega-iteracao3.md).
Detalhes da integração: [docs/iteracao3.md](docs/iteracao3.md).

Os PDFs e ZIPs originais do EAD ficam na pasta local `.context/`, ignorada
pelo Git. As imagens utilizadas pela aplicação estão em `imagens/oficiais/`
e fazem parte do pacote. Salvamento e recuperação ficam para a última iteração.

## 2ª iteração: tela gráfica

Para jogar, execute `controller.Main` (no Eclipse: *Run As > Java Application*).

1. A `JanelaInicial` pede de 2 a 5 jogadores, o nome de cada um e o herdeiro
   (sem repetição). Configuração inválida aparece como aviso na própria janela.
2. A `JanelaJogo` mostra o tabuleiro e, ao lado, o painel do turno.
3. No painel do turno o jogador ativa uma coluna da cartela e termina o turno.
   No mapa, clica na parada de destino para mover o peão, no outro peão de
   Altani para trocá-lo e num local do Khan para movê-lo.
4. Clique recusado é ignorado, sem mensagem. Troca de turno e fim da partida
   aparecem no painel do turno; o jogo não usa caixas de diálogo.

Arquitetura: View e Controller falam com o Model só pela fachada
`model.ModelAPI` (Facade + Singleton); as telas se atualizam pelo Observer
(`ObservadorIF`/`ObservadoIF`). Detalhes do contrato em `docs/iteracao2-divisao.md`.

## Estrutura do projeto

```
.project / .classpath / .settings   Projeto Eclipse (JavaSE-17, JUnit 4 em lib/)
src/model/                          Pacote Model; só a fachada ModelAPI e as interfaces do Observer são públicas
src/view/                           Janelas e painéis Swing (tabuleiro desenhado com drawImage())
src/controller/                     Controlador (Singleton) e Main, o ponto de entrada do jogo
test/model/                         Testes JUnit 4 (mesmo pacote, pasta de fontes separada)
imagens/                            tabuleiro.jpg, a imagem oficial do tabuleiro publicada no EAD
lib/                                junit-4.13.2.jar e hamcrest-core-1.3.jar
scripts/run_tests.sh                Roda os testes pela linha de comando (sem Eclipse)
docs/                               Regras usadas pelo Model, integração das partes e divisão da 2ª iteração
```

Conforme o enunciado, as classes do Model **não são públicas**: só o pacote
`model` as enxerga. As únicas públicas são a fachada `ModelAPI` (Façade +
Singleton), por onde View e Controller usam o Model, e as interfaces do
Observer (`ObservadorIF` e `ObservadoIF`). As coleções usam o framework de
coleções do Java, principalmente `HashMap`, `HashSet` e `ArrayList`.

## Classes do Model (parte 1: tabuleiro e jogadores)

| Classe | Papel |
|---|---|
| `Cor` (enum) | As 5 cores de jogador. |
| `Regiao` (enum) | Rússia, China e Pérsia; cada uma ligada a uma coluna colorida da cartela. |
| `Coluna` (enum) | Vermelha, Verde, Amarela e Branca (colunas de ações da cartela). |
| `TipoTributo` (enum) | Espada, Yurt e Moeda. |
| `TipoParada` (enum) | Simples (1 peão / 1 yurt), Dupla (2 / 2, tem conselheiro) e Karakorum (todos / 0). |
| `Parada` | Nó das rotas. Guarda os yurts construídos nela, respeitando a capacidade. |
| `Cidade` | Uma das 9 cidades: revelada → 4 tesouros → último tesouro conquista (yurt de conquista). |
| `Provincia` | Área entre rotas com um tipo de tributo (máx. 3 peças); 4 delas são "do Khan" e apontam para 2 vizinhas. |
| `Peao` | Peão de um jogador (Altani tem 2). |
| `Herdeiro` (enum) | Os 5 herdeiros, habilidade e as 4 colunas de ações transcritas das cartelas. |
| `Jogador` | Cor, herdeiro, peões, estoque de 12 yurts, 4 peças de ativação, tributos e votos. |
| `Tabuleiro` | `HashMap<String, Parada>` + `HashMap<Parada, Set<Parada>>` (rotas) + cidades, províncias, posição dos peões; consultas de vizinhança, yurts, maioria por região e movimento. |
| `MapaOficial` | Monta o `Tabuleiro` com a geografia do tabuleiro oficial (ver abaixo). |

### Correspondência com a divisão original (termos de War → Herdeiros de Khan)

| Combinado no grupo | Neste projeto | Observação |
|---|---|---|
| `Territorio` | `Parada` | Nó do mapa; não tem um único dono, guarda yurts de várias cores. |
| `HashMap<String, Territorio>` | `Tabuleiro.paradas` | Igual. |
| `HashMap<Territorio, Set<Territorio>>` fronteiras | `Tabuleiro.rotas` | Igual (rotas sempre simétricas). |
| `Continente` + bônus de exércitos | `Regiao` | O "bônus" do jogo são os votos de influência por maioria de yurts. |
| `getTerritorio(nome)` | `getParada(nome)` | Lança `IllegalArgumentException` se não existe. |
| `saoVizinhos(a, b)` | `saoVizinhas(a, b)` | |
| `donoDe(territorio)` | `temYurtDe(parada, jogador)` / `Parada.getYurts()` | |
| `exercitosEm(territorio)` | `yurtsEm(parada)` | |
| `adicionarExercitos` / `removerExercitos` | `colocarYurt(parada, jogador)` / `removerYurt(...)` | Mexem no estoque do jogador. |
| `territoriosDe(jogador)` | `paradasComYurtDe(jogador)` | |
| `dominaContinente(jogador, continente)` | `temMaioriaDeYurts(jogador, regiao)` + `lideresEmYurts` / `segundosEmYurts` | Regra de empate do manual (p. 15). |

O `Tabuleiro` não decide *quando* uma ação é permitida (sequência do turno,
gasto de cavalos, uso do Khan): ele oferece as consultas (`saoVizinhas`,
`podeTerminarMovimentoEm`, `cidadesAdjacentes`, `provinciasAdjacentes`...) que a
`Partida` (parte 3) usa para aplicar as regras do turno. Cartas, tesouros e
melhorias são da parte 2 e se encaixam em `Jogador`/`Cidade` sem alterar o que
existe.

## Mapa

`MapaOficial` foi transcrito da imagem oficial do tabuleiro (`imagens/tabuleiro.jpg`,
publicada no EAD para a 2ª iteração), que substituiu a aproximação feita a partir
do manual escaneado. O mapa tem 34 paradas: em cada região, 9 simples (3 no anel
de cada cidade) e 2 duplas de conselheiro, mais Karakorum, ligado a 4 rotas.
Tem também 12 províncias, 4 de cada tributo. Cada uma das 4 cabeças do Khan fica
numa província e aponta para ela e para 2 vizinhas. A posição de cada parada na
imagem está em `view.CoordenadasMapa`.

## Como rodar os testes sem Eclipse

```bash
./scripts/run_tests.sh
```

Compila `src/` e `test/` com `javac` e roda `org.junit.runner.JUnitCore`.
A base da 2ª iteração tem **159 testes** (29 da parte 1, 46 da parte 2,
71 da parte 3 e 13 da fachada `ModelAPI`). A suíte atual tem **205 testes passando**, incluindo
preparação completa, ações pela fachada, mercado, recompensas, entregas, bônus
objetivos, rotas e uma partida completa pelos controles Swing.
O script respeita `JAVA_HOME`; use um JDK 17 ou superior, como o incluído no Eclipse.

## Testes (JUnit 4)

Cada caso de teste documenta **Objetivo / Entrada / Saída esperada** no
Javadoc. Os principais (os três pedidos na divisão do grupo estão em negrito):

- `TabuleiroTest`: **vizinhança correta** (rota simétrica, não-vizinhas a dois
  passos, parada inexistente), **"dono muda"** (construir yurt retira do estoque
  e marca a parada; capacidade 1/2/0 por tipo de parada; remoção devolve ao
  estoque), **maioria na região** (empate no 1º lugar, 2º lugar, yurt de
  conquista de cidade contando para a região), movimento de peões (só vizinha,
  capacidade de peões, peões de Altani não se bloqueiam), adjacência de cidades e
  províncias.
- `CidadeProvinciaTest`: revelar/atacar/conquistar cidade; máximo de 3 peças
  por província; setas do Khan só em províncias do Khan.
- `JogadorTest`: preparação inicial; peças de ativação; gasto de tributo
  insuficiente; coerência das colunas transcritas das cartelas.
- `MapaOficialTest`: invariantes do mapa: 9 cidades (3/região), 7
  conselheiros, rotas simétricas, mapa conexo a partir de Karakorum, 4
  províncias do Khan com 2 vizinhas e 1 tributo inicial em cada província.
- `ModelAPITest` (2ª iteração): fachada Singleton, Observer, nova partida e
  configurações recusadas, consultas do mapa, colunas, movimento, troca de peão
  da Altani e movimento do Khan, inclusive para a área de melhorias.

## Parte 3: partida, turnos e movimento

Esta contribuição acrescenta `Partida`, `Turno`, `Fase`, `Movimento` e os testes da parte 3, preservando as classes e a documentação original da parte 1 acima. `Partida` decide as regras de preparação, turno, gasto de recursos, ações e encerramento no Model. O Controller, pela fachada `ModelAPI`, consulta a legalidade e solicita a execução das ações.

- `podeAtacarCidade`, `podePegarTributo` e `podeConstruirYurt` consultam a legalidade sem gastar recursos nem alterar o estado.
- `moverPara` escolhe um caminho de menor custo e salta automaticamente yurts próprios intermediários. O destino sempre custa um cavalo e conta como visitado.
- `moverPor(List<String>, List<Boolean>)` permite escolher em cada passagem intermediária se o peão salta o yurt próprio ou visita a parada pagando um cavalo. Assim, passar por um yurt pode habilitar coleta, ataque ou construção quando o jogador optar pela visita.
- A branca recupera todas as fichas para o próximo turno. O fim termina a rodada atual e permite mais uma rodada completa.

`ComponentesPartida` e `EfeitosColuna` ligam a partida à implementação da parte 2, `ComponentesKhan`. Os métodos padrão continuam disponíveis para testar isoladamente as partes 1 e 3. A integração acrescenta preparação dos componentes e seleção do tipo de tesouro nos ataques, preservando as assinaturas anteriores.

O construtor simples de `Partida` mantém o subconjunto das partes 1 e 3. Ao fornecer `ComponentesKhan`, a partida também usa cartas, tesouros, melhorias, entregas, bônus, influência regional e desempates finais. A configuração física das melhorias por cor e dos conselheiros deve ser fornecida explicitamente.

Consulte [docs/parte3-integracao.md](docs/parte3-integracao.md) para os contratos, consultas e escolhas de movimento, e [docs/testes-parte3.md](docs/testes-parte3.md) para os 71 testes dessa parte.

## Parte 2: cartas, tesouros, melhorias e votos

- `Carta`, `Baralho`, `CartasOficiais`, `Objetivo`, `ObjetivoMapa` e `ObjetivoRecursos`: os 14 conselheiros abertos e 10 secretos; escolha inicial, distribuição por jogador e descarte sem reposição.
- `Inventario`, `Melhoria`, `TipoTesouro`, `TipoBonus` e `Conselheiro`: peças físicas, efeitos virtuais, entregas e bônus de uso único.
- `ComponentesKhan`: integra a preparação, as ações e as escolhas pendentes à `Partida`.
- `Pontuacao`: influência por maioria regional, cartas secretas e desempates por tesouros não entregues e distância da capital.

A 3ª iteração usa o cadastro embutido em `PecasDoJogo`, com cores e pedidos conferidos nas imagens e multiplicidades documentadas como hipótese. Veja os dados pendentes, as interpretações de casos ambíguos e exemplos de uso em [docs/parte2-integracao.md](docs/parte2-integracao.md). Os testes novos estão documentados em [docs/testes-parte2.md](docs/testes-parte2.md).

No Eclipse, atualize o projeto com **F5** e execute `test/model/TodosOsTestes.java` em **Run As > JUnit Test**. No terminal, use `JAVA_HOME="/caminho/do/jdk" ./scripts/run_tests.sh`. Validação histórica da 2ª iteração: 159 testes. A suíte revisada da 3ª iteração tem **205 testes**, com compatibilidade Java 17.
