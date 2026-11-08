package view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.GridBagLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import controller.Controlador;
import model.ModelAPI;
import model.ObservadoIF;
import model.ObservadorIF;

/** Preparação local com passagem da tela antes de revelar as duas cartas. */
public class PainelPreparacao extends JPanel implements ObservadorIF {
	private final JLabel jogador = new JLabel();
	private final JLabel instrucao = new JLabel();
	private final JButton revelar = new JButton("Estou pronto para ver minhas cartas");
	private final JPanel opcoes = new JPanel(new GridLayout(1, 0, 12, 12));
	private final JButton confirmar = new JButton("Confirmar carta secreta");
	private final ButtonGroup grupo = new ButtonGroup();
	private int exibido = -1;
	private boolean reveladas;

	public PainelPreparacao() {
		setLayout(new GridBagLayout());
		JPanel cartela = new JPanel(new BorderLayout(16, 16));
		cartela.setPreferredSize(new Dimension(900, 650));
		cartela.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(new Color(200, 200, 200)),
				BorderFactory.createEmptyBorder(24, 24, 24, 24)));
		add(cartela);
		setPreferredSize(new Dimension(850, 600));
		JPanel topo = new JPanel(new GridLayout(0, 1, 8, 8));
		topo.add(instrucao);
		jogador.setFont(jogador.getFont().deriveFont(Font.BOLD, 16f));
		topo.add(jogador);
		revelar.addActionListener(new AcaoRevelar());
		topo.add(revelar);
		cartela.add(topo, BorderLayout.NORTH);
		cartela.add(opcoes, BorderLayout.CENTER);
		confirmar.addActionListener(new AcaoConfirmar());
		cartela.add(confirmar, BorderLayout.SOUTH);
		Controlador.getInstance().registra(this);
		atualizar();
	}

	private class AcaoRevelar implements ActionListener {
		@Override public void actionPerformed(ActionEvent e) {
			if (!ModelAPI.getInstance().isPreparacao()) return;
			reveladas = true;
			atualizar();
		}
	}

	private class AcaoSelecionar implements ActionListener {
		@Override public void actionPerformed(ActionEvent e) { confirmar.setEnabled(true); }
	}

	private class AcaoConfirmar implements ActionListener {
		@Override public void actionPerformed(ActionEvent e) {
			if (reveladas && grupo.getSelection() != null) {
				int indice = Integer.parseInt(grupo.getSelection().getActionCommand());
				if (ModelAPI.getInstance().isEscolhaMelhoriaInicialPendente()) Controlador.getInstance().escolherMelhoriaInicial(indice);
				else Controlador.getInstance().escolherCartaSecreta(indice);
			}
		}
	}

	@Override public void notify(ObservadoIF observado) { atualizar(); }

	private void atualizar() {
		ModelAPI m = ModelAPI.getInstance();
		if (!m.isPreparacao()) {
			opcoes.removeAll();
			confirmar.setEnabled(false);
			return;
		}
		int atual = m.getJogadorPreparando();
		if (exibido != atual) {
			exibido = atual;
			reveladas = false;
		}
		jogador.setText("<html>" + (m.isEscolhaMelhoriaInicialPendente() ? "Jochi: escolha uma melhoria para a branca, " : reveladas ? "Carta secreta de " : "Passe a tela para ")
				+ "<b>" + escapar(m.getNomeJogador(atual)) + "</b>.</html>");
		boolean melhoria = m.isEscolhaMelhoriaInicialPendente();
		instrucao.setText(melhoria ? "Escolha uma das duas melhorias para a coluna branca." : "Escolha uma carta secreta e descarte a outra.");
		if (melhoria) reveladas = true;
		revelar.setVisible(!reveladas);
		confirmar.setText(melhoria ? "Confirmar melhoria inicial" : "Confirmar carta secreta");
		opcoes.removeAll();
		while (grupo.getButtonCount() > 0) grupo.remove(grupo.getElements().nextElement());
		confirmar.setVisible(reveladas);
		confirmar.setEnabled(false);
		if (reveladas) {
			List<String> cartas = melhoria ? m.getOpcoesMelhoriaInicial() : m.getOpcoesCartaSecreta();
			List<String> ids = melhoria ? m.getIdsOpcoesMelhoriaInicial() : m.getIdsOpcoesCartaSecreta();
			for (int i = 0; i < cartas.size(); i++) {
				JRadioButton opcao = new JRadioButton("<html><div style='width:260px'>" + escapar(cartas.get(i)) + "</div></html>");
				opcao.setActionCommand(Integer.toString(i));
				opcao.addActionListener(new AcaoSelecionar());
				grupo.add(opcao);
				JPanel escolha = new JPanel(new BorderLayout());
				escolha.add(new PainelImagem(ids.get(i), 300, 390), BorderLayout.CENTER);
				escolha.add(opcao, BorderLayout.SOUTH);
				opcoes.add(escolha);
			}
		}
		revalidate();
		repaint();
	}

	static String escapar(String texto) {
		return texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}
}
