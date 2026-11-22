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
