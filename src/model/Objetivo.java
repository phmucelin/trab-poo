package model;

/** Consulta polimórfica: um patamar para carta aberta, três para secreta. */
abstract class Objetivo {
	private final int[] patamares;
	Objetivo(int... patamares) {
		if (patamares == null || (patamares.length != 1 && patamares.length != 3)) {
			throw new IllegalArgumentException("Objetivo precisa de um ou três patamares");
		}
		int anterior = 0;
		for (int patamar : patamares) {
			if (patamar <= anterior) throw new IllegalArgumentException("Patamares positivos e crescentes");
			anterior = patamar;
		}
		this.patamares = patamares.clone();
	}
	boolean isSecreto() { return patamares.length == 3; }
	boolean objetivoCumprido(Jogador jogador, Partida partida, Inventario inventario) {
		return votos(jogador, partida, inventario) > 0;
	}
	int votos(Jogador jogador, Partida partida, Inventario inventario) {
		if (jogador == null || partida == null || inventario == null || !partida.getJogadores().contains(jogador)) {
			throw new IllegalArgumentException("Jogador, partida e inventário válidos são obrigatórios");
		}
		int quantidade = progresso(jogador, partida, inventario);
		int votos = 0;
		for (int patamar : patamares) if (quantidade >= patamar) votos++;
		return votos;
	}
	String getDescricao() {
		StringBuilder texto = new StringBuilder(descreverCriterio()).append(". ");
		if (isSecreto()) texto.append("1/2/3 votos ao atingir ");
		else texto.append("1 voto ao atingir ");
		for (int i = 0; i < patamares.length; i++) {
			if (i > 0) texto.append("/");
			texto.append(patamares[i]);
		}
		return texto.append(".").toString();
	}
	abstract String descreverCriterio();
	abstract int progresso(Jogador jogador, Partida partida, Inventario inventario);
}
