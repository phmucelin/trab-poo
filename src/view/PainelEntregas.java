package view;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import controller.Controlador;
import model.ModelAPI;
import model.ObservadoIF;
import model.ObservadorIF;

/** Entregas com seleção explícita das peças e resolução dos bônus de uso único. */
public class PainelEntregas extends JPanel implements ObservadorIF {
	private final JComboBox<String> conselheiros = new JComboBox<>();
	private final JLabel pedido = new JLabel();
	private final Map<String, JSpinner> quantidades = new LinkedHashMap<>();
	private final Map<String, JLabel> saldos = new LinkedHashMap<>();
	private final JButton entregar = new JButton("Entregar tesouros");
	private final JComboBox<String> bonus = new JComboBox<>();
	private final JLabel descricaoBonus = new JLabel();
	private final JButton usar = new JButton("Usar bônus");
	private final JPanel recompensa = new JPanel();
	private final JComboBox<String> opcoes = new JComboBox<>();
	private final JButton escolher = new JButton("Confirmar tesouro");
	private final JLabel aviso = new JLabel();
	private boolean atualizando;

	public PainelEntregas() {
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBorder(BorderFactory.createEmptyBorder(12, 10, 12, 10));
		conselheiros.setName("conselheiro");
		bonus.setName("bonus");
		opcoes.setName("tesouroDoBonus");
		adicionar(new JLabel("<html><b>Conselheiros</b></html>"));
		adicionar(aviso);
		adicionar(conselheiros);
		adicionar(pedido);
		adicionar(new JLabel("<html><b>Tesouros a entregar</b></html>"));
		for (String tipo : ModelAPI.getInstance().getTiposTesouro()) {
			JSpinner quantidade = new JSpinner(new SpinnerNumberModel(0, 0, 3, 1));
			quantidade.setName("oferta_" + tipo);
			((JSpinner.NumberEditor) quantidade.getEditor()).getTextField().setColumns(2);
			quantidade.addChangeListener(new AlterarQuantidade());
			quantidades.put(tipo, quantidade);
			JLabel saldo = new JLabel();
			saldos.put(tipo, saldo);
			JPanel linha = new JPanel(new BorderLayout(4, 0));
			linha.add(saldo, BorderLayout.CENTER);
			linha.add(quantidade, BorderLayout.LINE_END);
			linha.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
			adicionar(linha);
		}
		adicionar(entregar);
		add(Box.createVerticalStrut(12));
		adicionar(new JLabel("<html><b>Bônus do jogador da vez</b></html>"));
		adicionar(bonus);
		adicionar(descricaoBonus);
		adicionar(usar);
		recompensa.setLayout(new BoxLayout(recompensa, BoxLayout.Y_AXIS));
		recompensa.add(new JLabel(html("<b>Escolha obrigatória:</b> guarde um dos tesouros sorteados. Os restantes serão descartados.")));
		opcoes.setAlignmentX(Component.LEFT_ALIGNMENT);
		escolher.setAlignmentX(Component.LEFT_ALIGNMENT);
		recompensa.add(opcoes);
		recompensa.add(escolher);
		adicionar(recompensa);
		conselheiros.addActionListener(new Selecionar());
		bonus.addActionListener(new Selecionar());
		opcoes.addActionListener(new Selecionar());
		entregar.addActionListener(new Entregar());
		usar.addActionListener(new UsarBonus());
		escolher.addActionListener(new EscolherTesouro());
		Controlador.getInstance().registra(this);
		atualizar();
	}
	@Override public Dimension getPreferredSize() {
		return new Dimension(PainelTurno.LARGURA - 20, super.getPreferredSize().height);
	}
	private class Selecionar implements ActionListener {
		@Override public void actionPerformed(ActionEvent evento) { if (!atualizando) atualizarSelecao(); }
	}
	private class AlterarQuantidade implements ChangeListener {
		@Override public void stateChanged(ChangeEvent evento) { if (!atualizando) atualizarSelecao(); }
	}
	private class Entregar implements ActionListener {
		@Override public void actionPerformed(ActionEvent evento) {
			Controlador.getInstance().entregarTesouros(selecionado(conselheiros), oferta());
		}
	}
	private class UsarBonus implements ActionListener {
		@Override public void actionPerformed(ActionEvent evento) { Controlador.getInstance().usarBonus(selecionado(bonus)); }
	}
	private class EscolherTesouro implements ActionListener {
		@Override public void actionPerformed(ActionEvent evento) { Controlador.getInstance().escolherTesouroDoBonus(selecionado(opcoes)); }
	}
	@Override public void notify(ObservadoIF observado) { atualizar(); }

	private void atualizar() {
		ModelAPI m = ModelAPI.getInstance();
		atualizando = true;
		try {
			preencher(conselheiros, m.getConselheiros());
			preencher(bonus, m.temPartida() ? m.getBonusJogador(m.getJogadorDaVez()) : List.of());
			List<String> tipos = new ArrayList<>();
			for (String tipo : m.getOpcoesTesouroDoBonus()) if (!tipos.contains(tipo)) tipos.add(tipo);
			preencher(opcoes, tipos);
			for (Map.Entry<String, JSpinner> item : quantidades.entrySet()) {
				item.getValue().setValue(0);
				int fisicos = m.temPartida() ? m.getTesourosJogador(m.getJogadorDaVez(), item.getKey()) : 0;
				saldos.get(item.getKey()).setText("<html>" + item.getKey() + "<br><small>" + fisicos + " físico(s) + "
						+ m.getTesourosVirtuais(item.getKey()) + " virtual(is)</small></html>");
			}
		} finally { atualizando = false; }
		aviso.setText(html(!m.isPartidaCompleta() ? "Entregas e bônus exigem o cadastro das peças." : m.isPreparacao()
				? "Conclua a preparação para realizar entregas." : m.isPartidaEncerrada() ? "Partida encerrada."
				: m.isTesouroDoBonusPendente() ? "<b>Escolha obrigatória:</b> confirme o tesouro do bônus abaixo antes de terminar o turno."
				: "Entregue em uma parada visitada pelo peão ativo neste turno. Tesouros virtuais são consumidos primeiro."));
		recompensa.setVisible(m.isTesouroDoBonusPendente());
		atualizarSelecao();
		revalidate();
		repaint();
	}
	private void atualizarSelecao() {
		ModelAPI m = ModelAPI.getInstance();
		String local = selecionado(conselheiros);
		StringBuilder texto = new StringBuilder();
		if (local != null) {
			Map<String, Integer> solicitado = m.getPedidoConselheiro(local);
			texto.append("<b>Pedido:</b> ");
			if (solicitado.isEmpty()) texto.append("3 tesouros quaisquer");
			else for (Map.Entry<String, Integer> item : solicitado.entrySet()) texto.append(item.getValue()).append(" ").append(item.getKey()).append("; ");
			texto.append("<br>Entrega: ").append(m.getVotosEntrega(local)).append(" voto(s).");
			String premio = m.getBonusConselheiro(local);
			texto.append("<br>").append(premio == null ? "Bônus já retirado." : "Primeira entrega: " + m.getDescricaoBonus(premio) + ".");
			texto.append("<br>").append(m.isConselheiroVisitado(local) ? "Visitado neste turno." : "Ainda não visitado neste turno.");
		}
		pedido.setText(html(texto.toString()));
		entregar.setEnabled(m.podeEntregarTesouros(local, oferta()));
		String tipoBonus = selecionado(bonus);
		boolean usado = m.temPartida() && m.isBonusUsado(m.getJogadorDaVez(), tipoBonus);
		descricaoBonus.setText(html(tipoBonus == null ? "Nenhum bônus obtido." : m.getDescricaoBonus(tipoBonus)
				+ (usado ? " — já utilizado." : " — ainda não utilizado.")));
		usar.setEnabled(m.podeUsarBonus(tipoBonus));
		escolher.setEnabled(m.getOpcoesTesouroDoBonus().contains(selecionado(opcoes)));
		boolean ativa = m.isPartidaCompleta() && !m.isPreparacao() && !m.isPartidaEncerrada();
		for (JSpinner quantidade : quantidades.values()) quantidade.setEnabled(ativa);
	}
	private Map<String, Integer> oferta() {
		Map<String, Integer> oferta = new LinkedHashMap<>();
		for (Map.Entry<String, JSpinner> item : quantidades.entrySet()) oferta.put(item.getKey(), (Integer) item.getValue().getValue());
		return oferta;
	}
	private static String selecionado(JComboBox<String> combo) { return (String) combo.getSelectedItem(); }
	private static void preencher(JComboBox<String> combo, List<String> valores) {
		String anterior = selecionado(combo);
		combo.removeAllItems();
		for (String valor : valores) combo.addItem(valor);
		if (valores.contains(anterior)) combo.setSelectedItem(anterior);
	}
	private static String html(String texto) { return "<html><div style='width:145px'>" + texto + "</div></html>"; }
	private void adicionar(JComponent componente) {
		componente.setAlignmentX(Component.LEFT_ALIGNMENT);
		if (componente instanceof JButton || componente instanceof JComboBox) componente.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
		add(componente);
		add(Box.createVerticalStrut(8));
	}
}
