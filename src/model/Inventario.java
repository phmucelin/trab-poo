package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Tesouros, melhorias e bônus de um jogador; cartas ficam no Baralho. */
final class Inventario {
	static final int LIMITE_POR_COLUNA = 4;
	private final Map<TipoTesouro, Integer> tesouros = new EnumMap<>(TipoTesouro.class);
	private final Map<Coluna, List<Melhoria>> melhorias = new EnumMap<>(Coluna.class);
	private final Map<Melhoria, Integer> adquiridaNoTurno = new HashMap<>();
	private final Map<TipoBonus, Boolean> bonus = new EnumMap<>(TipoBonus.class);

	Inventario() {
		for (TipoTesouro tipo : TipoTesouro.values()) tesouros.put(tipo, 0);
		for (Coluna coluna : Coluna.values()) melhorias.put(coluna, new ArrayList<>());
	}
	int contarTesouro(TipoTesouro tipo) { return tesouros.getOrDefault(tipo, 0); }
	int totalTesouros() { return tesouros.values().stream().mapToInt(Integer::intValue).sum(); }
	void receberTesouro(TipoTesouro tipo) {
		if (tipo == null) throw new IllegalArgumentException("Tesouro obrigatório");
		tesouros.put(tipo, contarTesouro(tipo) + 1);
	}
	boolean gastarTesouros(Map<TipoTesouro, Integer> pedido) {
		if (pedido == null) return false;
		for (Map.Entry<TipoTesouro, Integer> item : pedido.entrySet()) {
			if (item.getKey() == null || item.getValue() == null || item.getValue() < 0
					|| contarTesouro(item.getKey()) < item.getValue()) return false;
		}
		for (Map.Entry<TipoTesouro, Integer> item : pedido.entrySet()) {
			tesouros.put(item.getKey(), contarTesouro(item.getKey()) - item.getValue());
		}
		return true;
	}
	List<Melhoria> getMelhorias(Coluna coluna) {
		return Collections.unmodifiableList(melhorias.get(coluna));
	}
	int contarMelhorias(Coluna coluna) { return melhorias.get(coluna).size(); }
	boolean cabeMelhoria(Coluna coluna) {
		return coluna != null && contarMelhorias(coluna) < LIMITE_POR_COLUNA;
	}
	boolean adicionarMelhoria(Melhoria melhoria, Coluna coluna, int turno) {
		return adicionarMelhoria(melhoria, coluna, turno, false);
	}
	/** Apenas a ação coordenada de Jochi pode ignorar a cor da peça. */
	boolean adicionarMelhoria(Melhoria melhoria, Coluna coluna, int turno, boolean ignorarCor) {
		if (melhoria == null || !cabeMelhoria(coluna) || turno < 0
				|| (!ignorarCor && coluna != Coluna.BRANCA && melhoria.getCor() != coluna)
				|| adquiridaNoTurno.containsKey(melhoria)) return false;
		melhorias.get(coluna).add(melhoria);
		adquiridaNoTurno.put(melhoria, turno);
		return true;
	}
	boolean podeAtivar(Melhoria melhoria, int turno) {
		return adquiridaNoTurno.containsKey(melhoria) && adquiridaNoTurno.get(melhoria) < turno;
	}
	int linhasCompletas() {
		return Math.min(contarMelhorias(Coluna.VERMELHA),
				Math.min(contarMelhorias(Coluna.VERDE), contarMelhorias(Coluna.AMARELA)));
	}
	Map<TipoBonus, Boolean> getBonus() { return Collections.unmodifiableMap(bonus); }
	boolean receberBonus(TipoBonus tipo) {
		if (tipo == null || bonus.containsKey(tipo)) return false;
		bonus.put(tipo, false);
		return true;
	}
	boolean podeUsarBonus(TipoBonus tipo) { return Boolean.FALSE.equals(bonus.get(tipo)); }
	boolean usarBonus(TipoBonus tipo) {
		if (!podeUsarBonus(tipo)) return false;
		bonus.put(tipo, true);
		return true;
	}
}
