package model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Formato texto (ASCII puro) para salvar e recuperar uma partida, 4ª iteração.
 *
 * Só se salva antes de o jogador da vez começar a jogar, então o Turno não
 * entra no arquivo: ele é sempre o de começo de turno. Cada linha é
 * "CHAVE;campo;campo;...". Listas usam "," e pares usam ":". Todo texto livre
 * (nomes de jogador, de parada, de cidade) passa por escapar(), que troca
 * qualquer caractere fora do ASCII visível, e também \ ; , :, por \\uXXXX.
 * "-" representa lista vazia ou valor ausente.
 *
 * Esta classe não conhece arquivos: recebe e devolve linhas.
 */
final class SalvamentoPartida {
	static final String CABECALHO = "HERDEIROS_DE_KHAN";
	static final int VERSAO = 1;
	private static final String VAZIO = "-";
	private static final int TOTAL_ESPADAS = 10, TOTAL_YURTS = 10, TOTAL_MOEDAS = 15;
	private static final int TOTAL_MELHORIAS = 44, TOTAL_TESOUROS = 40;

	private SalvamentoPartida() { }

	/** Partida e componentes novos, prontos para substituir os atuais. */
	static final class Restaurado {
		final Partida partida;
		final ComponentesKhan componentes;
		Restaurado(Partida partida, ComponentesKhan componentes) {
			this.partida = partida;
			this.componentes = componentes;
		}
	}

	// ================================================================= texto

	static String escapar(String texto) {
		StringBuilder s = new StringBuilder();
		for (char c : texto.toCharArray()) {
			if (c < 33 || c > 126 || c == '\\' || c == ';' || c == ',' || c == ':' || (c == '-' && texto.length() == 1)) {
				s.append(String.format("\\u%04X", (int) c));
			} else {
				s.append(c);
			}
		}
		return s.toString();
	}

	static String desescapar(String texto) {
		StringBuilder s = new StringBuilder();
		for (int i = 0; i < texto.length(); i++) {
			char c = texto.charAt(i);
			if (c != '\\') {
				s.append(c);
				continue;
			}
			if (i + 6 > texto.length() || texto.charAt(i + 1) != 'u') {
				throw new IllegalArgumentException("Escape inválido: " + texto);
			}
			try {
				s.append((char) Integer.parseInt(texto.substring(i + 2, i + 6), 16));
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException("Escape inválido: " + texto);
			}
			i += 5;
		}
		return s.toString();
	}

	private static String linha(Object... campos) {
		StringBuilder s = new StringBuilder();
		for (int i = 0; i < campos.length; i++) {
			if (i > 0) s.append(';');
			s.append(campos[i]);
		}
		return s.toString();
	}

	private static String lista(Collection<String> itens) {
		if (itens.isEmpty()) return VAZIO;
		StringBuilder s = new StringBuilder();
		for (String item : itens) {
			if (s.length() > 0) s.append(',');
			s.append(item);
		}
		return s.toString();
	}

	private static <E extends Enum<E>> String mapa(Map<E, Integer> valores) {
		List<String> itens = new ArrayList<>();
		for (Map.Entry<E, Integer> e : valores.entrySet()) itens.add(e.getKey().name() + ":" + e.getValue());
		return lista(itens);
	}

	private static List<String> ids(List<Carta> cartas) {
		List<String> ids = new ArrayList<>();
		for (Carta c : cartas) ids.add(c.getId());
		return ids;
	}

	private static String idMelhoria(Melhoria m) { return m.getCor().name() + "_" + m.getSimbolo().name(); }

	// ================================================================ gravar

	/** Linhas ASCII da partida; só vale quando Partida.podeSalvar() é verdadeiro. */
	static List<String> gravar(Partida partida, ComponentesKhan componentes) {
		if (!partida.podeSalvar()) throw new IllegalStateException("Só é possível salvar antes do jogador começar a jogar");
		ComponentesKhan.Dados d = componentes.exportar();
		Tabuleiro tabuleiro = partida.getTabuleiro();
		List<Jogador> jogadores = partida.getJogadores();
		List<String> out = new ArrayList<>();
		out.add(linha(CABECALHO, VERSAO));
		out.add(linha("PARTIDA", partida.getRodada(), partida.getRodadaFinal(), partida.getIndiceJogador(),
				partida.getTurno().getNumero(), partida.getPosicaoKurultai(),
				partida.getPosicaoKhan() == null ? VAZIO : escapar(partida.getPosicaoKhan())));
		Map<TipoTributo, Integer> estoque = new EnumMap<>(TipoTributo.class);
		for (TipoTributo t : TipoTributo.values()) estoque.put(t, partida.getEstoque(t));
		out.add(linha("ESTOQUE", mapa(estoque)));
		for (int i = 0; i < jogadores.size(); i++) {
			Jogador j = jogadores.get(i);
			List<String> fichas = new ArrayList<>();
			for (Coluna c : Coluna.values()) if (j.temFichaDisponivel(c)) fichas.add(c.name());
			out.add(linha("JOGADOR", escapar(j.getNome()), j.getHerdeiro().name(), j.getYurtsDisponiveis(), j.getVotos(),
					partida.getTesourosRecebidos(j), lista(fichas), mapa(new EnumMap<>(j.getTributos()))));
			for (Peao peao : j.getPeoes()) out.add(linha("PEAO", i, peao.getIndice(), escapar(tabuleiro.posicaoDe(peao).getNome())));
		}
		List<Parada> paradas = new ArrayList<>(tabuleiro.getParadas());
		paradas.sort((a, b) -> a.getNome().compareTo(b.getNome()));
		for (Parada p : paradas) {
			if (p.contarYurts() == 0) continue;
			List<String> cores = new ArrayList<>();
			for (Cor c : p.getYurts()) cores.add(c.name());
			out.add(linha("YURT", escapar(p.getNome()), lista(cores)));
		}
		List<Cidade> cidades = new ArrayList<>(tabuleiro.getCidades());
		cidades.sort((a, b) -> a.getNome().compareTo(b.getNome()));
		for (Cidade c : cidades) {
			out.add(linha("CIDADE", escapar(c.getNome()), c.isRevelada() ? 1 : 0, c.getTesouros(),
					c.getConquistadaPor() == null ? VAZIO : c.getConquistadaPor().name()));
		}
		List<String> fechadas = new ArrayList<>();
		for (Cidade c : partida.getCidadesFechadas()) fechadas.add(escapar(c.getNome()));
		out.add(linha("FECHADAS", lista(fechadas)));
		List<Provincia> provincias = new ArrayList<>(tabuleiro.getProvincias());
		provincias.sort((a, b) -> a.getNome().compareTo(b.getNome()));
		for (Provincia p : provincias) out.add(linha("PROVINCIA", escapar(p.getNome()), p.getPecas()));
		for (int i = 0; i < jogadores.size(); i++) {
			Jogador j = jogadores.get(i);
			Inventario inv = d.inventarios.get(j);
			Map<TipoTesouro, Integer> tesouros = new EnumMap<>(TipoTesouro.class);
			for (TipoTesouro t : TipoTesouro.values()) tesouros.put(t, inv.contarTesouro(t));
			Map<TipoBonus, Integer> bonus = new EnumMap<>(TipoBonus.class);
			for (Map.Entry<TipoBonus, Boolean> e : inv.getBonus().entrySet()) bonus.put(e.getKey(), e.getValue() ? 1 : 0);
			out.add(linha("INVENTARIO", i, mapa(tesouros), mapa(bonus)));
			for (Coluna coluna : Coluna.values()) {
				List<String> itens = new ArrayList<>();
				for (Melhoria m : inv.getMelhorias(coluna)) itens.add(idMelhoria(m) + ":" + inv.turnoDeAquisicao(m));
				if (!itens.isEmpty()) out.add(linha("MELHORIAS", i, coluna.name(), lista(itens)));
			}
			out.add(linha("LINHAS", i, d.linhasPremiadas.get(j)));
			out.add(linha("MAO", i, lista(ids(d.maos.get(j)))));
		}
		out.add(linha("CARTAS_ABERTAS", lista(ids(d.cartasAbertas))));
		out.add(linha("ABERTAS_PILHA", lista(ids(d.abertasPilha))));
		out.add(linha("ABERTAS_DESCARTE", lista(ids(d.abertasDescarte))));
		out.add(linha("SECRETAS_PILHA", lista(ids(d.secretasPilha))));
		out.add(linha("SECRETAS_DESCARTE", lista(ids(d.secretasDescarte))));
		out.add(linha("PILHA_MELHORIAS", lista(idsMelhorias(d.pilhaMelhorias))));
		out.add(linha("MERCADO", lista(idsMelhorias(d.mercado))));
		out.add(linha("PILHA_TESOUROS", lista(nomes(d.pilhaTesouros))));
		out.add(linha("DESCARTE_TESOUROS", lista(nomes(d.descarteTesouros))));
		for (Cidade c : cidades) {
			List<TipoTesouro> pecas = d.tesourosCidades.get(c);
			if (pecas != null) out.add(linha("TESOUROS_CIDADE", escapar(c.getNome()), lista(nomes(pecas))));
		}
		List<String> paradasComBonus = new ArrayList<>(d.bonus.keySet());
		paradasComBonus.sort(null);
		for (String nome : paradasComBonus) out.add(linha("BONUS_CONSELHEIRO", escapar(nome), d.bonus.get(nome).name()));
		return out;
	}

	private static List<String> idsMelhorias(List<Melhoria> melhorias) {
		List<String> ids = new ArrayList<>();
		for (Melhoria m : melhorias) ids.add(idMelhoria(m));
		return ids;
	}

	private static List<String> nomes(List<TipoTesouro> tipos) {
		List<String> nomes = new ArrayList<>();
		for (TipoTesouro t : tipos) nomes.add(t.name());
		return nomes;
	}

	// ================================================================== ler

	/** Linhas do arquivo agrupadas por chave, na ordem em que apareceram. */
	private static final class Arquivo {
		final Map<String, List<String[]>> porChave = new LinkedHashMap<>();
		final Map<String[], Integer> numeroDaLinha = new HashMap<>();

		String[] um(String chave, int campos) {
			List<String[]> l = porChave.get(chave);
			if (l == null || l.size() != 1) throw new IllegalArgumentException("Esperava exatamente uma linha " + chave + ".");
			return conferir(l.get(0), chave, campos);
		}
		List<String[]> varias(String chave) {
			List<String[]> l = porChave.get(chave);
			return l == null ? new ArrayList<String[]>() : l;
		}
		String[] conferir(String[] campos, String chave, int esperados) {
			if (campos.length != esperados) {
				throw new IllegalArgumentException("Linha " + numeroDaLinha.get(campos) + ": " + chave
						+ " precisa de " + esperados + " campos.");
			}
			return campos;
		}
	}

	private static Arquivo separar(List<String> linhas) {
		if (linhas == null || linhas.isEmpty()) throw new IllegalArgumentException("Arquivo vazio.");
		Arquivo a = new Arquivo();
		for (int i = 0; i < linhas.size(); i++) {
			String bruta = linhas.get(i);
			if (bruta == null) throw new IllegalArgumentException("Linha " + (i + 1) + " inválida.");
			String linha = bruta.strip();
			if (linha.isEmpty() || linha.startsWith("#")) continue;
			for (char c : linha.toCharArray()) {
				if (c > 126 || c < 32) throw new IllegalArgumentException("Linha " + (i + 1) + ": só ASCII é aceito.");
			}
			String[] partes = linha.split(";", -1);
			String[] campos = new String[partes.length - 1];
			System.arraycopy(partes, 1, campos, 0, campos.length);
			a.numeroDaLinha.put(campos, i + 1);
			a.porChave.computeIfAbsent(partes[0], k -> new ArrayList<>()).add(campos);
		}
		return a;
	}

	private static void exigir(boolean condicao, String mensagem) {
		if (!condicao) throw new IllegalArgumentException(mensagem);
	}

	private static int inteiro(String campo) {
		try {
			return Integer.parseInt(campo);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Número inválido: " + campo);
		}
	}

	private static <E extends Enum<E>> E enumerar(Class<E> tipo, String nome) {
		try {
			return Enum.valueOf(tipo, nome);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Valor desconhecido: " + nome);
		}
	}

	private static List<String> itens(String campo) {
		List<String> l = new ArrayList<>();
		if (!campo.equals(VAZIO)) for (String s : campo.split(",", -1)) l.add(s);
		return l;
	}

	private static <E extends Enum<E>> Map<E, Integer> lerMapa(Class<E> tipo, String campo) {
		Map<E, Integer> m = new EnumMap<>(tipo);
		for (String item : itens(campo)) {
			String[] par = item.split(":", -1);
			exigir(par.length == 2, "Par inválido: " + item);
			exigir(m.put(enumerar(tipo, par[0]), inteiro(par[1])) == null, "Chave repetida: " + par[0]);
		}
		return m;
	}

	private static Melhoria lerMelhoria(String id) {
		int corte = id.indexOf('_');
		exigir(corte > 0, "Melhoria inválida: " + id);
		Coluna cor = enumerar(Coluna.class, id.substring(0, corte));
		exigir(cor != Coluna.BRANCA, "Melhoria sem cor: " + id);
		return new Melhoria(cor, enumerar(Melhoria.Simbolo.class, id.substring(corte + 1)));
	}

	private static List<Melhoria> lerMelhorias(String campo) {
		List<Melhoria> l = new ArrayList<>();
		for (String id : itens(campo)) l.add(lerMelhoria(id));
		return l;
	}

	private static List<TipoTesouro> lerTesouros(String campo) {
		List<TipoTesouro> l = new ArrayList<>();
		for (String nome : itens(campo)) l.add(enumerar(TipoTesouro.class, nome));
		return l;
	}

	private static List<Carta> lerCartas(Map<String, Carta> catalogo, Set<Carta> usadas, String campo) {
		List<Carta> l = new ArrayList<>();
		for (String id : itens(campo)) {
			Carta c = catalogo.get(id);
			exigir(c != null, "Carta desconhecida: " + id);
			exigir(usadas.add(c), "Carta repetida: " + id);
			l.add(c);
		}
		return l;
	}

	/**
	 * Lê e valida as linhas. Nada do que já existe é alterado: o resultado só
	 * contém objetos novos. Lança IllegalArgumentException com a causa.
	 */
	static Restaurado ler(List<String> linhas) {
		Arquivo a = separar(linhas);
		exigir(linhas.get(0).strip().equals(linha(CABECALHO, VERSAO)), "Não é uma partida salva de Herdeiros de Khan (versão " + VERSAO + ").");

		// ---- jogadores
		List<String[]> js = a.varias("JOGADOR");
		exigir(js.size() >= 2 && js.size() <= Cor.values().length, "A partida precisa de 2 a 5 jogadores.");
		List<Jogador> jogadores = new ArrayList<>();
		Set<Herdeiro> herdeiros = new HashSet<>();
		Map<Jogador, Integer> recebidos = new HashMap<>();
		for (int i = 0; i < js.size(); i++) {
			String[] f = a.conferir(js.get(i), "JOGADOR", 7);
			Herdeiro h = enumerar(Herdeiro.class, f[1]);
			exigir(herdeiros.add(h), "Herdeiro repetido: " + f[1]);
			Jogador j = new Jogador(desescapar(f[0]), Cor.values()[i], h);
			Set<Coluna> fichas = EnumSet.noneOf(Coluna.class);
			for (String c : itens(f[5])) exigir(fichas.add(enumerar(Coluna.class, c)), "Ficha repetida.");
			j.restaurar(inteiro(f[2]), fichas, lerMapa(TipoTributo.class, f[6]), inteiro(f[3]));
			exigir(inteiro(f[4]) >= 0, "Tesouros recebidos negativos.");
			recebidos.put(j, inteiro(f[4]));
			jogadores.add(j);
		}

		// ---- partida
		String[] p = a.um("PARTIDA", 6);
		Partida.Estado estado = new Partida.Estado();
		estado.rodada = inteiro(p[0]);
		estado.rodadaFinal = inteiro(p[1]);
		estado.vez = inteiro(p[2]);
		estado.turno = inteiro(p[3]);
		estado.kurultai = inteiro(p[4]);
		estado.khan = p[5].equals(VAZIO) ? null : desescapar(p[5]);
		exigir(estado.rodada >= 1 && estado.turno >= 1 && estado.kurultai >= 0, "Rodada, turno ou Kurultai inválidos.");
		exigir(estado.vez >= 0 && estado.vez < jogadores.size(), "Jogador da vez inexistente.");
		exigir(estado.rodadaFinal == -1 || estado.rodadaFinal >= estado.rodada, "Rodada final inválida.");
		estado.estoque.putAll(lerMapa(TipoTributo.class, a.um("ESTOQUE", 1)[0]));
		for (TipoTributo t : TipoTributo.values()) exigir(estado.estoque.getOrDefault(t, -1) >= 0, "Estoque ausente ou negativo: " + t);
		estado.tesourosRecebidos.putAll(recebidos);

		// ---- tabuleiro
		Tabuleiro tabuleiro = MapaOficial.criar();
		for (String[] f : a.varias("PEAO")) {
			a.conferir(f, "PEAO", 3);
			int ij = inteiro(f[0]), ip = inteiro(f[1]);
			exigir(ij >= 0 && ij < jogadores.size() && ip >= 0 && ip < jogadores.get(ij).getPeoes().size(), "Peão inexistente.");
			String nome = desescapar(f[2]);
			exigir(tabuleiro.existeParada(nome), "Parada inexistente: " + nome);
			Peao peao = jogadores.get(ij).getPeao(ip);
			exigir(tabuleiro.posicaoDe(peao) == null, "Peão repetido.");
			tabuleiro.posicionarPeao(peao, tabuleiro.getParada(nome));
		}
		for (Jogador j : jogadores) for (Peao peao : j.getPeoes()) exigir(tabuleiro.posicaoDe(peao) != null, "Peão sem posição: " + j.getNome());
		for (String[] f : a.varias("YURT")) {
			a.conferir(f, "YURT", 2);
			String nome = desescapar(f[0]);
			exigir(tabuleiro.existeParada(nome), "Parada inexistente: " + nome);
			Parada parada = tabuleiro.getParada(nome);
			for (String cor : itens(f[1])) parada.colocarYurt(enumerar(Cor.class, cor), true);
			exigir(parada.isKarakorum() || parada.contarYurts() <= parada.getCapacidadeYurts(), "Yurts demais em " + nome);
		}
		Set<String> reveladas = new HashSet<>();
		Set<String> vistas = new HashSet<>();
		for (String[] f : a.varias("CIDADE")) {
			a.conferir(f, "CIDADE", 4);
			String nome = desescapar(f[0]);
			exigir(tabuleiro.getCidades().stream().anyMatch(c -> c.getNome().equals(nome)), "Cidade inexistente: " + nome);
			exigir(vistas.add(nome), "Cidade repetida: " + nome);
			boolean rev = inteiro(f[1]) == 1;
			if (rev) reveladas.add(nome);
			tabuleiro.getCidade(nome).restaurar(rev, inteiro(f[2]), f[3].equals(VAZIO) ? null : enumerar(Cor.class, f[3]));
		}
		exigir(vistas.size() == tabuleiro.getCidades().size(), "Faltam cidades.");
		Set<String> fechadasNomes = new HashSet<>();
		for (String nome : itens(a.um("FECHADAS", 1)[0])) {
			nome = desescapar(nome);
			exigir(vistas.contains(nome) && !reveladas.contains(nome) && fechadasNomes.add(nome), "Cidade fechada inválida: " + nome);
			estado.cidadesFechadas.add(tabuleiro.getCidade(nome));
		}
		exigir(fechadasNomes.size() + reveladas.size() == vistas.size(), "Cidades fechadas inconsistentes.");
		vistas.clear();
		for (String[] f : a.varias("PROVINCIA")) {
			a.conferir(f, "PROVINCIA", 2);
			String nome = desescapar(f[0]);
			exigir(tabuleiro.getProvincias().stream().anyMatch(x -> x.getNome().equals(nome)) && vistas.add(nome), "Província inválida: " + nome);
			tabuleiro.getProvincia(nome).restaurarPecas(inteiro(f[1]));
		}
		exigir(vistas.size() == tabuleiro.getProvincias().size(), "Faltam províncias.");
		exigir(estado.khan == null || Partida.AREA_MELHORIAS.equals(estado.khan)
				|| (tabuleiro.getProvincia(estado.khan) != null && tabuleiro.getProvincia(estado.khan).isProvinciaDoKhan()), "Posição do Khan inválida.");

		// ---- componentes
		ComponentesKhan componentes = PecasDoJogo.criar(tabuleiro, new Random());
		ComponentesKhan.Dados d = new ComponentesKhan.Dados();
		Map<String, Carta> catalogo = new HashMap<>();
		for (Carta c : CartasOficiais.abertas()) catalogo.put(c.getId(), c);
		for (Carta c : CartasOficiais.secretas()) catalogo.put(c.getId(), c);
		Set<Carta> usadas = new HashSet<>();
		d.cartasAbertas = lerCartas(catalogo, usadas, a.um("CARTAS_ABERTAS", 1)[0]);
		d.abertasPilha = lerCartas(catalogo, usadas, a.um("ABERTAS_PILHA", 1)[0]);
		d.abertasDescarte = lerCartas(catalogo, usadas, a.um("ABERTAS_DESCARTE", 1)[0]);
		d.secretasPilha = lerCartas(catalogo, usadas, a.um("SECRETAS_PILHA", 1)[0]);
		d.secretasDescarte = lerCartas(catalogo, usadas, a.um("SECRETAS_DESCARTE", 1)[0]);
		exigir(d.cartasAbertas.size() <= 4, "Cartas abertas demais.");
		for (int i = 0; i < jogadores.size(); i++) {
			Jogador j = jogadores.get(i);
			d.inventarios.put(j, new Inventario());
			d.maos.put(j, new ArrayList<Carta>());
		}
		for (String[] f : a.varias("MAO")) {
			a.conferir(f, "MAO", 2);
			Jogador j = jogador(jogadores, f[0]);
			List<Carta> mao = lerCartas(catalogo, usadas, f[1]);
			exigir(mao.size() == 1 && mao.get(0).isSecreta(), "Cada jogador deve ter uma carta secreta: " + j.getNome());
			d.maos.get(j).addAll(mao);
		}
		for (Jogador j : jogadores) exigir(d.maos.get(j).size() == 1, "Falta a carta secreta de " + j.getNome());
		exigir(usadas.size() == catalogo.size(), "O baralho de conselheiros está incompleto.");
		for (Carta c : d.abertasPilha) exigir(!c.isSecreta(), "Carta secreta no baralho aberto.");
		for (Carta c : d.abertasDescarte) exigir(!c.isSecreta(), "Carta secreta no baralho aberto.");
		for (Carta c : d.cartasAbertas) exigir(!c.isSecreta(), "Carta secreta entre as abertas.");
		for (Carta c : d.secretasPilha) exigir(c.isSecreta(), "Carta aberta no baralho secreto.");
		for (Carta c : d.secretasDescarte) exigir(c.isSecreta(), "Carta aberta no baralho secreto.");

		int melhorias = 0, tesouros = 0;
		Set<TipoBonus> bonusVistos = new HashSet<>();
		for (String[] f : a.varias("INVENTARIO")) {
			a.conferir(f, "INVENTARIO", 3);
			Inventario inv = d.inventarios.get(jogador(jogadores, f[0]));
			for (Map.Entry<TipoTesouro, Integer> e : lerMapa(TipoTesouro.class, f[1]).entrySet()) {
				exigir(e.getValue() >= 0, "Tesouro negativo.");
				for (int n = 0; n < e.getValue(); n++) inv.receberTesouro(e.getKey());
				tesouros += e.getValue();
			}
			for (Map.Entry<TipoBonus, Integer> e : lerMapa(TipoBonus.class, f[2]).entrySet()) {
				exigir(bonusVistos.add(e.getKey()) && inv.receberBonus(e.getKey()), "Bônus repetido: " + e.getKey());
				if (e.getValue() == 1) inv.usarBonus(e.getKey());
				else exigir(e.getValue() == 0, "Bônus inválido.");
			}
		}
		for (String[] f : a.varias("MELHORIAS")) {
			a.conferir(f, "MELHORIAS", 3);
			Inventario inv = d.inventarios.get(jogador(jogadores, f[0]));
			Coluna coluna = enumerar(Coluna.class, f[1]);
			for (String item : itens(f[2])) {
				String[] par = item.split(":", -1);
				exigir(par.length == 2, "Melhoria inválida: " + item);
				exigir(inv.adicionarMelhoria(lerMelhoria(par[0]), coluna, inteiro(par[1]), true), "Coluna cheia ou melhoria inválida.");
				melhorias++;
			}
		}
		for (String[] f : a.varias("LINHAS")) {
			a.conferir(f, "LINHAS", 2);
			int n = inteiro(f[1]);
			exigir(n >= 0 && n <= Inventario.LIMITE_POR_COLUNA, "Linhas premiadas inválidas.");
			d.linhasPremiadas.put(jogador(jogadores, f[0]), n);
		}
		for (Jogador j : jogadores) exigir(d.linhasPremiadas.containsKey(j), "Faltam LINHAS de " + j.getNome());
		d.pilhaMelhorias = lerMelhorias(a.um("PILHA_MELHORIAS", 1)[0]);
		d.mercado = lerMelhorias(a.um("MERCADO", 1)[0]);
		exigir(d.mercado.size() <= 5, "Mercado com peças demais.");
		exigir(melhorias + d.pilhaMelhorias.size() + d.mercado.size() == TOTAL_MELHORIAS, "O total de melhorias deve ser " + TOTAL_MELHORIAS + ".");
		d.pilhaTesouros = lerTesouros(a.um("PILHA_TESOUROS", 1)[0]);
		d.descarteTesouros = lerTesouros(a.um("DESCARTE_TESOUROS", 1)[0]);
		tesouros += d.pilhaTesouros.size() + d.descarteTesouros.size();
		for (String[] f : a.varias("TESOUROS_CIDADE")) {
			a.conferir(f, "TESOUROS_CIDADE", 2);
			String nome = desescapar(f[0]);
			exigir(reveladas.contains(nome), "Cidade sem tesouros revelados: " + nome);
			Cidade cidade = tabuleiro.getCidade(nome);
			List<TipoTesouro> pecas = lerTesouros(f[1]);
			exigir(d.tesourosCidades.put(cidade, pecas) == null, "Cidade repetida: " + nome);
			exigir(pecas.size() == cidade.getTesouros(), "Tesouros de " + nome + " divergem do contador.");
			tesouros += pecas.size();
		}
		exigir(d.tesourosCidades.size() == reveladas.size(), "Falta a lista de tesouros de uma cidade revelada.");
		exigir(tesouros == TOTAL_TESOUROS, "O total de tesouros deve ser " + TOTAL_TESOUROS + ".");
		for (String[] f : a.varias("BONUS_CONSELHEIRO")) {
			a.conferir(f, "BONUS_CONSELHEIRO", 2);
			String nome = desescapar(f[0]);
			exigir(componentes.getConselheiro(nome) != null, "Conselheiro inexistente: " + nome);
			TipoBonus bonus = enumerar(TipoBonus.class, f[1]);
			exigir(bonusVistos.add(bonus) && d.bonus.put(nome, bonus) == null, "Bônus repetido: " + f[1]);
		}
		exigir(bonusVistos.size() == TipoBonus.values().length, "Os " + TipoBonus.values().length + " bônus devem aparecer uma vez.");

		// ---- conservação das peças e dos yurts
		int[] total = new int[TipoTributo.values().length];
		for (TipoTributo t : TipoTributo.values()) total[t.ordinal()] = estado.estoque.get(t);
		for (Jogador j : jogadores) for (TipoTributo t : TipoTributo.values()) total[t.ordinal()] += j.contarTributo(t);
		for (Provincia pv : tabuleiro.getProvincias()) total[pv.getTributo().ordinal()] += pv.getPecas();
		exigir(total[TipoTributo.ESPADA.ordinal()] == TOTAL_ESPADAS && total[TipoTributo.YURT.ordinal()] == TOTAL_YURTS
				&& total[TipoTributo.MOEDA.ordinal()] == TOTAL_MOEDAS, "As peças de tributo não somam o total do jogo.");
		for (Jogador j : jogadores) {
			int fora = j.getYurtsDisponiveis();
			for (Parada pa : tabuleiro.getParadas()) fora += pa.contarYurts(j.getCor());
			for (Cidade c : tabuleiro.getCidades()) if (c.getConquistadaPor() == j.getCor()) fora++;
			exigir(fora == Jogador.YURTS_INICIAIS, "Os yurts de " + j.getNome() + " não somam " + Jogador.YURTS_INICIAIS + ".");
		}
		for (Cor cor : Cor.values()) {
			if (cor.ordinal() < jogadores.size()) continue;
			for (Parada pa : tabuleiro.getParadas()) exigir(!pa.temYurtDe(cor), "Yurt de cor sem jogador.");
		}
		return new Restaurado(Partida.restaurar(tabuleiro, jogadores, componentes, estado, d), componentes);
	}

	private static Jogador jogador(List<Jogador> jogadores, String indice) {
		int i = inteiro(indice);
		exigir(i >= 0 && i < jogadores.size(), "Jogador inexistente: " + indice);
		return jogadores.get(i);
	}
}
