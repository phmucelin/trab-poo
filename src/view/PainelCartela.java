package view;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.swing.JPanel;
import model.ModelAPI;

/** Cartela oficial do herdeiro com as peças sobrepostas aos seus quatro espaços. */
public final class PainelCartela extends JPanel {
	public PainelCartela() { setPreferredSize(new Dimension(1240, 680)); }
	@Override protected void paintComponent(Graphics graphics) {
		super.paintComponent(graphics);
		ModelAPI m = ModelAPI.getInstance(); int jogador = m.getJogadorCartela();
		if (jogador < 0) return;
		Graphics2D g = (Graphics2D) graphics.create();
		CatalogoImagens catalogo = CatalogoImagens.getInstance();
		BufferedImage fundo = catalogo.get(m.getHerdeiroJogador(jogador));
		double escala = Math.min((getWidth() - 24.0) / fundo.getWidth(), (getHeight() - 24.0) / fundo.getHeight());
		int w = (int) Math.round(fundo.getWidth() * escala), h = (int) Math.round(fundo.getHeight() * escala);
		int x = (getWidth() - w) / 2, y = (getHeight() - h) / 2;
		g.drawImage(fundo, x, y, w, h, null);
		String[] colunas = { "VERMELHA", "VERDE", "AMARELA", "BRANCA" };
		for (int c = 0; c < colunas.length; c++) {
			List<String> ids = m.getIdsMelhoriasJogador(jogador, colunas[c]);
			for (int linha = 0; linha < ids.size(); linha++) catalogo.desenhar(g, ids.get(linha), new Rectangle(
					x + (int) Math.round(w * (0.257 + c * 0.180)), y + (int) Math.round(h * (0.595 - linha * 0.170)),
					(int) Math.round(w * 0.157), (int) Math.round(h * 0.145)));
		}
		g.dispose();
	}
}
