package view;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.List;
import javax.swing.JPanel;
import model.ModelAPI;

/** Desenha a carta secreta e até quatro cartas abertas com imagens oficiais. */
public final class PainelCartas extends JPanel {
	public PainelCartas() { setPreferredSize(new Dimension(1300, 720)); setBackground(new Color(243, 239, 229)); }
	@Override protected void paintComponent(Graphics graphics) {
		super.paintComponent(graphics);
		Graphics2D g = (Graphics2D) graphics.create();
		ModelAPI m = ModelAPI.getInstance();
		int largura = getWidth() / 5;
		String secreta = m.getIdCartaSecretaAtual();
		carta(g, 0, largura, secreta, "Carta secreta", secreta == null ? "Oculta. Apenas o jogador da vez deve revelar esta carta." : m.getCartaSecretaAtual());
		List<String> ids = m.getIdsCartasAbertas(), descricoes = m.getObjetivosAbertosComProgresso();
		if (descricoes.size() != ids.size()) descricoes = m.getCartasAbertas();
		for (int i = 0; i < 4; i++) carta(g, i + 1, largura, i < ids.size() ? ids.get(i) : null,
				"Objetivo aberto " + (i + 1), i < ids.size() ? descricoes.get(i) : "Objetivo já cumprido; sem reposição.");
		g.dispose();
	}
	private void carta(Graphics2D g, int indice, int largura, String id, String titulo, String descricao) {
		int x = indice * largura + 12;
		g.setColor(Color.DARK_GRAY); g.setFont(getFont().deriveFont(Font.BOLD, 15f)); g.drawString(titulo, x, 28);
		if (id != null) CatalogoImagens.getInstance().desenhar(g, id, new Rectangle(x, 42, largura - 24, 420));
		else { g.setColor(new Color(219, 211, 195)); g.fillRoundRect(x, 42, largura - 24, 400, 16, 16); }
		g.setColor(Color.DARK_GRAY); g.setFont(getFont().deriveFont(13f));
		int y = 484; String linha = "";
		for (String palavra : descricao.split(" ")) {
			if (g.getFontMetrics().stringWidth(linha + palavra) > largura - 24) { g.drawString(linha, x, y); y += 18; linha = ""; }
			linha += palavra + " ";
		}
		g.drawString(linha, x, y);
	}
}
