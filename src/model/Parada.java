package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Uma parada do mapa: ponto das rotas onde os peões param e onde os jogadores
 * constroem yurts.
 *
 * É o equivalente ao "território" de um jogo como War. A diferença é que uma
 * parada não tem um único dono: ela guarda os yurts (de uma ou mais cores) que
 * foram construídos nela, respeitando a capacidade do seu TipoParada.
 *
 * A identidade da parada é o seu nome (único dentro de um Tabuleiro),
 * o que permite usá-la como chave de HashMap e elemento de HashSet.
 * As rotas (vizinhança) NÃO ficam aqui: ficam no Tabuleiro, no mapa
 * HashMap<Parada, Set<Parada>>.
 */
class Parada {

	private final String nome;
	private final Regiao regiao; // null apenas para Karakorum
	private final TipoParada tipo;
	/** Cores dos yurts construídos aqui, na ordem em que foram colocados. */
	private final List<Cor> yurts = new ArrayList<>();

	/**
	 * @param nome   nome único da parada
	 * @param regiao região a que pertence; null somente para Karakorum
	 * @param tipo   tipo (simples, dupla ou Karakorum)
	 */
	Parada(String nome, Regiao regiao, TipoParada tipo) {
		if (nome == null || nome.isBlank()) {
			throw new IllegalArgumentException("Parada precisa de um nome");
		}
		if (tipo == null) {
			throw new IllegalArgumentException("Parada precisa de um tipo");
		}
		if (regiao == null && tipo != TipoParada.KARAKORUM) {
			throw new IllegalArgumentException("Só Karakorum pode ficar fora de uma região: " + nome);
		}
		this.nome = nome;
		this.regiao = regiao;
		this.tipo = tipo;
	}

	String getNome() {
		return nome;
	}

	/** Região da parada; null para Karakorum. */
	Regiao getRegiao() {
		return regiao;
	}

	TipoParada getTipo() {
		return tipo;
	}

	boolean isDupla() {
		return tipo == TipoParada.DUPLA;
	}

	boolean isKarakorum() {
		return tipo == TipoParada.KARAKORUM;
	}

	/** Se há um conselheiro nesta parada (paradas duplas e Karakorum). */
	boolean isConselheiro() {
		return tipo.isConselheiro();
	}

	int getCapacidadePeoes() {
		return tipo.getCapacidadePeoes();
	}

	int getCapacidadeYurts() {
		return tipo.getCapacidadeYurts();
	}

	// ------------------------------------------------------------------ yurts

	/** Total de yurts construídos nesta parada (de todas as cores). */
	int contarYurts() {
		return yurts.size();
	}

	/** Quantos yurts da cor informada existem nesta parada. */
	int contarYurts(Cor cor) {
		int n = 0;
		for (Cor c : yurts) {
			if (c == cor) {
				n++;
			}
		}
		return n;
	}

	boolean temYurtDe(Cor cor) {
		return yurts.contains(cor);
	}

	/** Lista (somente leitura) das cores dos yurts, na ordem de colocação. */
	List<Cor> getYurts() {
		return Collections.unmodifiableList(yurts);
	}

	/** Usado só por ArquivoPartida: troca os yurts pelos gravados, na mesma ordem. */
	void restaurarYurts(List<Cor> cores) {
		if (!tipo.equals(TipoParada.KARAKORUM) && cores.size() > tipo.getCapacidadeYurts()) {
			throw new IllegalArgumentException("Yurts demais na parada " + nome);
		}
		yurts.clear();
		yurts.addAll(cores);
	}

	/** Se ainda cabe um yurt aqui segundo a capacidade do tipo da parada. */
	boolean podeReceberYurt() {
		return yurts.size() < tipo.getCapacidadeYurts();
	}

	/**
	 * Coloca um yurt da cor informada, respeitando a capacidade.
	 *
	 * @return true se o yurt foi colocado; false se a parada já
	 * estava cheia
	 */
	boolean colocarYurt(Cor cor) {
		return colocarYurt(cor, false);
	}

	/**
	 * Coloca um yurt, opcionalmente ignorando a capacidade.
	 *
	 * O parâmetro ignorarCapacidade existe para a habilidade do
	 * herdeiro Tolui, que começa a partida com 1 yurt em Karakorum (onde
	 * normalmente não cabem yurts).
	 */
	boolean colocarYurt(Cor cor, boolean ignorarCapacidade) {
		if (cor == null) {
			throw new IllegalArgumentException("Cor do yurt não pode ser nula");
		}
		if (!ignorarCapacidade && !podeReceberYurt()) {
			return false;
		}
		yurts.add(cor);
		return true;
	}

	/**
	 * Remove um yurt da cor informada.
	 *
	 * @return true se havia um yurt dessa cor e ele foi removido
	 */
	boolean removerYurt(Cor cor) {
		return yurts.remove(cor);
	}

	// --------------------------------------------------------------- identidade

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof Parada)) {
			return false;
		}
		return nome.equals(((Parada) o).nome);
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
