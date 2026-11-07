package view;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

/** Busca O(1) por identificador do Model. Fotos oficiais; nunca usa ImageIcon. */
public final class CatalogoImagens {
	private static final CatalogoImagens INSTANCIA = new CatalogoImagens();
	private final Map<String, String> arquivos = new HashMap<>();
	private final Map<String, BufferedImage> imagens = new HashMap<>();
	private CatalogoImagens() {
		String[][] cartas = {
			{"ABERTA_QUATRO_ESPADAS", "1", "1"}, {"ABERTA_SEIS_YURTS", "1", "2"}, {"ABERTA_DEZ_YURTS", "1", "3"},
			{"ABERTA_QUATRO_TESOUROS", "1", "4"}, {"ABERTA_DOIS_YURTS_POR_REGIAO", "1", "5"}, {"ABERTA_UM_YURT_POR_REGIAO", "1", "6"},
			{"ABERTA_DOIS_BONUS", "1", "7"}, {"ABERTA_TRES_BONUS", "1", "8"}, {"ABERTA_CINCO_TIPOS_DE_TESOURO", "1", "9"},
			{"ABERTA_TRES_CONQUISTAS", "2", "1"}, {"ABERTA_DUAS_CONQUISTAS", "2", "2"}, {"ABERTA_UMA_LINHA", "2", "3"},
			{"ABERTA_UMA_COLUNA_COMPLETA", "3", "1"}, {"ABERTA_DUAS_COLUNAS", "3", "2"},
			{"SECRETA_SACERDOTE", "4", "1"}, {"SECRETA_COLECIONADOR", "4", "2"}, {"SECRETA_VIAJANTE", "5", "1"},
			{"SECRETA_SOLDADO", "5", "2"}, {"SECRETA_CIDADAO", "6", "1"}, {"SECRETA_CAVALEIRO", "6", "2"},
			{"SECRETA_MENSAGEIRO", "7", "1"}, {"SECRETA_GOVERNADOR", "7", "2"}, {"SECRETA_PREFEITO", "8", "1"}, {"SECRETA_BAJULADOR", "8", "2"}
		};
		for (String[] carta : cartas) arquivos.put(carta[0], "cartas/cartas" + carta[1] + "/cartas" + carta[1] + "-" + carta[2] + ".jpg");
		String[] herdeiros = { "ALTANI", "JOCHI", "CHAGATAI", "TOLUI", "OGEDEI" };
		for (int i = 0; i < herdeiros.length; i++) arquivos.put(herdeiros[i], "cartelas/cartela" + (i + 1) + ".jpg");
		String[] cores = { "AMARELA", "VERMELHA", "VERDE" }, letras = { "A", "R", "V" };
		String[] simbolos = { "MOEDA", "ESPADA", "YURT", "MAO", "CAVALOS" }, prefixos = { "MOE", "ESP", "CAS", "MAO", "CAV" };
		for (int c = 0; c < cores.length; c++) for (int s = 0; s < simbolos.length; s++) melhoria(cores[c], simbolos[s], prefixos[s] + "-" + letras[c]);
		melhoria("AMARELA", "GADO", "BOI-A"); melhoria("AMARELA", "ESPECIARIAS", "CUI-A");
		melhoria("VERMELHA", "FERRO", "MOB-R"); melhoria("VERDE", "PELES", "MOP-V"); melhoria("VERDE", "TECIDO", "PER-V");
	}
	private void melhoria(String cor, String simbolo, String arquivo) { arquivos.put("MELHORIA_" + cor + "_" + simbolo, "cartoes/cartoes1/" + arquivo + ".jpg"); }
	public static CatalogoImagens getInstance() { return INSTANCIA; }
	public Set<String> getIdentificadores() { return Set.copyOf(arquivos.keySet()); }
	public BufferedImage get(String id) {
		if (!arquivos.containsKey(id)) throw new IllegalArgumentException("Imagem não cadastrada: " + id);
		BufferedImage imagem = imagens.get(id);
		if (imagem != null) return imagem;
		Path caminho = localizar("imagens/oficiais/" + arquivos.get(id));
		try (ImageInputStream entrada = ImageIO.createImageInputStream(caminho.toFile())) {
			if (entrada == null) throw new IOException("Arquivo ausente");
			ImageReader leitor = ImageIO.getImageReaders(entrada).next();
			try {
				leitor.setInput(entrada);
				ImageReadParam parametros = leitor.getDefaultReadParam();
				int limite = id.startsWith("MELHORIA_") ? 700 : 1600;
				int fator = Math.max(1, Math.max(leitor.getWidth(0), leitor.getHeight(0)) / limite);
				parametros.setSourceSubsampling(fator, fator, 0, 0);
				imagem = leitor.read(0, parametros);
			} finally { leitor.dispose(); }
		} catch (IOException e) { throw new IllegalStateException("Não foi possível ler " + caminho, e); }
		imagens.put(id, imagem);
		return imagem;
	}
	private static Path localizar(String nome) {
		Path caminho = Path.of(nome);
		if (Files.isRegularFile(caminho)) return caminho;
		try {
			Path pasta = Path.of(CatalogoImagens.class.getProtectionDomain().getCodeSource().getLocation().toURI());
			for (int i = 0; i < 4 && pasta != null; i++, pasta = pasta.getParent()) if (Files.isRegularFile(pasta.resolve(nome))) return pasta.resolve(nome);
		} catch (java.net.URISyntaxException e) { throw new IllegalStateException(e); }
		return caminho;
	}
	public void desenhar(Graphics2D g, String id, Rectangle area) {
		BufferedImage imagem = get(id);
		double escala = Math.min(area.width / (double) imagem.getWidth(), area.height / (double) imagem.getHeight());
		int largura = (int) Math.round(imagem.getWidth() * escala), altura = (int) Math.round(imagem.getHeight() * escala);
		g.drawImage(imagem, area.x + (area.width - largura) / 2, area.y + (area.height - altura) / 2, largura, altura, null);
	}
}
