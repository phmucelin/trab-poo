package model;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** Salvamento e recuperação de partidas (4ª iteração), sempre pela fachada ModelAPI. */
public class SalvamentoTest {
	private static final String[] TESOUROS = { "FERRO", "PELES", "GADO", "TECIDO", "ESPECIARIAS" };
	private final ModelAPI m = ModelAPI.getInstance();
	private File pasta;

	@Before public void criarPasta() throws IOException { pasta = Files.createTempDirectory("khan-salvamento").toFile(); }
	@After public void apagarPasta() {
		File[] arquivos = pasta.listFiles();
		if (arquivos != null) for (File f : arquivos) f.delete();
		pasta.delete();
	}

	/**
	 * Objetivo: a partida recuperada é idêntica à original e continua igual.
	 * Entrada: 25 partidas sorteadas (2 a 5 jogadores), salvas num turno sorteado; a original e a
	 * recuperada seguem com a mesma sequência de jogadas sorteadas até o fim.
	 * Saída esperada: o arquivo regravado logo após carregar é igual ao original, os arquivos de
	 * cada troca de turno seguinte coincidem e o resultado final (votos e vencedores) é o mesmo.
	 */
	@Test public void partidaRecuperadaContinuaIgualAOriginal() throws IOException {
		for (int semente = 0; semente < 25; semente++) {
			Random sorteio = new Random(semente);
			iniciar(sorteio, semente);
			int turnoDoSalvamento = 1 + sorteio.nextInt(60);
			for (int t = 0; t < turnoDoSalvamento && !m.isPartidaEncerrada(); t++) jogarTurno(sorteio);
			if (m.isPartidaEncerrada()) continue;
			File arquivo = new File(pasta, "jogo" + semente + ".txt");
			assertTrue(m.getErroArquivo(), m.salvar(arquivo));
			List<String> salvo = Files.readAllLines(arquivo.toPath(), StandardCharsets.US_ASCII);

			List<String> original = jogarAteOFim(new Random(1000 + semente));
			assertTrue(m.getErroArquivo(), m.carregar(arquivo));
			File regravado = new File(pasta, "regravado" + semente + ".txt");
			assertTrue(m.salvar(regravado));
			assertEquals("semente " + semente + ": regravação", salvo, Files.readAllLines(regravado.toPath(), StandardCharsets.US_ASCII));
			assertEquals("semente " + semente + ": continuação", original, jogarAteOFim(new Random(1000 + semente)));
		}
	}

	/**
	 * Objetivo: regra 1 do enunciado, salvar só antes de o jogador começar a jogar.
	 * Entrada: preparação, início do turno, coluna ativada, troca de peão de Altani, turno seguinte.
	 * Saída esperada: falso na preparação; verdadeiro no início; falso após ativar coluna ou trocar o
	 * peão; verdadeiro de novo no turno seguinte; e salvar() recusa quando não pode.
	 */
	@Test public void salvarSoAntesDeOJogadorComecar() {
		assertTrue(m.novaPartidaCompleta(List.of("Ana", "Bia"), List.of("ALTANI", "TOLUI"), PecasDoJogo.cadastro(), new Random(3)));
		assertFalse(m.podeSalvar());
		while (m.isPreparacao()) assertTrue(m.escolherCartaSecreta(0));
		assertTrue(m.podeSalvar());
		assertTrue(m.escolherPeao(1));
		assertFalse("trocar o peão já é começar a jogar", m.podeSalvar());
		assertFalse(m.salvar(new File(pasta, "x.txt")));
		assertFalse(m.getErroArquivo().isEmpty());
		assertTrue(m.ativarColuna("BRANCA"));
		assertFalse(m.podeSalvar());
		assertTrue(m.moverKhan(m.getDestinosDoKhan().get(0)));
		assertTrue(m.terminarTurno());
		assertTrue("o próximo jogador ainda não começou", m.podeSalvar());
		assertTrue(m.ativarColuna("BRANCA"));
		assertFalse(m.podeSalvar());
	}

	/**
	 * Objetivo: regras 2 e 3, texto ASCII puro com nome livre e extensão .txt.
	 * Entrada: jogadores "João" e "Zoë"; arquivo escolhido sem extensão.
	 * Saída esperada: grava "partida.txt" só com bytes ASCII; os nomes com acento voltam iguais.
	 */
	@Test public void arquivoEhAsciiPuroComExtensaoTxt() throws IOException {
		assertTrue(m.novaPartidaCompleta(List.of("João", "Zoë"), List.of("JOCHI", "OGEDEI"), PecasDoJogo.cadastro(), new Random(5)));
		while (m.isPreparacao()) {
			if (m.isEscolhaMelhoriaInicialPendente()) assertTrue(m.escolherMelhoriaInicial(0));
			else assertTrue(m.escolherCartaSecreta(0));
		}
		assertTrue(m.getErroArquivo(), m.salvar(new File(pasta, "partida")));
		File arquivo = new File(pasta, "partida.txt");
		assertTrue(arquivo.isFile());
		for (byte b : Files.readAllBytes(arquivo.toPath())) assertTrue("byte fora do ASCII: " + b, b >= 0);
		assertTrue(m.novaPartidaCompleta(List.of("X", "Y"), List.of("ALTANI", "TOLUI"), PecasDoJogo.cadastro(), new Random(1)));
		assertTrue(m.getErroArquivo(), m.carregar(arquivo));
		assertEquals("João", m.getNomeJogador(0));
		assertEquals("Zoë", m.getNomeJogador(1));
		assertEquals("JOCHI", m.getHerdeiroJogador(0));
		assertEquals(1, m.getMelhoriasJogador(0, "BRANCA").size());
	}

	/**
	 * Objetivo: arquivo inválido é recusado sem estragar a partida em curso.
	 * Entrada: arquivo truncado (sem FIM), arquivo com acento cru, número inválido e arquivo inexistente.
	 * Saída esperada: carregar() devolve falso com mensagem; a partida em curso continua igual.
	 */
	@Test public void arquivoInvalidoNaoAlteraAPartida() throws IOException {
		iniciar(new Random(7), 7);
		File bom = new File(pasta, "bom.txt");
		assertTrue(m.salvar(bom));
		List<String> linhas = Files.readAllLines(bom.toPath(), StandardCharsets.US_ASCII);
		List<String> semFim = new ArrayList<>(linhas.subList(0, linhas.size() - 1));
		List<String> numeroRuim = new ArrayList<>();
		for (String linha : linhas) numeroRuim.add(linha.startsWith("PARTIDA;") ? linha.replace("rodada=", "rodada=x") : linha);

		File truncado = new File(pasta, "truncado.txt");
		Files.write(truncado.toPath(), semFim, StandardCharsets.US_ASCII);
		File acento = new File(pasta, "acento.txt");
		Files.write(acento.toPath(), List.of("HERDEIROS_DE_KHAN;versao=1", "JOGADOR;nome=João", "FIM"), StandardCharsets.UTF_8);
		File ruim = new File(pasta, "ruim.txt");
		Files.write(ruim.toPath(), numeroRuim, StandardCharsets.US_ASCII);

		for (File arquivo : List.of(truncado, acento, ruim, new File(pasta, "nao-existe.txt"))) {
			assertFalse(arquivo.getName(), m.carregar(arquivo));
			assertFalse(arquivo.getName(), m.getErroArquivo().isEmpty());
			File depois = new File(pasta, "depois.txt");
			assertTrue(m.salvar(depois));
			assertEquals(arquivo.getName() + " alterou a partida", linhas, Files.readAllLines(depois.toPath(), StandardCharsets.US_ASCII));
		}
		assertTrue(m.carregar(ruim) == false && m.getErroArquivo().contains("rodada"));
	}

	/**
	 * Objetivo: o professor pode editar o arquivo para montar um cenário de teste.
	 * Entrada: arquivo salvo com as moedas do jogador 0 trocadas para 9 e uma espada a mais.
	 * Saída esperada: a partida carregada mostra 9 moedas e a espada a mais.
	 */
	@Test public void arquivoEditadoAMaoEhRespeitado() throws IOException {
		iniciar(new Random(11), 11);
		File arquivo = new File(pasta, "cenario.txt");
		assertTrue(m.salvar(arquivo));
		int espadas = m.getTributosJogador(0, "ESPADA");
		List<String> editado = new ArrayList<>();
		for (String linha : Files.readAllLines(arquivo.toPath(), StandardCharsets.US_ASCII)) {
			if (linha.startsWith("JOGADOR;indice=0;")) linha = linha.replaceAll(";MOEDA=\\d+", ";MOEDA=9").replaceAll(";ESPADA=\\d+", ";ESPADA=" + (espadas + 1));
			editado.add(linha);
		}
		Files.write(arquivo.toPath(), editado, StandardCharsets.US_ASCII);
		assertTrue(m.getErroArquivo(), m.carregar(arquivo));
		assertEquals(9, m.getTributosJogador(0, "MOEDA"));
		assertEquals(espadas + 1, m.getTributosJogador(0, "ESPADA"));
	}

	// ------------------------------------------------------------ partida sorteada

	private void iniciar(Random sorteio, int semente) {
		int n = 2 + sorteio.nextInt(4);
		List<String> herdeiros = new ArrayList<>(List.of("ALTANI", "JOCHI", "CHAGATAI", "TOLUI", "OGEDEI"));
		Collections.shuffle(herdeiros, sorteio);
		List<String> nomes = new ArrayList<>();
		for (int i = 0; i < n; i++) nomes.add("Jogador " + i);
		assertTrue(m.novaPartidaCompleta(nomes, herdeiros.subList(0, n), PecasDoJogo.cadastro(), new Random(semente)));
		while (m.isPreparacao()) {
			if (m.isEscolhaMelhoriaInicialPendente()) assertTrue(m.escolherMelhoriaInicial(sorteio.nextInt(2)));
			else assertTrue(m.escolherCartaSecreta(sorteio.nextInt(2)));
		}
	}

	/** Joga até o fim e devolve o arquivo de cada troca de turno, mais o resultado. */
	private List<String> jogarAteOFim(Random sorteio) throws IOException {
		List<String> retratos = new ArrayList<>();
		File arquivo = new File(pasta, "retrato.txt");
		for (int t = 0; !m.isPartidaEncerrada(); t++) {
			assertTrue("partida longa demais", t < 2000);
			assertTrue(m.salvar(arquivo));
			retratos.addAll(Files.readAllLines(arquivo.toPath(), StandardCharsets.US_ASCII));
			jogarTurno(sorteio);
		}
		for (int j = 0; j < m.getNumJogadores(); j++) retratos.add("votos " + j + "=" + m.getVotosJogador(j));
		retratos.add("vencedores=" + m.getVencedores());
		return retratos;
	}

	private void jogarTurno(Random sorteio) {
		resolverPendencias(sorteio);
		List<String> colunas = m.getColunasDisponiveis();
		assertTrue(m.ativarColuna(colunas.get(sorteio.nextInt(colunas.size()))));
		for (int k = 0; k < 25; k++) jogadaSorteada(sorteio);
		if (m.isKhanPendente()) {
			List<String> destinos = m.getDestinosDoKhan();
			assertTrue(m.moverKhan(destinos.get(sorteio.nextInt(destinos.size()))));
		}
		resolverPendencias(sorteio);
		assertTrue(m.terminarTurno());
	}

	private void jogadaSorteada(Random sorteio) {
		int vez = m.getJogadorDaVez();
		switch (sorteio.nextInt(9)) {
		case 0: case 1: {
			List<String> destinos = m.getDestinosPossiveis();
			if (!destinos.isEmpty()) m.moverPara(destinos.get(sorteio.nextInt(destinos.size())));
			break;
		}
		case 2: for (String p : m.getProvincias()) if (m.podePegarTributo(p)) { m.pegarTributo(p); break; } break;
		case 3: for (String p : m.getParadas()) if (m.podeConstruirYurt(p)) { m.construirYurt(p); break; } break;
		case 4:
			for (String c : m.getCidades()) {
				List<String> t = m.getTesourosCidade(c);
				String tipo = t.isEmpty() ? null : t.get(sorteio.nextInt(t.size()));
				if (m.podeAtacarCidade(c, tipo)) { m.atacarCidade(c, tipo); break; }
				if (m.podePegarTesouroComMoeda(c, tipo)) { m.pegarTesouroComMoeda(c, tipo); break; }
			}
			break;
		case 5: if (m.podeTrocarCavaloPorEspada()) m.trocarCavaloPorEspada(); break;
		case 6:
			for (int i = -1; i < 5; i++) for (String col : new String[] { "VERMELHA", "VERDE", "AMARELA" }) {
				if (m.podeComprarMelhoria(i, col)) { m.comprarMelhoria(i, col); resolverPendencias(sorteio); return; }
			}
			break;
		case 7:
			for (String c : m.getConselheiros()) {
				Map<String, Integer> oferta = oferta(vez, c);
				if (m.podeEntregarTesouros(c, oferta)) { m.entregarTesouros(c, oferta); break; }
			}
			break;
		default:
			for (String b : m.getBonusJogador(vez)) if (m.podeUsarBonus(b)) { m.usarBonus(b); break; }
		}
		resolverPendencias(sorteio);
	}

	private void resolverPendencias(Random sorteio) {
		for (int i = 0; i < 10; i++) {
			if (m.isTesouroDoBonusPendente()) {
				List<String> opcoes = m.getOpcoesTesouroDoBonus();
				m.escolherTesouroDoBonus(opcoes.get(sorteio.nextInt(opcoes.size())));
			} else if (m.isMelhoriaGratuitaPendente()) {
				for (int k = -1; k < 5; k++) if (m.podeEscolherMelhoriaGratuita(k)) { m.escolherMelhoriaGratuita(k); break; }
			} else return;
		}
	}

	private Map<String, Integer> oferta(int vez, String conselheiro) {
		Map<String, Integer> pedido = m.getPedidoConselheiro(conselheiro), oferta = new HashMap<>();
		if (List.of(TESOUROS).containsAll(pedido.keySet())) return pedido;
		int falta = 3;
		for (String t : TESOUROS) {
			int q = Math.min(falta, m.getTesourosJogador(vez, t) + m.getTesourosVirtuais(t));
			if (q > 0) { oferta.put(t, q); falta -= q; }
		}
		return oferta;
	}
}
