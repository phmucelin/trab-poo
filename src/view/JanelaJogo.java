package view;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JFrame;

/**
 * Janela da partida (2ª iteração, Parte 3): tabuleiro no centro e painel do
 * turno ao lado. Limitada a 1400 x 1050, como pede o enunciado.
 */
public class JanelaJogo extends JFrame {

	public static final int LARGURA_MAX = 1400;
	public static final int ALTURA_MAX = 1050;

	public JanelaJogo() {
		super("Herdeiros de Khan");
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setLayout(new BorderLayout());
		add(new PainelTabuleiro(), BorderLayout.CENTER);
		add(new PainelTurno(), BorderLayout.LINE_END);
		pack();
		setSize(Math.min(getWidth(), LARGURA_MAX), Math.min(getHeight(), ALTURA_MAX));
		setMaximumSize(new Dimension(LARGURA_MAX, ALTURA_MAX));
		setLocationRelativeTo(null);
	}
}
