package model;

import java.util.List;
import java.util.Random;

/**
 * Cores/símbolos e pedidos conferidos na revisão contra as imagens do EAD.
 * As multiplicidades por cor são uma hipótese equilibrada: 15/15/14 peças.
 * O material disponível confirma os totais por símbolo, mas não essas multiplicidades.
 */
final class PecasDoJogo {
	private PecasDoJogo() { }
	static List<String> cadastro() {
		return List.of(
			"MELHORIA;AMARELA;MOEDA;3", "MELHORIA;AMARELA;ESPADA;3",
			"MELHORIA;AMARELA;YURT;2", "MELHORIA;AMARELA;MAO;2",
			"MELHORIA;AMARELA;CAVALOS;3", "MELHORIA;AMARELA;GADO;1",
			"MELHORIA;AMARELA;ESPECIARIAS;1",
			"MELHORIA;VERMELHA;MOEDA;3", "MELHORIA;VERMELHA;ESPADA;3",
			"MELHORIA;VERMELHA;YURT;3", "MELHORIA;VERMELHA;MAO;2",
			"MELHORIA;VERMELHA;CAVALOS;3", "MELHORIA;VERMELHA;FERRO;1",
			"MELHORIA;VERDE;MOEDA;2", "MELHORIA;VERDE;ESPADA;2",
			"MELHORIA;VERDE;YURT;3", "MELHORIA;VERDE;MAO;2",
			"MELHORIA;VERDE;CAVALOS;3", "MELHORIA;VERDE;PELES;1",
			"MELHORIA;VERDE;TECIDO;1",
			"CONSELHEIRO;Conselheiro Norte;TECIDO:2",
			"CONSELHEIRO;Conselheiro Nordeste;FERRO:2",
			"CONSELHEIRO;Conselheiro Oeste;PELES:2",
			"CONSELHEIRO;Conselheiro Leste;GADO:2",
			"CONSELHEIRO;Conselheiro Sudoeste;ESPECIARIAS:2",
			"CONSELHEIRO;Conselheiro Sul;QUAISQUER",
			"CONSELHEIRO;Karakorum;FERRO:1,PELES:1,GADO:1");
	}
	static ComponentesKhan criar(Tabuleiro tabuleiro, Random random) {
		return ConfiguracaoPecas.criar(cadastro(), tabuleiro, random);
	}
}
