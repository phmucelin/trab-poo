package view;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

import controller.Controlador;
import model.ModelAPI;
import model.ObservadoIF;
import model.ObservadorIF;

/**
 * Tabuleiro gráfico (2ª iteração, Parte 2).
 *
 * Desenha a imagem oficial do tabuleiro com drawImage() do Graphics2D e, por
 * cima dela, os peões, o Khan e os destaques do turno: destinos possíveis do
 * peão ativo e, quando a coluna exige, os lugares para onde o Khan pode ir. A
 * borda do painel fica na cor do jogador da vez.
 *
 * Toda jogada é feita clicando no tabuleiro: numa parada para mover o peão,
 * numa cabeça do Khan (ou na área de melhorias) para mover o Khan e, no caso
 * de Altani, no outro peão para trocar o peão que age. O painel só repassa o
 * clique ao Controlador; se a jogada não for permitida, nada acontece, sem
 * mensagem e sem troca do jogador da vez.
 *
 * Segue o padrão Observer do cap. 17: registra-se pelo Controlador e, a cada
 * notificação, redesenha lendo o estado atual da ModelAPI.
 */
public class PainelTabuleiro extends JPanel implements ObservadorIF {

	private static final long serialVersionUID = 1L;

	/** Tamanho sugerido; junto com o PainelTurno, cabe no limite de 1400 x 1050. */
	public static final int LARGURA = 940;
	public static final int ALTURA = 945;

	private static final String ARQUIVO_TABULEIRO = "imagens" + File.separator + "tabuleiro.jpg";

	/**
	 * Medidas em pixels da imagem original; são convertidas para a escala da
	 * tela. O peão fica em pé no centro da parada: a metade de baixo da parada
	 * continua visível e a cabeça sobra acima dela, onde o clique escolhe o peão.
	 */
	private static final int ALTURA_PEAO = 120;
	private static final int ALTURA_CABECA = 50;
	private static final int LARGURA_PEAO = 80;
	private static final int ESPACO_ENTRE_PEOES = 90;
	private static final int RAIO_KHAN = 60;

	/** Espessura, em pixels da tela, da borda na cor do jogador da vez. */
	private static final int BORDA = 12;

	/** Área de melhorias na imagem: o clique em qualquer ponto dela leva o Khan para lá. */
	private static final int[] RETANGULO_MELHORIAS = { 40, 2700, 1480, 2870 };

	private final BufferedImage tabuleiro;
	private boolean modoRota;
	private boolean saltarYurts = true;
	private final List<String> rotaPlanejada = new ArrayList<>();
	private final List<Boolean> preferenciasSalto = new ArrayList<>();
	private Runnable aoAlterarRota;
	/** Cópia da imagem já na escala da tela, refeita só quando o painel muda de tamanho. */
	private BufferedImage tabuleiroEscalado;

	private double escala;
	private int origemX;
	private int origemY;

	public PainelTabuleiro() {
		tabuleiro = carregarImagem(ARQUIVO_TABULEIRO);
		setPreferredSize(new Dimension(LARGURA, ALTURA));
		setBackground(new Color(60, 45, 35));
		addMouseListener(new CliqueNoTabuleiro());
		Controlador.getInstance().registra(this);
	}

	@Override
	public void notify(ObservadoIF o) {
		limparRota();
		repaint();
	}

	// ============================================================ desenho

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		atualizarEscala();
		desenharTabuleiro(g2);

		ModelAPI m = ModelAPI.getInstance();
		if (m.temPartida()) {
			desenharBordaDoJogadorDaVez(g2, m);
			if (!m.isPartidaEncerrada()) {
				desenharDestinos(g2, m);
				desenharDestinosDoKhan(g2, m);
			}
			desenharRota(g2, m);
			desenharYurts(g2, m);
			desenharKhan(g2, m);
			desenharPeoes(g2, m);
		}
		g2.dispose();
	}

	/** Calcula a escala que faz a imagem caber no painel, centralizada. */
	private void atualizarEscala() {
		int largura = Math.max(1, getWidth() - 2 * BORDA);
		int altura = Math.max(1, getHeight() - 2 * BORDA);
		escala = Math.min((double) largura / CoordenadasMapa.LARGURA_IMAGEM,
				(double) altura / CoordenadasMapa.ALTURA_IMAGEM);
		origemX = (getWidth() - (int) Math.round(CoordenadasMapa.LARGURA_IMAGEM * escala)) / 2;
		origemY = (getHeight() - (int) Math.round(CoordenadasMapa.ALTURA_IMAGEM * escala)) / 2;
	}

	private void desenharTabuleiro(Graphics2D g2) {
		int largura = (int) Math.round(CoordenadasMapa.LARGURA_IMAGEM * escala);
		int altura = (int) Math.round(CoordenadasMapa.ALTURA_IMAGEM * escala);
		if (tabuleiro == null) {
			g2.setColor(Color.WHITE);
			g2.drawString("Imagem não encontrada: " + ARQUIVO_TABULEIRO, 20, 30);
			return;
		}
		if (tabuleiroEscalado == null || tabuleiroEscalado.getWidth() != largura
				|| tabuleiroEscalado.getHeight() != altura) {
			tabuleiroEscalado = new BufferedImage(largura, altura, BufferedImage.TYPE_INT_RGB);
			Graphics2D gi = tabuleiroEscalado.createGraphics();
			gi.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
			gi.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			gi.drawImage(tabuleiro, 0, 0, largura, altura, null);
			gi.dispose();
		}
		g2.drawImage(tabuleiroEscalado, origemX, origemY, null);
	}

	private void desenharBordaDoJogadorDaVez(Graphics2D g2, ModelAPI m) {
		g2.setColor(CoresJogador.doJogador(m.getCorJogador(m.getJogadorDaVez())));
		g2.setStroke(new BasicStroke(BORDA));
		g2.drawRect(BORDA / 2, BORDA / 2, getWidth() - BORDA, getHeight() - BORDA);
	}

	/**
	 * Anel na cor do jogador da vez em cada parada onde o peão ativo pode
	 * terminar (nas duplas, um anel em cada círculo).
	 */
	private void desenharDestinos(Graphics2D g2, ModelAPI m) {
		Color cor = CoresJogador.doJogador(m.getCorJogador(m.getJogadorDaVez()));
		int raio = tela(CoordenadasMapa.RAIO_PARADA + 8);
		for (String nome : m.getDestinosPossiveis()) {
			for (Point p : CoordenadasMapa.circulosDaParada(nome)) {
				int x = telaX(p.x);
				int y = telaY(p.y);
				g2.setColor(new Color(255, 255, 255, 110));
				g2.fillOval(x - raio, y - raio, 2 * raio, 2 * raio);
				g2.setStroke(new BasicStroke(3f));
				g2.setColor(cor);
				g2.drawOval(x - raio, y - raio, 2 * raio, 2 * raio);
			}
		}
	}

	/** Anel tracejado em cada lugar para onde o Khan pode ir agora. */
	private void desenharDestinosDoKhan(Graphics2D g2, ModelAPI m) {
		if (!m.isKhanPendente()) {
			return;
		}
		Stroke tracejado = new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f,
				new float[] { 8f, 6f }, 0f);
		int raio = tela(RAIO_KHAN + 25);
		for (String nome : m.getDestinosDoKhan()) {
			Point p = CoordenadasMapa.doLocalDoKhan(nome);
			if (p == null) {
				continue;
			}
			int x = telaX(p.x);
			int y = telaY(p.y);
			g2.setColor(new Color(255, 215, 0, 90));
			g2.fillOval(x - raio, y - raio, 2 * raio, 2 * raio);
			g2.setColor(new Color(120, 20, 20));
			g2.setStroke(tracejado);
			g2.drawOval(x - raio, y - raio, 2 * raio, 2 * raio);
		}
	}

	/** O Khan: disco escuro com borda dourada sobre a cabeça onde ele está. */
	private void desenharKhan(Graphics2D g2, ModelAPI m) {
		Point p = CoordenadasMapa.doLocalDoKhan(m.getPosicaoKhan());
		if (p == null) {
			return; // no início ele fica fora do tabuleiro
		}
		int x = telaX(p.x);
		int y = telaY(p.y);
		int raio = tela(RAIO_KHAN);
		g2.setColor(new Color(25, 25, 25));
		g2.fillOval(x - raio, y - raio, 2 * raio, 2 * raio);
		g2.setColor(new Color(230, 180, 0));
		g2.setStroke(new BasicStroke(3f));
		g2.drawOval(x - raio, y - raio, 2 * raio, 2 * raio);
		g2.setFont(getFont().deriveFont(Font.BOLD, Math.max(10f, raio * 1.1f)));
		FontMetrics fm = g2.getFontMetrics();
		g2.drawString("K", x - fm.stringWidth("K") / 2, y + fm.getAscent() / 2 - 2);
	}

	/** Tendas abaixo dos peões, preservando as cores das peças em paradas duplas. */
	private void desenharYurts(Graphics2D g2, ModelAPI m) {
		for (String parada : m.getParadas()) {
			List<String> cores = m.getYurtsParada(parada);
			Point[] pontos = CoordenadasMapa.circulosDaParada(parada);
			for (int i = 0; i < cores.size() && pontos.length > 0; i++) {
				Point ponto = pontos[Math.min(i, pontos.length - 1)];
				desenharYurt(g2, ponto.x, ponto.y + 55, cores.get(i));
			}
		}
		for (String cidade : m.getCidades()) {
			String cor = m.getConquistadorCidade(cidade);
			if (cor == null) continue;
			int x = 0, y = 0, quantidade = 0;
			for (String parada : m.getParadasDaCidade(cidade)) {
				Point ponto = CoordenadasMapa.daParada(parada);
				if (ponto != null) { x += ponto.x; y += ponto.y; quantidade++; }
			}
			if (quantidade > 0) desenharYurt(g2, x / quantidade, y / quantidade, cor);
		}
	}

	private void desenharYurt(Graphics2D g2, int px, int py, String cor) {
		int x = telaX(px), y = telaY(py), raio = Math.max(8, tela(42));
		int[] xs = { x - raio, x, x + raio, x + raio, x - raio };
		int[] ys = { y, y - raio, y, y + raio / 2, y + raio / 2 };
		g2.setColor(CoresJogador.doJogador(cor));
		g2.fillPolygon(xs, ys, xs.length);
		g2.setColor(Color.WHITE);
		g2.setStroke(new BasicStroke(3f));
		g2.drawPolygon(xs, ys, xs.length);
		g2.setColor(Color.BLACK);
		g2.setStroke(new BasicStroke(1f));
		g2.drawPolygon(xs, ys, xs.length);
	}

	private void desenharPeoes(Graphics2D g2, ModelAPI m) {
		int vez = m.getJogadorDaVez();
		int ativo = m.getPeaoAtivo();
		for (PeaoNaTela peao : posicionarPeoes(m)) {
			boolean destacado = !m.isPartidaEncerrada() && peao.jogador == vez && peao.peao == ativo;
			desenharPeao(g2, peao, CoresJogador.doJogador(m.getCorJogador(peao.jogador)), destacado);
		}
	}

	/** Peão no formato tradicional (cabeça e corpo), com halo branco se for o que age. */
	private void desenharPeao(Graphics2D g2, PeaoNaTela peao, Color cor, boolean destacado) {
		double x = telaX(peao.x);
		double base = telaY(peao.y);
		double altura = tela(ALTURA_PEAO);
		double largura = tela(LARGURA_PEAO);
		double raioCabeca = largura * 0.3;

		GeneralPath corpo = new GeneralPath();
		corpo.moveTo(x - largura / 2, base);
		corpo.curveTo(x - largura / 2, base - altura * 0.35, x - largura * 0.15, base - altura * 0.45,
				x - largura * 0.12, base - altura + raioCabeca * 1.6);
		corpo.lineTo(x + largura * 0.12, base - altura + raioCabeca * 1.6);
		corpo.curveTo(x + largura * 0.15, base - altura * 0.45, x + largura / 2, base - altura * 0.35,
				x + largura / 2, base);
		corpo.closePath();
		Ellipse2D cabeca = new Ellipse2D.Double(x - raioCabeca, base - altura, 2 * raioCabeca, 2 * raioCabeca);

		if (destacado) {
			g2.setColor(Color.WHITE);
			g2.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g2.draw(corpo);
			g2.draw(cabeca);
			// seta apontando para o peão que age
			double topo = base - altura - 4;
			GeneralPath seta = new GeneralPath();
			seta.moveTo(x, topo);
			seta.lineTo(x - largura * 0.3, topo - largura * 0.4);
			seta.lineTo(x + largura * 0.3, topo - largura * 0.4);
			seta.closePath();
			g2.fill(seta);
			g2.setColor(Color.BLACK);
			g2.setStroke(new BasicStroke(1.2f));
			g2.draw(seta);
		}
		g2.setColor(cor);
		g2.fill(corpo);
		g2.fill(cabeca);
		g2.setColor(Color.BLACK);
		g2.setStroke(new BasicStroke(1.5f));
		g2.draw(corpo);
		g2.draw(cabeca);
	}

	// ============================================================ posições

	/**
	 * Um peão desenhado: de quem é, qual é (0 ou 1) e onde fica a base dele na
	 * imagem original.
	 */
	private static final class PeaoNaTela {
		final int jogador;
		final int peao;
		final int x;
		final int y;

		PeaoNaTela(int jogador, int peao, int x, int y) {
			this.jogador = jogador;
			this.peao = peao;
			this.x = x;
			this.y = y;
		}
	}

	/**
	 * Calcula onde cada peão fica na imagem. Peões na mesma parada são postos
	 * lado a lado, centralizados no ponto da parada.
	 */
	private List<PeaoNaTela> posicionarPeoes(ModelAPI m) {
		Map<String, List<int[]>> porParada = new HashMap<>();
		List<String> ordem = new ArrayList<>();
		for (int j = 0; j < m.getNumJogadores(); j++) {
			for (int p = 0; p < m.getNumPeoes(j); p++) {
				String parada = m.getPosicaoPeao(j, p);
				if (!porParada.containsKey(parada)) {
					porParada.put(parada, new ArrayList<>());
					ordem.add(parada);
				}
				porParada.get(parada).add(new int[] { j, p });
			}
		}
		List<PeaoNaTela> peoes = new ArrayList<>();
		for (String parada : ordem) {
			Point centro = CoordenadasMapa.daParada(parada);
			if (centro == null) {
				continue;
			}
			List<int[]> aqui = porParada.get(parada);
			int inicio = centro.x - (aqui.size() - 1) * ESPACO_ENTRE_PEOES / 2;
			for (int i = 0; i < aqui.size(); i++) {
				int[] peao = aqui.get(i);
				peoes.add(new PeaoNaTela(peao[0], peao[1], inicio + i * ESPACO_ENTRE_PEOES, centro.y));
			}
		}
		return peoes;
	}

	private int tela(int medida) {
		return (int) Math.round(medida * escala);
	}

	private int telaX(int x) {
		return origemX + (int) Math.round(x * escala);
	}

	private int telaY(int y) {
		return origemY + (int) Math.round(y * escala);
	}

	private static double distancia(int x1, int y1, int x2, int y2) {
		return Math.hypot(x1 - x2, y1 - y2);
	}

	private static BufferedImage carregarImagem(String arquivo) {
		try {
			return ImageIO.read(localizar(arquivo));
		} catch (IOException e) {
			System.err.println("Não foi possível ler " + arquivo + ": " + e.getMessage());
			return null;
		}
	}

	/**
	 * Procura o arquivo a partir do diretório de trabalho, que no Eclipse é a
	 * raiz do projeto. Se a configuração de execução usar outro diretório,
	 * procura a partir da pasta das classes compiladas (bin/), subindo até a
	 * raiz do projeto.
	 */
	private static File localizar(String arquivo) {
		File noDiretorioDeTrabalho = new File(arquivo);
		if (noDiretorioDeTrabalho.isFile()) {
			return noDiretorioDeTrabalho;
		}
		CodeSource codigo = PainelTabuleiro.class.getProtectionDomain().getCodeSource();
		if (codigo != null) {
			try {
				File pasta = new File(codigo.getLocation().toURI());
				for (int nivel = 0; nivel < 3 && pasta != null; nivel++) {
					File candidato = new File(pasta, arquivo);
					if (candidato.isFile()) {
						return candidato;
					}
					pasta = pasta.getParentFile();
				}
			} catch (URISyntaxException e) {
				// fica com o caminho relativo ao diretório de trabalho
			}
		}
		return noDiretorioDeTrabalho;
	}

	public void setModoRota(boolean ativo) { modoRota = ativo; limparRota(); }
	public void setSaltarYurts(boolean saltar) { saltarYurts = saltar; }
	public void setAoAlterarRota(Runnable atualizacao) { aoAlterarRota = atualizacao; }
	public List<String> getRotaPlanejada() { return new ArrayList<>(rotaPlanejada); }
	public void limparRota() {
		rotaPlanejada.clear(); preferenciasSalto.clear();
		if (aoAlterarRota != null) aoAlterarRota.run();
		repaint();
	}
	private List<Boolean> saltosPlanejados() {
		List<Boolean> saltos = new ArrayList<>(preferenciasSalto);
		if (!saltos.isEmpty()) saltos.set(saltos.size() - 1, false);
		return saltos;
	}
	public boolean isRotaValida() { return ModelAPI.getInstance().podeMoverPor(rotaPlanejada, saltosPlanejados()); }
	private void planejarParada(String nome) {
		ModelAPI m = ModelAPI.getInstance();
		if (m.getCavalosRestantes() <= 0) return;
		if (!rotaPlanejada.isEmpty() && rotaPlanejada.get(rotaPlanejada.size() - 1).equals(nome)) {
			Controlador.getInstance().moverPor(new ArrayList<>(rotaPlanejada), saltosPlanejados());
			return;
		}
		String anterior = rotaPlanejada.isEmpty() ? m.getPosicaoPeao(m.getJogadorDaVez(), m.getPeaoAtivo()) : rotaPlanejada.get(rotaPlanejada.size() - 1);
		if (!m.getVizinhas(anterior).contains(nome)) return;
		rotaPlanejada.add(nome);
		preferenciasSalto.add(saltarYurts && m.getYurtsParada(nome).contains(m.getCorJogador(m.getJogadorDaVez())));
		if (aoAlterarRota != null) aoAlterarRota.run();
		repaint();
	}
	private void desenharRota(Graphics2D g2, ModelAPI m) {
		if (rotaPlanejada.isEmpty() || m.getPeaoAtivo() < 0) return;
		Point anterior = CoordenadasMapa.daParada(m.getPosicaoPeao(m.getJogadorDaVez(), m.getPeaoAtivo()));
		g2.setColor(new Color(0, 100, 255));
		g2.setStroke(new BasicStroke(4f));
		for (String nome : rotaPlanejada) {
			Point proximo = CoordenadasMapa.daParada(nome);
			if (anterior != null && proximo != null) g2.drawLine(telaX(anterior.x), telaY(anterior.y), telaX(proximo.x), telaY(proximo.y));
			anterior = proximo;
		}
	}

	// ============================================================ clique

	/**
	 * Trata o clique no tabuleiro (cap. 12: classe interna que estende
	 * MouseAdapter). A ordem de teste é: trocar o peão de Altani, mover o Khan
	 * e, por último, mover o peão para a parada mais próxima do clique.
	 *
	 * O outro peão de Altani pode estar numa parada para onde o peão ativo pode
	 * ir. Por isso o clique na cabeça dele sempre troca o peão que age, e o
	 * clique no corpo só troca se a parada embaixo não for um destino possível;
	 * se for, o peão ativo vai para lá.
	 */
	private class CliqueNoTabuleiro extends MouseAdapter {

		@Override
		public void mouseClicked(MouseEvent e) {
			ModelAPI m = ModelAPI.getInstance();
			atualizarEscala();
			if (!m.temPartida() || m.isPartidaEncerrada() || escala <= 0) {
				return;
			}
			int x = (int) Math.round((e.getX() - origemX) / escala);
			int y = (int) Math.round((e.getY() - origemY) / escala);
			Controlador c = Controlador.getInstance();
			String parada = paradaClicada(x, y);
			if (modoRota && parada != null) {
				planejarParada(parada);
				return;
			}

			int naCabeca = peaoClicado(m, x, y, ALTURA_CABECA);
			if (naCabeca >= 0) {
				c.escolherPeao(naCabeca);
				return;
			}
			int noCorpo = peaoClicado(m, x, y, ALTURA_PEAO);
			if (noCorpo >= 0 && (parada == null || !m.podeMoverPara(parada))) {
				c.escolherPeao(noCorpo);
				return;
			}
			String khan = localDoKhanClicado(m, x, y);
			if (khan != null) {
				c.moverKhan(khan);
				return;
			}
			if (parada != null) {
				c.paradaClicada(parada);
			}
		}

		/**
		 * Outro peão do jogador da vez, quando ele ainda pode trocar (Altani),
		 * se o clique cair nos primeiros pixels do peão, contados do topo: a
		 * cabeça (ALTURA_CABECA) ou o peão inteiro (ALTURA_PEAO).
		 */
		private int peaoClicado(ModelAPI m, int x, int y, int alturaDoTopo) {
			if (!m.podeEscolherPeao()) {
				return -1;
			}
			int vez = m.getJogadorDaVez();
			for (PeaoNaTela peao : posicionarPeoes(m)) {
				int topo = peao.y - ALTURA_PEAO;
				if (peao.jogador == vez && peao.peao != m.getPeaoAtivo()
						&& Math.abs(x - peao.x) <= LARGURA_PEAO / 2 && y >= topo && y <= topo + alturaDoTopo) {
					return peao.peao;
				}
			}
			return -1;
		}

		/** Lugar do Khan sob o clique, só enquanto a coluna ativada exige movê-lo. */
		private String localDoKhanClicado(ModelAPI m, int x, int y) {
			if (!m.isKhanPendente()) {
				return null;
			}
			if (x >= RETANGULO_MELHORIAS[0] && y >= RETANGULO_MELHORIAS[1] && x <= RETANGULO_MELHORIAS[2]
					&& y <= RETANGULO_MELHORIAS[3]) {
				return ModelAPI.AREA_MELHORIAS;
			}
			for (String nome : CoordenadasMapa.getLocaisDoKhan()) {
				Point p = CoordenadasMapa.doLocalDoKhan(nome);
				if (distancia(x, y, p.x, p.y) <= RAIO_KHAN + 30) {
					return nome;
				}
			}
			return null;
		}

		/**
		 * Parada mais próxima do clique, dentro do raio de tolerância de algum
		 * dos seus círculos (as duplas têm dois). Karakorum vale o quadro todo.
		 */
		private String paradaClicada(int x, int y) {
			if (CoordenadasMapa.noQuadroDeKarakorum(x, y)) {
				return CoordenadasMapa.KARAKORUM;
			}
			String maisProxima = null;
			double menor = CoordenadasMapa.RAIO_CLIQUE;
			for (String nome : CoordenadasMapa.getParadas()) {
				for (Point p : CoordenadasMapa.circulosDaParada(nome)) {
					double d = distancia(x, y, p.x, p.y);
					if (d <= menor) {
						menor = d;
						maisProxima = nome;
					}
				}
			}
			return maisProxima;
		}
	}
}
