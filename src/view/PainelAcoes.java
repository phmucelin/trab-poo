package view;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import controller.Controlador;
import model.ModelAPI;
import model.ObservadoIF;
import model.ObservadorIF;

/** Ações sem deslocamento; só o Model decide alcance, custos e legalidade. */
public class PainelAcoes extends JPanel implements ObservadorIF {
	private final JLabel recursos = new JLabel();
	private final JComboBox<String> paradas = new JComboBox<>();
	private final JComboBox<String> provincias = new JComboBox<>();
	private final JLabel tributo = new JLabel();
	private final JComboBox<String> cidades = new JComboBox<>();
	private final JLabel estadoCidade = new JLabel();
	private final JComboBox<String> tesouros = new JComboBox<>();
	private final JButton construir = new JButton("Construir yurt");
	private final JButton coletar = new JButton("Pegar tributo");
	private final JButton atacar = new JButton("Atacar cidade");
	private final JButton moeda = new JButton("Ogedei: pagar 1 moeda");
	private final JButton espada = new JButton("Chagatai: cavalo por espada");
	private boolean atualizando;

	public PainelAcoes() {
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBorder(BorderFactory.createEmptyBorder(12, 10, 12, 10));
		paradas.setName("parada");
		provincias.setName("provincia");
		cidades.setName("cidade");
		tesouros.setName("tesouro");
		adicionar(recursos);
		secao("Construção (1 tributo de yurt)");
		adicionar(paradas);
		adicionar(construir);
		secao("Coleta (1 mão por peça)");
		adicionar(provincias);
		adicionar(tributo);
		adicionar(coletar);
		secao("Cidades e tesouros");
		adicionar(cidades);
		adicionar(estadoCidade);
		adicionar(new JLabel("Tesouro a receber:"));
		adicionar(tesouros);
		adicionar(atacar);
		adicionar(moeda);
		secao("Habilidade de Chagatai");
		adicionar(espada);
		adicionar(new JLabel("<html><div style='width:150px'>O movimento do peão e do Khan continua no mapa.</div></html>"));
		add(Box.createVerticalGlue());
		construir.addActionListener(new Executar("CONSTRUIR"));
		coletar.addActionListener(new Executar("COLETAR"));
		atacar.addActionListener(new Executar("ATACAR"));
		moeda.addActionListener(new Executar("MOEDA"));
		espada.addActionListener(new Executar("ESPADA"));
		paradas.addActionListener(new Selecionar());
		provincias.addActionListener(new Selecionar());
		cidades.addActionListener(new SelecionarCidade());
		tesouros.addActionListener(new Selecionar());
		Controlador.getInstance().registra(this);
		atualizar();
	}

	private class Executar implements ActionListener {
		private final String acao;
		Executar(String acao) { this.acao = acao; }
		@Override public void actionPerformed(ActionEvent evento) {
			Controlador c = Controlador.getInstance();
			switch (acao) {
			case "CONSTRUIR": c.construirYurt(selecionado(paradas)); break;
			case "COLETAR": c.pegarTributo(selecionado(provincias)); break;
			case "ATACAR": c.atacarCidade(selecionado(cidades), tesouroSelecionado()); break;
			case "MOEDA": c.pegarTesouroComMoeda(selecionado(cidades), tesouroSelecionado()); break;
			case "ESPADA": c.trocarCavaloPorEspada(); break;
			default: throw new IllegalStateException("Ação desconhecida");
			}
		}
	}

	private class Selecionar implements ActionListener {
		@Override public void actionPerformed(ActionEvent evento) {
			if (!atualizando) atualizarSelecao();
		}
	}
	private class SelecionarCidade implements ActionListener {
		@Override public void actionPerformed(ActionEvent evento) {
			if (!atualizando) {
				atualizarTesouros();
				atualizarSelecao();
			}
		}
	}

	@Override public Dimension getPreferredSize() { return new Dimension(PainelTurno.LARGURA - 20, super.getPreferredSize().height); }

	@Override public void notify(ObservadoIF observado) { atualizar(); }

	private void atualizar() {
		ModelAPI m = ModelAPI.getInstance();
		atualizando = true;
		try {
			List<String> construcoes = new ArrayList<>(), coletas = new ArrayList<>();
			for (String parada : m.getParadas()) if (m.podeConstruirYurt(parada)) construcoes.add(parada);
			for (String provincia : m.getProvincias()) if (m.podePegarTributo(provincia)) coletas.add(provincia);
			preencher(paradas, construcoes);
			preencher(provincias, coletas);
			preencher(cidades, m.getCidades());
			if (m.temPartida()) {
				int jogador = m.getJogadorDaVez();
				StringBuilder texto = new StringBuilder("<html><div style='width:150px'><b>")
						.append(PainelPreparacao.escapar(m.getNomeJogador(jogador))).append("</b><br>");
				for (String tipo : new String[] { "MOEDA", "ESPADA", "YURT" }) {
					texto.append(tipo).append(": ").append(m.getTributosJogador(jogador, tipo));
					int virtuais = m.getTributosVirtuais(tipo);
					if (virtuais > 0) texto.append(" (+").append(virtuais).append(" virtual)");
					texto.append("<br>");
				}
				texto.append("Mãos restantes: ").append(m.getMaosRestantes())
						.append("<br>Yurts no estoque: ").append(m.getYurtsDisponiveis(jogador))
						.append("<br>Votos: ").append(m.getVotosJogador(jogador));
				if (m.isPartidaCompleta()) {
					texto.append("<br><b>Tesouros físicos</b><br>");
					for (String tipo : new String[] { "FERRO", "PELES", "GADO", "TECIDO", "ESPECIARIAS" }) {
						texto.append(tipo).append(": ").append(m.getTesourosJogador(jogador, tipo)).append(" ");
					}
				} else texto.append("<br>Tesouros recebidos: ").append(m.getTotalTesourosRecebidos(jogador));
				recursos.setText(texto.append("</div></html>").toString());
			} else recursos.setText("Nenhuma partida");
		} finally { atualizando = false; }
		atualizarTesouros();
		atualizarSelecao();
		revalidate();
		repaint();
	}

	private void atualizarTesouros() {
		ModelAPI m = ModelAPI.getInstance();
		atualizando = true;
		try {
			List<String> tipos = new ArrayList<>();
			if (m.isPartidaCompleta()) {
				for (String tipo : m.getTesourosCidade(selecionado(cidades))) if (!tipos.contains(tipo)) tipos.add(tipo);
			} else tipos.add("Automático");
			preencher(tesouros, tipos);
		} finally { atualizando = false; }
	}

	private void atualizarSelecao() {
		ModelAPI m = ModelAPI.getInstance();
		String cidade = selecionado(cidades), provincia = selecionado(provincias);
		construir.setEnabled(m.podeConstruirYurt(selecionado(paradas)));
		coletar.setEnabled(m.podePegarTributo(provincia));
		tributo.setText(provincia == null ? "Sem coleta disponível" : m.getTipoTributoProvincia(provincia)
				+ ": " + m.getPecasProvincia(provincia) + " peça(s)");
		String conquistador = m.getConquistadorCidade(cidade);
		estadoCidade.setText(!m.isCidadeRevelada(cidade) ? "Cidade fechada" : conquistador != null
				? "Conquistada: " + conquistador : "Tesouros restantes: " + m.getTesourosRestantesCidade(cidade));
		boolean escolha = !m.isPartidaCompleta() || tesouros.getSelectedItem() != null;
		atacar.setText("Atacar (" + m.getCustoAtaqueCidade(cidade) + " espada(s))");
		atacar.setEnabled(escolha && m.podeAtacarCidade(cidade, tesouroSelecionado()));
		moeda.setEnabled(escolha && m.podePegarTesouroComMoeda(cidade, tesouroSelecionado()));
		espada.setEnabled(m.podeTrocarCavaloPorEspada());
		paradas.setEnabled(paradas.getItemCount() > 0);
		provincias.setEnabled(provincias.getItemCount() > 0);
		tesouros.setEnabled(m.isPartidaCompleta() && tesouros.getItemCount() > 0);
	}

	private String tesouroSelecionado() {
		return ModelAPI.getInstance().isPartidaCompleta() ? selecionado(tesouros) : null;
	}
	private static String selecionado(JComboBox<String> combo) { return (String) combo.getSelectedItem(); }
	private static void preencher(JComboBox<String> combo, List<String> valores) {
		String anterior = selecionado(combo);
		combo.removeAllItems();
		for (String valor : valores) combo.addItem(valor);
		if (valores.contains(anterior)) combo.setSelectedItem(anterior);
	}
	private void secao(String titulo) {
		add(Box.createVerticalStrut(14));
		adicionar(new JLabel("<html><b>" + titulo + "</b></html>"));
	}
	private void adicionar(JComponent componente) {
		componente.setAlignmentX(Component.LEFT_ALIGNMENT);
		if (componente instanceof JComboBox || componente instanceof JButton) {
			componente.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
		}
		add(componente);
		add(Box.createVerticalStrut(5));
	}
}
