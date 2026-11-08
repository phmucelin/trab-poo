package model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** Testes da fachada pública usada pela View e pelo Controller (2ª iteração). */
public class ModelAPITest {
	static final int TEMPO_LIMITE = 1000;

	private final ModelAPI api = ModelAPI.getInstance();
	private final Contador contador = new Contador();

	/** Observador de teste que conta as notificações recebidas. */
	private static final class Contador implements ObservadorIF {
		int notificacoes;
		ObservadoIF ultimo;
		@Override public void notify(ObservadoIF o) {
			notificacoes++;
			ultimo = o;
		}
	}

	@Before
	public void prepararPartida() {
		assertTrue("A partida de Ogedei e Chagatai deve ser criada",
				PreparacaoCompletaTest.iniciarPadrao(Arrays.asList("Ana", "Bia"), Arrays.asList("OGEDEI", "CHAGATAI")));
		api.add(contador);
	}

	@After
	public void removerObservador() {
		api.remove(contador);
	}

	/**
	 * Objetivo: garantir uma única instância da fachada.
	 * Entrada: duas chamadas a getInstance().
	 * Saída esperada: o mesmo objeto.
	 */
	@Test(timeout = TEMPO_LIMITE)
	public void fachadaEhSingleton() {
		assertSame("getInstance deve devolver sempre a mesma fachada", api, ModelAPI.getInstance());
	}

	/**
	 * Objetivo: preparar a partida no mapa oficial com os peões na capital.
	 * Entrada: Ana (Ogedei) e Bia (Chagatai).
	 * Saída esperada: cores AZUL e VERDE, peões em Karakorum, vez de Ana e quatro colunas livres.
	 */
	@Test(timeout = TEMPO_LIMITE)
	public void novaPartidaPosicionaPeoesEmKarakorum() {
		assertEquals("Número de jogadores", 2, api.getNumJogadores());
		assertEquals("Cor do primeiro jogador", "AZUL", api.getCorJogador(0));
		assertEquals("Cor do segundo jogador", "VERDE", api.getCorJogador(1));
		assertEquals("Herdeiro do segundo jogador", "CHAGATAI", api.getHerdeiroJogador(1));
		assertEquals("Peão de Ana deve começar na capital", "Karakorum", api.getPosicaoPeao(0, 0));
		assertEquals("Peão de Bia deve começar na capital", "Karakorum", api.getPosicaoPeao(1, 0));
		assertEquals("O primeiro jogador começa", 0, api.getJogadorDaVez());
		assertEquals("Colunas livres no início", Arrays.asList("VERMELHA", "VERDE", "AMARELA", "BRANCA"),
				api.getColunasDisponiveis());
	}

	/**
	 * Objetivo: recusar configurações inválidas sem perder a partida atual.
	 * Entrada: herdeiro repetido, herdeiro inexistente, um único jogador e listas de tamanhos diferentes.
	 * Saída esperada: todas recusadas, partida anterior intacta e nenhuma notificação.
	 */
	@Test(timeout = TEMPO_LIMITE)
	public void configuracaoInvalidaEhRecusada() {
		assertFalse("Herdeiro repetido", PreparacaoCompletaTest.iniciarPadrao(Arrays.asList("A", "B"), Arrays.asList("JOCHI", "JOCHI")));
		assertFalse("Herdeiro inexistente", PreparacaoCompletaTest.iniciarPadrao(Arrays.asList("A", "B"), Arrays.asList("JOCHI", "GENGHIS")));
		assertFalse("Um único jogador", PreparacaoCompletaTest.iniciarPadrao(List.of("A"), List.of("JOCHI")));
		assertFalse("Listas de tamanhos diferentes", PreparacaoCompletaTest.iniciarPadrao(Arrays.asList("A", "B"), List.of("JOCHI")));
		assertEquals("A partida anterior deve ser mantida", "Ana", api.getNomeJogador(0));
		assertEquals("Configurações recusadas não notificam", 0, contador.notificacoes);
	}

	/**
	 * Objetivo: expor a geografia necessária para desenhar o mapa.
	 * Entrada: consultas sobre Karakorum, Kiev e nomes inexistentes.
	 * Saída esperada: 34 paradas, 9 cidades, quatro rotas da capital e valores nulos para nomes inválidos.
	 */
	@Test(timeout = TEMPO_LIMITE)
	public void mapaEhConsultavelPorNomes() {
		assertEquals("Paradas do mapa oficial", 34, api.getParadas().size());
		assertEquals("Cidades do mapa oficial", 9, api.getCidades().size());
		assertEquals("Tipo da capital", "KARAKORUM", api.getTipoParada("Karakorum"));
		assertNull("Karakorum não pertence a região", api.getRegiaoParada("Karakorum"));
		assertEquals("Tipo de parada de conselheiro", "DUPLA", api.getTipoParada("Conselheiro Norte"));
		assertEquals("Região de Kiev Nordeste", "RUSSIA", api.getRegiaoParada("Kiev Nordeste"));
		assertEquals("Rotas que saem da capital",
				Arrays.asList("Conselheiro Nordeste", "Pequim Norte", "Samarcanda Sudeste", "Sarai Sudoeste"),
				api.getVizinhas("Karakorum"));
		assertEquals("Anel de Kiev", Arrays.asList("Kiev Nordeste", "Kiev Sudeste", "Kiev Sudoeste"), api.getParadasDaCidade("Kiev"));
		assertNull("Parada inexistente não tem tipo", api.getTipoParada("Atlântida"));
		assertTrue("Parada inexistente não tem vizinhas", api.getVizinhas("Atlântida").isEmpty());
	}

	/**
	 * Objetivo: impedir movimento antes de ativar uma coluna.
	 * Entrada: pedir destinos e mover para uma vizinha da capital sem ativar.
	 * Saída esperada: nenhum destino, movimento recusado e peão parado.
	 */
	@Test(timeout = TEMPO_LIMITE)
	public void semColunaAtivadaNaoHaMovimento() {
		assertEquals("Sem coluna ativada não há cavalos", 0, api.getCavalosRestantes());
		assertTrue("Sem cavalos não há destinos", api.getDestinosPossiveis().isEmpty());
		assertFalse("Movimento sem cavalos deve ser recusado", api.moverPara("Sarai Sudoeste"));
		assertEquals("O peão deve continuar na capital", "Karakorum", api.getPosicaoPeao(0, 0));
	}

	/**
	 * Objetivo: liberar os cavalos da coluna ativada e calcular os destinos.
	 * Entrada: Ogedei ativa a vermelha (2 cavalos) e tenta ativar a verde no mesmo turno.
	 * Saída esperada: 2 cavalos; destinos a um e a dois passos, sem a origem; segunda ativação recusada.
	 */
	@Test(timeout = TEMPO_LIMITE)
	public void colunaAtivadaLiberaCavalosEDestinos() {
		assertTrue("A vermelha deve ser ativada", api.ativarColuna("VERMELHA"));
		assertEquals("Coluna ativada", "VERMELHA", api.getColunaAtivada());
		assertEquals("A vermelha de Ogedei dá dois cavalos", 2, api.getCavalosRestantes());
		List<String> destinos = api.getDestinosPossiveis();
		assertTrue("Vizinha da capital é destino", destinos.contains("Sarai Sudoeste"));
		assertTrue("Parada a dois passos é destino", destinos.contains("Moscou Nordeste"));
		assertFalse("A origem não é destino", destinos.contains("Karakorum"));
		assertFalse("Parada a três passos não é destino", destinos.contains("Moscou Noroeste"));
		assertFalse("Só uma coluna por turno", api.ativarColuna("VERDE"));
		assertEquals("A ativação notifica uma vez", 1, contador.notificacoes);
		assertSame("O observado notificado é a fachada", api, contador.ultimo);
	}

	/**
	 * Objetivo: mover o peão gastando cavalos e notificar a View.
	 * Entrada: vermelha (2 cavalos); mover para Sarai Sudoeste; depois tentar Moscou Noroeste com 1 cavalo.
	 * Saída esperada: peão em Sarai Sudoeste com 1 cavalo; segundo movimento recusado sem alterar nada.
	 */
	@Test(timeout = TEMPO_LIMITE)
	public void moverGastaCavalosENotifica() {
		api.ativarColuna("VERMELHA");
		assertTrue("Movimento para vizinha deve ser aceito", api.moverPara("Sarai Sudoeste"));
		assertEquals("Nova posição do peão", "Sarai Sudoeste", api.getPosicaoPeao(0, 0));
		assertEquals("Um cavalo gasto", 1, api.getCavalosRestantes());
		assertEquals("Ativação e movimento notificam", 2, contador.notificacoes);
		assertTrue("Com 1 cavalo é possível voltar à capital", api.getDestinosPossiveis().contains("Karakorum"));
		assertFalse("Destino distante deve ser recusado", api.moverPara("Moscou Noroeste"));
		assertEquals("Movimento recusado preserva a posição", "Sarai Sudoeste", api.getPosicaoPeao(0, 0));
		assertEquals("Movimento recusado preserva os cavalos", 1, api.getCavalosRestantes());
		assertEquals("Movimento recusado não notifica", 2, contador.notificacoes);
	}

	/**
	 * Objetivo: exigir o movimento do Khan antes de terminar o turno.
	 * Entrada: Ogedei ativa a amarela (Khan); tenta terminar; tenta província comum; move para Khan do Noroeste.
	 * Saída esperada: término recusado até o Khan ir para um local do Khan; depois a vez passa a Bia.
	 */
	@Test(timeout = TEMPO_LIMITE)
	public void khanPendenteImpedeTerminarTurno() {
		api.ativarColuna("AMARELA");
		assertTrue("A amarela de Ogedei obriga a mover o Khan", api.isKhanPendente());
		assertFalse("Não pode terminar com o Khan pendente", api.podeTerminarTurno());
		assertFalse("Terminar deve ser recusado", api.terminarTurno());
		assertEquals("Quatro províncias do Khan e a área de melhorias", 5, api.getDestinosDoKhan().size());
		assertFalse("Província comum não recebe o Khan", api.moverKhan("Estepe de Moscou"));
		assertTrue("Província do Khan deve ser aceita", api.moverKhan("Khan do Noroeste"));
		assertEquals("Posição do Khan", "Khan do Noroeste", api.getPosicaoKhan());
		assertTrue("Agora o turno pode terminar", api.terminarTurno());
		assertEquals("A vez passa para Bia", 1, api.getJogadorDaVez());
		assertTrue("Bia começa sem coluna ativada", api.getColunaAtivada() == null);
	}

	/**
	 * Objetivo: deixar Altani escolher o peão até a primeira ação de posição.
	 * Entrada: Altani escolhe o peão 1, ativa a verde e vai a Sarai Sudoeste.
	 * Saída esperada: só o peão 1 se move e a escolha fica bloqueada depois do movimento.
	 */
	@Test(timeout = TEMPO_LIMITE)
	public void altaniEscolhePeaoAntesDeMover() {
		PreparacaoCompletaTest.iniciarPadrao(Arrays.asList("Ana", "Bia"), Arrays.asList("Altani", "Tolui"));
		assertEquals("Altani tem dois peões", 2, api.getNumPeoes(0));
		assertTrue("Altani pode escolher o peão no início", api.podeEscolherPeao());
		assertTrue("Escolha do segundo peão", api.escolherPeao(1));
		assertEquals("Peão ativo", 1, api.getPeaoAtivo());
		api.ativarColuna("VERDE");
		assertTrue("O peão escolhido deve se mover", api.moverPara("Sarai Sudoeste"));
		assertEquals("O segundo peão foi para Sarai Sudoeste", "Sarai Sudoeste", api.getPosicaoPeao(0, 1));
		assertEquals("O primeiro peão ficou na capital", "Karakorum", api.getPosicaoPeao(0, 0));
		assertFalse("Depois de mover não se troca de peão", api.escolherPeao(0));
	}

	/**
	 * Objetivo: não oferecer como destino uma parada simples ocupada por outro jogador.
	 * Entrada: Ana termina em Sarai Sudoeste; Bia (Chagatai) ativa a verde, com 1 cavalo.
	 * Saída esperada: Sarai Sudoeste fora dos destinos de Bia; Samarcanda Sudeste disponível.
	 */
	@Test(timeout = TEMPO_LIMITE)
	public void paradaSimplesOcupadaNaoEhDestino() {
		api.ativarColuna("VERMELHA");
		api.moverPara("Sarai Sudoeste");
		assertTrue("Ana termina o turno", api.terminarTurno());
		assertTrue("Bia ativa a verde", api.ativarColuna("VERDE"));
		List<String> destinos = api.getDestinosPossiveis();
		assertFalse("Parada simples ocupada não é destino", destinos.contains("Sarai Sudoeste"));
		assertTrue("Parada livre vizinha é destino", destinos.contains("Samarcanda Sudeste"));
		assertFalse("Mover para a parada ocupada deve ser recusado", api.moverPara("Sarai Sudoeste"));
	}

	/**
	 * Objetivo: parar de notificar um observador removido.
	 * Entrada: remover o contador e ativar uma coluna.
	 * Saída esperada: nenhuma notificação.
	 */
	@Test(timeout = TEMPO_LIMITE)
	public void observadorRemovidoNaoEhNotificado() {
		api.remove(contador);
		api.ativarColuna("VERMELHA");
		assertEquals("Observador removido não é notificado", 0, contador.notificacoes);
	}

	/**
	 * Objetivo: expor os locais clicáveis do Khan no mapa.
	 * Entrada: consulta com a partida recém-criada.
	 * Saída esperada: as quatro províncias do Khan em ordem alfabética e, por último, a área de melhorias.
	 */
	@Test(timeout = TEMPO_LIMITE)
	public void locaisDoKhanIncluemAreaDeMelhorias() {
		List<String> locais = api.getLocaisDoKhan();
		assertEquals("Quatro províncias e a área de melhorias", 5, locais.size());
		assertEquals("A área de melhorias vem por último", ModelAPI.AREA_MELHORIAS, locais.get(4));
		assertTrue("Província do Khan é um local do Khan", locais.contains("Khan do Noroeste"));
		assertFalse("Província comum não é local do Khan", locais.contains("Estepe de Moscou"));
		assertTrue("Sem Khan pendente não há destinos", api.getDestinosDoKhan().isEmpty());
	}

	/**
	 * Objetivo: permitir mover o Khan para a área de melhorias (manual, p. 9) e não deixá-lo ficar onde está.
	 * Entrada: Ogedei ativa a amarela e move o Khan para a área; Bia ativa a vermelha (Khan) e tenta a área de novo.
	 * Saída esperada: primeiro movimento aceito; no turno de Bia a área sai dos destinos e o movimento é recusado.
	 */
	@Test(timeout = TEMPO_LIMITE)
	public void khanPodeIrParaAreaDeMelhorias() {
		api.ativarColuna("AMARELA");
		assertTrue("A área de melhorias é destino do Khan", api.getDestinosDoKhan().contains(ModelAPI.AREA_MELHORIAS));
		assertTrue("Mover para a área deve ser aceito", api.moverKhan(ModelAPI.AREA_MELHORIAS));
		assertEquals("Posição do Khan", ModelAPI.AREA_MELHORIAS, api.getPosicaoKhan());
		assertTrue("Com o Khan resolvido o turno pode terminar", api.podeTerminarTurno());
		assertTrue("Ana termina o turno", api.terminarTurno());
		assertTrue("A vermelha de Chagatai obriga a mover o Khan", api.ativarColuna("VERMELHA") && api.isKhanPendente());
		assertEquals("Só as quatro províncias continuam disponíveis", 4, api.getDestinosDoKhan().size());
		assertFalse("O Khan não pode ficar onde está", api.moverKhan(ModelAPI.AREA_MELHORIAS));
		assertTrue("A obrigação continua pendente", api.isKhanPendente());
	}
}
