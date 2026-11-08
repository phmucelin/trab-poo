package model;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.junit.Test;

/**
 * Partidas completas com jogadas sorteadas, feitas só pelos comandos públicos
 * que a interface chama. Depois de cada jogada aceita, o teste confere regras
 * do manual que precisam valer em qualquer estado; depois de cada jogada
 * recusada, confere que nada mudou. No fim, recalcula a influência regional
 * e o vencedor de forma independente do Model.
 *
 * As sementes são fixas: uma falha sempre se repete com a mesma semente.
 */
public class RegrasEmPartidasAleatoriasTest {
	private static final int PARTIDAS = 40;
	private static final String[] TRIBUTOS = { "MOEDA", "ESPADA", "YURT" };
	private static final String[] TESOUROS = { "FERRO", "PELES", "GADO", "TECIDO", "ESPECIARIAS" };
	private static final String[] COLORIDAS = { "VERMELHA", "VERDE", "AMARELA" };
	private static final String[] TODAS_COLUNAS = { "VERMELHA", "VERDE", "AMARELA", "BRANCA" };
	/** Manual, p. 15: votos por número de melhorias na coluna (0 a 4). */
	private static final int[] VOTOS_PRIMEIRO = { 1, 1, 2, 2, 3 };
	private static final int[] VOTOS_SEGUNDO = { 0, 1, 1, 2, 2 };
	/** Manual, p. 14: limite do Kurultai para 2, 3, 4 e 5 jogadores. */
	private static final int[] LIMITE_KURULTAI = { 0, 0, 10, 14, 16, 17 };

	private final ModelAPI m = ModelAPI.getInstance();
	private Random sorteio;
	private int semente;
	private List<String> herdeiros;
	private int[] votosAntes;
	private int rodadaDoGatilho;
	private int jogadas;
	private int recusas;

	/**
	 * Objetivo: regras do manual em qualquer estado de partidas completas.
	 * Entrada: 40 partidas de quatro jogadores (sementes 0 a 39), herdeiros e jogadas sorteados por semente fixa.
	 * Saída esperada: todas terminam, nenhuma regra é violada e o resultado bate com o recálculo independente.
	 */
	@Test public void regrasValemDoInicioAoResultado() {
		for (semente = 0; semente < PARTIDAS; semente++) jogarPartida();
		assertTrue("Poucas jogadas recusadas foram exercitadas", recusas > 1000);
	}

	private void jogarPartida() {
		sorteio = new Random(semente);
		int n = 2 + sorteio.nextInt(4);
		List<String> todos = new ArrayList<>(List.of("ALTANI", "JOCHI", "CHAGATAI", "TOLUI", "OGEDEI"));
		Collections.shuffle(todos, sorteio);
		herdeiros = todos.subList(0, n);
		List<String> nomes = new ArrayList<>();
		for (int i = 0; i < n; i++) nomes.add("J" + i);
		assertTrue(m.novaPartidaCompleta(nomes, herdeiros, PecasDoJogo.cadastro(), new Random(semente)));
		conferirPreparacao(n);
		while (m.isPreparacao()) {
			if (m.isEscolhaMelhoriaInicialPendente()) assertTrue(m.escolherMelhoriaInicial(sorteio.nextInt(2)));
			else assertTrue(m.escolherCartaSecreta(sorteio.nextInt(2)));
		}
		votosAntes = new int[n];
		rodadaDoGatilho = -1;
		for (int turno = 0; !m.isPartidaEncerrada(); turno++) {
			assertTrue(falha("partida longa demais"), turno < 2000);
			jogarTurno();
		}
		assertTrue(falha("partida encerrada sem gatilho"), rodadaDoGatilho >= 0);
		conferirResultado(n);
	}

	private void jogarTurno() {
		int vez = m.getJogadorDaVez();
		Map<String, String> peoesNoInicio = posicoesDosPeoes();
		resolverPendencias();
		String coluna = sortear(m.getColunasDisponiveis());
		assertNotNull(falha("sem coluna disponível"), coluna);
		tentarRecusa("ativar coluna indisponível", () -> m.ativarColuna("INEXISTENTE"));
		aceitar("ativar coluna", m.ativarColuna(coluna));
		if (m.isKhanPendente()) tentarRecusa("terminar com Khan pendente", () -> m.terminarTurno());
		for (int k = 0; k < 30; k++) jogadaSorteada(vez);
		if (m.isKhanPendente()) {
			tentarRecusa("Khan parado no lugar", () -> m.moverKhan(m.getPosicaoKhan()));
			aceitar("mover Khan", m.moverKhan(sortear(m.getDestinosDoKhan())));
		}
		resolverPendencias();
		conferirPeoesDeOutrosParados(vez, peoesNoInicio);
		int rodada = m.getRodada();
		aceitar("terminar turno", m.terminarTurno());
		// Manual, p. 14: a partida acaba ao fim da rodada seguinte à do gatilho, depois do último jogador.
		if (m.isPartidaEncerrada()) {
			assertEquals(falha("acabou fora da rodada final"), m.getRodadaFinal(), rodada);
			assertEquals(falha("acabou antes do último jogador"), m.getNumJogadores() - 1, vez);
		}
	}

	private void jogadaSorteada(int vez) {
		switch (sorteio.nextInt(12)) {
		case 0: case 1: {
			String destino = sortear(m.getDestinosPossiveis());
			if (destino == null) break;
			int cavalos = m.getCavalosRestantes();
			aceitar("mover", m.moverPara(destino));
			assertTrue(falha("mover sem gastar cavalo"), m.getCavalosRestantes() < cavalos);
			break;
		}
		case 2: {
			String parada = destinoQualquer();
			if (!m.podeMoverPara(parada)) tentarRecusa("mover para parada sorteada", () -> m.moverPara(parada));
			break;
		}
		case 3: {
			String provincia = sortear(m.getProvincias());
			String tipo = m.getTipoTributoProvincia(provincia);
			if (!m.podePegarTributo(provincia)) { tentarRecusa("tributo", () -> m.pegarTributo(provincia)); break; }
			int pecas = m.getPecasProvincia(provincia), maos = m.getMaosRestantes(), antes = m.getTributosJogador(vez, tipo);
			aceitar("tributo", m.pegarTributo(provincia));
			assertEquals(falha("peças da província"), pecas - 1, m.getPecasProvincia(provincia));
			assertEquals(falha("mãos"), maos - 1, m.getMaosRestantes());
			assertEquals(falha("tributo recebido"), antes + 1, m.getTributosJogador(vez, tipo));
			break;
		}
		case 4: {
			String parada = sortear(m.getParadas());
			if (!m.podeConstruirYurt(parada)) { tentarRecusa("yurt", () -> m.construirYurt(parada)); break; }
			int disponiveis = m.getYurtsDisponiveis(vez), naParada = m.getYurtsParada(parada).size();
			int pecas = m.getTributosJogador(vez, "YURT") + m.getTributosVirtuais("YURT");
			aceitar("yurt", m.construirYurt(parada));
			assertEquals(falha("estoque de yurts"), disponiveis - 1, m.getYurtsDisponiveis(vez));
			assertEquals(falha("yurt na parada"), naParada + 1, m.getYurtsParada(parada).size());
			assertEquals(falha("tributo de yurt gasto"), pecas - 1, m.getTributosJogador(vez, "YURT") + m.getTributosVirtuais("YURT"));
			break;
		}
		case 5: {
			String cidade = sortear(m.getCidades());
			String tipo = sortear(m.getTesourosCidade(cidade));
			if (!m.podeAtacarCidade(cidade, tipo)) {
				if (m.podePegarTesouroComMoeda(cidade, tipo)) {
					int moedas = moedas(vez), tesouros = m.getTesourosRestantesCidade(cidade);
					aceitar("Ogedei", m.pegarTesouroComMoeda(cidade, tipo));
					assertEquals(falha("Ogedei paga 1 moeda"), moedas - 1, moedas(vez));
					assertEquals(falha("Ogedei tira 1 tesouro"), tesouros - 1, m.getTesourosRestantesCidade(cidade));
					assertTrue(falha("Ogedei não tira o último"), m.getTesourosRestantesCidade(cidade) >= 1);
				} else tentarRecusa("ataque", () -> m.atacarCidade(cidade, tipo));
				break;
			}
			int custo = m.getCustoAtaqueCidade(cidade), espadas = m.getTributosJogador(vez, "ESPADA") + m.getTributosVirtuais("ESPADA");
			int tesouros = m.getTesourosRestantesCidade(cidade);
			aceitar("ataque", m.atacarCidade(cidade, tipo));
			// Manual, p. 10: o 1º tesouro do turno numa cidade custa 1 espada; os seguintes, 2.
			assertEquals(falha("custo do ataque"), espadas - custo, m.getTributosJogador(vez, "ESPADA") + m.getTributosVirtuais("ESPADA"));
			assertEquals(falha("tesouro retirado"), tesouros - 1, m.getTesourosRestantesCidade(cidade));
			if (tesouros == 1) assertEquals(falha("conquista"), m.getCorJogador(vez), m.getConquistadorCidade(cidade));
			if (m.getTesourosRestantesCidade(cidade) > 0) assertEquals(falha("2º ataque custa 2"), 2, m.getCustoAtaqueCidade(cidade));
			break;
		}
		case 6:
			if (m.podeTrocarCavaloPorEspada()) {
				int cavalos = m.getCavalosRestantes();
				aceitar("Chagatai", m.trocarCavaloPorEspada());
				assertEquals(falha("Chagatai troca 1 cavalo"), cavalos - 1, m.getCavalosRestantes());
				assertFalse(falha("Chagatai só 1 vez por turno"), m.podeTrocarCavaloPorEspada());
			} else tentarRecusa("Chagatai", () -> m.trocarCavaloPorEspada());
			break;
		case 7: {
			int indice = sorteio.nextInt(7) - 1;
			String destino = TODAS_COLUNAS[sorteio.nextInt(4)];
			if (!m.podeComprarMelhoria(indice, destino)) { tentarRecusa("compra", () -> m.comprarMelhoria(indice, destino)); break; }
			assertNotEquals(falha("compra para a branca"), "BRANCA", destino);
			int moedas = moedas(vez), naColuna = m.getMelhoriasJogador(vez, destino).size();
			aceitar("compra", m.comprarMelhoria(indice, destino));
			assertEquals(falha("compra custa 2 moedas"), moedas - 2, moedas(vez));
			assertEquals(falha("melhoria na coluna"), naColuna + 1, m.getMelhoriasJogador(vez, destino).size());
			break;
		}
		case 8: {
			String conselheiro = sortear(m.getConselheiros());
			Map<String, Integer> oferta = ofertaPara(vez, conselheiro);
			if (!m.podeEntregarTesouros(conselheiro, oferta)) { tentarRecusa("entrega", () -> m.entregarTesouros(conselheiro, oferta)); break; }
			int votos = m.getVotosJogador(vez), valor = m.getVotosEntrega(conselheiro);
			// A peça de bônus continua no conselheiro até a primeira entrega.
			String bonus = m.getBonusConselheiro(conselheiro);
			boolean primeiro = bonus != null;
			int abertas = m.getCartasAbertas().size();
			aceitar("entrega", m.entregarTesouros(conselheiro, oferta));
			int ganho = m.getVotosJogador(vez) - votos;
			// Manual, pp. 13, 17 e 18: votos da entrega, mais 1 pela peça de voto (só o primeiro a entregar)
			// e 1 por carta aberta cumprida nessa jogada.
			int esperado = valor + (primeiro && "VOTO".equals(bonus) ? 1 : 0) + abertas - m.getCartasAbertas().size();
			assertEquals(falha("votos da entrega"), esperado, ganho);
			assertNull(falha("bônus fica com o primeiro"), m.getBonusConselheiro(conselheiro));
			if (primeiro) assertTrue(falha("primeiro recebe o bônus"), m.getBonusJogador(vez).contains(bonus));
			// Manual, p. 13: só entrega quem está ou passou pela parada do conselheiro neste turno.
			assertTrue(falha("entrega sem passar pelo conselheiro"), m.isConselheiroVisitado(conselheiro));
			break;
		}
		case 9:
			for (String bonus : m.getBonusJogador(vez)) {
				if (m.podeUsarBonus(bonus)) {
					aceitar("bônus", m.usarBonus(bonus));
					assertTrue(falha("bônus de uso único"), m.isBonusUsado(vez, bonus));
					assertFalse(falha("bônus reutilizado"), m.podeUsarBonus(bonus));
					break;
				}
			}
			break;
		case 10:
			if (m.podeEscolherPeao()) aceitar("trocar peão", m.escolherPeao(sorteio.nextInt(m.getNumPeoes(vez))));
			else tentarRecusa("trocar peão", () -> m.escolherPeao(sorteio.nextInt(m.getNumPeoes(vez))));
			break;
		default:
			if (m.isKhanPendente()) tentarRecusa("terminar antes do Khan", () -> m.terminarTurno());
			tentarRecusa("bônus inexistente", () -> m.usarBonus("INEXISTENTE"));
			break;
		}
		resolverPendencias();
	}

	private void resolverPendencias() {
		for (int i = 0; i < 10; i++) {
			if (m.isTesouroDoBonusPendente()) { aceitar("tesouro do bônus", m.escolherTesouroDoBonus(sortear(m.getOpcoesTesouroDoBonus()))); continue; }
			if (m.isMelhoriaGratuitaPendente()) {
				List<Integer> validos = new ArrayList<>();
				for (int k = -1; k < 5; k++) if (m.podeEscolherMelhoriaGratuita(k)) validos.add(k);
				if (validos.isEmpty()) return;
				int vez = m.getJogadorDaVez(), brancas = m.getMelhoriasJogador(vez, "BRANCA").size();
				aceitar("melhoria grátis", m.escolherMelhoriaGratuita(sortear(validos)));
				assertEquals(falha("grátis vai para a branca"), brancas + 1, m.getMelhoriasJogador(vez, "BRANCA").size());
				continue;
			}
			return;
		}
	}

	// ------------------------------------------------------------ regras gerais

	private void aceitar(String jogada, boolean aceita) {
		assertTrue(falha(jogada + " recusada"), aceita);
		jogadas++;
		conferirInvariantes();
		if (m.isPartidaEncerrada()) return;
		if (rodadaDoGatilho < 0 && (m.getPosicaoKurultai() >= m.getLimiteKurultai() || todasConquistadas())) {
			rodadaDoGatilho = m.getRodada();
			// Manual, p. 14: termina a rodada atual e joga mais uma rodada completa.
			assertEquals(falha("rodada final"), rodadaDoGatilho + 1, m.getRodadaFinal());
		}
		if (rodadaDoGatilho < 0) assertEquals(falha("fim disparado sem gatilho"), -1, m.getRodadaFinal());
	}

	private interface Comando { boolean executar(); }

	/** Jogada que as regras proíbem: tem de ser recusada e não pode alterar nada do estado público. */
	private void tentarRecusa(String jogada, Comando comando) {
		String antes = estado();
		assertFalse(falha(jogada + " deveria ser recusada"), comando.executar());
		recusas++;
		assertEquals(falha(jogada + " recusada alterou o estado"), antes, estado());
	}

	private void conferirPreparacao(int n) {
		assertEquals(falha("limite do Kurultai"), LIMITE_KURULTAI[n], m.getLimiteKurultai());
		for (int j = 0; j < n; j++) {
			// Manual, p. 3: 1ª e 2ª pessoa recebem 1 moeda; as demais, 2.
			assertEquals(falha("moedas iniciais"), j < 2 ? 1 : 2, m.getTributosJogador(j, "MOEDA"));
			for (int p = 0; p < m.getNumPeoes(j); p++) assertEquals(falha("peão começa em Karakorum"), "Karakorum", m.getPosicaoPeao(j, p));
			assertEquals(falha("Altani tem 2 peões"), herdeiros.get(j).equals("ALTANI") ? 2 : 1, m.getNumPeoes(j));
		}
		int reveladas = 0;
		for (String cidade : m.getCidades()) if (m.isCidadeRevelada(cidade)) {
			reveladas++;
			assertEquals(falha("cidade revelada com 4 tesouros"), 4, m.getTesourosRestantesCidade(cidade));
		}
		assertEquals(falha("3 cidades reveladas"), 3, reveladas);
		for (String provincia : m.getProvincias()) assertEquals(falha("1 tributo por província"), 1, m.getPecasProvincia(provincia));
		assertEquals(falha("5 melhorias no mercado"), 5, m.getMercadoMelhorias().size());
		assertEquals(falha("4 cartas abertas"), 4, m.getCartasAbertas().size());
		assertNull(falha("Khan começa fora do tabuleiro"), m.getPosicaoKhan());
	}

	private void conferirInvariantes() {
		int n = m.getNumJogadores();
		assertTrue(falha("cavalos negativos"), m.getCavalosRestantes() >= 0);
		assertTrue(falha("mãos negativas"), m.getMaosRestantes() >= 0);
		Map<String, Integer> yurtsNoMapa = new HashMap<>();
		for (String parada : m.getParadas()) {
			List<String> yurts = m.getYurtsParada(parada);
			String tipo = m.getTipoParada(parada);
			// Manual, p. 11: parada simples comporta 1 yurt; dupla, 2; Karakorum, só o de Tolui.
			if (tipo.equals("SIMPLES")) assertTrue(falha("2 yurts em parada simples " + parada), yurts.size() <= 1);
			if (tipo.equals("DUPLA")) assertTrue(falha("3 yurts em parada dupla " + parada), yurts.size() <= 2);
			if (tipo.equals("KARAKORUM")) for (String cor : yurts) assertEquals(falha("yurt de não Tolui em Karakorum"), "TOLUI", herdeiros.get(indiceDaCor(cor)));
			for (String cor : yurts) yurtsNoMapa.merge(cor, 1, Integer::sum);
			int peoes = 0; Set<Integer> donos = new HashSet<>();
			for (int j = 0; j < n; j++) for (int p = 0; p < m.getNumPeoes(j); p++) if (parada.equals(m.getPosicaoPeao(j, p))) { peoes++; donos.add(j); }
			// Manual, p. 7: só se termina em parada ocupada se ela for dupla (2 peões) ou Karakorum.
			if (tipo.equals("SIMPLES")) assertTrue(falha("peões de jogadores diferentes em parada simples " + parada), donos.size() <= 1);
			if (tipo.equals("DUPLA")) assertTrue(falha("3 peões em parada dupla " + parada), peoes <= 2);
		}
		int disponiveis = 0;
		for (String cidade : m.getCidades()) {
			String dono = m.getConquistadorCidade(cidade);
			if (dono != null) yurtsNoMapa.merge(dono, 1, Integer::sum);
			assertTrue(falha("tesouros da cidade"), m.getTesourosRestantesCidade(cidade) >= 0 && m.getTesourosRestantesCidade(cidade) <= 4);
			if (m.isCidadeRevelada(cidade) && dono == null) disponiveis++;
		}
		// Manual, p. 5: sempre há 3 cidades disponíveis para ataque, enquanto houver.
		int naoConquistadas = 0;
		for (String cidade : m.getCidades()) if (m.getConquistadorCidade(cidade) == null) naoConquistadas++;
		assertEquals(falha("cidades disponíveis"), Math.min(3, naoConquistadas), disponiveis);
		for (String provincia : m.getProvincias()) {
			int pecas = m.getPecasProvincia(provincia);
			assertTrue(falha("província com mais de 3 tributos"), pecas >= 0 && pecas <= 3);
		}
		int melhorias = m.getMercadoMelhorias().size() + m.getMelhoriasNaReserva();
		int somaVotos = 0;
		for (int j = 0; j < n; j++) {
			String cor = m.getCorJogador(j);
			for (String tributo : TRIBUTOS) assertTrue(falha("tributo negativo"), m.getTributosJogador(j, tributo) >= 0);
			// Manual, p. 2: 12 yurts por jogador, no estoque, no mapa ou nas cidades conquistadas.
			assertEquals(falha("12 yurts de " + cor), 12, m.getYurtsDisponiveis(j) + yurtsNoMapa.getOrDefault(cor, 0));
			int linhas = Integer.MAX_VALUE;
			for (String coluna : COLORIDAS) {
				List<String> ids = m.getIdsMelhoriasJogador(j, coluna);
				assertTrue(falha("coluna com mais de 4"), ids.size() <= 4);
				if (!herdeiros.get(j).equals("JOCHI")) for (String id : ids) assertTrue(falha("cor errada na coluna " + coluna + ": " + id), id.startsWith("MELHORIA_" + coluna + "_"));
				linhas = Math.min(linhas, ids.size());
				melhorias += ids.size();
			}
			int brancas = m.getMelhoriasJogador(j, "BRANCA").size();
			melhorias += brancas;
			// Manual, pp. 11 e 20: uma branca por linha completa; a inicial de Jochi ocupa o espaço da 1ª linha.
			int esperadas = herdeiros.get(j).equals("JOCHI") ? Math.max(1, linhas) : linhas;
			boolean pendente = j == m.getJogadorDaVez() && m.isMelhoriaGratuitaPendente();
			boolean semPecas = m.getMercadoMelhorias().isEmpty() && m.getMelhoriasNaReserva() == 0;
			if (pendente || semPecas) assertTrue(falha("brancas demais"), brancas <= esperadas);
			else assertEquals(falha("brancas por linha completa"), esperadas, brancas);
			int votos = m.getVotosJogador(j);
			assertTrue(falha("votos diminuíram"), votos >= votosAntes[j]);
			// Guarda só os votos ganhos durante o jogo; os finais são conferidos em conferirResultado.
			if (!m.isPartidaEncerrada()) votosAntes[j] = votos;
			somaVotos += votos;
		}
		assertEquals(falha("44 melhorias"), 44, melhorias);
		// Manual, p. 14: a ficha branca avança sempre que alguém ganha votos.
		if (!m.isPartidaEncerrada()) assertEquals(falha("Kurultai"), somaVotos, m.getPosicaoKurultai());
	}

	private void conferirPeoesDeOutrosParados(int vez, Map<String, String> antes) {
		Map<String, String> depois = posicoesDosPeoes();
		int movidos = 0;
		for (String peao : antes.keySet()) if (!antes.get(peao).equals(depois.get(peao))) {
			assertTrue(falha("peão de outro jogador andou: " + peao), peao.startsWith(vez + ":"));
			movidos++;
		}
		// Manual, p. 20: Altani age com um peão só por turno.
		assertTrue(falha("dois peões andaram no mesmo turno"), movidos <= 1);
	}

	// ------------------------------------------------------------ fim de jogo

	private void conferirResultado(int n) {
		int[] total = new int[n];
		for (int j = 0; j < n; j++) {
			int secretos = m.getVotosSecretosFinal(j);
			assertTrue(falha("carta secreta vale 0 a 3"), secretos >= 0 && secretos <= 3);
			assertEquals(falha("influência de J" + j), influenciaEsperada(j, n), m.getVotosInfluenciaFinal(j));
			total[j] = votosAntes[j] + m.getVotosInfluenciaFinal(j) + secretos;
			assertEquals(falha("total de votos"), total[j], m.getVotosJogador(j));
		}
		// Manual, p. 16: mais votos; empate, mais tesouros não entregues; depois, mais perto de Karakorum.
		List<Integer> esperados = new ArrayList<>();
		for (int j = 0; j < n; j++) {
			if (esperados.isEmpty()) { esperados.add(j); continue; }
			int c = comparar(j, esperados.get(0), total);
			if (c > 0) { esperados.clear(); esperados.add(j); }
			else if (c == 0) esperados.add(j);
		}
		assertEquals(falha("vencedores"), esperados, m.getVencedores());
	}

	private int comparar(int a, int b, int[] total) {
		if (total[a] != total[b]) return Integer.compare(total[a], total[b]);
		if (m.getTotalTesourosRestantes(a) != m.getTotalTesourosRestantes(b)) return Integer.compare(m.getTotalTesourosRestantes(a), m.getTotalTesourosRestantes(b));
		return Integer.compare(m.getDistanciaCapital(b), m.getDistanciaCapital(a));
	}

	/** Recalcula a influência pelo manual, p. 15, só com consultas públicas. */
	private int influenciaEsperada(int jogador, int n) {
		Map<String, String> colunaDaRegiao = Map.of("PERSIA", "VERDE", "RUSSIA", "AMARELA", "CHINA", "VERMELHA");
		int votos = 0;
		for (Map.Entry<String, String> regiao : colunaDaRegiao.entrySet()) {
			int[] yurts = new int[n];
			for (String parada : m.getParadas()) if (regiao.getKey().equals(m.getRegiaoParada(parada)))
				for (String cor : m.getYurtsParada(parada)) yurts[indiceDaCor(cor)]++;
			for (String cidade : m.getCidades()) {
				String dono = m.getConquistadorCidade(cidade);
				if (dono != null && regiao.getKey().equals(m.getRegiaoParada(m.getParadasDaCidade(cidade).get(0)))) yurts[indiceDaCor(dono)]++;
			}
			int maior = 0, segundo = 0;
			for (int y : yurts) maior = Math.max(maior, y);
			int empatadosNoTopo = 0;
			for (int y : yurts) if (y == maior) empatadosNoTopo++;
			for (int y : yurts) if (y < maior) segundo = Math.max(segundo, y);
			int melhorias = m.getMelhoriasJogador(jogador, regiao.getValue()).size();
			if (maior > 0 && yurts[jogador] == maior) votos += VOTOS_PRIMEIRO[melhorias];
			// Empate no 1º lugar: ninguém fica em 2º.
			else if (empatadosNoTopo == 1 && segundo > 0 && yurts[jogador] == segundo) votos += VOTOS_SEGUNDO[melhorias];
		}
		return votos;
	}

	// ------------------------------------------------------------ auxiliares

	private String estado() {
		StringBuilder s = new StringBuilder();
		s.append(m.getJogadorDaVez()).append('|').append(m.getRodada()).append('|').append(m.getColunaAtivada())
				.append('|').append(m.getCavalosRestantes()).append('|').append(m.getMaosRestantes()).append('|').append(m.getPeaoAtivo())
				.append('|').append(m.getPosicaoKhan()).append('|').append(m.isKhanPendente()).append('|').append(m.getPosicaoKurultai())
				.append('|').append(m.getMercadoMelhorias()).append('|').append(m.getMelhoriasNaReserva()).append('|').append(posicoesDosPeoes());
		for (String tributo : TRIBUTOS) s.append('|').append(m.getTributosVirtuais(tributo));
		for (int j = 0; j < m.getNumJogadores(); j++) {
			s.append("|J").append(j).append(':').append(m.getVotosJogador(j)).append(',').append(m.getYurtsDisponiveis(j));
			for (String tributo : TRIBUTOS) s.append(',').append(m.getTributosJogador(j, tributo));
			for (String tesouro : TESOUROS) s.append(',').append(m.getTesourosJogador(j, tesouro));
			for (String coluna : TODAS_COLUNAS) s.append(',').append(m.getMelhoriasJogador(j, coluna));
			s.append(',').append(m.getBonusJogador(j));
		}
		for (String provincia : m.getProvincias()) s.append('|').append(m.getPecasProvincia(provincia));
		for (String cidade : m.getCidades()) s.append('|').append(m.getTesourosCidade(cidade)).append(m.getConquistadorCidade(cidade)).append(m.isCidadeRevelada(cidade));
		for (String parada : m.getParadas()) s.append('|').append(m.getYurtsParada(parada));
		for (String conselheiro : m.getConselheiros()) s.append('|').append(m.getBonusConselheiro(conselheiro)).append(m.isConselheiroVisitado(conselheiro));
		return s.toString();
	}

	private Map<String, String> posicoesDosPeoes() {
		Map<String, String> posicoes = new HashMap<>();
		for (int j = 0; j < m.getNumJogadores(); j++) for (int p = 0; p < m.getNumPeoes(j); p++) posicoes.put(j + ":" + p, m.getPosicaoPeao(j, p));
		return posicoes;
	}

	private Map<String, Integer> ofertaPara(int vez, String conselheiro) {
		Map<String, Integer> pedido = m.getPedidoConselheiro(conselheiro), oferta = new HashMap<>();
		if (List.of(TESOUROS).containsAll(pedido.keySet())) { oferta.putAll(pedido); return oferta; }
		int falta = 3;
		for (String tesouro : TESOUROS) {
			int q = Math.min(falta, m.getTesourosJogador(vez, tesouro) + m.getTesourosVirtuais(tesouro));
			if (q > 0) { oferta.put(tesouro, q); falta -= q; }
		}
		return oferta;
	}

	private int moedas(int vez) { return m.getTributosJogador(vez, "MOEDA") + m.getTributosVirtuais("MOEDA"); }
	private String destinoQualquer() { return m.getParadas().get(sorteio.nextInt(m.getParadas().size())); }
	private boolean todasConquistadas() {
		for (String cidade : m.getCidades()) if (m.getConquistadorCidade(cidade) == null) return false;
		return true;
	}
	private int indiceDaCor(String cor) {
		for (int j = 0; j < m.getNumJogadores(); j++) if (m.getCorJogador(j).equals(cor)) return j;
		throw new AssertionError("Cor sem jogador: " + cor);
	}
	private <T> T sortear(List<T> lista) { return lista.isEmpty() ? null : lista.get(sorteio.nextInt(lista.size())); }
	private String falha(String regra) { return "semente " + semente + ", jogada " + jogadas + ": " + regra; }
}
