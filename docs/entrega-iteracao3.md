# Conferência da entrega — 3ª iteração

## Executar e testar

Requer JDK 17+. No Eclipse, importe a pasta do projeto e execute
`controller.Main`. Pelo terminal:

```bash
./scripts/run_game.sh
./scripts/run_tests.sh
python3 scripts/package_delivery.py
```

Os scripts respeitam `JAVA_HOME`. A tela inicial já prepara a partida completa
com o cadastro embutido; basta escolher jogadores e herdeiros.

## Correções solicitadas no PR #8

| Item | Resultado |
|---|---|
| Cartas em janela própria | `JanelaCartas`, aberta/fechada por botões e Observer; fotos oficiais via Java2D. |
| Cartela com melhorias | `JanelaCartela`, imagem do herdeiro e sobreposição das peças nas colunas com `drawImage()`. |
| Identificadores no Model | IDs estáveis de cartas e melhorias; `HashMap` de imagens exclusivamente na View. |
| Partida completa padrão | Cadastro embutido em `PecasDoJogo`; sem modo reduzido ou seletor de arquivo. |
| Pedidos e cores | Corrigidos conforme tabuleiro e imagens do EAD. Multiplicidades por cor documentadas como hipótese. |
| Jochi | Escolhe uma entre duas melhorias iniciais e escolhe a coluna das compras; capacidade, moedas e recompensas verificadas. |
| PDFs locais | Material original em `.context/`, ignorado e fora do ZIP. |
| Autoria dos próximos commits | E-mail local configurado com o endereço público da conta GitHub. |

## Roteiro gráfico

1. Inicie uma partida de dois a cinco jogadores. Cada jogador revela e escolhe
   uma carta secreta, vendo as duas imagens oficiais. Com Jochi, confira a
   escolha de uma entre duas melhorias para a branca.
2. Em **Turno**, ative uma coluna e mova peão e Khan por clique. Com Altani,
   troque o peão antes de sua primeira ação de posição.
3. Em **Ações**, colete tributos, construa yurts, ataque cidades escolhendo os
   tesouros e teste Ogedei/Chagatai. Confira os saldos e os yurts desenhados.
4. Clique **Ver cartas**. Revele e oculte sua carta; confira as quatro cartas
   abertas. Feche pelo botão e pelo controle da janela. Ao trocar de turno,
   a carta secreta deve ficar oculta.
5. Clique **Ver cartela**. Confira a foto do herdeiro, a seleção de outro
   jogador e os espaços de melhoria. Com a janela aberta, compre uma peça na
   aba **Melhorias** e confira a atualização automática da sobreposição.
6. Com Jochi, compre para uma coluna de outra cor e tente uma coluna cheia.
   A compra recusada deve preservar moedas e peças. A compra cega também usa
   o destino escolhido e não revela antecipadamente a peça.
7. Complete uma linha e resolva a recompensa para a branca. Reponha o mercado
   movendo o Khan para a área de melhorias no mapa.
8. Em **Entregas**, escolha quantidades, realize uma entrega numa parada
   visitada e use o bônus. Resolva a escolha de tesouro antes de encerrar o turno.
9. Use **Traçar rota no mapa** para passar por paradas ocupadas e escolher
   saltar ou visitar um yurt próprio. Clique novamente no destino para executar.
10. Acompanhe votos, Kurultai e rodada final. Confira o resultado e os desempates.

## Evidências

- 205 testes automatizados, incluindo os dados, as 49 imagens mapeadas, Jochi,
  Observer e os fluxos Swing na EDT.
- Partida completa da preparação ao resultado por botões e eventos de mouse,
  com o cadastro de produção e sem injetar votos, posições ou recursos.
- Inspeção visual das janelas de cartas e cartela, da preparação e das
  sobreposições; limites de 1400 × 1050 conferidos.
- Teste de fechamento pelo botão e pelo evento de janela; privacidade da carta
  verificada na troca de turno.

## Dados assumidos

A distribuição de 15 melhorias amarelas, 15 vermelhas e 14 verdes segue a
proposta da revisão e respeita todos os totais por símbolo. As cores e pedidos
foram conferidos; a quantidade física de cada combinação não foi comprovada.
A peça inicial rejeitada por Jochi volta ao fundo da pilha: o texto da cartela
não especifica o destino dessa peça. Detalhes em `docs/iteracao3.md`.

## ZIP

`python3 scripts/package_delivery.py` gera
`output/entrega/HerdeirosDeKhan-iteracao3.zip`, com raiz `HerdeirosDeKhan`.
Inclui `.project`, `.classpath`, `.settings`, fontes, testes, imagens,
bibliotecas, scripts e documentação. O script confere a integridade do arquivo.
PDFs, ZIPs originais, `.context`, `.git` e builds ficam fora do pacote.
