package controller;

import javax.swing.SwingUtilities;

/** Ponto de entrada do jogo: abre a janela de configuração da partida. */
public class Main {

	private static class Abrir implements Runnable {
		@Override
		public void run() {
			Controlador.getInstance().abrirJanelaInicial();
		}
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Abrir());
	}
}
