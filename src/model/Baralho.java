package model;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/** Compra sem reposição. Cartas descartadas não voltam à partida. */
final class Baralho {
	private final List<Carta> pilha;
	private final Deque<Carta> descarte = new ArrayDeque<>();
	private final Set<Carta> compradas = new HashSet<>();
	private final Map<Jogador, List<Carta>> cartasPorJogador = new HashMap<>();
	Baralho(List<Carta> cartas, Random random) {
		if (cartas == null || random == null || cartas.stream().anyMatch(c -> c == null) || new HashSet<>(cartas).size() != cartas.size()) {
			throw new IllegalArgumentException("Cartas válidas, sem peças repetidas, e sorteio obrigatórios");
		}
		pilha = new ArrayList<>(cartas);
		Collections.shuffle(pilha, random);
	}
	int restantes() { return pilha.size(); }
	Carta comprarCarta() {
		if (pilha.isEmpty()) return null;
		Carta carta = pilha.remove(pilha.size() - 1);
		compradas.add(carta);
		return carta;
	}
	boolean distribuir(Jogador jogador, int quantidade) {
		if (jogador == null || quantidade <= 0 || quantidade > restantes()) return false;
		List<Carta> mao = cartasPorJogador.computeIfAbsent(jogador, j -> new ArrayList<>());
		for (int i = 0; i < quantidade; i++) mao.add(comprarCarta());
		return true;
	}
	List<Carta> cartasDe(Jogador jogador) {
		return Collections.unmodifiableList(new ArrayList<>(cartasPorJogador.getOrDefault(jogador, List.of())));
	}
	boolean descartar(Carta carta) {
		if (!compradas.remove(carta)) return false;
		for (List<Carta> mao : cartasPorJogador.values()) mao.remove(carta);
		descarte.push(carta);
		return true;
	}
	List<Carta> getDescarte() { return List.copyOf(descarte); }
	/** Pilha fechada, do fundo para o topo (a próxima compra é a última). */
	List<Carta> getPilha() { return List.copyOf(pilha); }
	/** Todas as cartas deste baralho, onde quer que estejam. */
	List<Carta> todas() {
		List<Carta> cartas = new ArrayList<>(pilha);
		cartas.addAll(compradas);
		cartas.addAll(descarte);
		return cartas;
	}
	/**
	 * Usado só por ArquivoPartida. As cartas fora da pilha e do descarte ficam
	 * com os jogadores ou expostas na mesa (as abertas).
	 */
	void restaurar(List<Carta> novaPilha, List<Carta> novoDescarte, Map<Jogador, List<Carta>> maos, List<Carta> naMesa) {
		pilha.clear();
		pilha.addAll(novaPilha);
		descarte.clear();
		descarte.addAll(novoDescarte);
		cartasPorJogador.clear();
		compradas.clear();
		for (Map.Entry<Jogador, List<Carta>> mao : maos.entrySet()) {
			cartasPorJogador.put(mao.getKey(), new ArrayList<>(mao.getValue()));
			compradas.addAll(mao.getValue());
		}
		compradas.addAll(naMesa);
	}
}
