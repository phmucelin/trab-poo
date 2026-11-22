package model;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Uma província do mapa: área entre as rotas que fornece peças de tributo.
 *
 * Regras do manual (pp. 5, 8 e 9):
 *
 * - Cada província tem um ícone de tributo (yurt, espada ou moeda) e começa
 * a partida com 1 peça; o máximo é 3 peças por província.
 * - Um jogador pode pegar tributo das províncias adjacentes às paradas
 * onde seu peão esteve no turno.
 * - Quatro províncias são "províncias do Khan": quando o Khan é colocado
 * em uma delas, ela e as 2 províncias vizinhas indicadas pelas setas
 * recebem 1 peça de tributo cada (respeitando o máximo de 3).
 *
 */
class Provincia {

	static final int MAXIMO_PECAS = 3;

	private final String nome;
	private final Regiao regiao;
	private final TipoTributo tributo;
	private final boolean provinciaDoKhan;
	private final Set<Parada> paradasAdjacentes = new HashSet<>();
	/** Para províncias do Khan: as 2 províncias apontadas pelas setas. */
	private final Set<Provincia> vizinhasDoKhan = new HashSet<>();
	private int pecas;

	Provincia(String nome, Regiao regiao, TipoTributo tributo, boolean provinciaDoKhan) {
		if (nome == null || nome.isBlank()) {
			throw new IllegalArgumentException("Província precisa de um nome");
		}
		if (regiao == null || tributo == null) {
			throw new IllegalArgumentException("Província precisa de região e tipo de tributo: " + nome);
		}
		this.nome = nome;
		this.regiao = regiao;
		this.tributo = tributo;
		this.provinciaDoKhan = provinciaDoKhan;
	}

	String getNome() {
		return nome;
	}

	Regiao getRegiao() {
		return regiao;
	}

	TipoTributo getTributo() {
		return tributo;
	}

	boolean isProvinciaDoKhan() {
		return provinciaDoKhan;
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

	void adicionarVizinhaDoKhan(Provincia outra) {
		if (!provinciaDoKhan) {
			throw new IllegalStateException(nome + " não é província do Khan");
		}
		vizinhasDoKhan.add(outra);
	}

	Set<Provincia> getVizinhasDoKhan() {
		return Collections.unmodifiableSet(vizinhasDoKhan);
	}

	// ------------------------------------------------------------------ peças

	/** Usado só por ArquivoPartida, ao carregar uma partida salva. */
	void restaurarPecas(int quantidade) {
		if (quantidade < 0 || quantidade > MAXIMO_PECAS) {
			throw new IllegalArgumentException("Província com peças fora do limite: " + nome);
		}
		pecas = quantidade;
	}

	int getPecas() {
		return pecas;
	}

	boolean temPecas() {
		return pecas > 0;
	}

	/**
	 * Coloca uma peça de tributo, respeitando o máximo de 3.
	 *
	 * @return true se a peça foi colocada
	 */
	boolean colocarPeca() {
		if (pecas >= MAXIMO_PECAS) {
			return false;
		}
		pecas++;
		return true;
	}

	/**
	 * Retira uma peça de tributo (quando um jogador a pega).
	 *
	 * @return true se havia peça para retirar
	 */
	boolean retirarPeca() {
		if (pecas <= 0) {
			return false;
		}
		pecas--;
		return true;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof Provincia)) {
			return false;
		}
		return nome.equals(((Provincia) o).nome);
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
