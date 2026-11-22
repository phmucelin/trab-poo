package model;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/** Pedido de entrega de uma parada; o bônus é reservado ao primeiro cliente. */
final class Conselheiro {
	private final Parada parada;
	private final Map<TipoTesouro, Integer> pedido;
	private TipoBonus bonus;

	/** Pedido vazio representa o conselheiro que aceita três tesouros quaisquer. */
	Conselheiro(Parada parada, Map<TipoTesouro, Integer> pedido, TipoBonus bonus) {
		if (parada == null || !parada.isConselheiro() || pedido == null || bonus == null) {
			throw new IllegalArgumentException("Conselheiro precisa de parada, pedido e bônus");
		}
		Map<TipoTesouro, Integer> copia = new EnumMap<>(TipoTesouro.class);
		for (Map.Entry<TipoTesouro, Integer> item : pedido.entrySet()) {
			if (item.getKey() == null || item.getValue() == null || item.getValue() <= 0) {
				throw new IllegalArgumentException("Pedido inválido");
			}
			copia.put(item.getKey(), item.getValue());
		}
		boolean valido = parada.isKarakorum()
				? copia.size() == 3 && copia.values().stream().allMatch(n -> n == 1)
				: copia.isEmpty() || (copia.size() == 1 && copia.values().iterator().next() == 2);
		if (!valido) throw new IllegalArgumentException("Pedido incompatível com o conselheiro");
		this.parada = parada;
		this.pedido = Collections.unmodifiableMap(copia);
		this.bonus = bonus;
	}
	Parada getParada() { return parada; }
	Map<TipoTesouro, Integer> getPedido() { return pedido; }
	int getVotos() { return parada.isKarakorum() ? 2 : 1; }
	TipoBonus getBonus() { return bonus; }
	/** Recoloca o bônus de uma partida salva; null se já foi retirado. */
	void restaurarBonus(TipoBonus restante) { bonus = restante; }
	TipoBonus retirarBonus() { TipoBonus recebido = bonus; bonus = null; return recebido; }
	boolean aceita(Map<TipoTesouro, Integer> oferta) {
		if (oferta == null || oferta.isEmpty()) return false;
		int total = 0;
		for (Map.Entry<TipoTesouro, Integer> item : oferta.entrySet()) {
			if (item.getKey() == null || item.getValue() == null || item.getValue() <= 0 || item.getValue() > 3) return false;
			total += item.getValue();
		}
		return pedido.isEmpty() ? total == 3 : pedido.equals(oferta);
	}
}
