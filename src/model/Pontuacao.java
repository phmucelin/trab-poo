package model;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Influência regional e desempates do manual, pp. 15-16. */
final class Pontuacao {
	/** Votos por número de melhorias na coluna (0 a 4), tabela do tabuleiro (p. 15). */
	private static final List<Integer> PRIMEIRO = List.of(1, 1, 2, 2, 3);
	private static final List<Integer> SEGUNDO = List.of(0, 1, 1, 2, 2);
	private Pontuacao() { }
	static int votosPorMelhorias(int melhorias, boolean primeiro) {
		if (melhorias < 0 || melhorias > 4) throw new IllegalArgumentException("Coluna comporta zero a quatro melhorias");
		return (primeiro ? PRIMEIRO : SEGUNDO).get(melhorias);
	}
	static int influencia(Jogador jogador, Tabuleiro tabuleiro, Inventario inventario) {
		int votos = 0;
		for (Regiao regiao : Regiao.values()) {
			int melhorias = inventario.contarMelhorias(regiao.getColuna());
			if (tabuleiro.lideresEmYurts(regiao).contains(jogador.getCor())) votos += votosPorMelhorias(melhorias, true);
			else if (tabuleiro.segundosEmYurts(regiao).contains(jogador.getCor())) votos += votosPorMelhorias(melhorias, false);
		}
		return votos;
	}
	static List<Jogador> vencedores(Partida partida, Map<Jogador, Inventario> inventarios) {
		if (partida.getFase() != Fase.ENCERRADA) return List.of();
		List<Jogador> resultado = new ArrayList<>();
		int maiorVoto = -1, maiorTesouro = -1, menorDistancia = Integer.MAX_VALUE;
		for (Jogador jogador : partida.getJogadores()) {
			int votos = jogador.getVotos();
			int tesouros = inventarios.get(jogador).totalTesouros();
			int distancia = distanciaDaCapital(jogador, partida.getTabuleiro());
			boolean melhor = votos > maiorVoto || (votos == maiorVoto && (tesouros > maiorTesouro
					|| (tesouros == maiorTesouro && distancia < menorDistancia)));
			if (melhor) {
				resultado.clear();
				maiorVoto = votos; maiorTesouro = tesouros; menorDistancia = distancia;
			}
			if (votos == maiorVoto && tesouros == maiorTesouro && distancia == menorDistancia) resultado.add(jogador);
		}
		return List.copyOf(resultado);
	}
	/** Distância em rotas; para Altani considera o peão mais próximo. */
	static int distanciaDaCapital(Jogador jogador, Tabuleiro tabuleiro) {
		Set<Parada> visitadas = new HashSet<>();
		ArrayDeque<Parada> fila = new ArrayDeque<>();
		fila.add(tabuleiro.getKarakorum()); visitadas.add(tabuleiro.getKarakorum());
		int distancia = 0;
		while (!fila.isEmpty()) {
			int tamanho = fila.size();
			for (int i = 0; i < tamanho; i++) {
				Parada atual = fila.removeFirst();
				for (Peao peao : jogador.getPeoes()) if (atual.equals(tabuleiro.posicaoDe(peao))) return distancia;
				for (Parada vizinha : tabuleiro.vizinhas(atual)) if (visitadas.add(vizinha)) fila.addLast(vizinha);
			}
			distancia++;
		}
		return Integer.MAX_VALUE;
	}
}
