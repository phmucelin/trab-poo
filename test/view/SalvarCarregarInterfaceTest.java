package view;

import static org.junit.Assert.*;

import java.awt.Component;
import java.awt.Container;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.SwingUtilities;

import org.junit.After;
import org.junit.Assume;
import org.junit.Test;

import controller.Controlador;
import model.EncerramentoModelAPITest;
import model.ModelAPI;

/**
 * Salvar e carregar pela interface (4ª iteração): botão habilitado só antes
 * de o jogador jogar, arquivo ASCII em .txt e recusa de arquivo ruim sem
 * mexer na partida. O JFileChooser não é aberto; o painel chama o Controlador
 * com o arquivo escolhido, e é isso que o teste exercita.
 */
public class SalvarCarregarInterfaceTest {
	private final ModelAPI m = ModelAPI.getInstance();
	private final List<File> temporarios = new ArrayList<File>();

	@After public void limpar() {
		for (File f : temporarios) f.delete();
		for (Frame f : Frame.getFrames()) if (f instanceof JanelaJogo || f instanceof JanelaInicial) f.dispose();
	}

	private static void naEdt(Runnable r) throws Exception { SwingUtilities.invokeAndWait(r); }

	private static void botoes(Container raiz, List<JButton> achados) {
		for (Component c : raiz.getComponents()) {
			if (c instanceof JButton) achados.add((JButton) c);
			if (c instanceof Container) botoes((Container) c, achados);
		}
	}
	private static JButton botao(Container raiz, String trecho) {
		List<JButton> todos = new ArrayList<JButton>();
		botoes(raiz, todos);
		for (JButton b : todos) if (b.getText().contains(trecho)) return b;
		throw new AssertionError("Botão ausente: " + trecho);
	}
	private File temporario() throws Exception {
		File f = File.createTempFile("partida", ".txt");
		temporarios.add(f);
		return f;
	}
	private void comecarPartida() {
		EncerramentoModelAPITest.prepararPartida();
		assertTrue(m.escolherCartaSecreta(0));
		assertTrue(m.escolherCartaSecreta(0));
	}

	/** O botão Salvar acompanha o início do turno: ativo, some ao ativar a coluna, volta no turno seguinte. */
	@Test public void botaoSalvarSoFicaAtivoAntesDeJogar() throws Exception {
		naEdt(new Runnable() { @Override public void run() {
			comecarPartida();
			PainelTurno turno = new PainelTurno();
			try {
				assertTrue(botao(turno, "Salvar partida").isEnabled());
				assertTrue(botao(turno, "Carregar partida").isEnabled());
				botao(turno, "<b>Branca</b>").doClick(0);
				assertFalse(botao(turno, "Salvar partida").isEnabled());
				while (!m.podeTerminarTurno()) assertTrue(m.moverKhan(m.getDestinosDoKhan().get(0)));
				botao(turno, "Terminar turno").doClick(0);
				assertTrue(botao(turno, "Salvar partida").isEnabled());
			} finally {
				Controlador.getInstance().desregistra(turno);
			}
		} });
	}

	@Test public void salvaArquivoAsciiETextoELeDeVolta() throws Exception {
		Assume.assumeFalse(GraphicsEnvironment.isHeadless());
		final File arquivo = temporario();
		naEdt(new Runnable() { @Override public void run() {
			comecarPartida();
			Controlador c = Controlador.getInstance();
			assertTrue(c.podeSalvar());
			assertTrue(c.salvarPartida(arquivo));
			assertTrue(m.ativarColuna("BRANCA"));
			assertFalse(c.podeSalvar());
			assertFalse(c.salvarPartida(temporarioSeguro()));
			assertFalse(c.getErroArquivo().isEmpty());
		} });
		for (byte b : Files.readAllBytes(arquivo.toPath())) assertTrue(b >= 0);
		assertTrue(Files.readAllLines(arquivo.toPath(), StandardCharsets.US_ASCII).get(0).startsWith("HERDEIROS_DE_KHAN"));
		naEdt(new Runnable() { @Override public void run() {
			assertEquals("BRANCA", m.getColunaAtivada());
			assertTrue(Controlador.getInstance().carregarPartida(arquivo));
			// Voltou ao começo do turno: a coluna ativada depois do save não existe mais.
			assertNull(m.getColunaAtivada());
			assertTrue(m.podeSalvar());
			assertEquals(0, m.getJogadorDaVez());
		} });
		boolean abriu = false;
		for (Frame f : Frame.getFrames()) if (f instanceof JanelaJogo && f.isDisplayable()) abriu = true;
		assertTrue("carregar abre o tabuleiro", abriu);
	}

	@Test public void arquivoRuimNaoAlteraAPartida() throws Exception {
		final File lixo = temporario();
		final File naoAscii = temporario();
		Files.write(lixo.toPath(), List.of("isto nao e uma partida"), StandardCharsets.US_ASCII);
		Files.write(naoAscii.toPath(), "HERDEIROS_DE_KHAN;1\nJOGADOR;José".getBytes(StandardCharsets.UTF_8));
		naEdt(new Runnable() { @Override public void run() {
			comecarPartida();
			Controlador c = Controlador.getInstance();
			int vez = m.getJogadorDaVez();
			assertFalse(c.carregarPartida(lixo));
			assertFalse(c.getErroArquivo().isEmpty());
			assertFalse(c.carregarPartida(naoAscii));
			assertFalse(c.carregarPartida(new File(lixo.getParentFile(), "nao-existe-" + System.nanoTime() + ".txt")));
			assertEquals(vez, m.getJogadorDaVez());
			assertTrue(m.podeSalvar());
		} });
	}

	@Test public void janelaInicialTemBotaoDeCarregar() throws Exception {
		Assume.assumeFalse(GraphicsEnvironment.isHeadless());
		naEdt(new Runnable() { @Override public void run() {
			JanelaInicial inicial = new JanelaInicial();
			assertTrue(botao(inicial, "Carregar partida").isEnabled());
			inicial.dispose();
		} });
	}

	private File temporarioSeguro() {
		try { return temporario(); } catch (Exception e) { throw new AssertionError(e); }
	}
}
