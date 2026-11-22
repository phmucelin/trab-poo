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
	/** Pilha de compra, do fundo (índice 0) ao topo (último índice). */
	List<Carta> getPilha() { return List.copyOf(pilha); }
	/**
	 * Recoloca o baralho salvo. descartadas vem da mais recente para a mais
	 * antiga (a ordem de getDescarte); emJogo são as cartas já compradas que
	 * não estão nas mãos (as abertas viradas).
	 */
	void restaurar(List<Carta> novaPilha, List<Carta> descartadas, Map<Jogador, List<Carta>> maos, List<Carta> emJogo) {
		pilha.clear();
		pilha.addAll(novaPilha);
		descarte.clear();
		for (int i = descartadas.size() - 1; i >= 0; i--) descarte.push(descartadas.get(i));
		cartasPorJogador.clear();
		compradas.clear();
		for (Map.Entry<Jogador, List<Carta>> mao : maos.entrySet()) {
			cartasPorJogador.put(mao.getKey(), new ArrayList<>(mao.getValue()));
			compradas.addAll(mao.getValue());
		}
		compradas.addAll(emJogo);
	}
}
