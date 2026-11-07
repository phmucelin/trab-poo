package view;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import javax.swing.JPanel;

/** Superfície Java2D para uma imagem de jogo; nenhum componente contém ImageIcon. */
public final class PainelImagem extends JPanel {
	private final String id;
	public PainelImagem(String id, int largura, int altura) { this.id = id; setPreferredSize(new Dimension(largura, altura)); }
	@Override protected void paintComponent(Graphics graphics) {
		super.paintComponent(graphics);
		Graphics2D g = (Graphics2D) graphics.create();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		CatalogoImagens.getInstance().desenhar(g, id, new Rectangle(8, 8, Math.max(1, getWidth() - 16), Math.max(1, getHeight() - 16)));
		g.dispose();
	}
}
