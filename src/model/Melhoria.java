package model;

import java.util.Map;

/** Peça imutável. A cor e o símbolo identificam sua posição e seu efeito. */
final class Melhoria {
	enum Simbolo { MOEDA, ESPADA, YURT, MAO, CAVALOS, FERRO, PELES, GADO, TECIDO, ESPECIARIAS }
	private final Coluna cor;
	private final Simbolo simbolo;

	Melhoria(Coluna cor, Simbolo simbolo) {
		if (cor == null || cor == Coluna.BRANCA || simbolo == null) {
			throw new IllegalArgumentException("Melhoria precisa de cor regional e símbolo");
		}
		this.cor = cor;
		this.simbolo = simbolo;
	}

	String getId() { return "MELHORIA_" + cor.name() + "_" + simbolo.name(); }
	Coluna getCor() { return cor; }
	Simbolo getSimbolo() { return simbolo; }
	TipoTesouro getTesouroVirtual() {
		for (TipoTesouro tipo : TipoTesouro.values()) {
			if (tipo.name().equals(simbolo.name())) return tipo;
		}
		return null;
	}
	String getDescricao() {
		switch (simbolo) {
		case MOEDA: return "1 moeda virtual";
		case ESPADA: return "1 espada virtual";
		case YURT: return "1 tributo de yurt virtual";
		case MAO: return "1 mão adicional";
		case CAVALOS: return "2 cavalos adicionais";
		default: return "1 tesouro virtual de " + simbolo.name();
		}
	}

	EfeitosColuna getEfeitos() {
		switch (simbolo) {
		case MOEDA: return new EfeitosColuna(0, 0, Map.of(TipoTributo.MOEDA, 1));
		case ESPADA: return new EfeitosColuna(0, 0, Map.of(TipoTributo.ESPADA, 1));
		case YURT: return new EfeitosColuna(0, 0, Map.of(TipoTributo.YURT, 1));
		case MAO: return new EfeitosColuna(0, 1, Map.of());
		case CAVALOS: return new EfeitosColuna(2, 0, Map.of());
		default: return EfeitosColuna.semEfeitos();
		}
	}
}
