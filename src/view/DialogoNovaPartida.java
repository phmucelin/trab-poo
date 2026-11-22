package view;

import java.awt.Component;

import javax.swing.JOptionPane;

import controller.ConfirmadorNovaPartida;

/** Diálogo Sim/Não exibido logo depois que o vencedor é apontado. */
public final class DialogoNovaPartida implements ConfirmadorNovaPartida {
	@Override
	public boolean confirmar(Component pai, String vencedores) {
		int resposta = JOptionPane.showConfirmDialog(pai,
				vencedores + "\n\nDeseja iniciar uma nova partida?",
				"Fim da partida", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
		return resposta == JOptionPane.YES_OPTION;
	}
}
