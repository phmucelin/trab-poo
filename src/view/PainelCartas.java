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

/** Desenha a carta secreta e as (até) quatro cartas abertas, lado a lado. */
public final class PainelCartas extends JPanel {
	private static final Color FUNDO = new Color(243, 239, 229);
	private static final Color CARTA_OCULTA = new Color(219, 211, 195);

	public PainelCartas() {
		setPreferredSize(new Dimension(1300, 720));
		setBackground(FUNDO);
	}

	@Override
	protected void paintComponent(Graphics graphics) {
		super.paintComponent(graphics);
		Graphics2D g = (Graphics2D) graphics.create();
		ModelAPI m = ModelAPI.getInstance();
		int largura = getWidth() / 5;

		String secreta = m.getIdCartaSecretaAtual();
		String textoSecreta = secreta == null
				? "Oculta. Apenas o jogador da vez deve revelar esta carta."
				: m.getCartaSecretaAtual();
		desenharCarta(g, 0, largura, secreta, "Carta secreta", textoSecreta);

		List<String> ids = m.getIdsCartasAbertas();
		List<String> descricoes = m.getObjetivosAbertosComProgresso();
		if (descricoes.size() != ids.size()) {
			descricoes = m.getCartasAbertas();
		}
		for (int i = 0; i < 4; i++) {
			// Carta aberta cumprida sai do jogo e não é reposta (manual, p. 18).
			String id = i < ids.size() ? ids.get(i) : null;
			String descricao = i < ids.size() ? descricoes.get(i) : "Objetivo já cumprido; sem reposição.";
			desenharCarta(g, i + 1, largura, id, "Objetivo aberto " + (i + 1), descricao);
		}
		g.dispose();
	}

	private void desenharCarta(Graphics2D g, int coluna, int largura, String id, String titulo, String descricao) {
		int x = coluna * largura + 12;
		g.setColor(Color.DARK_GRAY);
		g.setFont(getFont().deriveFont(Font.BOLD, 15f));
		g.drawString(titulo, x, 28);

		if (id != null) {
			CatalogoImagens.getInstance().desenhar(g, id, new Rectangle(x, 42, largura - 24, 420));
		} else {
			g.setColor(CARTA_OCULTA);
			g.fillRoundRect(x, 42, largura - 24, 400, 16, 16);
		}

		// Quebra a descrição em linhas que caibam na largura da carta.
		g.setColor(Color.DARK_GRAY);
		g.setFont(getFont().deriveFont(13f));
		int y = 484;
		String linha = "";
		for (String palavra : descricao.split(" ")) {
			if (g.getFontMetrics().stringWidth(linha + palavra) > largura - 24) {
				g.drawString(linha, x, y);
				y += 18;
				linha = "";
			}
			linha += palavra + " ";
		}
		g.drawString(linha, x, y);
	}
}
