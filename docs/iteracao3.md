# 3ª iteração e correções da revisão do PR #8

O jogo completo é o fluxo padrão de `Main`. As regras são coordenadas pelo
Model; o Controller recebe comandos e organiza a preparação, escolhas,
janelas auxiliares e resultado. A View usa a fachada Singleton `ModelAPI` e
as interfaces `ObservadoIF`/`ObservadorIF` para refletir o estado.

## Correções dos bloqueios

### Imagens e janelas próprias

`JanelaCartas` e `JanelaCartela` abrem por botões Swing. O botão Fechar e o
fechamento da janela pelo sistema encaminham o comando ao Controller e ao
Model; a notificação Observer atualiza a visibilidade. As janelas acompanham
compras, seleção de jogador e troca de turno. São limitadas a 1400 × 1050.

`CatalogoImagens` mantém um `HashMap` de identificadores para arquivos e um
cache de imagens. A leitura usa subamostragem para limitar o consumo de memória
das fotos de alta resolução. Os caminhos pertencem à View; o Model expõe
somente IDs, como `SECRETA_VIAJANTE` e `MELHORIA_VERDE_TECIDO`.

As imagens são desenhadas com `Graphics2D.drawImage()` nos painéis Java2D:

- `PainelCartas`: carta secreta e até quatro cartas abertas, acompanhadas das
  descrições e do progresso. A carta secreta fica oculta até o jogador revelar.
- `PainelCartela`: cartela oficial do herdeiro e melhorias sobrepostas aos
  espaços de cada coluna, na ordem de aquisição, de baixo para cima.
- `PainelImagem`: opções da preparação de cartas e melhorias, além das ofertas
  abertas do mercado. A compra da pilha fechada não revela a próxima peça.

O catálogo cobre 49 imagens: 24 cartas, cinco cartelas e 20 tipos distintos de
melhoria presentes nos arquivos do EAD (15 combinações comuns e cinco tesouros).
As fotos originais extraídas usadas pela aplicação estão em `imagens/oficiais/`.

### Configuração padrão

`PecasDoJogo` embute o cadastro. A tela inicial pede os jogadores e herdeiros
e prepara uma partida completa; não exige arquivo nem oferece modo reduzido.
Os cenários sintéticos permanecem exclusivamente nos testes.

Pedidos conferidos no tabuleiro:

| Parada | Pedido |
|---|---|
| Conselheiro Norte | TECIDO:2 |
| Conselheiro Nordeste | FERRO:2 |
| Conselheiro Oeste | PELES:2 |
| Conselheiro Leste | GADO:2 |
| Conselheiro Sudoeste | ESPECIARIAS:2 |
| Conselheiro Sul | Três tesouros quaisquer |
| Karakorum | FERRO:1, PELES:1, GADO:1 (face impressa no tabuleiro) |

Tesouros nas melhorias: amarela possui GADO e ESPECIARIAS; vermelha possui
FERRO; verde possui PELES e TECIDO. As três cores também possuem moeda,
espada, tributo de yurt, mão e cavalos.

**Hipótese de multiplicidades:** 15 amarelas, 15 vermelhas e 14 verdes, conforme
a proposta da revisão. São oito moedas, oito espadas, oito yurts, seis mãos,
nove cavalos e um de cada tesouro. As fotos mostram os tipos e suas cores,
mas não comprovam a quantidade física de cada combinação.

### Jochi

A preparação reserva duas melhorias para Jochi. Ele escolhe uma para a branca
após escolher sua carta secreta; os turnos só iniciam depois que todas as
escolhas forem concluídas. A peça não escolhida volta ao fundo da pilha fechada,
preservando a contagem. Esse destino é uma decisão de implementação: o texto
visível na cartela não especifica como devolver a peça rejeitada.

Nas compras, Jochi escolhe a coluna de destino, inclusive branca, respeitando
os quatro espaços. Os demais herdeiros continuam obrigados a seguir a cor da
peça. A validação de destino, capacidade e moedas acontece antes da compra.
Peças adquiridas no turno atual só produzem efeitos em turnos seguintes.

O número de linhas já premiadas é registrado separadamente das peças da
branca: comprar nessa coluna não suprime a recompensa de uma linha posterior.
A melhoria inicial já preenche a recompensa da primeira linha de Jochi.

## Demais fluxos integrados

- Movimento por clique, troca do peão de Altani e planejamento de rota com
  escolha de salto/visita em cada yurt próprio intermediário.
- Construção de yurts, coleta de tributos, ataques com escolha de tesouro e
  habilidades de Ogedei e Chagatai.
- Mercado, compras às cegas, reposição pelo Khan e recompensas para a branca.
- Entregas aos conselheiros, consumo de tesouros virtuais antes dos físicos,
  votos, bônus exclusivos da primeira entrega e bônus de uso único.
- Escolhas obrigatórias de melhoria e tesouro bloqueando o término do turno.
- Objetivos abertos automáticos, consulta da carta secreta, Kurultai, rodada
  final e resultado com influência, cartas e desempates.

`Controlador.getEtapaPartida()` prioriza preparação, resultado, escolha de
tesouro, escolha de melhoria e jogo normal. As Views aplicam essa sequência
em callbacks do Observer. Abrir/fechar cartas ou cartela também altera o estado
observado. A carta secreta fica oculta na troca de turno e no fechamento da janela.

## Validação e pacote

A suíte tem 205 testes. Os cenários verificam dados, carregamento real de todas
as imagens, regras de Jochi, atomicidade das recusas, notificações, compras,
recompensas, entregas, bônus e uma partida completa por botões e cliques no
mapa. O teste completo usa o cadastro de produção com sorteio reproduzível.

Os PDFs e ZIPs originais ficam em `.context/`, ignorada pelo Git. O pacote
Eclipse inclui fontes, imagens, bibliotecas, testes, configurações e documentação.
Gerar com `python3 scripts/package_delivery.py`; conferir pelo roteiro em
`docs/entrega-iteracao3.md`. Salvamento e recuperação pertencem à última iteração.
