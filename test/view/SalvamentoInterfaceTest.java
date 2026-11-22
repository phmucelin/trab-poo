package view;

import static org.junit.Assert.*;

import java.awt.Component;
import java.awt.Container;
import java.util.List;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import org.junit.Test;
import model.ModelAPI;

/** Botão de salvamento do PainelTurno, ligado e desligado pelo Observer (4ª iteração). */
public class SalvamentoInterfaceTest {
	private static JButton salvar(Container raiz) {
		for (Component c : raiz.getComponents()) {
			if (c instanceof JButton && "salvarPartida".equals(c.getName())) return (JButton) c;
			if (c instanceof Container) {
				JButton achado = salvar((Container) c);
				if (achado != null) return achado;
			}
		}
		return null;
	}

	/**
	 * Objetivo: regra 1, o botão só fica habilitado antes de o jogador da vez começar.
	 * Entrada: preparação, início do turno, coluna ativada, turno seguinte.
	 * Saída esperada: desabilitado, habilitado, desabilitado, habilitado.
	 */
	@Test public void botaoSalvarSegueOInicioDaJogada() throws Exception {
		SwingUtilities.invokeAndWait(new Runnable() {
			@Override public void run() {
				ModelAPI m = ModelAPI.getInstance();
				assertTrue(m.novaPartida(List.of("Ana", "Bia"), List.of("CHAGATAI", "OGEDEI")));
				PainelTurno painel = new PainelTurno();
				try {
					JButton botao = salvar(painel);
					assertNotNull(botao);
					assertFalse("preparação", botao.isEnabled());
					while (m.isPreparacao()) assertTrue(m.escolherCartaSecreta(0));
					assertTrue("antes de começar", botao.isEnabled());
					assertTrue(m.ativarColuna("BRANCA"));
					assertFalse("depois de ativar a coluna", botao.isEnabled());
					assertTrue(m.moverKhan(m.getDestinosDoKhan().get(0)));
					assertTrue(m.terminarTurno());
					assertTrue("antes do próximo jogador começar", botao.isEnabled());
				} finally {
					m.remove(painel);
				}
			}
		});
	}
}
