package view;

import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * JFileChooser das partidas salvas (4ª iteração, regras 3 e 4): o usuário
 * escolhe livremente o nome e a pasta; só o tipo de arquivo (.txt) é
 * pré-definido.
 */
final class SeletorArquivo {
	private static File ultimaPasta;

	private SeletorArquivo() { }

	static JFileChooser criar(String titulo) {
		JFileChooser seletor = new JFileChooser(ultimaPasta != null ? ultimaPasta : new File(System.getProperty("user.home")));
		seletor.setDialogTitle(titulo);
		seletor.setFileFilter(new FileNameExtensionFilter("Partida salva de Herdeiros de Khan (*.txt)", "txt"));
		return seletor;
	}

	/** Lembra a pasta usada, para o próximo salvamento ou carregamento começar nela. */
	static void lembrar(File arquivo) {
		if (arquivo != null && arquivo.getParentFile() != null) ultimaPasta = arquivo.getParentFile();
	}
}
