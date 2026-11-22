package model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Salvamento e recuperação de partidas (4ª iteração).
 *
 * O estado é gravado entre dois turnos, num arquivo texto ASCII puro. Cada
 * linha é um registro: o tipo, seguido de campos "chave=valor" separados por
 * ";". Listas usam "," e pares usam ":". Lista vazia é "-". Caracteres fora do
 * ASCII simples (acentos dos nomes de paradas, cartas e jogadores) e os
 * separadores são escritos como \\uXXXX. Linhas em branco ou começadas por "#"
 * são ignoradas, o que permite comentar ou editar o arquivo à mão.
 *
 * As pilhas são gravadas do fundo para o topo, então a partida recuperada
 * continua com as mesmas compras e revelações que a original teria.
 */
final class ArquivoPartida {
	static final String CABECALHO = "HERDEIROS_DE_KHAN";
	static final int VERSAO = 1;
	private static final String VAZIO = "-";

	private ArquivoPartida() { }

	/** Partida recuperada, com os componentes que a acompanham. */
	static final class Recuperada {
		final Partida partida;
		final ComponentesKhan componentes;
		Recuperada(Partida partida, ComponentesKhan componentes) {
			this.partida = partida;
			this.componentes = componentes;
		}
	}

	// =================================================================== gravar

	static List<String> gravar(Partida partida, ComponentesKhan componentes) {
		if (!partida.podeSalvar()) throw new IllegalStateException("A partida só pode ser salva entre dois turnos");
		Tabuleiro tabuleiro = partida.getTabuleiro();
		List<Jogador> jogadores = partida.getJogadores();
		List<String> linhas = new ArrayList<>();
		linhas.add("# Herdeiros de Khan - partida salva entre dois turnos (texto ASCII).");
		linhas.add("# Formato: TIPO;chave=valor;... Listas com \",\", pares com \":\", lista vazia \"-\".");
		linhas.add("# Pilhas do fundo para o topo. Acentos e separadores aparecem como \\uXXXX.");
		linhas.add(CABECALHO + ";versao=" + VERSAO);
		linhas.add("PARTIDA;vez=" + jogadores.indexOf(partida.getJogadorDaVez()) + ";rodada=" + partida.getRodada()
				+ ";rodadaFinal=" + partida.getRodadaFinal() + ";kurultai=" + partida.getPosicaoKurultai()
				+ ";turno=" + partida.getTurno().getNumero() + ";khan=" + texto(partida.getPosicaoKhan()));
		StringBuilder estoque = new StringBuilder("ESTOQUE");
		for (TipoTributo tipo : TipoTributo.values()) estoque.append(';').append(tipo.name()).append('=').append(partida.getEstoque(tipo));
		linhas.add(estoque.toString());

		for (int i = 0; i < jogadores.size(); i++) {
			Jogador j = jogadores.get(i);
			Inventario inv = componentes.inventarioDe(j);
			StringBuilder s = new StringBuilder("JOGADOR;indice=" + i + ";nome=" + escapar(j.getNome()) + ";cor=" + j.getCor().name()
					+ ";herdeiro=" + j.getHerdeiro().name() + ";yurts=" + j.getYurtsDisponiveis() + ";votos=" + j.getVotos());
			List<String> fichas = new ArrayList<>();
			for (Coluna coluna : Coluna.values()) if (j.temFichaDisponivel(coluna)) fichas.add(coluna.name());
			s.append(";fichas=").append(lista(fichas));
			for (TipoTributo tipo : TipoTributo.values()) s.append(';').append(tipo.name()).append('=').append(j.contarTributo(tipo));
			s.append(";tesourosRecebidos=").append(partida.getTesourosRecebidos(j));
			s.append(";linhasPremiadas=").append(componentes.getLinhasPremiadas(j));
			linhas.add(s.toString());
			for (int p = 0; p < j.getPeoes().size(); p++) {
				linhas.add("PEAO;jogador=" + i + ";peao=" + p + ";parada=" + escapar(tabuleiro.posicaoDe(j.getPeao(p)).getNome()));
			}
			StringBuilder tesouros = new StringBuilder("TESOUROS_JOGADOR;jogador=" + i);
			for (TipoTesouro tipo : TipoTesouro.values()) tesouros.append(';').append(tipo.name()).append('=').append(inv.contarTesouro(tipo));
			linhas.add(tesouros.toString());
			List<String> bonus = new ArrayList<>();
			for (Map.Entry<TipoBonus, Boolean> b : inv.getBonus().entrySet()) bonus.add(b.getKey().name() + ":" + (b.getValue() ? "usado" : "disponivel"));
			linhas.add("BONUS_JOGADOR;jogador=" + i + ";pecas=" + lista(bonus));
			for (Coluna coluna : Coluna.values()) {
				for (Melhoria m : inv.getMelhorias(coluna)) {
					linhas.add("MELHORIA_JOGADOR;jogador=" + i + ";coluna=" + coluna.name() + ";peca=" + melhoria(m)
							+ ";turno=" + inv.turnoDeAquisicao(m));
				}
			}
			for (Carta carta : componentes.cartasDe(j)) linhas.add("CARTA_SECRETA;jogador=" + i + ";carta=" + escapar(carta.getNome()));
		}

		List<Parada> paradas = new ArrayList<>(tabuleiro.getParadas());
		paradas.sort(Comparator.comparing(Parada::getNome));
		for (Parada parada : paradas) {
			if (parada.getYurts().isEmpty()) continue;
			List<String> cores = new ArrayList<>();
			for (Cor cor : parada.getYurts()) cores.add(cor.name());
			linhas.add("YURTS;parada=" + escapar(parada.getNome()) + ";cores=" + lista(cores));
		}
		List<Cidade> cidades = new ArrayList<>(tabuleiro.getCidades());
		cidades.sort(Comparator.comparing(Cidade::getNome));
		for (Cidade cidade : cidades) {
			List<String> tesouros = new ArrayList<>();
			for (TipoTesouro tipo : componentes.tesourosEm(cidade)) tesouros.add(tipo.name());
			linhas.add("CIDADE;nome=" + escapar(cidade.getNome()) + ";revelada=" + (cidade.isRevelada() ? 1 : 0)
					+ ";tesouros=" + lista(tesouros) + ";conquistador=" + (cidade.isConquistada() ? cidade.getConquistadaPor().name() : VAZIO));
		}
		List<String> fechadas = new ArrayList<>();
		for (Cidade cidade : partida.getCidadesFechadas()) fechadas.add(escapar(cidade.getNome()));
		linhas.add("CIDADES_FECHADAS;ordem=" + lista(fechadas));
		List<Provincia> provincias = new ArrayList<>(tabuleiro.getProvincias());
		provincias.sort(Comparator.comparing(Provincia::getNome));
		for (Provincia provincia : provincias) linhas.add("PROVINCIA;nome=" + escapar(provincia.getNome()) + ";pecas=" + provincia.getPecas());
		for (Conselheiro conselheiro : componentes.getConselheiros()) {
			linhas.add("CONSELHEIRO;parada=" + escapar(conselheiro.getParada().getNome()) + ";bonus="
					+ (conselheiro.getBonus() == null ? VAZIO : conselheiro.getBonus().name()));
		}

		linhas.add("CARTAS_ABERTAS;cartas=" + cartas(componentes.getCartasAbertas()));
		linhas.add("PILHA_ABERTAS;cartas=" + cartas(componentes.getBaralhoAbertas().getPilha()));
		linhas.add("DESCARTE_ABERTAS;cartas=" + cartas(componentes.getBaralhoAbertas().getDescarte()));
		linhas.add("PILHA_SECRETAS;cartas=" + cartas(componentes.getBaralhoSecretas().getPilha()));
		linhas.add("DESCARTE_SECRETAS;cartas=" + cartas(componentes.getBaralhoSecretas().getDescarte()));
		linhas.add("PILHA_MELHORIAS;pecas=" + melhorias(componentes.getPilhaMelhorias()));
		linhas.add("MERCADO;pecas=" + melhorias(componentes.getMercado()));
		linhas.add("PILHA_TESOUROS;pecas=" + tesouros(componentes.getPilhaTesouros()));
		linhas.add("DESCARTE_TESOUROS;pecas=" + tesouros(componentes.getDescarteTesouros()));
		linhas.add("FIM");
		return linhas;
	}

	// ===================================================================== ler

	/**
	 * Monta uma partida nova com os jogadores gravados e troca o estado sorteado
	 * pelo estado do arquivo.
	 *
	 * @throws IllegalArgumentException com a linha e o motivo, se o arquivo
	 *         estiver incompleto ou com dados inválidos
	 */
	static Recuperada ler(List<String> linhas) {
		Map<String, List<Registro>> registros = new LinkedHashMap<>();
		boolean fim = false;
		for (int n = 0; n < linhas.size(); n++) {
			String linha = linhas.get(n).strip();
			if (linha.isEmpty() || linha.startsWith("#")) continue;
			if (fim) throw new IllegalArgumentException("Linha " + (n + 1) + ": conteúdo depois de FIM.");
			for (char c : linha.toCharArray()) {
				if (c > 126 || c < 32) throw new IllegalArgumentException("Linha " + (n + 1) + ": o arquivo deve ser texto ASCII.");
			}
			Registro r = new Registro(linha, n + 1);
			if (r.tipo.equals("FIM")) { fim = true; continue; }
			registros.computeIfAbsent(r.tipo, t -> new ArrayList<>()).add(r);
		}
		if (!fim) throw new IllegalArgumentException("Arquivo incompleto: falta a linha FIM.");
		Registro cabecalho = unico(registros, CABECALHO);
		if (cabecalho.inteiro("versao") != VERSAO) throw cabecalho.erro("versão de arquivo desconhecida.");

		Registro dadosPartida = unico(registros, "PARTIDA");
		List<Registro> registrosJogadores = todos(registros, "JOGADOR");
		registrosJogadores.sort(Comparator.comparingInt(r -> r.inteiro("indice")));
		if (registrosJogadores.size() < 2 || registrosJogadores.size() > 5) {
			throw new IllegalArgumentException("A partida precisa de 2 a 5 jogadores.");
		}
		List<Jogador> jogadores = new ArrayList<>();
		for (int i = 0; i < registrosJogadores.size(); i++) {
			Registro r = registrosJogadores.get(i);
			if (r.inteiro("indice") != i) throw r.erro("os jogadores devem ter índices 0, 1, 2...");
			try {
				jogadores.add(new Jogador(desescapar(r.texto("nome")), Cor.valueOf(r.texto("cor")), Herdeiro.valueOf(r.texto("herdeiro"))));
			} catch (IllegalArgumentException e) {
				throw r.erro(e.getMessage());
			}
		}

		Tabuleiro tabuleiro = MapaOficial.criar();
		ComponentesKhan componentes = PecasDoJogo.criar(tabuleiro, new Random(0));
		Partida partida;
		try {
			partida = new Partida(tabuleiro, jogadores, componentes, new Random(0));
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Jogadores inválidos: " + e.getMessage(), e);
		}

		// Tabuleiro: yurts, cidades e províncias.
		for (Parada parada : tabuleiro.getParadas()) parada.restaurarYurts(List.of());
		for (Registro r : todos(registros, "YURTS")) {
			List<Cor> cores = new ArrayList<>();
			for (String cor : r.lista("cores")) cores.add(r.enumeracao(Cor.class, cor));
			try {
				parada(tabuleiro, r, "parada").restaurarYurts(cores);
			} catch (IllegalArgumentException e) {
				throw r.erro(e.getMessage());
			}
		}
		Map<Cidade, List<TipoTesouro>> tesourosCidades = new HashMap<>();
		for (Cidade cidade : tabuleiro.getCidades()) cidade.restaurar(false, 0, null);
		for (Registro r : todos(registros, "CIDADE")) {
			Cidade cidade = cidade(tabuleiro, r, r.texto("nome"));
			List<TipoTesouro> tesouros = new ArrayList<>();
			for (String tipo : r.lista("tesouros")) tesouros.add(r.enumeracao(TipoTesouro.class, tipo));
			boolean revelada = r.inteiro("revelada") == 1;
			String dono = r.texto("conquistador");
			try {
				cidade.restaurar(revelada, tesouros.size(), dono.equals(VAZIO) ? null : r.enumeracao(Cor.class, dono));
			} catch (IllegalArgumentException e) {
				throw r.erro(e.getMessage());
			}
			if (revelada) tesourosCidades.put(cidade, tesouros);
		}
		List<Cidade> fechadas = new ArrayList<>();
		Registro registroFechadas = unico(registros, "CIDADES_FECHADAS");
		for (String nome : registroFechadas.lista("ordem")) {
			Cidade cidade = cidade(tabuleiro, registroFechadas, nome);
			if (cidade.isRevelada()) throw registroFechadas.erro("a cidade " + cidade.getNome() + " já está revelada.");
			fechadas.add(cidade);
		}
		for (Registro r : todos(registros, "PROVINCIA")) {
			try {
				provincia(tabuleiro, r).restaurarPecas(r.inteiro("pecas"));
			} catch (IllegalArgumentException e) {
				throw r.erro(e.getMessage());
			}
		}
		Map<Parada, TipoBonus> bonusConselheiros = new HashMap<>();
		for (Registro r : todos(registros, "CONSELHEIRO")) {
			Parada parada = parada(tabuleiro, r, "parada");
			if (componentes.getConselheiro(parada.getNome()) == null) throw r.erro("não há conselheiro em " + parada.getNome() + ".");
			String bonus = r.texto("bonus");
			bonusConselheiros.put(parada, bonus.equals(VAZIO) ? null : r.enumeracao(TipoBonus.class, bonus));
		}

		// Jogadores: recursos, peões, cartelas e cartas secretas.
		Map<Jogador, Integer> tesourosRecebidos = new HashMap<>();
		Map<Jogador, Integer> linhasPremiadas = new HashMap<>();
		Map<Jogador, Inventario> inventarios = new HashMap<>();
		for (int i = 0; i < jogadores.size(); i++) {
			Registro r = registrosJogadores.get(i);
			Jogador jogador = jogadores.get(i);
			Set<Coluna> fichas = EnumSet.noneOf(Coluna.class);
			for (String coluna : r.lista("fichas")) fichas.add(r.enumeracao(Coluna.class, coluna));
			Map<TipoTributo, Integer> tributos = new EnumMap<>(TipoTributo.class);
			for (TipoTributo tipo : TipoTributo.values()) tributos.put(tipo, r.inteiro(tipo.name()));
			try {
				jogador.restaurar(r.inteiro("yurts"), fichas, tributos, r.inteiro("votos"));
			} catch (IllegalArgumentException e) {
				throw r.erro(e.getMessage());
			}
			tesourosRecebidos.put(jogador, r.naoNegativo("tesourosRecebidos"));
			linhasPremiadas.put(jogador, r.naoNegativo("linhasPremiadas"));
			inventarios.put(jogador, new Inventario());
		}
		boolean[][] peoes = new boolean[jogadores.size()][2];
		for (Registro r : todos(registros, "PEAO")) {
			Jogador jogador = jogador(jogadores, r);
			int p = r.inteiro("peao");
			if (p < 0 || p >= jogador.getPeoes().size()) throw r.erro("peão inexistente.");
			tabuleiro.posicionarPeao(jogador.getPeao(p), parada(tabuleiro, r, "parada"));
			peoes[jogadores.indexOf(jogador)][p] = true;
		}
		for (int i = 0; i < jogadores.size(); i++) {
			for (int p = 0; p < jogadores.get(i).getPeoes().size(); p++) {
				if (!peoes[i][p]) throw new IllegalArgumentException("Falta a posição do peão " + p + " do jogador " + i + ".");
			}
		}
		for (Registro r : todos(registros, "TESOUROS_JOGADOR")) {
			Inventario inv = inventarios.get(jogador(jogadores, r));
			for (TipoTesouro tipo : TipoTesouro.values()) for (int k = r.naoNegativo(tipo.name()); k > 0; k--) inv.receberTesouro(tipo);
		}
		for (Registro r : todos(registros, "BONUS_JOGADOR")) {
			Inventario inv = inventarios.get(jogador(jogadores, r));
			for (String item : r.lista("pecas")) {
				String[] par = r.par(item);
				TipoBonus bonus = r.enumeracao(TipoBonus.class, par[0]);
				if (!inv.receberBonus(bonus)) throw r.erro("bônus repetido.");
				if (par[1].equals("usado")) inv.usarBonus(bonus);
				else if (!par[1].equals("disponivel")) throw r.erro("use \"usado\" ou \"disponivel\".");
			}
		}
		for (Registro r : todos(registros, "MELHORIA_JOGADOR")) {
			Jogador jogador = jogador(jogadores, r);
			Coluna coluna = r.enumeracao(Coluna.class, r.texto("coluna"));
			Melhoria m = r.melhoria(r.texto("peca"));
			int turno = r.inteiro("turno");
			boolean livre = coluna == Coluna.BRANCA || jogador.getHerdeiro() == Herdeiro.JOCHI;
			if (!inventarios.get(jogador).adicionarMelhoria(m, coluna, Math.max(0, turno), livre) || turno < 0) {
				throw r.erro("melhoria não cabe na coluna " + coluna + " ou tem cor errada.");
			}
		}

		Baralho abertas = componentes.getBaralhoAbertas();
		Baralho secretas = componentes.getBaralhoSecretas();
		Map<String, Carta> cartasAbertas = porNome(abertas.todas());
		Map<String, Carta> cartasSecretas = porNome(secretas.todas());
		Map<Jogador, List<Carta>> maos = new HashMap<>();
		for (Registro r : todos(registros, "CARTA_SECRETA")) {
			maos.computeIfAbsent(jogador(jogadores, r), j -> new ArrayList<>()).add(carta(cartasSecretas, r, desescapar(r.texto("carta"))));
		}
		for (Jogador jogador : jogadores) {
			if (maos.getOrDefault(jogador, List.of()).size() != 1) {
				throw new IllegalArgumentException("Cada jogador precisa de exatamente 1 carta secreta: " + jogador.getNome() + ".");
			}
		}
		List<Carta> naMesa = cartasDoRegistro(registros, "CARTAS_ABERTAS", cartasAbertas);
		abertas.restaurar(cartasDoRegistro(registros, "PILHA_ABERTAS", cartasAbertas),
				cartasDoRegistro(registros, "DESCARTE_ABERTAS", cartasAbertas), Map.of(), naMesa);
		secretas.restaurar(cartasDoRegistro(registros, "PILHA_SECRETAS", cartasSecretas),
				cartasDoRegistro(registros, "DESCARTE_SECRETAS", cartasSecretas), maos, List.of());

		componentes.restaurar(inventarios, linhasPremiadas, melhoriasDoRegistro(registros, "PILHA_MELHORIAS"),
				melhoriasDoRegistro(registros, "MERCADO"), tesourosDoRegistro(registros, "PILHA_TESOUROS"),
				tesourosDoRegistro(registros, "DESCARTE_TESOUROS"), tesourosCidades, naMesa, bonusConselheiros);

		Registro e = unico(registros, "ESTOQUE");
		Map<TipoTributo, Integer> estoque = new EnumMap<>(TipoTributo.class);
		for (TipoTributo tipo : TipoTributo.values()) estoque.put(tipo, e.naoNegativo(tipo.name()));
		String khan = dadosPartida.texto("khan");
		if (!khan.equals(VAZIO) && !khan.equals(Partida.AREA_MELHORIAS)) {
			Provincia provincia = provinciaPorNome(tabuleiro, dadosPartida, desescapar(khan));
			if (!provincia.isProvinciaDoKhan()) throw dadosPartida.erro("o Khan só fica em província do Khan ou na área de melhorias.");
			khan = provincia.getNome();
		}
		try {
			partida.restaurar(dadosPartida.inteiro("vez"), dadosPartida.inteiro("rodada"), dadosPartida.inteiro("rodadaFinal"),
					dadosPartida.inteiro("kurultai"), khan.equals(VAZIO) ? null : khan, dadosPartida.inteiro("turno"),
					estoque, tesourosRecebidos, fechadas);
		} catch (IllegalArgumentException ex) {
			throw dadosPartida.erro(ex.getMessage());
		}
		return new Recuperada(partida, componentes);
	}

	// ================================================================ auxiliares

	/** Escreve em ASCII: letras, dígitos, espaço e pontuação simples ficam; o resto vira \\uXXXX. */
	static String escapar(String texto) {
		StringBuilder s = new StringBuilder();
		for (char c : texto.toCharArray()) {
			boolean simples = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
					|| " _-.()'!?".indexOf(c) >= 0;
			if (simples) s.append(c);
			else s.append(String.format("\\u%04x", (int) c));
		}
		return s.toString();
	}

	static String desescapar(String texto) {
		StringBuilder s = new StringBuilder();
		for (int i = 0; i < texto.length(); i++) {
			char c = texto.charAt(i);
			if (c == '\\') {
				if (i + 6 > texto.length() || texto.charAt(i + 1) != 'u') {
					throw new IllegalArgumentException("escape inválido em \"" + texto + "\"");
				}
				try {
					s.append((char) Integer.parseInt(texto.substring(i + 2, i + 6), 16));
				} catch (RuntimeException e) {
					throw new IllegalArgumentException("escape inválido em \"" + texto + "\"");
				}
				i += 5;
			} else s.append(c);
		}
		return s.toString();
	}

	private static String texto(String valor) { return valor == null ? VAZIO : escapar(valor); }
	private static String lista(List<String> itens) { return itens.isEmpty() ? VAZIO : String.join(",", itens); }
	private static String melhoria(Melhoria m) { return m.getCor().name() + ":" + m.getSimbolo().name(); }
	private static String melhorias(List<Melhoria> pecas) {
		List<String> itens = new ArrayList<>();
		for (Melhoria m : pecas) itens.add(melhoria(m));
		return lista(itens);
	}
	private static String tesouros(List<TipoTesouro> pecas) {
		List<String> itens = new ArrayList<>();
		for (TipoTesouro tipo : pecas) itens.add(tipo.name());
		return lista(itens);
	}
	private static String cartas(Collection<Carta> cartas) {
		List<String> itens = new ArrayList<>();
		for (Carta carta : cartas) itens.add(escapar(carta.getNome()));
		return lista(itens);
	}

	private static Map<String, Carta> porNome(List<Carta> cartas) {
		Map<String, Carta> mapa = new HashMap<>();
		for (Carta carta : cartas) mapa.put(carta.getNome(), carta);
		return mapa;
	}
	private static Carta carta(Map<String, Carta> cartas, Registro r, String nome) {
		Carta carta = cartas.get(nome);
		if (carta == null) throw r.erro("carta desconhecida: " + nome + ".");
		return carta;
	}
	private static List<Carta> cartasDoRegistro(Map<String, List<Registro>> registros, String tipo, Map<String, Carta> cartas) {
		Registro r = unico(registros, tipo);
		List<Carta> lista = new ArrayList<>();
		for (String nome : r.lista("cartas")) lista.add(carta(cartas, r, desescapar(nome)));
		return lista;
	}
	private static List<Melhoria> melhoriasDoRegistro(Map<String, List<Registro>> registros, String tipo) {
		Registro r = unico(registros, tipo);
		List<Melhoria> lista = new ArrayList<>();
		for (String item : r.lista("pecas")) lista.add(r.melhoria(item));
		return lista;
	}
	private static List<TipoTesouro> tesourosDoRegistro(Map<String, List<Registro>> registros, String tipo) {
		Registro r = unico(registros, tipo);
		List<TipoTesouro> lista = new ArrayList<>();
		for (String item : r.lista("pecas")) lista.add(r.enumeracao(TipoTesouro.class, item));
		return lista;
	}
	private static Jogador jogador(List<Jogador> jogadores, Registro r) {
		int i = r.inteiro("jogador");
		if (i < 0 || i >= jogadores.size()) throw r.erro("jogador inexistente: " + i + ".");
		return jogadores.get(i);
	}
	private static Parada parada(Tabuleiro tabuleiro, Registro r, String chave) {
		String nome = desescapar(r.texto(chave));
		if (!tabuleiro.existeParada(nome)) throw r.erro("parada desconhecida: " + nome + ".");
		return tabuleiro.getParada(nome);
	}
	private static Cidade cidade(Tabuleiro tabuleiro, Registro r, String nomeEscapado) {
		String nome = desescapar(nomeEscapado);
		for (Cidade cidade : tabuleiro.getCidades()) if (cidade.getNome().equals(nome)) return cidade;
		throw r.erro("cidade desconhecida: " + nome + ".");
	}
	private static Provincia provincia(Tabuleiro tabuleiro, Registro r) {
		return provinciaPorNome(tabuleiro, r, desescapar(r.texto("nome")));
	}
	private static Provincia provinciaPorNome(Tabuleiro tabuleiro, Registro r, String nome) {
		for (Provincia provincia : tabuleiro.getProvincias()) if (provincia.getNome().equals(nome)) return provincia;
		throw r.erro("província desconhecida: " + nome + ".");
	}
	private static Registro unico(Map<String, List<Registro>> registros, String tipo) {
		List<Registro> lista = registros.getOrDefault(tipo, List.of());
		if (lista.size() != 1) throw new IllegalArgumentException("O arquivo precisa de exatamente uma linha " + tipo + ".");
		return lista.get(0);
	}
	private static List<Registro> todos(Map<String, List<Registro>> registros, String tipo) {
		return new ArrayList<>(registros.getOrDefault(tipo, List.of()));
	}

	/** Uma linha do arquivo: tipo e campos chave=valor. */
	private static final class Registro {
		final String tipo;
		final int linha;
		private final Map<String, String> campos = new HashMap<>();

		Registro(String texto, int linha) {
			this.linha = linha;
			String[] partes = texto.split(";", -1);
			tipo = partes[0].strip();
			for (int i = 1; i < partes.length; i++) {
				int igual = partes[i].indexOf('=');
				if (igual <= 0) throw erro("campo sem \"chave=valor\": " + partes[i]);
				if (campos.put(partes[i].substring(0, igual).strip(), partes[i].substring(igual + 1).strip()) != null) {
					throw erro("campo repetido: " + partes[i]);
				}
			}
		}
		IllegalArgumentException erro(String motivo) {
			return new IllegalArgumentException("Linha " + linha + " (" + tipo + "): " + motivo);
		}
		String texto(String chave) {
			String valor = campos.get(chave);
			if (valor == null || valor.isEmpty()) throw erro("falta o campo " + chave + ".");
			return valor;
		}
		int inteiro(String chave) {
			try {
				return Integer.parseInt(texto(chave));
			} catch (NumberFormatException e) {
				throw erro("o campo " + chave + " deve ser um número.");
			}
		}
		int naoNegativo(String chave) {
			int valor = inteiro(chave);
			if (valor < 0) throw erro("o campo " + chave + " não pode ser negativo.");
			return valor;
		}
		List<String> lista(String chave) {
			String valor = texto(chave);
			List<String> itens = new ArrayList<>();
			if (valor.equals(VAZIO)) return itens;
			for (String item : valor.split(",", -1)) {
				if (item.strip().isEmpty()) throw erro("item vazio na lista " + chave + ".");
				itens.add(item.strip());
			}
			return itens;
		}
		String[] par(String item) {
			String[] par = item.split(":", -1);
			if (par.length != 2) throw erro("par inválido: " + item);
			return par;
		}
		<E extends Enum<E>> E enumeracao(Class<E> tipoEnum, String nome) {
			try {
				return Enum.valueOf(tipoEnum, nome);
			} catch (IllegalArgumentException e) {
				throw erro("valor desconhecido: " + nome + ".");
			}
		}
		Melhoria melhoria(String item) {
			String[] par = par(item);
			try {
				return new Melhoria(enumeracao(Coluna.class, par[0]), enumeracao(Melhoria.Simbolo.class, par[1]));
			} catch (IllegalArgumentException e) {
				throw erro("melhoria inválida: " + item + ".");
			}
		}
	}
}
