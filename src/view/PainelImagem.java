package view;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import javax.swing.JPanel;

/**
 * Painel que só desenha uma imagem do catálogo com drawImage(). Usado nas
 * escolhas da preparação e no mercado, no lugar de um JLabel com ícone.
 */
public final class PainelImagem extends JPanel {
	private final String id;

	public PainelImagem(String id, int largura, int altura) {
		this.id = id;
		setPreferredSize(new Dimension(largura, altura));
	}

	@Override
	protected void paintComponent(Graphics graphics) {
		super.paintComponent(graphics);
		Graphics2D g = (Graphics2D) graphics.create();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		Rectangle area = new Rectangle(8, 8, Math.max(1, getWidth() - 16), Math.max(1, getHeight() - 16));
		CatalogoImagens.getInstance().desenhar(g, id, area);
		g.dispose();
	}
}
