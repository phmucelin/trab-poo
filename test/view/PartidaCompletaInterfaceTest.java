package view;

import static org.junit.Assert.*;
import java.awt.Component;
import java.awt.Container;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JRadioButton;
import javax.swing.SwingUtilities;
import org.junit.Test;
import controller.Controlador;
import model.EncerramentoModelAPITest;
import model.ModelAPI;
import model.ObservadorIF;

/** Partida completa por botões e cliques no mapa, sem injetar recursos ou pontuação. */
public class PartidaCompletaInterfaceTest {
	private static <T> List<T> componentes(Container raiz, Class<T> tipo) {
		List<T> encontrados = new ArrayList<>();
		for (Component c : raiz.getComponents()) {
			if (tipo.isInstance(c)) encontrados.add(tipo.cast(c));
			if (c instanceof Container) encontrados.addAll(componentes((Container)c, tipo));
		}
		return encontrados;
	}
	private static JButton botao(Container raiz, String trecho) {
		for (JButton botao : componentes(raiz, JButton.class)) if (botao.getText().contains(trecho)) return botao;
		throw new AssertionError("Botão ausente: " + trecho);
	}
	private static JComboBox<?> combo(Container raiz, String nome) {
		for (JComboBox<?> combo : componentes(raiz, JComboBox.class)) if (nome.equals(combo.getName())) return combo;
		throw new AssertionError("Seletor ausente: " + nome);
	}
	public static void clicar(PainelTabuleiro mapa, Point ponto) {
		double escala = Math.min((mapa.getWidth() - 24.0) / CoordenadasMapa.LARGURA_IMAGEM, (mapa.getHeight() - 24.0) / CoordenadasMapa.ALTURA_IMAGEM);
		int x = (mapa.getWidth() - (int)Math.round(CoordenadasMapa.LARGURA_IMAGEM * escala)) / 2;
		int y = (mapa.getHeight() - (int)Math.round(CoordenadasMapa.ALTURA_IMAGEM * escala)) / 2;
		mapa.dispatchEvent(new MouseEvent(mapa, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0,
				x + (int)Math.round(ponto.x * escala), y + (int)Math.round(ponto.y * escala), 1, false));
	}
	/** Também usada pela prévia visual final; sempre executar na EDT. */
	public static void jogarAteFim() {
		ModelAPI api = ModelAPI.getInstance();
		EncerramentoModelAPITest.prepararPartida();
		PainelPreparacao preparacao = new PainelPreparacao();
		PainelTurno turno = new PainelTurno();
		PainelAcoes acoes = new PainelAcoes();
		PainelResultado resultado = new PainelResultado();
		PainelTabuleiro mapa = new PainelTabuleiro();
		mapa.setSize(PainelTabuleiro.LARGURA, PainelTabuleiro.ALTURA);
		try {
			for (int jogador = 0; jogador < 2; jogador++) {
				botao(preparacao, "Estou pronto").doClick(0);
				componentes(preparacao, JRadioButton.class).get(0).doClick(0);
				botao(preparacao, "Confirmar carta").doClick(0);
			}
			assertEquals("JOGO", Controlador.getInstance().getEtapaPartida());
			assertTrue(Controlador.getInstance().mostrarCartaSecreta());
			assertTrue(api.isCartaSecretaVisivel());
			for (int limite = 0; limite < 300 && !api.isPartidaEncerrada(); limite++) {
				botao(turno, "<b>Branca</b>").doClick(0);
				if (api.getJogadorDaVez() == 0) {
					assertEquals(api.podeTrocarCavaloPorEspada(), botao(acoes, "Chagatai:").isEnabled());
					if (botao(acoes, "Chagatai:").isEnabled()) botao(acoes, "Chagatai:").doClick(0);
					String alvo = alvoAdjacenteOuProximo();
					if (alvo != null) {
						String posicao = api.getPosicaoPeao(0, 0);
						if (!api.getParadasDaCidade(alvo).contains(posicao)) {
							String passo = rotaMaisCurta(api.getParadasDaCidade(alvo)).get(0);
							clicar(mapa, CoordenadasMapa.daParada(passo));
							assertEquals(passo, api.getPosicaoPeao(0, 0));
						}
						if (api.podeAtacarCidade(alvo, null)) {
							combo(acoes, "cidade").setSelectedItem(alvo);
							combo(acoes, "tesouro").setSelectedItem(api.getTesourosCidade(alvo).get(0));
							assertTrue(botao(acoes, "Atacar (").isEnabled());
							botao(acoes, "Atacar (").doClick(0);
						}
					}
				}
				String destino = api.getDestinosDoKhan().get(0);
				clicar(mapa, CoordenadasMapa.doLocalDoKhan(destino));
				assertEquals(destino, api.getPosicaoKhan());
				assertTrue(botao(turno, "Terminar turno").isEnabled());
				botao(turno, "Terminar turno").doClick(0);
				assertFalse(api.isCartaSecretaVisivel());
			}
			assertTrue("A partida deve encerrar pelas conquistas", api.isPartidaEncerrada());
			assertEquals("RESULTADO", Controlador.getInstance().getEtapaPartida());
			assertFalse(api.getVencedores().isEmpty());
			assertTrue(api.getVencedores().contains(0));
			assertTrue(api.getRodadaFinal() > 0);
			assertFalse(botao(turno, "Terminar turno").isEnabled());
			assertFalse(api.atacarCidade("Sarai", null));
			assertFalse(api.mostrarCartaSecreta());
			int votos = api.getVotosJogador(0);
			assertFalse(api.terminarTurno());
			assertEquals(votos, api.getVotosJogador(0));
			assertTrue(api.getVotosInfluenciaFinal(0) > 0);
			assertFalse(api.getCartaSecretaFinal(0).isEmpty());
			boolean vencedorVisivel = false;
			for (javax.swing.JLabel label : componentes(resultado, javax.swing.JLabel.class)) if (label.getText().contains("Vencedor:") || label.getText().contains("Vitória compartilhada")) vencedorVisivel = true;
			assertTrue(vencedorVisivel);
		} finally {
			for (ObservadorIF painel : List.of(preparacao, turno, acoes, resultado, mapa)) api.remove(painel);
		}
	}
	@Test public void partidaCompletaDaPreparacaoAoResultado() throws Exception {
		SwingUtilities.invokeAndWait(new Runnable() { @Override public void run() { jogarAteFim(); } });
	}
	private static String alvoAdjacenteOuProximo() {
		ModelAPI api = ModelAPI.getInstance();
		String melhor = null; int distancia = Integer.MAX_VALUE;
		for (String cidade : api.getCidades()) if (api.getTesourosRestantesCidade(cidade) > 0) {
			int passos = rotaMaisCurta(api.getParadasDaCidade(cidade)).size();
			if (passos < distancia) { distancia = passos; melhor = cidade; }
		}
		return melhor;
	}
	private static List<String> rotaMaisCurta(List<String> destinos) {
		ModelAPI api = ModelAPI.getInstance();
		String origem = api.getPosicaoPeao(0, 0);
		Map<String, String> anterior = new HashMap<>();
		ArrayDeque<String> fila = new ArrayDeque<>(); fila.add(origem); anterior.put(origem, null);
		while (!fila.isEmpty()) {
			String atual = fila.remove();
			if (destinos.contains(atual)) {
				List<String> rota = new ArrayList<>();
				for (String p = atual; !p.equals(origem); p = anterior.get(p)) rota.add(0, p);
				return rota;
			}
			for (String vizinha : api.getVizinhas(atual)) if (!anterior.containsKey(vizinha)) { anterior.put(vizinha, atual); fila.add(vizinha); }
		}
		throw new AssertionError("Cidade inalcançável");
	}
}
