package view;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

/** Controles de planejamento; a execução do movimento continua no clique no mapa. */
public class PainelRota extends JPanel {
	private final PainelTabuleiro mapa;
	private final JCheckBox planejar = new JCheckBox("Traçar rota no mapa");
	private final JCheckBox saltar = new JCheckBox("Saltar yurt próprio ao passar", true);
	private final JLabel dica = new JLabel("Clique no mapa para mover pelo caminho mais barato.");
	public PainelRota(PainelTabuleiro mapa) {
		this.mapa = mapa;
		setLayout(new BorderLayout());
		JPanel controles = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JButton limpar = new JButton("Limpar rota");
		planejar.addActionListener(new AtivarRota());
		saltar.addActionListener(new AlterarSalto());
		limpar.addActionListener(new Limpar());
		controles.add(planejar); controles.add(saltar); controles.add(limpar);
		add(controles, BorderLayout.NORTH); add(dica, BorderLayout.SOUTH);
		mapa.setAoAlterarRota(new Atualizar());
	}
	private class AtivarRota implements ActionListener {
		@Override public void actionPerformed(ActionEvent evento) { mapa.setModoRota(planejar.isSelected()); }
	}
	private class AlterarSalto implements ActionListener {
		@Override public void actionPerformed(ActionEvent evento) { mapa.setSaltarYurts(saltar.isSelected()); }
	}
	private class Limpar implements ActionListener {
		@Override public void actionPerformed(ActionEvent evento) { mapa.limparRota(); }
	}
	private class Atualizar implements Runnable {
		@Override public void run() {
			if (!planejar.isSelected()) dica.setText("Clique no mapa para mover pelo caminho mais barato.");
			else dica.setText(mapa.getRotaPlanejada().isEmpty() ? "Clique em cada parada da rota; a opção de salto vale para a próxima parada clicada."
					: mapa.isRotaValida() ? "Rota válida: clique novamente na última parada para mover o peão."
					: "Rota ainda inválida: continue até um destino permitido ou limpe a rota.");
		}
	}
}
