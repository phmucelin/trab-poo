package model;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/** Objetivos que contam yurts, conquistas, adjacências ou regiões. */
final class ObjetivoMapa extends Objetivo {
	enum Criterio { YURTS, YURTS_POR_REGIAO, CIDADES, REDE, PROVINCIAS_KHAN,
		CIDADES_COM_DOIS_YURTS, CIDADES_COM_UM_YURT, CONSELHEIROS, REGIOES_DESENVOLVIDAS }
	private final Criterio criterio;
	ObjetivoMapa(Criterio criterio, int... patamares) {
		super(patamares);
		if (criterio == null) throw new IllegalArgumentException("Critério obrigatório");
		this.criterio = criterio;
	}
	@Override String descreverCriterio() {
		switch (criterio) {
		case YURTS: return "Total de yurts, incluindo conquistas";
		case YURTS_POR_REGIAO: return "Yurts em cada região";
		case CIDADES: return "Cidades conquistadas";
		case REDE: return "Yurts na maior rede conectada";
		case PROVINCIAS_KHAN: return "Províncias do Khan com pelo menos dois yurts adjacentes";
		case CIDADES_COM_DOIS_YURTS: return "Cidades com pelo menos dois yurts adjacentes";
		case CIDADES_COM_UM_YURT: return "Cidades com pelo menos um yurt adjacente";
		case CONSELHEIROS: return "Paradas de conselheiro com yurt próprio, exceto Karakorum";
		case REGIOES_DESENVOLVIDAS: return "Regiões com pelo menos dois yurts e duas melhorias";
		default: throw new IllegalStateException("Critério desconhecido");
		}
	}

	@Override int progresso(Jogador jogador, Partida partida, Inventario inventario) {
		Tabuleiro tabuleiro = partida.getTabuleiro();
		int quantidade = 0;
		switch (criterio) {
		case YURTS:
			for (Parada parada : tabuleiro.getParadas()) quantidade += parada.contarYurts(jogador.getCor());
			return quantidade + conquistas(jogador, tabuleiro);
		case CIDADES: return conquistas(jogador, tabuleiro);
		case YURTS_POR_REGIAO:
			quantidade = Integer.MAX_VALUE;
			for (Regiao regiao : Regiao.values()) quantidade = Math.min(quantidade, tabuleiro.contarYurts(jogador, regiao));
			return quantidade;
		case REDE: return maiorRede(jogador, tabuleiro);
		case PROVINCIAS_KHAN:
			for (Provincia provincia : tabuleiro.provinciasDoKhan()) {
				if (yurtsAdjacentes(jogador, provincia.getParadasAdjacentes()) >= 2) quantidade++;
			}
			return quantidade;
		case CIDADES_COM_DOIS_YURTS:
		case CIDADES_COM_UM_YURT:
			int minimo = criterio == Criterio.CIDADES_COM_DOIS_YURTS ? 2 : 1;
			for (Cidade cidade : tabuleiro.getCidades()) {
				if (yurtsAdjacentes(jogador, cidade.getParadasAdjacentes()) >= minimo) quantidade++;
			}
			return quantidade;
		case CONSELHEIROS:
			for (Parada parada : tabuleiro.paradasDeConselheiro()) {
				if (!parada.isKarakorum() && parada.temYurtDe(jogador.getCor())) quantidade++;
			}
			return quantidade;
		case REGIOES_DESENVOLVIDAS:
			for (Regiao regiao : Regiao.values()) {
				if (tabuleiro.contarYurts(jogador, regiao) >= 2 && inventario.contarMelhorias(regiao.getColuna()) >= 2) quantidade++;
			}
			return quantidade;
		default: throw new IllegalStateException("Critério desconhecido");
		}
	}
	private int conquistas(Jogador jogador, Tabuleiro tabuleiro) {
		int n = 0;
		for (Cidade cidade : tabuleiro.getCidades()) if (cidade.getConquistadaPor() == jogador.getCor()) n++;
		return n;
	}
	private int yurtsAdjacentes(Jogador jogador, Set<Parada> paradas) {
		int n = 0;
		for (Parada parada : paradas) n += parada.contarYurts(jogador.getCor());
		return n;
	}
	/** Conta peças, inclusive duas na mesma parada; cidade não é nó de rota. */
	private int maiorRede(Jogador jogador, Tabuleiro tabuleiro) {
		Set<Parada> restantes = new HashSet<>(tabuleiro.paradasComYurtDe(jogador));
		int maior = 0;
		while (!restantes.isEmpty()) {
			ArrayDeque<Parada> fila = new ArrayDeque<>();
			Parada inicio = restantes.iterator().next();
			restantes.remove(inicio);
			fila.add(inicio);
			int tamanho = 0;
			while (!fila.isEmpty()) {
				Parada parada = fila.removeFirst();
				tamanho += parada.contarYurts(jogador.getCor());
				for (Parada vizinha : tabuleiro.vizinhas(parada)) if (restantes.remove(vizinha)) fila.add(vizinha);
			}
			maior = Math.max(maior, tamanho);
		}
		return maior;
	}
}
