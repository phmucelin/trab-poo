# 4ª iteração: fim de partida, salvar e carregar

Enunciado: `4A-ITERACAO.PDF`. As funcionalidades das iterações anteriores já
estavam completas, então esta iteração acrescentou o término da partida e o
salvamento e a recuperação.

## Término de partida

- `Controlador.terminarTurno()` agenda, com `SwingUtilities.invokeLater`, a
  pergunta de nova partida quando o turno final é concluído. `terminarTurno`
  só devolve `true` uma vez no turno final, então a pergunta aparece uma vez
  por partida.
- O diálogo (`view.DialogoNovaPartida`) mostra o(s) vencedor(es) de
  `ModelAPI.getVencedores()`. Sim: o Controlador fecha `JanelaJogo`,
  `JanelaCartas` e `JanelaCartela` e reabre a `JanelaInicial`. Não: encerra.
- O diálogo e o encerramento são trocáveis (`ConfirmadorNovaPartida`,
  `Controlador.setConfirmador` e `setEncerrador`) para os testes não travarem.
- `JanelaJogo.dispose()` desregistra a janela e seus painéis do Observer;
  sem isso uma partida nova continuaria notificando painéis de janelas
  descartadas.
- Fechar o tabuleiro continua encerrando a aplicação (`EXIT_ON_CLOSE`).

## Salvar e carregar

### Regras do enunciado

| Regra | Como foi feita |
|---|---|
| Salvar só antes de o jogador jogar | `ModelAPI.podeSalvar()`: partida em andamento e nenhuma coluna ativada no turno. O botão do `PainelTurno` usa isso. |
| Arquivo `.txt` em ASCII | Gravado com `US_ASCII`; texto fora do ASCII visível é escapado como `\uXXXX`. Na leitura, byte fora do ASCII é recusado. |
| Nome e pasta livres, só o tipo fixo | `JFileChooser` com filtro `.txt`; o `.txt` é acrescentado se faltar. |
| `JFileChooser` obrigatório | `view.SeletorArquivoPartida`, usado ao salvar e ao carregar. |

### Quem faz o quê

| Camada | Classe | Papel |
|---|---|---|
| Model | `SalvamentoPartida` | Converte a partida em `List<String>` e o contrário, e valida. Não conhece arquivos. |
| Model | `ModelAPI` | `podeSalvar()`, `gerarSalvamento()`, `restaurarPartida(linhas)`. A partida só é trocada depois de a leitura validar tudo. |
| Model | `Partida.restaurar`, `ComponentesKhan.restaurar` e `exportar` | Reconstroem e exportam o estado sem refazer a preparação. |
| Controller | `Controlador` | `salvarPartida(File)`, `carregarPartida(File)` e `getErroArquivo()`. Faz a E/S de arquivo. |
| View | `PainelTurno`, `JanelaInicial`, `SeletorArquivoPartida` | Botões e `JFileChooser`. |

### O que vai no arquivo

Como só se salva no início do turno, o `Turno` não é gravado: é sempre o de
começo de turno, e só seu número vai no arquivo. Não há escolha pendente nem
ação do turno por restaurar.

Cada linha é `CHAVE;campo;campo`. Listas usam `,`, pares usam `:` e `-` é
vazio. A primeira linha é `HERDEIROS_DE_KHAN;1`. Chaves:

| Chave | Conteúdo |
|---|---|
| `PARTIDA` | rodada, rodada final (-1 se não definida), jogador da vez, número do turno, Kurultai, posição do Khan |
| `ESTOQUE` | peças de tributo no banco |
| `JOGADOR`, `PEAO` | nome, herdeiro, yurts, votos, tesouros recebidos, fichas, tributos; posição de cada peão |
| `YURT`, `CIDADE`, `FECHADAS`, `PROVINCIA` | yurts por parada (em ordem), estado de cada cidade, ordem das cidades ainda fechadas, peças de cada província |
| `INVENTARIO`, `MELHORIAS`, `LINHAS`, `MAO` | tesouros e bônus de cada jogador, melhorias por coluna (em ordem, com o turno de aquisição), linhas premiadas, carta secreta |
| `CARTAS_ABERTAS`, `ABERTAS_*`, `SECRETAS_*` | as cartas abertas viradas e as pilhas e descartes dos dois baralhos |
| `PILHA_MELHORIAS`, `MERCADO` | pilha de melhorias e mercado |
| `PILHA_TESOUROS`, `DESCARTE_TESOUROS`, `TESOUROS_CIDADE` | reserva, descarte e tesouros de cada cidade revelada |
| `BONUS_CONSELHEIRO` | bônus ainda não retirado de cada conselheiro |

Não há sorteio depois da preparação, então não se grava semente.

### Validação na leitura

Arquivo vazio, de outra versão, truncado ou com valor desconhecido é recusado
com a causa em `getErroConfiguracao()`, e a partida atual não muda. Também é
conferido que:

- as peças de tributo (banco, jogadores, províncias) somam 10 espadas, 10
  yurts e 15 moedas, e cada jogador tem 12 yurts (estoque, mapa e cidades
  conquistadas);
- há 44 melhorias e 40 tesouros no total, e o contador de cada cidade bate
  com a lista dos seus tesouros;
- os 14 conselheiros abertos e os 10 secretos aparecem uma vez, cada jogador
  tem uma carta secreta e os 7 bônus aparecem uma vez;
- herdeiros e posições são válidos e as cidades fechadas são as não reveladas.

## Testes

São 224 testes (`./scripts/run_tests.sh`). Os novos desta iteração:

- `FimDePartidaInterfaceTest`: Sim reabre a `JanelaInicial` e descarta o
  tabuleiro antigo, Não encerra, e a pergunta aparece uma vez.
- `SalvamentoPartidaTest`: ida e volta em 30 partidas sorteadas; jogar até o
  fim a partir de um save dá o mesmo estado final da partida original; só
  salva no início do turno; nomes com acento viram ASCII e voltam iguais;
  arquivos inválidos são recusados sem alterar a partida.
- `SalvarCarregarInterfaceTest`: botão Salvar habilitado só antes de jogar,
  salvar e carregar por arquivo, e recusa de arquivo ruim.

O diálogo do `JFileChooser` não é aberto nos testes. Para conferir na mão:
salve no início de um turno com um nome sem `.txt`, jogue um pouco, carregue
o arquivo (pela aba Turno e, depois de reabrir o jogo, pela janela inicial) e
veja se a partida volta ao início daquele turno.

## Conferência final

- [x] fim de partida com diálogo, nova partida e encerramento
- [x] salvar só antes de jogar, em `.txt` ASCII, com `JFileChooser`
- [x] carregar com validação e sem estragar a partida em curso
- [ ] roteiro manual completo (`docs/iteracao3.md`) mais salvar e carregar
- [ ] gerar o ZIP (`python3 scripts/package_delivery.py`)
