package view;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import controller.Controlador;
import model.ModelAPI;
import model.ObservadoIF;
import model.ObservadorIF;

/**
 * Painel lateral do turno (2ª iteração, Parte 1).
 *
 * Mostra de quem é a vez e oferece as duas ações do turno que não são
 * deslocamentos: ativar uma coluna da cartela e terminar o turno. Tudo o que
 * se desloca no mapa (o peão, o Khan e a escolha do peão de Altani) é feito
 * clicando no PainelTabuleiro; aqui aparecem só a situação e a dica do passo.
 *
 * Segue o padrão Observer do cap. 17: registra-se pelo Controlador e, a cada
 * notificação, consulta a ModelAPI para habilitar só as ações permitidas.
 * Os eventos dos botões são tratados por classes internas que implementam
 * ActionListener (cap. 12); o enunciado não permite expressões lambda aqui.
 */
public class PainelTurno extends JPanel implements ObservadorIF {

	public static final int LARGURA = 260;

	/** As colunas na ordem da cartela, com os nomes do contrato da ModelAPI. */
	private static final String[] COLUNAS = { "VERMELHA", "VERDE", "AMARELA", "BRANCA" };

	private final JLabel lbJogador = new JLabel(" ");
	private final JLabel lbHerdeiro = new JLabel(" ");
	private final JLabel lbRodada = new JLabel(" ");
	private final JLabel lbKurultai = new JLabel(" ");
	private final JLabel lbPlacar = new JLabel(" ");
	private final Map<String, JButton> btColunas = new LinkedHashMap<String, JButton>();
	private final JLabel lbCavalos = new JLabel(" ");
	private final JLabel lbPeao = new JLabel(" ");
	private final JLabel lbKhan = new JLabel(" ");
	private final JButton btTerminar = new JButton("Terminar turno");
	private final JButton btCartas = new JButton("Ver cartas");
	private final JButton btCartela = new JButton("Ver cartela");
	private final JLabel lbDica = new JLabel(" ");

	/** Ativa a coluna informada no construtor (um tratador por botão). */
	private class AcaoColuna implements ActionListener {
		private final String coluna;

		AcaoColuna(String coluna) {
			this.coluna = coluna;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			Controlador.getInstance().ativarColuna(coluna);
		}
	}

	/** Encerra o turno do jogador da vez. */
	private class AcaoTerminarTurno implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			Controlador.getInstance().terminarTurno();
		}
	}

	private class AbrirCartas implements ActionListener { @Override public void actionPerformed(ActionEvent e) { Controlador.getInstance().abrirJanelaCartas(); } }
	private class AbrirCartela implements ActionListener { @Override public void actionPerformed(ActionEvent e) { Controlador.getInstance().abrirJanelaCartela(); } }

	public PainelTurno() {
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

		lbJogador.setFont(lbJogador.getFont().deriveFont(Font.BOLD, 18f));
		adicionar(lbJogador);
		adicionar(lbHerdeiro);
		adicionar(lbRodada);
		adicionar(lbKurultai);
		adicionar(lbPlacar);
		add(Box.createVerticalStrut(12));

		adicionar(titulo("Ativar coluna"));
		for (String coluna : COLUNAS) {
			JButton bt = new JButton(nomeLegivel(coluna));
			bt.setHorizontalAlignment(JButton.LEFT);
			bt.setBorder(BorderFactory.createCompoundBorder(
					BorderFactory.createMatteBorder(0, 8, 0, 0, CoresJogador.daColuna(coluna)),
					BorderFactory.createEmptyBorder(6, 8, 6, 8)));
			bt.addActionListener(new AcaoColuna(coluna));
			btColunas.put(coluna, bt);
			adicionar(bt);
			add(Box.createVerticalStrut(4));
		}
		add(Box.createVerticalStrut(8));
		adicionar(lbCavalos);
		add(Box.createVerticalStrut(8));
		adicionar(lbPeao);
		add(Box.createVerticalStrut(8));
		adicionar(lbKhan);
		add(Box.createVerticalStrut(12));

		btTerminar.addActionListener(new AcaoTerminarTurno());
		adicionar(btTerminar);
		add(Box.createVerticalStrut(8));
		adicionar(lbDica);
		btCartas.addActionListener(new AbrirCartas());
		btCartela.addActionListener(new AbrirCartela());
		adicionar(btCartas);
		adicionar(btCartela);
		add(Box.createVerticalGlue());

		Controlador.getInstance().registra(this);
		atualizar(ModelAPI.getInstance());
	}

	@Override public Dimension getPreferredSize() { return new Dimension(LARGURA, super.getPreferredSize().height); }

	@Override
	public void notify(ObservadoIF o) {
		atualizar((ModelAPI) o);
	}

	/** Lê o estado atual do Model e habilita somente as ações permitidas. */
	private void atualizar(ModelAPI m) {
		if (!m.temPartida()) {
			lbJogador.setText("Nenhuma partida");
			habilitarTudo(false);
			return;
		}
		int vez = m.getJogadorDaVez();
		lbJogador.setText(m.getNomeJogador(vez));
		lbJogador.setForeground(CoresJogador.doJogador(m.getCorJogador(vez)));
		lbHerdeiro.setText("Herdeiro: " + nomeLegivel(m.getHerdeiroJogador(vez)));
		lbRodada.setText("Rodada " + m.getRodada() + (m.getRodadaFinal() >= 0 ? " (última: " + m.getRodadaFinal() + ")" : ""));
		lbKurultai.setText("Kurultai: " + m.getPosicaoKurultai() + " / " + m.getLimiteKurultai());
		StringBuilder placar = new StringBuilder();
		for (int i = 0; i < m.getNumJogadores(); i++) placar.append(PainelPreparacao.escapar(m.getNomeJogador(i))).append(": ").append(m.getVotosJogador(i)).append(" voto(s)<br>");
		lbPlacar.setText(html(placar.toString(), 150));

		if (m.isPreparacao()) {
			lbJogador.setText("Preparando partida");
			habilitarTudo(false);
			return;
		}

		if (m.isPartidaEncerrada()) {
			lbJogador.setText("Partida encerrada");
			lbJogador.setForeground(Color.DARK_GRAY);
			habilitarTudo(false);
			lbDica.setText(html("Consulte o vencedor e a pontuação na aba Resultado.", 150));
			return;
		}

		List<String> livres = m.getColunasDisponiveis();
		String ativada = m.getColunaAtivada();
		for (Map.Entry<String, JButton> item : btColunas.entrySet()) {
			String nome = item.getKey();
			JButton bt = item.getValue();
			bt.setText(html("<b>" + nomeLegivel(nome) + "</b><br><small>" + m.getDescricaoColuna(nome) + "</small>", 150));
			bt.setEnabled(ativada == null && livres.contains(nome));
		}
		lbCavalos.setText(html(ativada == null ? "Nenhuma coluna ativada"
				: nomeLegivel(ativada) + ": " + m.getCavalosRestantes() + " cavalo(s) restante(s)", 150));

		if (m.getNumPeoes(vez) > 1) {
			String texto = "Peão ativo: " + (m.getPeaoAtivo() + 1);
			if (m.podeEscolherPeao()) {
				texto += ". Para trocar, clique no outro peão no mapa.";
			}
			lbPeao.setText(html(texto, 150));
			lbPeao.setVisible(true);
		} else {
			lbPeao.setVisible(false);
		}

		boolean khan = m.isKhanPendente();
		lbKhan.setText(html(khan ? "<b>Khan:</b> clique num local do Khan no mapa."
				: "Khan: " + localDoKhan(m.getPosicaoKhan()), 150));

		btTerminar.setEnabled(m.podeTerminarTurno());
		lbDica.setText(dica(m, ativada, khan));
		btCartas.setEnabled(true);
		btCartela.setEnabled(true);

		revalidate();
		repaint();
	}

	private static String dica(ModelAPI m, String ativada, boolean khan) {
		if (m.isTesouroDoBonusPendente()) {
			return html("Escolha o tesouro do bônus na aba Entregas antes de terminar o turno.", 150);
		}
		if (m.isMelhoriaGratuitaPendente()) {
			return html("Escolha sua melhoria gratuita na aba Melhorias antes de terminar o turno.", 150);
		}
		if (ativada == null) {
			return html("Escolha uma coluna para ativar.", 150);
		}
		if (m.getCavalosRestantes() > 0 && !m.getDestinosPossiveis().isEmpty()) {
			return html("Clique numa parada destacada no mapa para mover o peão.", 150);
		}
		if (khan) {
			return html("Mova o Khan antes de terminar o turno.", 150);
		}
		return html("Termine o turno quando quiser.", 150);
	}

	private static String localDoKhan(String posicao) {
		if (posicao == null) {
			return "fora do tabuleiro";
		}
		return ModelAPI.AREA_MELHORIAS.equals(posicao) ? "área de melhorias" : posicao;
	}

	private void habilitarTudo(boolean valor) {
		for (JButton bt : btColunas.values()) {
			bt.setEnabled(valor);
		}
		btTerminar.setEnabled(valor);
		btCartas.setEnabled(valor);
		btCartela.setEnabled(valor);
		lbPeao.setVisible(valor);
		lbDica.setText(" ");
	}

	private void adicionar(JComponent c) {
		c.setAlignmentX(Component.LEFT_ALIGNMENT);
		if (c instanceof JButton) {
			c.setMaximumSize(new Dimension(Integer.MAX_VALUE, c.getPreferredSize().height + 40));
		}
		add(c);
	}

	/** Texto HTML com largura fixa, para quebrar linha dentro do painel. */
	private static String html(String texto, int largura) {
		return "<html><div style='width:" + largura + "px'>" + texto + "</div></html>";
	}

	private static JLabel titulo(String texto) {
		JLabel lb = new JLabel(texto);
		lb.setFont(lb.getFont().deriveFont(Font.BOLD));
		return lb;
	}

	/** "VERMELHA" vira "Vermelha"; "ALTANI" vira "Altani". */
	private static String nomeLegivel(String constante) {
		if (constante == null || constante.isEmpty()) {
			return "";
		}
		return constante.charAt(0) + constante.substring(1).toLowerCase();
	}
}
