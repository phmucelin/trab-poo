# 4ª iteração: término, salvamento e recuperação

O enunciado da 4ª iteração (15/11/2026) pede três coisas: o que faltou nas
iterações anteriores, o término da partida e o salvamento e a recuperação.
Nenhuma funcionalidade ficou pendente da 3ª iteração; esta iteração
acrescenta as outras duas.

## Janelas iniciais

O enunciado da 2ª iteração pede duas janelas antes do tabuleiro. A
`JanelaAbertura`, que o `Main` abre, oferece **Nova partida** ou **Continuar
partida salva...**. "Nova partida" fecha a abertura e abre a `JanelaInicial`,
a janela de escolha dos personagens; "Continuar" escolhe o arquivo no
`JFileChooser` e abre o tabuleiro direto.

## Término da partida

- Quando o último jogador da rodada final termina o turno, a aba Resultado
  mostra o vencedor e, logo depois, abre um diálogo (`JOptionPane`, exigido
  pelo enunciado) com o vencedor e a pergunta "Desejam iniciar uma nova
  partida?".
- **Sim:** o `Controlador` fecha o tabuleiro e as janelas de cartas e de
  cartela, solta todos os observadores (`ModelAPI.removerObservadores`) e
  reabre a janela de escolha dos personagens.
- **Não:** a aplicação é encerrada.
- Fechar o tabuleiro encerra a aplicação a qualquer momento
  (`EXIT_ON_CLOSE` da `JanelaJogo`).

A pergunta parte do `Controlador.terminarTurno`, por `SwingUtilities.invokeLater`,
para aparecer depois que os observadores já mostraram o resultado.

## Salvamento e recuperação

| Regra do enunciado | Como ficou |
|---|---|
| 1. Salvar só antes de o jogador começar a jogar | O botão **Salvar partida** (aba Turno) fica habilitado só enquanto `ModelAPI.podeSalvar()` é verdadeiro. O `Turno` tem um indicador que liga na primeira ação aceita (ativar coluna, trocar o peão de Altani, usar bônus...), e o Observer desliga o botão. Ele volta a ligar no turno seguinte. |
| 2. Arquivo texto ASCII puro | O arquivo é gravado e lido com `US_ASCII`. Acentos dos nomes viram `\uXXXX`. Um arquivo com byte fora do ASCII é recusado. |
| 3. Nome e pasta livres, só o tipo pré-definido | O usuário escolhe nome e pasta; o filtro é `*.txt`, e `.txt` é acrescentado se faltar. |
| 4. `JFileChooser` | `view.SeletorArquivo` cria o `JFileChooser` usado por **Salvar partida** e por **Continuar partida salva...** (janela de abertura). |

### O que vai no arquivo

Tudo o que muda durante a partida:
- a vez, a rodada, a rodada final, o Kurultai, o número do turno e o Khan;
- o estoque de tributos;
- cada jogador: recursos, fichas, votos, peões, tesouros, bônus, melhorias
  (com o turno em que entraram) e carta secreta;
- os yurts das paradas, as cidades e a ordem das que faltam revelar;
- as peças das províncias e os bônus que ainda estão nos conselheiros;
- as cartas abertas, as pilhas e os descartes.

Como nada é sorteado depois da preparação, gravar a ordem das pilhas faz a
partida recuperada continuar exatamente como a original continuaria.

Cada linha é um registro `TIPO;chave=valor;...`. Linhas começadas por `#` são
comentários, então o arquivo pode ser lido e editado à mão para montar um
cenário de teste:

```text
HERDEIROS_DE_KHAN;versao=1
PARTIDA;vez=0;rodada=2;rodadaFinal=-1;kurultai=0;turno=3;khan=Khan do Noroeste
ESTOQUE;ESPADA=4;YURT=4;MOEDA=7
JOGADOR;indice=0;nome=Jogador 1;cor=AZUL;herdeiro=ALTANI;yurts=12;votos=0;fichas=VERMELHA,VERDE,AMARELA,BRANCA;ESPADA=0;YURT=0;MOEDA=1;tesourosRecebidos=0;linhasPremiadas=0
PEAO;jogador=0;peao=0;parada=Karakorum
CIDADE;nome=Cantão;revelada=1;tesouros=PELES,PELES,PELES,PELES;conquistador=-
...
FIM
```

Um arquivo inválido (truncado, com acento cru, número errado, parada
inexistente, coluna com mais de 4 melhorias...) é recusado com a linha e o
motivo. A partida em memória fica como estava.

### Onde está no código

- `model/ArquivoPartida`: grava e lê o arquivo. Ao ler, monta uma partida nova
  com os jogadores gravados e troca o estado sorteado pelo do arquivo, usando
  métodos `restaurar` de pacote em `Partida`, `Jogador`, `Parada`, `Cidade`,
  `Provincia`, `Conselheiro`, `Baralho` e `ComponentesKhan`.
- `model/ModelAPI`: `podeSalvar()`, `salvar(File)`, `carregar(File)` e
  `getErroArquivo()`.
- `controller/Controlador`: `salvarPartida`, `carregarPartida`, a pergunta de
  fim de partida e `fimDaPartida(boolean)`.
- `view/PainelTurno` (botão e mensagem), `view/JanelaAbertura` (nova partida
  ou continuar) e `view/SeletorArquivo`.

## Testes

- `SalvamentoTest`:
  - Em 25 partidas sorteadas, salva num turno qualquer, continua a original até
    o fim, carrega o arquivo e repete as mesmas jogadas. O arquivo regravado
    logo depois de carregar é idêntico ao original, os arquivos de cada troca
    de turno coincidem e o resultado final é o mesmo.
  - Confere também a regra 1, o ASCII com acentos, os arquivos inválidos e a
    edição à mão.
- `SalvamentoInterfaceTest`: o botão **Salvar partida** liga e desliga nos
  momentos certos.
- Para garantir que os testes pegam erros de verdade, foram injetados de
  propósito erros na restauração: pilha de tesouros, fichas, turno das
  melhorias, linhas premiadas e votos registrados no Kurultai. Todos foram
  detectados.

## Roteiro para testar na mão

1. Rode `controller.Main`, clique em **Nova partida**, escolha os personagens
   e conclua a preparação.
2. Antes de ativar uma coluna, clique em **Salvar partida** e grave, por
   exemplo, `teste`. O painel mostra "Partida salva em teste.txt".
3. Ative uma coluna: o botão fica desabilitado até o próximo jogador.
4. Feche o tabuleiro (a aplicação encerra) e rode o `Main` de novo.
5. Clique em **Continuar partida salva...** e escolha `teste.txt`: a partida
   volta no mesmo ponto, com a mesma vez, peças e cartas.
6. Para ver o fim da partida rapidamente, edite no arquivo a linha `PARTIDA`:
   ponha `vez` no último jogador e `rodadaFinal` igual a `rodada`. Carregue,
   jogue esse turno e termine: aparece o diálogo de nova partida.
