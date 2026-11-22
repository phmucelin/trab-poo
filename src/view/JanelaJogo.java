package view;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import java.awt.GraphicsEnvironment;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import java.awt.Rectangle;

import controller.Controlador;
import model.ModelAPI;
import model.ObservadoIF;
import model.ObservadorIF;

/**
 * Alterna entre preparação e partida conforme as notificações do Model.
 * Tabuleiro e painel de turno limitados a 1400 x 1050.
 */
public class JanelaJogo extends JFrame implements ObservadorIF {
	private final CardLayout telas = new CardLayout();
	private final JPanel conteudo = new JPanel(telas);
	private final JTabbedPane lateral = new JTabbedPane();
	private final List<ObservadorIF> paineis = new ArrayList<ObservadorIF>();

	public static final int LARGURA_MAX = 1400;
	public static final int ALTURA_MAX = 1050;

	public JanelaJogo() {
		super("Herdeiros de Khan");
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setLayout(new BorderLayout());
		JPanel jogo = new JPanel(new BorderLayout());
		PainelTabuleiro mapa = new PainelTabuleiro();
		paineis.add(mapa);
		JPanel areaMapa = new JPanel(new BorderLayout());
		areaMapa.add(new PainelRota(mapa), BorderLayout.NORTH);
		areaMapa.add(mapa, BorderLayout.CENTER);
		jogo.add(areaMapa, BorderLayout.CENTER);
		lateral.setTabLayoutPolicy(JTabbedPane.WRAP_TAB_LAYOUT);
		lateral.setUI(new javax.swing.plaf.basic.BasicTabbedPaneUI());
		lateral.setPreferredSize(new Dimension(PainelTurno.LARGURA + 12, PainelTabuleiro.ALTURA));
		lateral.addTab("Turno", rolagem(adicionar(new PainelTurno())));
		lateral.addTab("Ações", rolagem(adicionar(new PainelAcoes())));
		lateral.addTab("Melhorias", rolagem(adicionar(new PainelMelhorias())));
		lateral.addTab("Entregas", rolagem(adicionar(new PainelEntregas())));
		lateral.addTab("Resultado", rolagem(adicionar(new PainelResultado())));
		jogo.add(lateral, BorderLayout.LINE_END);
		conteudo.add(adicionar(new PainelPreparacao()), "PREPARACAO");
		conteudo.add(jogo, "JOGO");
		add(conteudo, BorderLayout.CENTER);
		Controlador.getInstance().registra(this);
		notify(ModelAPI.getInstance());
		pack();
		setSize(tamanhoQueCabe(getSize(), GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds()));
		// setMaximumSize não impede o usuário de maximizar um JFrame; sem
		// redimensionar, a janela nunca passa de 1400 x 1050.
		setResizable(false);
		setLocationRelativeTo(null);
	}

	/**
	 * Tamanho da janela: o preferido, sem passar de 1400 x 1050 nem da área
	 * útil da tela (num notebook de 1512 x 982, a área de melhorias no rodapé
	 * do mapa ficava fora da tela). O mapa é quase quadrado e se ajusta ao
	 * painel, então a largura perde o mesmo que a altura perdeu.
	 */
	static Dimension tamanhoQueCabe(Dimension preferido, Rectangle areaUtil) {
		int altura = Math.min(preferido.height, Math.min(ALTURA_MAX, areaUtil.height));
		int largura = preferido.width - (preferido.height - altura);
		largura = Math.min(largura, Math.min(LARGURA_MAX, areaUtil.width));
		return new Dimension(largura, altura);
	}

	private <T extends JPanel & ObservadorIF> T adicionar(T painel) {
		paineis.add(painel);
		return painel;
	}

	/** Desregistra a janela e seus painéis, para que uma nova partida não os notifique. */
	@Override
	public void dispose() {
		Controlador.getInstance().desregistra(this);
		for (ObservadorIF painel : paineis) {
			Controlador.getInstance().desregistra(painel);
		}
		super.dispose();
	}

	private static JScrollPane rolagem(JPanel painel) {
		JScrollPane rolagem = new JScrollPane(new ConteudoVertical(painel));
		rolagem.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		return rolagem;
	}
	/** Mantém os controles na largura da barra lateral, sem rolagem horizontal. */
	private static final class ConteudoVertical extends JPanel implements Scrollable {
		ConteudoVertical(JPanel painel) { super(new BorderLayout()); add(painel, BorderLayout.NORTH); }
		@Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
		@Override public int getScrollableUnitIncrement(Rectangle area, int orientacao, int direcao) { return 16; }
		@Override public int getScrollableBlockIncrement(Rectangle area, int orientacao, int direcao) { return Math.max(16, area.height - 16); }
		@Override public boolean getScrollableTracksViewportWidth() { return true; }
		@Override public boolean getScrollableTracksViewportHeight() { return false; }
	}

	@Override public void notify(ObservadoIF observado) {
		String etapa = Controlador.getInstance().getEtapaPartida();
		telas.show(conteudo, etapa.equals("PREPARACAO") ? "PREPARACAO" : "JOGO");
		if (etapa.equals("RESULTADO")) lateral.setSelectedIndex(4);
		else if (etapa.equals("ENTREGAS")) lateral.setSelectedIndex(3);
		else if (etapa.equals("MELHORIAS")) lateral.setSelectedIndex(2);
	}
}
