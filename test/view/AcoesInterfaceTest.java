package view;

import static org.junit.Assert.*;

import java.awt.Component;
import java.awt.Container;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import org.junit.Test;

import model.ModelAPI;
import model.PreparacaoCompletaTest;

/** Fluxos reais dos botões, com atualizações via Observer na EDT. */
public class AcoesInterfaceTest {
	private static JButton botao(Container raiz, String prefixo) {
		for (Component c : raiz.getComponents()) {
			if (c instanceof JButton && ((JButton) c).getText().startsWith(prefixo)) return (JButton) c;
			if (c instanceof Container) {
				JButton encontrado = botao((Container) c, prefixo);
				if (encontrado != null) return encontrado;
			}
		}
		return null;
	}
	private static JComboBox<?> combo(Container raiz, String nome) {
		for (Component c : raiz.getComponents()) {
			if (c instanceof JComboBox && nome.equals(((JComponent) c).getName())) return (JComboBox<?>) c;
			if (c instanceof Container) {
				JComboBox<?> encontrado = combo((Container) c, nome);
				if (encontrado != null) return encontrado;
			}
		}
		return null;
	}

	@Test public void coletaEConstroiPelosBotoes() throws Exception {
		SwingUtilities.invokeAndWait(new Runnable() {
			@Override public void run() {
				ModelAPI api = ModelAPI.getInstance();
				assertTrue(PreparacaoCompletaTest.iniciarPadrao(List.of("Ana", "Bia"), List.of("ALTANI", "TOLUI")));
				PainelAcoes painel = new PainelAcoes();
				try {
					assertFalse(botao(painel, "Pegar tributo").isEnabled());
					assertTrue(api.ativarColuna("VERDE"));
					combo(painel, "provincia").setSelectedItem("Estepe Central");
					assertTrue(botao(painel, "Pegar tributo").isEnabled());
					botao(painel, "Pegar tributo").doClick();
					assertEquals(1, api.getTributosJogador(0, "YURT"));
					assertEquals(0, combo(painel, "provincia").getItemCount());
					assertTrue(api.moverPara("Sarai Sudoeste"));
					combo(painel, "parada").setSelectedItem("Sarai Sudoeste");
					assertTrue(botao(painel, "Construir yurt").isEnabled());
					botao(painel, "Construir yurt").doClick();
					assertEquals(List.of("AZUL"), api.getYurtsParada("Sarai Sudoeste"));
					assertFalse(botao(painel, "Construir yurt").isEnabled());
				} finally { api.remove(painel); }
			}
		});
	}

	@Test public void ogedeiEscolheTesouroEObserverBloqueiaSegundaCompra() throws Exception {
		SwingUtilities.invokeAndWait(new Runnable() {
			@Override public void run() {
				ModelAPI api = ModelAPI.getInstance();
				assertTrue(PreparacaoCompletaTest.criarSintetica(List.of("Ana", "Bia"), List.of("OGEDEI", "ALTANI")));
				PainelAcoes painel = new PainelAcoes();
				try {
					assertFalse(botao(painel, "Ogedei").isEnabled());
					assertTrue(api.escolherCartaSecreta(0));
					assertTrue(api.escolherCartaSecreta(0));
					assertTrue(api.ativarColuna("VERDE"));
					String cidade = api.getCidades().stream().filter(api::isCidadeRevelada).findFirst().orElseThrow();
					combo(painel, "cidade").setSelectedItem(cidade);
					String tipo = api.getTesourosCidade(cidade).get(0);
					combo(painel, "tesouro").setSelectedItem(tipo);
					assertTrue(botao(painel, "Ogedei").isEnabled());
					botao(painel, "Ogedei").doClick();
					assertEquals(1, api.getTesourosJogador(0, tipo));
					assertEquals(3, api.getTesourosRestantesCidade(cidade));
					assertFalse(botao(painel, "Ogedei").isEnabled());
					assertFalse(botao(painel, "Chagatai").isEnabled());
				} finally { api.remove(painel); }
			}
		});
	}
}
