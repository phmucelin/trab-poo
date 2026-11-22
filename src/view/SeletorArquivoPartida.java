package view;

import java.awt.Component;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Escolha do arquivo de uma partida salva com JFileChooser (4ª iteração).
 * O usuário escolhe nome e pasta livremente; só o tipo (.txt) é fixo.
 */
public final class SeletorArquivoPartida {
	public static final String EXTENSAO = "txt";

	private SeletorArquivoPartida() { }

	private static JFileChooser novo(String titulo) {
		JFileChooser seletor = new JFileChooser();
		seletor.setDialogTitle(titulo);
		seletor.setAcceptAllFileFilterUsed(false);
		seletor.setFileFilter(new FileNameExtensionFilter("Partida salva (*." + EXTENSAO + ")", EXTENSAO));
		return seletor;
	}

	/** @return o arquivo a gravar (com .txt acrescentado se faltar), ou null se o usuário cancelou. */
	public static File paraSalvar(Component pai) {
		JFileChooser seletor = novo("Salvar partida");
		while (seletor.showSaveDialog(pai) == JFileChooser.APPROVE_OPTION) {
			File arquivo = comExtensao(seletor.getSelectedFile());
			if (!arquivo.exists() || JOptionPane.showConfirmDialog(pai,
					"O arquivo " + arquivo.getName() + " já existe. Deseja substituí-lo?",
					"Salvar partida", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
				return arquivo;
			}
		}
		return null;
	}

	/** @return o arquivo a ler, ou null se o usuário cancelou. */
	public static File paraAbrir(Component pai) {
		JFileChooser seletor = novo("Carregar partida");
		return seletor.showOpenDialog(pai) == JFileChooser.APPROVE_OPTION ? seletor.getSelectedFile() : null;
	}

	static File comExtensao(File arquivo) {
		return arquivo.getName().toLowerCase().endsWith("." + EXTENSAO) ? arquivo
				: new File(arquivo.getParentFile(), arquivo.getName() + "." + EXTENSAO);
	}
}
