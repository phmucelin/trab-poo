package model;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Uma cidade do mapa (há 9: Kiev, Moscou, Sarai, Samarcanda, Bagdá, Cabul,
 * Pequim, Kaesong e Cantão).
 *
 * Ciclo de vida segundo o manual (pp. 4 e 10):
 *
 * - A cidade começa "fechada". Quando sua peça de cidade é revelada, ela
 * recebe 4 peças de tesouro e passa a poder ser atacada.
 * - Cada ataque com espadas retira um tesouro.
 * - Quem retira o último tesouro conquista a cidade e coloca nela um yurt
 * seu, que conta para a maioria de yurts da região (mas não ajuda na
 * movimentação).
 *
 * Uma cidade é atacável a partir de qualquer parada adjacente a ela.
 */
class Cidade {

	static final int TESOUROS_AO_REVELAR = 4;

	private final String nome;
	private final Regiao regiao;
	private final Set<Parada> paradasAdjacentes = new HashSet<>();
	private boolean revelada;
	private int tesouros;
	private Cor yurtDeConquista; // null enquanto não conquistada

	Cidade(String nome, Regiao regiao) {
		if (nome == null || nome.isBlank()) {
			throw new IllegalArgumentException("Cidade precisa de um nome");
		}
		if (regiao == null) {
			throw new IllegalArgumentException("Cidade precisa de uma região: " + nome);
		}
		this.nome = nome;
		this.regiao = regiao;
	}

	String getNome() {
		return nome;
	}

	Regiao getRegiao() {
		return regiao;
	}

	void adicionarParadaAdjacente(Parada p) {
		paradasAdjacentes.add(p);
	}

	Set<Parada> getParadasAdjacentes() {
		return Collections.unmodifiableSet(paradasAdjacentes);
	}

	boolean isAdjacente(Parada p) {
		return paradasAdjacentes.contains(p);
	}

	// ---------------------------------------------------------------- tesouros

	boolean isRevelada() {
		return revelada;
	}

	/** Revela a cidade com os 4 tesouros padrão. */
	void revelar() {
		revelar(TESOUROS_AO_REVELAR);
	}

	/** Revela a cidade colocando nela quantidadeTesouros tesouros. */
	void revelar(int quantidadeTesouros) {
		if (revelada) {
			throw new IllegalStateException("Cidade já revelada: " + nome);
		}
		if (quantidadeTesouros < 0) {
			throw new IllegalArgumentException("Quantidade de tesouros inválida");
		}
		revelada = true;
		tesouros = quantidadeTesouros;
	}

	int getTesouros() {
		return tesouros;
	}

	/** Se a cidade está revelada e ainda tem tesouros para serem tomados. */
	boolean podeSerAtacada() {
		return revelada && tesouros > 0 && !isConquistada();
	}

	/**
	 * Retira um tesouro da cidade em favor do jogador da cor informada. Se era
	 * o último, a cidade passa a estar conquistada por essa cor.
	 *
	 * @return true se um tesouro foi retirado
	 */
	boolean retirarTesouro(Cor atacante) {
		if (!podeSerAtacada()) {
			return false;
		}
		tesouros--;
		if (tesouros == 0) {
			yurtDeConquista = atacante;
		}
		return true;
	}

	/** Usado só por ArquivoPartida, ao carregar uma partida salva. */
	void restaurar(boolean revelada, int tesouros, Cor conquistador) {
		if (tesouros < 0 || tesouros > TESOUROS_AO_REVELAR || (!revelada && (tesouros > 0 || conquistador != null))
				|| (conquistador != null && tesouros > 0)) {
			throw new IllegalArgumentException("Estado inválido da cidade " + nome);
		}
		this.revelada = revelada;
		this.tesouros = tesouros;
		this.yurtDeConquista = conquistador;
	}

	// --------------------------------------------------------------- conquista

	boolean isConquistada() {
		return yurtDeConquista != null;
	}

	/** Cor do yurt de conquista; null se a cidade não foi conquistada. */
	Cor getConquistadaPor() {
		return yurtDeConquista;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof Cidade)) {
			return false;
		}
		return nome.equals(((Cidade) o).nome);
	}

	@Override
	public int hashCode() {
		return nome.hashCode();
	}

	@Override
	public String toString() {
		return nome;
	}
}
