package view;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import controller.Controlador;
import model.ModelAPI;
import model.ObservadoIF;
import model.ObservadorIF;

public final class JanelaCartela extends JFrame implements ObservadorIF {
	private final PainelCartela cartela = new PainelCartela();
	private final JComboBox<String> jogadores = new JComboBox<>();
	private boolean atualizando;
	public JanelaCartela() {
		super("Cartela"); setDefaultCloseOperation(DO_NOTHING_ON_CLOSE); setLayout(new BorderLayout());
		JPanel comandos = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JButton fechar = new JButton("Fechar cartela"); jogadores.addActionListener(new Selecionar()); fechar.addActionListener(new Fechar());
		comandos.add(jogadores); comandos.add(fechar); add(comandos, BorderLayout.NORTH); add(cartela, BorderLayout.CENTER);
		addWindowListener(new Fechamento()); pack(); setResizable(false); setLocationRelativeTo(null);
		Controlador.getInstance().registra(this); notify(ModelAPI.getInstance());
	}
	private class Selecionar implements ActionListener { @Override public void actionPerformed(ActionEvent evento) { if (!atualizando) Controlador.getInstance().selecionarCartela(jogadores.getSelectedIndex()); } }
	private class Fechar implements ActionListener { @Override public void actionPerformed(ActionEvent evento) { Controlador.getInstance().fecharJanelaCartela(); } }
	private class Fechamento extends WindowAdapter { @Override public void windowClosing(WindowEvent evento) { Controlador.getInstance().fecharJanelaCartela(); } }
	@Override public void notify(ObservadoIF observado) {
		ModelAPI m = (ModelAPI) observado; atualizando = true;
		try {
			jogadores.removeAllItems();
			for (int i = 0; i < m.getNumJogadores(); i++) jogadores.addItem(m.getNomeJogador(i) + " — " + m.getHerdeiroJogador(i));
			if (m.getJogadorCartela() >= 0) jogadores.setSelectedIndex(m.getJogadorCartela());
		} finally { atualizando = false; }
		cartela.repaint(); setVisible(m.isJanelaCartelaAberta());
	}
	@Override public void dispose() { Controlador.getInstance().desregistra(this); super.dispose(); }

}
