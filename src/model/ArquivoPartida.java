package model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Salvamento de partidas (4ª iteração).
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
}
