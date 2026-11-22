package model;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.Test;

/**
 * Salvar e recuperar (4ª iteração), só pela fachada pública. As partidas são
 * jogadas com jogadas sorteadas de semente fixa; a "fotografia" reúne tudo o
 * que a fachada deixa ver do estado.
 */
public class SalvamentoPartidaTest {
	private static final String[] COLUNAS = { "VERMELHA", "VERDE", "AMARELA", "BRANCA" };
	private static final String[] TESOUROS = { "FERRO", "PELES", "GADO", "TECIDO", "ESPECIARIAS" };
	private static final String[] TRIBUTOS = { "MOEDA", "ESPADA", "YURT" };
	private final ModelAPI m = ModelAPI.getInstance();

	// ------------------------------------------------------------- preparação

	private void novaPartida(int semente) {
		Random r = new Random(semente);
		int n = 2 + r.nextInt(4);
		List<String> todos = new ArrayList<>(List.of("ALTANI", "JOCHI", "CHAGATAI", "TOLUI", "OGEDEI"));
		Collections.shuffle(todos, r);
		List<String> nomes = new ArrayList<>();
		for (int i = 0; i < n; i++) nomes.add("J" + i);
		assertTrue(m.novaPartidaCompleta(nomes, todos.subList(0, n), PecasDoJogo.cadastro(), new Random(semente)));
		while (m.isPreparacao()) {
			if (m.isEscolhaMelhoriaInicialPendente()) assertTrue(m.escolherMelhoriaInicial(r.nextInt(2)));
			else assertTrue(m.escolherCartaSecreta(r.nextInt(2)));
		}
	}

	// ------------------------------------------------------- jogadas sorteadas

	private <T> T sortear(Random r, List<T> l) { return l.isEmpty() ? null : l.get(r.nextInt(l.size())); }

	/** Um turno inteiro com tentativas sorteadas; recusas são ignoradas. */
	private void jogarTurno(Random r) {
		int vez = m.getJogadorDaVez();
		resolverPendencias();
		assertTrue(m.ativarColuna(sortear(r, m.getColunasDisponiveis())));
		for (int k = 0; k < 40; k++) {
			switch (r.nextInt(11)) {
			case 0: case 1: { String d = sortear(r, m.getDestinosPossiveis()); if (d != null) m.moverPara(d); break; }
			case 2: m.pegarTributo(sortear(r, m.getProvincias())); break;
			case 3: m.construirYurt(sortear(r, m.getParadas())); break;
			case 4: case 5: {
				String c = sortear(r, m.getCidades());
				String t = sortear(r, m.getTesourosCidade(c));
				if (!m.atacarCidade(c, t)) m.pegarTesouroComMoeda(c, t);
				break;
			}
			case 6: m.trocarCavaloPorEspada(); break;
			case 7: m.comprarMelhoria(r.nextInt(6) - 1, COLUNAS[r.nextInt(3)]); break;
			case 8: {
				String c = sortear(r, m.getConselheiros());
				m.entregarTesouros(c, oferta(vez, c));
				break;
			}
			case 9: for (String b : m.getBonusJogador(vez)) if (m.usarBonus(b)) break; break;
			default: if (m.podeEscolherPeao()) m.escolherPeao(r.nextInt(m.getNumPeoes(vez))); break;
			}
			resolverPendencias();
		}
		if (m.isKhanPendente()) assertTrue(m.moverKhan(sortear(r, m.getDestinosDoKhan())));
		resolverPendencias();
		assertTrue("turno travado", m.terminarTurno());
	}

	private Map<String, Integer> oferta(int jogador, String conselheiro) {
		Map<String, Integer> pedido = m.getPedidoConselheiro(conselheiro);
		if (!pedido.isEmpty()) return pedido;
		Map<String, Integer> qualquer = new java.util.HashMap<>();
		int falta = 3;
		for (String t : TESOUROS) {
			int n = Math.min(falta, m.getTesourosJogador(jogador, t) + m.getTesourosVirtuais(t));
			if (n > 0) { qualquer.put(t, n); falta -= n; }
		}
		return qualquer;
	}

	private void resolverPendencias() {
		for (int i = 0; i < 10; i++) {
			if (m.isTesouroDoBonusPendente()) { assertTrue(m.escolherTesouroDoBonus(m.getOpcoesTesouroDoBonus().get(0))); continue; }
			if (m.isMelhoriaGratuitaPendente()) {
				boolean ok = false;
				for (int k = -1; k < 5 && !ok; k++) ok = m.escolherMelhoriaGratuita(k);
				if (!ok) return;
				continue;
			}
			return;
		}
	}

	// -------------------------------------------------------------- fotografia

	private String foto() {
		StringBuilder s = new StringBuilder();
		s.append(m.getRodada()).append('|').append(m.getRodadaFinal()).append('|').append(m.getJogadorDaVez())
				.append('|').append(m.getPosicaoKurultai()).append('|').append(m.getPosicaoKhan())
				.append('|').append(m.isPartidaEncerrada()).append('\n');
		for (int j = 0; j < m.getNumJogadores(); j++) {
			s.append(m.getNomeJogador(j)).append(m.getHerdeiroJogador(j)).append(m.getCorJogador(j))
					.append(m.getYurtsDisponiveis(j)).append('/').append(m.getVotosJogador(j))
					.append('/').append(m.getTotalTesourosRecebidos(j)).append('/').append(m.getTotalTesourosRestantes(j)).append(':');
			for (String t : TRIBUTOS) s.append(m.getTributosJogador(j, t)).append(',');
			for (String t : TESOUROS) s.append(m.getTesourosJogador(j, t)).append(',');
			for (String c : COLUNAS) s.append(c).append(m.getIdsMelhoriasJogador(j, c));
			s.append(m.getBonusJogador(j));
			for (String b : m.getBonusJogador(j)) s.append(m.isBonusUsado(j, b));
			for (int p = 0; p < m.getNumPeoes(j); p++) s.append(m.getPosicaoPeao(j, p)).append(';');
			s.append('\n');
		}
		for (String p : m.getParadas()) if (!m.getYurtsParada(p).isEmpty()) s.append(p).append(m.getYurtsParada(p)).append('\n');
		for (String c : m.getCidades()) {
			s.append(c).append(m.isCidadeRevelada(c)).append(m.getTesourosCidade(c)).append(m.getConquistadorCidade(c)).append('\n');
		}
		for (String p : m.getProvincias()) s.append(p).append(m.getPecasProvincia(p)).append(',');
		s.append('\n').append(m.getIdsMercadoMelhorias()).append(m.getMelhoriasNaReserva()).append(m.getIdsCartasAbertas()).append('\n');
		for (String c : m.getConselheiros()) s.append(c).append(m.getBonusConselheiro(c)).append(',');
		for (String t : TRIBUTOS) s.append(m.getTributosVirtuais(t));
		if (m.isPartidaEncerrada()) {
			s.append(m.getVencedores());
			for (int j = 0; j < m.getNumJogadores(); j++) s.append(m.getCartaSecretaFinal(j)).append(m.getVotosInfluenciaFinal(j)).append(m.getVotosSecretosFinal(j));
		}
		return s.toString();
	}

	private void jogarAte(Random r, int turnos) {
		for (int i = 0; i < turnos && !m.isPartidaEncerrada(); i++) jogarTurno(r);
	}

	// ------------------------------------------------------------------ testes

	/**
	 * Objetivo: o estado restaurado é igual ao salvo e salvar de novo gera o mesmo texto.
	 * Entrada: 30 partidas (2 a 5 jogadores, herdeiros sorteados) salvas em turnos sorteados.
	 * Saída esperada: fotografias iguais, texto idêntico e só caracteres ASCII.
	 */
	@Test public void idaEVoltaPreservaOEstado() {
		for (int semente = 0; semente < 30; semente++) {
			novaPartida(semente);
			Random r = new Random(1000 + semente);
			jogarAte(r, r.nextInt(60));
			if (m.isPartidaEncerrada()) continue;
			assertTrue(m.podeSalvar());
			String antes = foto();
			List<String> linhas = m.gerarSalvamento();
			for (String l : linhas) for (char c : l.toCharArray()) assertTrue("ASCII: " + l, c >= 32 && c < 127);
			assertTrue("semente " + semente, m.restaurarPartida(linhas));
			assertEquals("semente " + semente + ": " + m.getErroConfiguracao(), antes, foto());
			assertEquals(linhas, m.gerarSalvamento());
		}
	}

	/**
	 * Objetivo: jogar a partir de um save é igual a continuar a partida original.
	 * Entrada: de cada save, a mesma sequência sorteada, até o fim da partida, na original e na restaurada.
	 * Saída esperada: fotografias finais iguais (inclui pilhas, descartes e votos finais).
	 */
	@Test public void continuarDoSaveEIgualAPartidaOriginal() {
		int encerradas = 0;
		for (int semente = 0; semente < 12; semente++) {
			novaPartida(100 + semente);
			Random r = new Random(2000 + semente);
			jogarAte(r, 3 + r.nextInt(20));
			if (!m.podeSalvar()) continue;
			List<String> linhas = m.gerarSalvamento();
			jogarAte(new Random(5000 + semente), 1500);
			String original = foto();
			if (m.isPartidaEncerrada()) encerradas++;
			assertTrue(m.restaurarPartida(linhas));
			jogarAte(new Random(5000 + semente), 1500);
			assertEquals("semente " + semente, original, foto());
		}
		assertTrue("Poucas partidas chegaram ao fim", encerradas >= 6);
	}

	/** Objetivo: salvar só antes de o jogador jogar. Saída: falso depois de ativar a coluna e na preparação. */
	@Test public void soSalvaNoInicioDoTurno() {
		assertTrue(m.novaPartidaCompleta(List.of("Ana", "Bia"), List.of("CHAGATAI", "ALTANI"), PecasDoJogo.cadastro(), new Random(7)));
		assertFalse(m.podeSalvar());
		assertTrue(m.gerarSalvamento().isEmpty());
		assertTrue(m.escolherCartaSecreta(0));
		assertTrue(m.escolherCartaSecreta(0));
		assertTrue(m.podeSalvar());
		assertTrue(m.ativarColuna("BRANCA"));
		assertFalse(m.podeSalvar());
		assertTrue(m.gerarSalvamento().isEmpty());
		while (!m.podeTerminarTurno()) assertTrue(m.moverKhan(m.getDestinosDoKhan().get(0)));
		assertTrue(m.terminarTurno());
		assertTrue(m.podeSalvar());
	}

	/** Objetivo: arquivo ASCII mesmo com acentos. Saída: nomes voltam iguais e nenhum byte passa de 127. */
	@Test public void nomesComAcentoViramAscii() {
		assertTrue(m.novaPartidaCompleta(List.of("José; Ünal, \\ -", "Çağrı:"), List.of("TOLUI", "JOCHI"), PecasDoJogo.cadastro(), new Random(3)));
		while (m.isPreparacao()) {
			if (m.isEscolhaMelhoriaInicialPendente()) assertTrue(m.escolherMelhoriaInicial(0));
			else assertTrue(m.escolherCartaSecreta(0));
		}
		List<String> linhas = m.gerarSalvamento();
		for (String l : linhas) for (char c : l.toCharArray()) assertTrue(c >= 32 && c < 127);
		assertTrue(m.restaurarPartida(linhas));
		assertEquals("José; Ünal, \\ -", m.getNomeJogador(0));
		assertEquals("Çağrı:", m.getNomeJogador(1));
		assertEquals("TOLUI", m.getHerdeiroJogador(0));
	}

	/** Objetivo: arquivo ruim não estraga a partida. Saída: restaurar devolve falso, com causa, e o estado não muda. */
	@Test public void arquivoInvalidoNaoAlteraAPartida() {
		novaPartida(11);
		jogarAte(new Random(1), 5);
		assertTrue(m.podeSalvar());
		String antes = foto();
		List<String> bom = m.gerarSalvamento();
		List<List<String>> ruins = new ArrayList<>();
		ruins.add(new ArrayList<String>());
		ruins.add(List.of("OUTRA_COISA;1"));
		ruins.add(List.of("HERDEIROS_DE_KHAN;2"));
		ruins.add(bom.subList(0, bom.size() / 2));
		ruins.add(trocar(bom, "JOGADOR;", "JOGADOR;x;"));
		ruins.add(trocar(bom, "ESTOQUE;", "ESTOQUE;ESPADA:99,YURT:0,MOEDA:0;"));
		ruins.add(trocar(bom, "PARTIDA;", "PARTIDA;0;"));
		ruins.add(remover(bom, "MERCADO;"));
		ruins.add(trocar(bom, "PILHA_TESOUROS;", "PILHA_TESOUROS;FERRO,"));
		ruins.add(trocar(bom, "PILHA_MELHORIAS;", "PILHA_MELHORIAS;VERDE_MOEDA,"));
		ruins.add(trocar(bom, "CARTAS_ABERTAS;", "CARTAS_ABERTAS;ABERTA_INEXISTENTE,"));
		List<String> comAcento = new ArrayList<>(bom);
		comAcento.add("JOGADOR;José;ALTANI");
		ruins.add(comAcento);
		for (List<String> ruim : ruins) {
			assertFalse(String.valueOf(ruim), m.restaurarPartida(ruim));
			assertFalse(m.getErroConfiguracao().isEmpty());
			assertEquals(antes, foto());
		}
		assertTrue(m.restaurarPartida(bom));
		assertEquals(antes, foto());
	}

	private static List<String> trocar(List<String> linhas, String prefixo, String novoPrefixo) {
		List<String> l = new ArrayList<>(linhas);
		for (int i = 0; i < l.size(); i++) if (l.get(i).startsWith(prefixo)) { l.set(i, novoPrefixo + l.get(i).substring(prefixo.length())); break; }
		return l;
	}
	private static List<String> remover(List<String> linhas, String prefixo) {
		List<String> l = new ArrayList<>(linhas);
		l.removeIf(x -> x.startsWith(prefixo));
		return l;
	}
}
