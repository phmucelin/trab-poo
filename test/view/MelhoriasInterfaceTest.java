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
import model.ModelAPI;
import model.MelhoriasModelAPITest;
import model.PreparacaoCompletaTest;
import org.junit.Test;

/** Confirma compras e escolhas obrigatórias pelo Controller, na EDT. */
public class MelhoriasInterfaceTest {
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
	@Test public void compraAtualizaMercadoECartelaSemReaproveitarSelecao() throws Exception {
		SwingUtilities.invokeAndWait(new Runnable() {
			@Override public void run() {
				ModelAPI api = ModelAPI.getInstance();
				assertTrue(PreparacaoCompletaTest.criarSintetica(List.of("Ana", "Bia"), List.of("OGEDEI", "ALTANI")));
				assertTrue(api.escolherCartaSecreta(0));
				assertTrue(api.escolherCartaSecreta(0));
				PainelMelhorias painel = new PainelMelhorias();
				try {
					assertFalse(botao(painel, "Comprar").isEnabled());
					assertTrue(api.ativarColuna("VERDE"));
					List<JRadioButton> opcoes = componentes(painel, JRadioButton.class);
					assertEquals(6, opcoes.size()); // cinco abertas e a pilha fechada.
					assertTrue(opcoes.get(5).getText().contains("às cegas"));
					String coluna = api.getMercadoMelhorias().get(0).split(":")[0];
					opcoes.get(0).doClick();
					assertTrue(botao(painel, "Comprar").isEnabled());
					botao(painel, "Comprar").doClick();
					assertEquals(4, api.getMercadoMelhorias().size());
					assertEquals(1, api.getMelhoriasJogador(0, coluna).size());
					assertFalse(botao(painel, "Comprar").isEnabled());
					assertTrue(api.getMelhoriasJogador(0, coluna).get(0).contains("próximo turno"));
					assertTrue(botao(painel, "Abrir cartela").isEnabled());
				} finally { api.remove(painel); }
			}
		});
	}
	@Test public void escolhaGratuitaDesbloqueiaTurnoPeloBotao() throws Exception {
		SwingUtilities.invokeAndWait(new Runnable() {
			@Override public void run() {
				ModelAPI api = ModelAPI.getInstance();
				MelhoriasModelAPITest.prepararRecompensaPendente();
				PainelMelhorias painel = new PainelMelhorias();
				try {
					assertTrue(botao(painel, "Escolher grátis").isVisible());
					assertFalse(botao(painel, "Escolher grátis").isEnabled());
					componentes(painel, JRadioButton.class).get(0).doClick();
					assertFalse(botao(painel, "Comprar").isEnabled());
					assertTrue(botao(painel, "Escolher grátis").isEnabled());
					int moedas = api.getTributosJogador(0, "MOEDA");
					botao(painel, "Escolher grátis").doClick();
					assertFalse(api.isMelhoriaGratuitaPendente());
					assertFalse(botao(painel, "Escolher grátis").isVisible());
					assertEquals(1, api.getMelhoriasJogador(0, "BRANCA").size());
					assertEquals(moedas, api.getTributosJogador(0, "MOEDA"));
					if (api.isKhanPendente()) assertTrue(api.moverKhan(api.getDestinosDoKhan().get(0)));
					assertTrue(api.podeTerminarTurno());
				} finally { api.remove(painel); }
			}
		});
	}
	@Test public void jochiCompraNaColunaSelecionadaPeloControle() throws Exception {
		SwingUtilities.invokeAndWait(new Runnable() {
			@Override public void run() {
				ModelAPI api = ModelAPI.getInstance();
				assertTrue(PreparacaoCompletaTest.iniciarPadrao(List.of("Ana", "Bia"), List.of("JOCHI", "ALTANI")));
				assertTrue(api.ativarColuna("VERMELHA"));
				PainelMelhorias painel = new PainelMelhorias();
				try {
					String cor = api.getMercadoMelhorias().get(0).split(":")[0];
					String destino = cor.equals("VERDE") ? "VERMELHA" : "VERDE";
					String id = api.getIdsMercadoMelhorias().get(0);
					for (javax.swing.JComboBox<?> combo : componentes(painel, javax.swing.JComboBox.class)) {
						if ("colunaCompraJochi".equals(combo.getName())) combo.setSelectedItem(destino);
					}
					componentes(painel, JRadioButton.class).get(0).doClick(0);
					assertTrue(botao(painel, "Comprar").isEnabled());
					botao(painel, "Comprar").doClick(0);
					assertEquals(List.of(id), api.getIdsMelhoriasJogador(0, destino));
					assertTrue(api.getIdsMelhoriasJogador(0, cor).isEmpty());
				} finally { api.remove(painel); }
			}
		});
	}

}
