package view;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import controller.Controlador;
import model.ModelAPI;
import model.ObservadoIF;
import model.ObservadorIF;

/**
 * Janela com a carta secreta do jogador da vez e os objetivos abertos.
 * Quem abre e fecha é o Controlador; a visibilidade vem do Model pelo Observer.
 */
public final class JanelaCartas extends JFrame implements ObservadorIF {
	private final PainelCartas cartas = new PainelCartas();
	private final JButton revelar = new JButton("Revelar minha carta");
	private final JButton ocultar = new JButton("Ocultar carta secreta");

	public JanelaCartas() {
		super("Cartas");
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		setLayout(new BorderLayout());

		JButton fechar = new JButton("Fechar cartas");
		revelar.addActionListener(new Revelar());
		ocultar.addActionListener(new Ocultar());
		fechar.addActionListener(new Fechar());

		JPanel comandos = new JPanel(new FlowLayout(FlowLayout.LEFT));
		comandos.add(revelar);
		comandos.add(ocultar);
		comandos.add(fechar);
		add(comandos, BorderLayout.NORTH);
		add(cartas, BorderLayout.CENTER);

		// O X da janela passa pelo Controlador, como o botão Fechar.
		addWindowListener(new Fechamento());
		pack();
		setResizable(false);
		setLocationRelativeTo(null);
		Controlador.getInstance().registra(this);
		notify(ModelAPI.getInstance());
	}

	private class Revelar implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent evento) {
			Controlador.getInstance().mostrarCartaSecreta();
		}
	}

	private class Ocultar implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent evento) {
			Controlador.getInstance().ocultarCartaSecreta();
		}
	}

	private class Fechar implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent evento) {
			Controlador.getInstance().fecharJanelaCartas();
		}
	}

	private class Fechamento extends WindowAdapter {
		@Override
		public void windowClosing(WindowEvent evento) {
			Controlador.getInstance().fecharJanelaCartas();
		}
	}

	@Override
	public void notify(ObservadoIF observado) {
		ModelAPI m = (ModelAPI) observado;
		if (m.temPartida()) {
			setTitle("Cartas - " + m.getNomeJogador(m.getJogadorDaVez()));
		}
		revelar.setEnabled(!m.isPreparacao() && !m.isPartidaEncerrada() && !m.isCartaSecretaVisivel());
		ocultar.setEnabled(m.isCartaSecretaVisivel());
		cartas.repaint();
		setVisible(m.isJanelaCartasAberta());
	}

	@Override
	public void dispose() {
		Controlador.getInstance().desregistra(this);
		super.dispose();
	}
}
