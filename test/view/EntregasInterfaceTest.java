package view;

import static org.junit.Assert.*;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JSpinner;
import javax.swing.SwingUtilities;
import model.EntregasModelAPITest;
import model.ModelAPI;
import org.junit.Test;

/** Fluxo da entrega, ativação do bônus e escolha obrigatória pelos botões. */
public class EntregasInterfaceTest {
	private static <T> List<T> componentes(Container raiz, Class<T> tipo) {
		List<T> encontrados = new ArrayList<>();
		for (Component c : raiz.getComponents()) {
			if (tipo.isInstance(c)) encontrados.add(tipo.cast(c));
			if (c instanceof Container) encontrados.addAll(componentes((Container) c, tipo));
		}
		return encontrados;
	}
	private static JButton botao(Container raiz, String prefixo) {
		for (JButton botao : componentes(raiz, JButton.class)) if (botao.getText().startsWith(prefixo)) return botao;
		throw new AssertionError("Botão ausente: " + prefixo);
	}
	private static JComboBox<?> combo(Container raiz, String nome) {
		for (JComboBox<?> combo : componentes(raiz, JComboBox.class)) if (nome.equals(combo.getName())) return combo;
		throw new AssertionError("Seletor ausente: " + nome);
	}
	@Test public void entregaUsaBonusEConcluiEscolhaDeTesouro() throws Exception {
		SwingUtilities.invokeAndWait(new Runnable() {
			@Override public void run() {
				ModelAPI api = ModelAPI.getInstance();
				EntregasModelAPITest.prepararEntregaCapital(12);
				PainelEntregas painel = new PainelEntregas();
				try {
					combo(painel, "conselheiro").setSelectedItem("Karakorum");
					assertFalse(botao(painel, "Entregar").isEnabled());
					for (JSpinner spinner : componentes(painel, JSpinner.class)) {
						String tipo = spinner.getName().substring("oferta_".length());
						spinner.setValue(EntregasModelAPITest.ofertaCapital().getOrDefault(tipo, 0));
					}
					assertTrue(botao(painel, "Entregar").isEnabled());
					botao(painel, "Entregar").doClick();
					assertEquals(2, api.getVotosJogador(0));
					assertFalse(botao(painel, "Entregar").isEnabled());
					for (JSpinner spinner : componentes(painel, JSpinner.class)) assertEquals(0, spinner.getValue());
					assertTrue(botao(painel, "Usar bônus").isEnabled());
					botao(painel, "Usar bônus").doClick();
					assertTrue(api.isTesouroDoBonusPendente());
					assertFalse(api.podeTerminarTurno());
					String tipo = api.getOpcoesTesouroDoBonus().get(0);
					combo(painel, "tesouroDoBonus").setSelectedItem(tipo);
					assertTrue(botao(painel, "Confirmar tesouro").isEnabled());
					botao(painel, "Confirmar tesouro").doClick();
					assertEquals(1, api.getTesourosJogador(0, tipo));
					assertFalse(api.isTesouroDoBonusPendente());
					assertFalse(botao(painel, "Usar bônus").isEnabled());
					assertTrue(api.podeTerminarTurno());
				} finally { api.remove(painel); }
			}
		});
	}
	@Test public void preparacaoNaoPermiteEntregasNemUsoDeBonus() throws Exception {
		SwingUtilities.invokeAndWait(new Runnable() {
			@Override public void run() {
				ModelAPI api = ModelAPI.getInstance();
				assertTrue(api.novaPartida(List.of("Ana", "Bia"), List.of("OGEDEI", "ALTANI")));
				PainelEntregas painel = new PainelEntregas();
				try {
					assertFalse(botao(painel, "Entregar").isEnabled());
					assertFalse(botao(painel, "Usar bônus").isEnabled());
					assertEquals(7, combo(painel, "conselheiro").getItemCount());
					for (JSpinner spinner : componentes(painel, JSpinner.class)) assertFalse(spinner.isEnabled());
				} finally { api.remove(painel); }
			}
		});
	}
}
