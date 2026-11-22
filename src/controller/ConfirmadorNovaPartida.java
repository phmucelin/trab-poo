package controller;

import java.awt.Component;

/**
 * Pergunta aos jogadores, no fim de uma partida, se querem jogar outra.
 * O Controlador usa a implementação da View; os testes trocam por um stub
 * para não ficarem presos num diálogo.
 */
public interface ConfirmadorNovaPartida {
	/** @return true para iniciar uma nova partida, false para encerrar a aplicação. */
	boolean confirmar(Component pai, String vencedores);
}
