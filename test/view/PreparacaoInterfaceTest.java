package view;

import static org.junit.Assert.*;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.SwingUtilities;

import org.junit.Test;

import model.ModelAPI;
import model.PreparacaoCompletaTest;

/** Verifica a privacidade da passagem de tela e o fluxo real dos botões na EDT. */
public class PreparacaoInterfaceTest {
	private static <T> List<T> componentes(Container raiz, Class<T> tipo) {
		List<T> encontrados = new ArrayList<>();
		for (Component componente : raiz.getComponents()) {
			if (tipo.isInstance(componente)) encontrados.add(tipo.cast(componente));
			if (componente instanceof Container) encontrados.addAll(componentes((Container) componente, tipo));
		}
		return encontrados;
	}

	private static JButton botao(Container raiz, String texto) {
		for (JButton botao : componentes(raiz, JButton.class)) {
			if (botao.getText().equals(texto)) return botao;
		}
		throw new AssertionError("Botão ausente: " + texto);
	}

	@Test public void escondeCartasEntreJogadoresEConcluiPelosBotoes() throws Exception {
		SwingUtilities.invokeAndWait(new Runnable() {
			@Override public void run() {
				ModelAPI api = ModelAPI.getInstance();
				assertTrue(PreparacaoCompletaTest.criarSintetica(List.of("Ana <teste>", "Bia"), List.of("JOCHI", "ALTANI")));
				PainelPreparacao painel = new PainelPreparacao();
				try {
					assertTrue(componentes(painel, JRadioButton.class).isEmpty());
					assertFalse(botao(painel, "Confirmar carta secreta").isVisible());
					boolean escapado = false;
					for (JLabel label : componentes(painel, JLabel.class)) {
						if (label.getText().contains("Ana &lt;teste&gt;")) escapado = true;
					}
					assertTrue("Nomes são exibidos como texto, sem interpretar HTML", escapado);
					for (int i = 0; i < 2; i++) {
						assertEquals(i, api.getJogadorPreparando());
						assertTrue(componentes(painel, JRadioButton.class).isEmpty());
						botao(painel, "Estou pronto para ver minhas cartas").doClick();
						List<JRadioButton> opcoes = componentes(painel, JRadioButton.class);
						assertEquals(2, opcoes.size());
						assertFalse(botao(painel, "Confirmar carta secreta").isEnabled());
						opcoes.get(1).doClick();
						assertTrue(botao(painel, "Confirmar carta secreta").isEnabled());
						botao(painel, "Confirmar carta secreta").doClick();
						if (api.isEscolhaMelhoriaInicialPendente()) {
							assertEquals(2, componentes(painel, JRadioButton.class).size());
							componentes(painel, JRadioButton.class).get(0).doClick();
							botao(painel, "Confirmar melhoria inicial").doClick();
						}
					}
					assertFalse(api.isPreparacao());
					assertTrue(componentes(painel, JRadioButton.class).isEmpty());
					assertEquals(4, api.getColunasDisponiveis().size());
				} finally { api.remove(painel); }
			}
		});
	}
}
