package view;

import static org.junit.Assert.*;

import java.awt.Component;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import controller.ConfirmadorNovaPartida;
import controller.Controlador;
import model.EncerramentoModelAPITest;
import model.ModelAPI;

/**
 * Fim de partida (4ª iteração): depois do vencedor o Controlador pergunta se
 * há nova partida. Sim reabre a JanelaInicial; Não encerra a aplicação.
 * O diálogo e o encerramento são trocados por stubs para o teste não travar.
 */
public class FimDePartidaInterfaceTest {
	private static class Resposta implements ConfirmadorNovaPartida {
		final boolean resposta;
		final List<String> perguntas = new ArrayList<String>();
		Resposta(boolean resposta) { this.resposta = resposta; }
		@Override public boolean confirmar(Component pai, String vencedores) {
			perguntas.add(vencedores);
			return resposta;
		}
	}
	private static class Contador implements Runnable {
		int chamadas;
		@Override public void run() { chamadas++; }
	}
	private static class Jogar implements Runnable {
		@Override public void run() { PartidaCompletaInterfaceTest.jogarAteFim(); }
	}
	private static class Nada implements Runnable {
		@Override public void run() { }
	}

	@Before public void exigirTela() {
		Assume.assumeFalse(GraphicsEnvironment.isHeadless());
	}
	@After public void restaurar() {
		Controlador.getInstance().setConfirmador(new DialogoNovaPartida());
		for (Frame f : Frame.getFrames()) if (f instanceof JanelaInicial) f.dispose();
	}

	private static boolean haJanelaInicial() {
		for (Frame f : Frame.getFrames()) if (f instanceof JanelaInicial && f.isVisible()) return true;
		return false;
	}
	private static void jogarEEsperarFila() throws Exception {
		SwingUtilities.invokeAndWait(new Jogar());
		SwingUtilities.invokeAndWait(new Nada()); // o diálogo é agendado com invokeLater
	}

	@Test public void respostaSimReabreAEscolhaDosPersonagens() throws Exception {
		Resposta sim = new Resposta(true);
		Contador encerrou = new Contador();
		Controlador.getInstance().setConfirmador(sim);
		Controlador.getInstance().setEncerrador(encerrou);
		jogarEEsperarFila();
		assertEquals("pergunta uma única vez", 1, sim.perguntas.size());
		assertTrue(sim.perguntas.get(0).contains("Vencedor") || sim.perguntas.get(0).contains("Vitória compartilhada"));
		assertTrue(haJanelaInicial());
		assertEquals(0, encerrou.chamadas);
	}

	@Test public void respostaNaoEncerraAAplicacao() throws Exception {
		Resposta nao = new Resposta(false);
		Contador encerrou = new Contador();
		Controlador.getInstance().setConfirmador(nao);
		Controlador.getInstance().setEncerrador(encerrou);
		jogarEEsperarFila();
		assertEquals(1, nao.perguntas.size());
		assertEquals(1, encerrou.chamadas);
		assertFalse(haJanelaInicial());
	}

	private static class AbrirJogo implements Runnable {
		final File arquivo;
		boolean abriu;
		AbrirJogo(File arquivo) { this.arquivo = arquivo; }
		@Override public void run() { abriu = Controlador.getInstance().carregarPartida(arquivo); }
	}
	private static class Preparar implements Runnable {
		File arquivo;
		@Override public void run() {
			EncerramentoModelAPITest.prepararPartida();
			ModelAPI.getInstance().escolherCartaSecreta(0);
			ModelAPI.getInstance().escolherCartaSecreta(0);
			try {
				arquivo = File.createTempFile("partida", ".txt");
				Files.write(arquivo.toPath(), ModelAPI.getInstance().gerarSalvamento(), StandardCharsets.US_ASCII);
			} catch (java.io.IOException e) { throw new AssertionError(e); }
		}
	}

	/** Com a JanelaJogo real aberta, o Sim a fecha e abre a escolha dos personagens. */
	@Test public void respostaSimDescartaATelaDoTabuleiro() throws Exception {
		Preparar preparar = new Preparar();
		SwingUtilities.invokeAndWait(preparar);
		AbrirJogo abrir = new AbrirJogo(preparar.arquivo);
		SwingUtilities.invokeAndWait(abrir);
		preparar.arquivo.delete();
		assertTrue(abrir.abriu);
		Frame jogo = null;
		for (Frame f : Frame.getFrames()) if (f instanceof JanelaJogo && f.isDisplayable()) jogo = f;
		assertNotNull(jogo);
		assertEquals(javax.swing.JFrame.EXIT_ON_CLOSE, ((JanelaJogo) jogo).getDefaultCloseOperation());
		Resposta sim = new Resposta(true);
		Controlador.getInstance().setConfirmador(sim);
		Controlador.getInstance().setEncerrador(new Contador());
		jogarEEsperarFila();
		assertEquals(1, sim.perguntas.size());
		assertFalse("o tabuleiro antigo deve ser descartado", jogo.isDisplayable());
		assertTrue(haJanelaInicial());
		for (Frame f : Frame.getFrames()) if (f instanceof JanelaJogo) f.dispose();
	}
}
