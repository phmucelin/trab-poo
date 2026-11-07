package view;

import java.awt.Component;
import java.awt.Dimension;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import controller.Controlador;
import model.ModelAPI;
import model.ObservadoIF;
import model.ObservadorIF;

/** Resultado calculado pelo Model, incluindo os critérios reais de desempate. */
public class PainelResultado extends JPanel implements ObservadorIF {
	public PainelResultado() {
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBorder(BorderFactory.createEmptyBorder(12, 10, 12, 10));
		Controlador.getInstance().registra(this);
		atualizar();
	}
	@Override public Dimension getPreferredSize() { return new Dimension(PainelTurno.LARGURA - 20, super.getPreferredSize().height); }
	@Override public void notify(ObservadoIF observado) { atualizar(); }
	private void atualizar() {
		removeAll();
		ModelAPI m = ModelAPI.getInstance();
		if (!m.isPartidaEncerrada()) adicionar("O resultado será apresentado ao encerrar a partida.");
		else if (!m.isPartidaCompleta()) adicionar("Demonstração encerrada. O resultado completo exige o cadastro das peças.");
		else {
			List<Integer> vencedores = m.getVencedores();
			StringBuilder nomes = new StringBuilder();
			for (int jogador : vencedores) {
				if (nomes.length() > 0) nomes.append(", ");
				nomes.append(PainelPreparacao.escapar(m.getNomeJogador(jogador)));
			}
			adicionar("<b>" + (vencedores.size() > 1 ? "Vitória compartilhada: " : "Vencedor: ") + nomes + "</b>");
			adicionar("Partida encerrada na rodada " + m.getRodada() + ".");
			for (int jogador = 0; jogador < m.getNumJogadores(); jogador++) {
				int total = m.getVotosJogador(jogador), influencia = m.getVotosInfluenciaFinal(jogador), secretos = m.getVotosSecretosFinal(jogador);
				adicionar("<b>" + PainelPreparacao.escapar(m.getNomeJogador(jogador)) + ": " + total + " votos</b>"
						+ "<br>Durante o jogo: " + (total - influencia - secretos) + "<br>Influência regional: " + influencia
						+ "<br>Carta secreta: " + secretos + "<br>Tesouros não entregues: " + m.getTotalTesourosRestantes(jogador)
						+ "<br>Distância de Karakorum: " + m.getDistanciaCapital(jogador));
				adicionar(PainelPreparacao.escapar(m.getCartaSecretaFinal(jogador)));
			}
			adicionar("Desempates: mais votos, depois mais tesouros físicos não entregues e depois menor distância de Karakorum. Persistindo o empate, a vitória é compartilhada.");
		}
		revalidate(); repaint();
	}
	private void adicionar(String texto) {
		JLabel label = new JLabel("<html><div style='width:145px'>" + texto + "</div></html>");
		label.setAlignmentX(Component.LEFT_ALIGNMENT);
		add(label); add(Box.createVerticalStrut(12));
	}
}
