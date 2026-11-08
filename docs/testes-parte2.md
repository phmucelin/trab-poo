# Testes da parte 2

> Desde a 3ª iteração o jogo usa o cadastro de `PecasDoJogo`, com pedidos e
> cores conferidos nas imagens do EAD (a divisão 15/15/14 por cor é suposição
> nossa). Este arquivo ficou como estava na parte 2; o estado atual das regras
> está em `docs/iteracao3.md`.


JUnit 4, pacote `model`, cenários independentes. Cada teste inclui
**Objetivo / Entrada / Saída esperada** no Javadoc, seguindo a documentação
já utilizada pelas outras partes. O documento original do Capítulo 18 não
foi fornecido; este relatório segue o formato presente no repositório.

| Classe | Casos | Cobertura |
|---|---:|---|
| `BaralhoTest` | 6 | Catálogo, sorteio reproduzível, compra sem reposição, distribuição atômica, descarte, proteção de coleções e peças inválidas. |
| `ObjetivosTest` | 10 | Recursos virtuais, variedade de tesouros, total de yurts, três regiões, redes, adjacência, patamares secretos, pares de melhorias e bônus usados. |
| `InventarioTest` | 4 | Gasto atômico, capacidade/cor/identidade de melhorias, pedidos dos conselheiros e efeitos de todos os símbolos. |
| `ComponentesKhanTest` | 20 | Preparação real, compra, branca gratuita, Jochi, esgotamento, Khan, ataques tipados, entregas, Altani, bônus, cartas automáticas e fases. |
| `PontuacaoTest` | 6 | Tabela regional, empates de primeiro/segundo, exclusão da branca, encerramento único e desempates. |

Total novo: **46 testes**. Com os 29 da parte 1 e 71 da parte 3:
**146 testes, zero falhas**.

## Cenários de integração prioritários

- Duas cartas secretas são distribuídas por pessoa; a partida espera todas
  as escolhas e não permite que sejam trocadas depois.
- Uma compra usa a moeda virtual antes da física e devolve apenas a física
  ao banco. Índice inválido, saldo insuficiente ou coluna cheia não gastam nada.
- Comprar antes da ativação também não permite usar a melhoria no mesmo turno.
- Completar uma linha exige escolher a melhoria branca; Jochi não recebe
  duas vezes pela primeira linha. Esgotamento total não gera escolha impossível.
- Os cinco estados do mercado (cinco a uma peça) verificam separadamente
  os ícones expostos de moeda e espada antes da reposição pelo Khan.
- Um ataque pode escolher o tipo do tesouro. Um tipo ausente não gasta espada.
  A conquista transfere a última peça e abastece a nova cidade revelada.
- A entrega usa tesouros virtuais primeiro, preserva o saldo físico
  correspondente e fixa o peão de Altani. Falta de um tipo não consome os demais.
- Cada conselheiro dá seu bônus só na primeira entrega. Bônus usados não
  podem ser reaplicados, mas continuam contando para cartas.
- O bônus de tesouro bloqueia o fim do turno até escolher uma das três peças;
  as outras duas vão para o descarte.
- A carta de quatro espadas pode ser cumprida na ativação com uma espada
  virtual, sem consumir as três físicas. Não há voto repetido nem reposição.
- Configuração inválida é detectada antes de distribuir moedas ou revelar cidades.
- A contagem final ocorre uma única vez e não altera a ficha neutra. O
  desempate usa tesouros físicos restantes, depois distância, admitindo vitória conjunta.

## Execução e limites

```sh
JAVA_HOME="/caminho/do/jdk" ./scripts/run_tests.sh
```

No Eclipse: **TodosOsTestes > Run As > JUnit Test**. Os cinco novos arquivos
estão registrados na suíte; o script também os descobre pelo sufixo `Test`.

Validação local: JDK 25 do Eclipse, compilação `--release 17`, JUnit 4.13.2.
Os testes verificam as regras implementadas e a integração com mapas pequenos;
não certificam a geografia aproximada nem a composição física das melhorias
por cor. Esses dados pendentes estão em [parte2-integracao.md](parte2-integracao.md).
