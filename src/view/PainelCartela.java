package view;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.swing.JPanel;
import model.ModelAPI;

/** Cartela do herdeiro com as melhorias desenhadas por cima de cada espaço. */
public final class PainelCartela extends JPanel {
	private static final String[] COLUNAS = { "VERMELHA", "VERDE", "AMARELA", "BRANCA" };

	// Posição dos espaços medida na foto da cartela, em fração da largura/altura
	// da imagem. A linha 0 é a de baixo; cada linha sobe 17% da altura.
	private static final double X_PRIMEIRA_COLUNA = 0.257;
	private static final double DISTANCIA_COLUNAS = 0.180;
	private static final double Y_PRIMEIRA_LINHA = 0.595;
	private static final double DISTANCIA_LINHAS = 0.170;
	private static final double LARGURA_ESPACO = 0.157;
	private static final double ALTURA_ESPACO = 0.145;

	public PainelCartela() {
		setPreferredSize(new Dimension(1240, 680));
	}

	@Override
	protected void paintComponent(Graphics graphics) {
		super.paintComponent(graphics);
		ModelAPI m = ModelAPI.getInstance();
		int jogador = m.getJogadorCartela();
		if (jogador < 0) {
			return;
		}
		Graphics2D g = (Graphics2D) graphics.create();
		CatalogoImagens catalogo = CatalogoImagens.getInstance();

		BufferedImage fundo = catalogo.get(m.getHerdeiroJogador(jogador));
		double escala = Math.min((getWidth() - 24.0) / fundo.getWidth(), (getHeight() - 24.0) / fundo.getHeight());
		int w = (int) Math.round(fundo.getWidth() * escala);
		int h = (int) Math.round(fundo.getHeight() * escala);
		int x = (getWidth() - w) / 2;
		int y = (getHeight() - h) / 2;
		g.drawImage(fundo, x, y, w, h, null);

		for (int c = 0; c < COLUNAS.length; c++) {
			List<String> ids = m.getIdsMelhoriasJogador(jogador, COLUNAS[c]);
			for (int linha = 0; linha < ids.size(); linha++) {
				Rectangle espaco = new Rectangle(
						x + (int) Math.round(w * (X_PRIMEIRA_COLUNA + c * DISTANCIA_COLUNAS)),
						y + (int) Math.round(h * (Y_PRIMEIRA_LINHA - linha * DISTANCIA_LINHAS)),
						(int) Math.round(w * LARGURA_ESPACO),
						(int) Math.round(h * ALTURA_ESPACO));
				catalogo.desenhar(g, ids.get(linha), espaco);
			}
		}
		g.dispose();
	}
}
