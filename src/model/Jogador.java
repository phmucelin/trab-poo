package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Um jogador de Herdeiros de Khan e tudo o que ele guarda à sua frente.
 *
 * Preparação (manual, p. 3): cada jogador escolhe uma cor e recebe o
 * tabuleiro de jogador (herdeiro) dessa cor, 4 peças de ativação, 12 yurts,
 * 2 cartas de conselheiro secreto e 1 moeda (1º e 2º a jogar) ou 2 moedas
 * (demais).
 *
 * O que fica aqui: cor, herdeiro, peões, estoque de yurts, peças de ativação,
 * peças de tributo e votos. As cartas, os tesouros e as melhorias serão
 * modelados pelas classes de cartas/peças (outras partes do trabalho) e podem
 * ser ligados a esta classe depois, sem alterar o que já existe (princípio
 * aberto/fechado).
 */
class Jogador {

	static final int YURTS_INICIAIS = 12;

	private final String nome;
	private final Cor cor;
	private final Herdeiro herdeiro;
	private final List<Peao> peoes = new ArrayList<>();
	private int yurtsDisponiveis = YURTS_INICIAIS;
	/** Colunas cuja peça de ativação ainda não foi usada nesta "rodada de fichas". */
	private final Set<Coluna> fichasDisponiveis = new HashSet<>();
	private final Map<TipoTributo, Integer> tributos = new HashMap<>();
	private int votos;

	Jogador(String nome, Cor cor, Herdeiro herdeiro) {
		if (nome == null || nome.isBlank()) {
			throw new IllegalArgumentException("Jogador precisa de um nome");
		}
		if (cor == null || herdeiro == null) {
			throw new IllegalArgumentException("Jogador precisa de cor e herdeiro: " + nome);
		}
		this.nome = nome;
		this.cor = cor;
		this.herdeiro = herdeiro;
		for (int i = 0; i < herdeiro.getQuantidadePeoes(); i++) {
			peoes.add(new Peao(cor, i));
		}
		recuperarFichas();
		for (TipoTributo t : TipoTributo.values()) {
			tributos.put(t, 0);
		}
	}

	String getNome() {
		return nome;
	}

	Cor getCor() {
		return cor;
	}

	Herdeiro getHerdeiro() {
		return herdeiro;
	}

	// ------------------------------------------------------------------ peões

	/** Peões do jogador (1, ou 2 para Altani). Lista somente leitura. */
	List<Peao> getPeoes() {
		return Collections.unmodifiableList(peoes);
	}

	Peao getPeao(int indice) {
		return peoes.get(indice);
	}

	// ------------------------------------------------------------------ yurts

	int getYurtsDisponiveis() {
		return yurtsDisponiveis;
	}

	boolean temYurtDisponivel() {
		return yurtsDisponiveis > 0;
	}

	/**
	 * Retira um yurt do estoque do jogador (para ser colocado no mapa).
	 *
	 * @return false se o estoque já estava vazio
	 */
	boolean retirarYurtDoEstoque() {
		if (yurtsDisponiveis <= 0) {
			return false;
		}
		yurtsDisponiveis--;
		return true;
	}

	/** Devolve um yurt ao estoque (caso uma colocação seja desfeita). */
	void devolverYurtAoEstoque() {
		if (yurtsDisponiveis >= YURTS_INICIAIS) {
			throw new IllegalStateException("Estoque de yurts já está completo");
		}
		yurtsDisponiveis++;
	}

	// ------------------------------------------------------------ fichas

	Set<Coluna> getFichasDisponiveis() {
		return Collections.unmodifiableSet(fichasDisponiveis);
	}

	boolean temFichaDisponivel(Coluna coluna) {
		return fichasDisponiveis.contains(coluna);
	}

	/**
	 * Usa a peça de ativação de uma coluna.
	 *
	 * @return false se a ficha daquela coluna já tinha sido usada
	 */
	boolean usarFicha(Coluna coluna) {
		return fichasDisponiveis.remove(coluna);
	}

	/** Recupera todas as peças de ativação (efeito da coluna branca). */
	void recuperarFichas() {
		fichasDisponiveis.clear();
		Collections.addAll(fichasDisponiveis, Coluna.values());
	}

	// ---------------------------------------------------------------- tributos

	int contarTributo(TipoTributo tipo) {
		return tributos.get(tipo);
	}

	void adicionarTributo(TipoTributo tipo, int quantidade) {
		if (quantidade < 0) {
			throw new IllegalArgumentException("Quantidade negativa");
		}
		tributos.put(tipo, tributos.get(tipo) + quantidade);
	}

	/**
	 * Gasta peças de tributo.
	 *
	 * @return false (e não altera nada) se o jogador não tem o
	 * suficiente
	 */
	boolean gastarTributo(TipoTributo tipo, int quantidade) {
		if (quantidade < 0) {
			throw new IllegalArgumentException("Quantidade negativa");
		}
		int atual = tributos.get(tipo);
		if (atual < quantidade) {
			return false;
		}
		tributos.put(tipo, atual - quantidade);
		return true;
	}

	Map<TipoTributo, Integer> getTributos() {
		return Collections.unmodifiableMap(tributos);
	}

	// ------------------------------------------------------------------- votos

	int getVotos() {
		return votos;
	}

	void adicionarVotos(int quantidade) {
		if (quantidade < 0) {
			throw new IllegalArgumentException("Quantidade negativa");
		}
		votos += quantidade;
	}

	/** Usado só por ArquivoPartida, ao carregar uma partida salva. */
	void restaurar(int yurts, Set<Coluna> fichas, Map<TipoTributo, Integer> tributos, int votos) {
		if (yurts < 0 || yurts > YURTS_INICIAIS || votos < 0) {
			throw new IllegalArgumentException("Yurts ou votos fora dos limites: " + nome);
		}
		yurtsDisponiveis = yurts;
		fichasDisponiveis.clear();
		fichasDisponiveis.addAll(fichas);
		for (TipoTributo t : TipoTributo.values()) {
			int quantidade = tributos.getOrDefault(t, 0);
			if (quantidade < 0) {
				throw new IllegalArgumentException("Tributo negativo: " + nome);
			}
			this.tributos.put(t, quantidade);
		}
		this.votos = votos;
	}

	@Override
	public String toString() {
		return nome + " (" + cor + ", " + herdeiro + ")";
	}
}
