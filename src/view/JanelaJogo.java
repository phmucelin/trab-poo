package view;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;

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

	public static final int LARGURA_MAX = 1400;
	public static final int ALTURA_MAX = 1050;

	public JanelaJogo() {
		super("Herdeiros de Khan");
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setLayout(new BorderLayout());
		JPanel jogo = new JPanel(new BorderLayout());
		PainelTabuleiro mapa = new PainelTabuleiro();
		JPanel areaMapa = new JPanel(new BorderLayout());
		areaMapa.add(new PainelRota(mapa), BorderLayout.NORTH);
		areaMapa.add(mapa, BorderLayout.CENTER);
		jogo.add(areaMapa, BorderLayout.CENTER);
		lateral.setTabLayoutPolicy(JTabbedPane.WRAP_TAB_LAYOUT);
		lateral.setUI(new javax.swing.plaf.basic.BasicTabbedPaneUI());
		lateral.setPreferredSize(new Dimension(PainelTurno.LARGURA + 12, PainelTabuleiro.ALTURA));
		lateral.addTab("Turno", rolagem(new PainelTurno()));
		lateral.addTab("Ações", rolagem(new PainelAcoes()));
		lateral.addTab("Melhorias", rolagem(new PainelMelhorias()));
		lateral.addTab("Entregas", rolagem(new PainelEntregas()));
		lateral.addTab("Resultado", rolagem(new PainelResultado()));
		jogo.add(lateral, BorderLayout.LINE_END);
		conteudo.add(new PainelPreparacao(), "PREPARACAO");
		conteudo.add(jogo, "JOGO");
		add(conteudo, BorderLayout.CENTER);
		Controlador.getInstance().registra(this);
		notify(ModelAPI.getInstance());
		pack();
		setSize(Math.min(getWidth(), LARGURA_MAX), Math.min(getHeight(), ALTURA_MAX));
		// setMaximumSize não impede o usuário de maximizar um JFrame; sem
		// redimensionar, a janela nunca passa de 1400 x 1050.
		setResizable(false);
		setLocationRelativeTo(null);
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
