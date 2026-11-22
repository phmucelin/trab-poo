package view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import controller.Controlador;
import model.ModelAPI;

/**
 * Primeira janela (enunciado da 2ª iteração, "Janelas Iniciais"): os
 * jogadores escolhem entre começar uma nova partida, que abre a janela de
 * escolha dos personagens, ou continuar uma partida salva num arquivo,
 * escolhido num JFileChooser (4ª iteração).
 */
public class JanelaAbertura extends JFrame {

	private final JLabel lbAviso = new JLabel(" ");

	/** Fecha esta janela e abre a escolha dos personagens. */
	private class AcaoNovaPartida implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			dispose();
			Controlador.getInstance().abrirJanelaInicial();
		}
	}

	/** Escolhe o arquivo .txt e pede ao Controlador para recuperar a partida. */
	private class AcaoContinuar implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			JFileChooser seletor = SeletorArquivo.criar("Continuar partida salva");
			if (seletor.showOpenDialog(JanelaAbertura.this) != JFileChooser.APPROVE_OPTION) {
				return;
			}
			File arquivo = seletor.getSelectedFile();
			SeletorArquivo.lembrar(arquivo);
			carregar(arquivo);
		}
	}

	public JanelaAbertura() {
		super("Herdeiros de Khan");
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setLayout(new BorderLayout(8, 8));

		JLabel titulo = new JLabel("Herdeiros de Khan", SwingConstants.CENTER);
		titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 20f));
		titulo.setBorder(BorderFactory.createEmptyBorder(16, 24, 0, 24));
		add(titulo, BorderLayout.NORTH);

		JPanel botoes = new JPanel(new GridLayout(2, 1, 8, 8));
		botoes.setBorder(BorderFactory.createEmptyBorder(8, 48, 8, 48));
		JButton btNova = new JButton("Nova partida");
		btNova.addActionListener(new AcaoNovaPartida());
		JButton btContinuar = new JButton("Continuar partida salva...");
		btContinuar.addActionListener(new AcaoContinuar());
		botoes.add(btNova);
		botoes.add(btContinuar);
		add(botoes, BorderLayout.CENTER);

		lbAviso.setForeground(new Color(180, 0, 0));
		lbAviso.setBorder(BorderFactory.createEmptyBorder(0, 16, 12, 16));
		add(lbAviso, BorderLayout.SOUTH);

		pack();
		// Sem redimensionar, a janela nunca passa de 1400 x 1050 (enunciado).
		setResizable(false);
		setLocationRelativeTo(null);
	}

	/** Público para os testes de interface, que não abrem o JFileChooser. */
	public boolean carregar(File arquivo) {
		if (Controlador.getInstance().carregarPartida(arquivo)) {
			dispose();
			return true;
		}
		String erro = ModelAPI.getInstance().getErroArquivo();
		lbAviso.setText("<html><div style='width:300px'>" + PainelPreparacao.escapar(erro) + "</div></html>");
		pack();
		return false;
	}
}
