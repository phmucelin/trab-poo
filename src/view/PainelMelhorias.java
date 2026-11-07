package view;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import controller.Controlador;
import model.ModelAPI;
import model.ObservadoIF;
import model.ObservadorIF;

/** Mercado e cartelas públicas. A pilha fechada não revela sua próxima peça. */
public class PainelMelhorias extends JPanel implements ObservadorIF {
	private final JLabel aviso = new JLabel();
	private final JPanel ofertas = new JPanel();
	private final ButtonGroup selecao = new ButtonGroup();
	private final JButton comprar = new JButton("Comprar por 2 moedas");
	private final JButton gratuita = new JButton("Escolher grátis para branca");
	private final JComboBox<String> destino = new JComboBox<>(new String[] { "VERMELHA", "VERDE", "AMARELA", "BRANCA" });
	private final JComboBox<String> jogadores = new JComboBox<>();
	private final JButton abrirCartela = new JButton("Abrir cartela com imagens");
	private boolean atualizando;

	public PainelMelhorias() {
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBorder(BorderFactory.createEmptyBorder(12, 10, 12, 10));
		ofertas.setLayout(new BoxLayout(ofertas, BoxLayout.Y_AXIS));
		adicionar(new JLabel("<html><b>Mercado de melhorias</b></html>"));
		adicionar(aviso);
		adicionar(ofertas);
		destino.setName("colunaCompraJochi");
		destino.addActionListener(new SelecionarOferta());
		adicionar(destino);
		comprar.addActionListener(new Comprar());
		gratuita.addActionListener(new EscolherGratuita());
		adicionar(comprar);
		adicionar(gratuita);
		adicionar(texto("A compra segue a cor da peça; Jochi escolhe a coluna acima. A recompensa vai para a branca. Ambas só produzem efeitos em turnos seguintes."));
		add(Box.createVerticalStrut(12));
		adicionar(new JLabel("<html><b>Cartela de melhorias</b></html>"));
		jogadores.setName("jogadorCartela");
		jogadores.addActionListener(new SelecionarJogador());
		adicionar(jogadores);
		abrirCartela.addActionListener(new AbrirCartela());
		adicionar(abrirCartela);
		Controlador.getInstance().registra(this);
		atualizar();
	}

	@Override public Dimension getPreferredSize() {
		Dimension tamanho = super.getPreferredSize();
		return new Dimension(PainelTurno.LARGURA - 20, tamanho.height);
	}

	private class Comprar implements ActionListener {
		@Override public void actionPerformed(ActionEvent evento) {
			if (selecao.getSelection() != null) {
				if (destino.isVisible()) Controlador.getInstance().comprarMelhoria(indice(), (String) destino.getSelectedItem());
				else Controlador.getInstance().comprarMelhoria(indice());
			}
		}
	}
	private class EscolherGratuita implements ActionListener {
		@Override public void actionPerformed(ActionEvent evento) {
			if (selecao.getSelection() != null) Controlador.getInstance().escolherMelhoriaGratuita(indice());
		}
	}
	private class SelecionarOferta implements ActionListener {
		@Override public void actionPerformed(ActionEvent evento) { atualizarBotoes(); }
	}
	private class SelecionarJogador implements ActionListener {
		@Override public void actionPerformed(ActionEvent evento) {
			if (!atualizando) Controlador.getInstance().selecionarCartela(jogadores.getSelectedIndex());
		}
	}

	@Override public void notify(ObservadoIF observado) { atualizar(); }

	private void atualizar() {
		ModelAPI m = ModelAPI.getInstance();
		atualizando = true;
		try {
			int jogador = m.getJogadorCartela();
			jogadores.removeAllItems();
			for (int i = 0; i < m.getNumJogadores(); i++) jogadores.addItem((i + 1) + ". " + m.getNomeJogador(i));
			if (m.temPartida()) jogadores.setSelectedIndex(jogador >= 0 && jogador < m.getNumJogadores() ? jogador : m.getJogadorDaVez());
		} finally { atualizando = false; }
		ofertas.removeAll();
		while (selecao.getButtonCount() > 0) selecao.remove(selecao.getElements().nextElement());
		if (!m.isPartidaCompleta()) aviso.setText(textoHtml("O mercado exige uma partida com cadastro das peças."));
		else if (m.isPreparacao()) aviso.setText(textoHtml("Conclua a escolha das cartas secretas para comprar."));
		else if (m.isPartidaEncerrada()) aviso.setText("Partida encerrada.");
		else if (m.isMelhoriaGratuitaPendente()) aviso.setText(textoHtml("<b>Escolha obrigatória:</b> pegue uma melhoria gratuita para a branca antes de terminar o turno."));
		else aviso.setText(textoHtml("Compra: 2 moedas físicas ou virtuais. O Khan repõe o mercado ao ir para a área de melhorias no mapa."));
		List<String> mercado = m.getMercadoMelhorias();
		for (int i = 0; i < mercado.size(); i++) {
			adicionarOferta(i, "Posição " + (i + 1) + (i == 0 ? " (direita)" : "") + "<br>"
					+ PainelPreparacao.escapar(mercado.get(i)));
		}
		if (m.getMelhoriasNaReserva() > 0) adicionarOferta(-1, "Pilha fechada: compra às cegas<br>"
				+ m.getMelhoriasNaReserva() + " peça(s) na reserva");
		else if (m.isPartidaCompleta()) ofertas.add(texto("Pilha fechada esgotada."));
		if (mercado.isEmpty() && m.isPartidaCompleta()) ofertas.add(texto("Mercado vazio."));
		atualizarBotoes();
		abrirCartela.setEnabled(m.temPartida() && !m.isPreparacao() && !m.isPartidaEncerrada());
		revalidate();
		repaint();
	}

	private void adicionarOferta(int indice, String descricao) {
		JRadioButton opcao = new JRadioButton(textoHtml(descricao));
		opcao.setActionCommand(Integer.toString(indice));
		opcao.setAlignmentX(Component.LEFT_ALIGNMENT);
		opcao.addActionListener(new SelecionarOferta());
		selecao.add(opcao);
		if (indice >= 0) {
			PainelImagem imagem = new PainelImagem(ModelAPI.getInstance().getIdsMercadoMelhorias().get(indice), 190, 90);
			imagem.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
			imagem.setAlignmentX(Component.LEFT_ALIGNMENT);
			ofertas.add(imagem);
		}
		ofertas.add(opcao);
		ofertas.add(Box.createVerticalStrut(8));
	}
	private int indice() { return Integer.parseInt(selecao.getSelection().getActionCommand()); }
	private void atualizarBotoes() {
		ModelAPI m = ModelAPI.getInstance();
		destino.setVisible(m.temPartida() && m.getHerdeiroJogador(m.getJogadorDaVez()).equals("JOCHI"));
		boolean selecionada = selecao.getSelection() != null;
		comprar.setEnabled(selecionada && !m.isMelhoriaGratuitaPendente() && (destino.isVisible() ? m.podeComprarMelhoria(indice(), (String) destino.getSelectedItem()) : m.podeComprarMelhoria(indice())));
		gratuita.setVisible(m.isMelhoriaGratuitaPendente());
		gratuita.setEnabled(selecionada && m.podeEscolherMelhoriaGratuita(indice()));
	}
	private class AbrirCartela implements ActionListener {
		@Override public void actionPerformed(ActionEvent evento) { Controlador.getInstance().abrirJanelaCartela(); }
	}

	private static String textoHtml(String conteudo) { return "<html><div style='width:145px'>" + conteudo + "</div></html>"; }
	private static JLabel texto(String conteudo) { return new JLabel(textoHtml(conteudo)); }
	private void adicionar(JComponent componente) {
		componente.setAlignmentX(Component.LEFT_ALIGNMENT);
		if (componente instanceof JButton || componente instanceof JComboBox) componente.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
		add(componente);
		add(Box.createVerticalStrut(8));
	}
}
