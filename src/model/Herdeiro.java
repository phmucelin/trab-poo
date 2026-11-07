package model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Os cinco herdeiros de Genghis Khan (tabuleiros de jogador / cartelas).
 *
 * Cada herdeiro tem uma habilidade especial (manual, p. 20) e quatro colunas
 * de ações cuja composição inicial difere entre os herdeiros. As composições
 * abaixo foram transcritas das cartelas (arquivo cartelas.zip):
 *
 * Vermelha              Verde                 Amarela               Branca
 * Altani     1 cav, 2 mãos, Khan   2 cav, 1 mão          1 cav, 1 mão, moeda   2 cav, Khan, recuperar
 * Chagatai   1 cav, 2 mãos, Khan   1 cav, 1 mão, moeda   2 cav, 1 mão          2 cav, Khan, recuperar
 * Jochi      1 cav, 1 mão, moeda   1 cav, 2 mãos, Khan   2 cav, 1 mão          2 cav, Khan, recuperar
 * Ogedei     2 cav, 1 mão          1 cav, 1 mão, moeda   1 cav, 2 mãos, Khan   2 cav, Khan, recuperar
 * Tolui      1 cav, 1 mão, moeda   2 cav, 1 mão          1 cav, 2 mãos, Khan   2 cav, Khan, recuperar
 *
 * (cav = cavalos = movimentos; mão = ação de pegar tributo; Khan = obrigação de
 * mover o Khan; moeda = moeda virtual válida só naquele turno.)
 */
enum Herdeiro {

	ALTANI("Altani", 2,
			"Você tem 2 peões. Todo turno, escolha um para mover e fazer ações. O outro não pode fazer nada.",
			new AcoesColuna(1, 2, true, false, false),
			new AcoesColuna(2, 1, false, false, false),
			new AcoesColuna(1, 1, false, true, false)),

	CHAGATAI("Chagatai", 1,
			"1x por turno, você pode deixar de usar 1 cavalo para ganhar 1 peça de espada.",
			new AcoesColuna(1, 2, true, false, false),
			new AcoesColuna(1, 1, false, true, false),
			new AcoesColuna(2, 1, false, false, false)),

	JOCHI("Jochi", 1,
			"No início do jogo, pegue 2 melhorias ao acaso e coloque uma delas na sua coluna branca. Você não precisa seguir a cor das peças de melhoria quando pegá-las.",
			new AcoesColuna(1, 1, false, true, false),
			new AcoesColuna(1, 2, true, false, false),
			new AcoesColuna(2, 1, false, false, false)),

	OGEDEI("Ogedei", 1,
			"1x por turno, você pode pagar uma moeda para pegar um tesouro de qualquer cidade no mapa "
					+ "(desde que não seja o último tesouro da cidade).",
			new AcoesColuna(2, 1, false, false, false),
			new AcoesColuna(1, 1, false, true, false),
			new AcoesColuna(1, 2, true, false, false)),

	TOLUI("Tolui", 1,
			"No início do jogo, coloque 1 yurt em Karakorum. Ele não pertence a nenhuma região. "
					+ "Você pode colocar yurts em espaços adjacentes a você.",
			new AcoesColuna(1, 1, false, true, false),
			new AcoesColuna(2, 1, false, false, false),
			new AcoesColuna(1, 2, true, false, false));

	private final String nome;
	private final int quantidadePeoes;
	private final String habilidade;
	private final Map<Coluna, AcoesColuna> colunas = new HashMap<>();

	Herdeiro(String nome, int quantidadePeoes, String habilidade,
			AcoesColuna vermelha, AcoesColuna verde, AcoesColuna amarela) {
		this.nome = nome;
		this.quantidadePeoes = quantidadePeoes;
		this.habilidade = habilidade;
		colunas.put(Coluna.VERMELHA, vermelha);
		colunas.put(Coluna.VERDE, verde);
		colunas.put(Coluna.AMARELA, amarela);
		// A coluna branca é igual para todos os herdeiros: 2 cavalos, Khan, recuperar.
		colunas.put(Coluna.BRANCA, new AcoesColuna(2, 0, true, false, true));
	}

	String getNome() {
		return nome;
	}

	/** 2 para Altani, 1 para os demais. */
	int getQuantidadePeoes() {
		return quantidadePeoes;
	}

	String getHabilidade() {
		return habilidade;
	}

	/** Ações impressas na coluna informada da cartela deste herdeiro. */
	AcoesColuna getAcoes(Coluna coluna) {
		return colunas.get(coluna);
	}

	Map<Coluna, AcoesColuna> getColunas() {
		return Collections.unmodifiableMap(colunas);
	}

	@Override
	public String toString() {
		return nome;
	}

	/**
	 * Composição de ações impressa em uma coluna da cartela. Imutável.
	 *
	 * As melhorias compradas durante a partida aumentam essas quantidades;
	 * isso será tratado pela classe que modelar o tabuleiro do jogador com as
	 * melhorias (fora do escopo desta iteração).
	 */
	static final class AcoesColuna {
		private final int cavalos;
		private final int maos;
		private final boolean khan;
		private final boolean moedaVirtual;
		private final boolean recuperarFichas;

		AcoesColuna(int cavalos, int maos, boolean khan, boolean moedaVirtual, boolean recuperarFichas) {
			this.cavalos = cavalos;
			this.maos = maos;
			this.khan = khan;
			this.moedaVirtual = moedaVirtual;
			this.recuperarFichas = recuperarFichas;
		}

		/** Número de movimentos. */
		int getCavalos() {
			return cavalos;
		}

		/** Número de ações de pegar tributo. */
		int getMaos() {
			return maos;
		}

		/** Se ativar esta coluna obriga a mover o Khan. */
		boolean temKhan() {
			return khan;
		}

		/** Se a coluna dá uma moeda virtual válida só neste turno. */
		boolean temMoedaVirtual() {
			return moedaVirtual;
		}

		/** Se ativar esta coluna recupera todas as peças de ativação usadas. */
		boolean recuperaFichas() {
			return recuperarFichas;
		}

		@Override
		public String toString() {
			return cavalos + " cavalo(s), " + maos + " mão(s)"
					+ (khan ? ", Khan" : "")
					+ (moedaVirtual ? ", moeda virtual" : "")
					+ (recuperarFichas ? ", recuperar fichas" : "");
		}
	}
}
