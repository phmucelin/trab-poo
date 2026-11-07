package model;

import static org.junit.Assert.*;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.junit.After;
import org.junit.Test;

/** Testa as novas ações pela fachada, sem acessar ou alterar o estado interno. */
public class AcoesModelAPITest {
	private final ModelAPI api = ModelAPI.getInstance();
	private final Contador observador = new Contador();
	private static class Contador implements ObservadorIF {
		int chamadas;
		@Override public void notify(ObservadoIF observado) { chamadas++; }
	}
	@After public void limpar() { api.remove(observador); }

	private void completa(String primeiro, String segundo) {
		assertTrue(api.novaPartidaCompleta(List.of("Ana", "Bia"), List.of(primeiro, segundo), PreparacaoCompletaTest.cadastro()));
		assertTrue(api.escolherCartaSecreta(0));
		assertTrue(api.escolherCartaSecreta(1));
	}

	/** Coleta e construção atualizam as peças e notificam; recusas preservam tudo. */
	@Test public void coletaConstroiEAtualizaRecursos() {
		completa("ALTANI", "OGEDEI");
		assertTrue(api.ativarColuna("VERDE"));
		api.add(observador);
		assertTrue(api.podePegarTributo("Estepe Central"));
		assertTrue(api.podePegarTributo("Estepe Central"));
		assertEquals(0, observador.chamadas);
		assertTrue(api.pegarTributo("Estepe Central"));
		assertEquals(0, api.getPecasProvincia("Estepe Central"));
		assertEquals(0, api.getMaosRestantes());
		assertEquals(1, api.getTributosJogador(0, "YURT"));
		assertFalse(api.escolherPeao(1));
		assertFalse(api.pegarTributo("Estepe Central"));
		assertFalse(api.pegarTributo("Oásis Central"));
		assertTrue(api.moverPara("Sarai Sudoeste"));
		assertTrue(api.podeConstruirYurt("Sarai Sudoeste"));
		assertTrue(api.construirYurt("Sarai Sudoeste"));
		assertEquals(List.of("AZUL"), api.getYurtsParada("Sarai Sudoeste"));
		assertEquals(11, api.getYurtsDisponiveis(0));
		assertEquals(0, api.getTributosJogador(0, "YURT"));
		assertFalse(api.construirYurt("Sarai Sudoeste"));
		assertFalse(api.construirYurt("Karakorum"));
		assertEquals(3, observador.chamadas);
	}

	/** Habilidade de Tolui pode construir na vizinha sem deslocar o peão. */
	@Test public void toluiConstroiNaVizinha() {
		completa("TOLUI", "ALTANI");
		assertEquals(List.of("AZUL"), api.getYurtsParada("Karakorum"));
		assertTrue(api.ativarColuna("VERDE"));
		assertTrue(api.pegarTributo("Estepe Central"));
		assertTrue(api.construirYurt("Sarai Sudoeste"));
		assertEquals("Karakorum", api.getPosicaoPeao(0, 0));
		assertEquals(10, api.getYurtsDisponiveis(0));
	}

	/** Ogedei escolhe um tipo existente, paga virtuais antes das físicas e só usa uma vez. */
	@Test public void ogedeiRecebeTesouroEscolhidoSemGastarMoedaFisica() {
		completa("OGEDEI", "ALTANI");
		assertTrue(api.ativarColuna("VERDE"));
		String cidade = api.getCidades().stream().filter(api::isCidadeRevelada).findFirst().orElseThrow();
		String tipo = api.getTesourosCidade(cidade).get(0);
		int quantidade = java.util.Collections.frequency(api.getTesourosCidade(cidade), tipo);
		api.add(observador);
		assertTrue(api.podePegarTesouroComMoeda(cidade, tipo));
		assertTrue(api.podePegarTesouroComMoeda(cidade, tipo));
		assertEquals(0, observador.chamadas);
		assertFalse(api.pegarTesouroComMoeda(cidade, "INVALIDO"));
		assertTrue(api.pegarTesouroComMoeda(cidade, tipo));
		assertEquals(1, api.getTributosJogador(0, "MOEDA"));
		assertEquals(0, api.getTributosVirtuais("MOEDA"));
		assertEquals(1, api.getTesourosJogador(0, tipo));
		assertEquals(3, api.getTesourosRestantesCidade(cidade));
		assertEquals(quantidade - 1, java.util.Collections.frequency(api.getTesourosCidade(cidade), tipo));
		assertFalse(api.pegarTesouroComMoeda(cidade, tipo));
		assertEquals(1, observador.chamadas);
	}

	/** Recusas na preparação e nomes inválidos não abrem ações nem notificam. */
	@Test public void preparacaoRecusaTodasAsAcoes() {
		assertTrue(api.novaPartidaCompleta(List.of("Ana", "Bia"), List.of("CHAGATAI", "OGEDEI"), PreparacaoCompletaTest.cadastro()));
		api.add(observador);
		assertFalse(api.pegarTributo("Estepe Central"));
		assertFalse(api.construirYurt("Sarai Sudoeste"));
		assertFalse(api.atacarCidade("Sarai", null));
		assertFalse(api.pegarTesouroComMoeda("Sarai", null));
		assertFalse(api.trocarCavaloPorEspada());
		assertFalse(api.podeConstruirYurt(null));
		assertFalse(api.podePegarTributo(null));
		assertEquals(0, api.getPecasProvincia("inexistente"));
		assertTrue(api.getYurtsParada(null).isEmpty());
		assertTrue(api.getTesourosCidade(null).isEmpty());
		assertEquals(0, observador.chamadas);
	}

	/** Ataque retira o tipo escolhido e o segundo exige duas espadas, sem cobrança na recusa. */
	@Test public void atacarPelaFachadaRespeitaEscolhaECustoCrescente() {
		completa("CHAGATAI", "TOLUI");
		assertTrue(api.ativarColuna("AMARELA"));
		assertTrue(api.trocarCavaloPorEspada());
		assertEquals(1, api.getCavalosRestantes());
		assertFalse(api.trocarCavaloPorEspada());
		assertTrue(api.pegarTributo("Vale de Kaesong"));
		assertEquals(2, api.getTributosJogador(0, "ESPADA"));
		String cidade = api.getCidades().stream().filter(api::isCidadeRevelada).findFirst().orElseThrow();
		caminharAte(api.getParadasDaCidade(cidade).get(0));
		String tipo = api.getTesourosCidade(cidade).get(0);
		api.add(observador);
		assertEquals(1, api.getCustoAtaqueCidade(cidade));
		assertTrue(api.podeAtacarCidade(cidade, tipo));
		assertFalse(api.atacarCidade(cidade, "INVALIDO"));
		assertEquals(0, observador.chamadas);
		assertTrue(api.atacarCidade(cidade, tipo));
		assertEquals(3, api.getTesourosRestantesCidade(cidade));
		assertEquals(1, api.getTesourosJogador(0, tipo));
		assertEquals(1, api.getTributosJogador(0, "ESPADA"));
		assertEquals(2, api.getCustoAtaqueCidade(cidade));
		assertFalse(api.atacarCidade(cidade, null));
		assertEquals(1, api.getTributosJogador(0, "ESPADA"));
		assertEquals(1, observador.chamadas);
	}

	/** Ogedei nunca conquista pela habilidade, mesmo em turnos seguintes. */
	@Test public void ogedeiNaoRetiraUltimoTesouro() {
		completa("OGEDEI", "ALTANI");
		String cidade = api.getCidades().stream().filter(api::isCidadeRevelada).findFirst().orElseThrow();
		for (int i = 0; i < 3; i++) {
			if (api.getColunaAtivada() == null) assertTrue(api.ativarColuna("VERDE"));
			assertTrue(api.pegarTesouroComMoeda(cidade, api.getTesourosCidade(cidade).get(0)));
			if (i < 2) turnoVerdeSeguinte();
		}
		turnoVerdeSeguinte();
		assertFalse(api.podePegarTesouroComMoeda(cidade, null));
		assertFalse(api.pegarTesouroComMoeda(cidade, null));
		assertEquals(1, api.getTesourosRestantesCidade(cidade));
		assertNull(api.getConquistadorCidade(cidade));
	}

	private void turnoVerdeSeguinte() {
		proximoTurnoDoPrimeiro(); // Branca recupera as fichas ao terminar.
		finalizar();
		assertTrue(api.ativarColuna(api.getColunasDisponiveis().get(0)));
		finalizar();
		assertTrue(api.ativarColuna("VERDE"));
	}

	private void finalizar() {
		if (api.isKhanPendente()) assertTrue(api.moverKhan(api.getDestinosDoKhan().get(0)));
		assertTrue(api.terminarTurno());
	}
	private void proximoTurnoDoPrimeiro() {
		finalizar();
		assertTrue(api.ativarColuna(api.getColunasDisponiveis().get(0)));
		finalizar();
		String coluna = api.getColunasDisponiveis().contains("BRANCA") ? "BRANCA" : api.getColunasDisponiveis().get(0);
		assertTrue(api.ativarColuna(coluna));
	}
	private void caminharAte(String destino) {
		Map<String, String> anterior = new HashMap<>();
		Queue<String> fila = new ArrayDeque<>();
		String origem = api.getPosicaoPeao(0, 0);
		fila.add(origem);
		anterior.put(origem, null);
		while (!fila.isEmpty()) {
			String atual = fila.remove();
			for (String vizinha : api.getVizinhas(atual)) if (!anterior.containsKey(vizinha)) {
				anterior.put(vizinha, atual);
				fila.add(vizinha);
			}
		}
		List<String> rota = new ArrayList<>();
		for (String atual = destino; !atual.equals(origem); atual = anterior.get(atual)) rota.add(0, atual);
		for (String parada : rota) {
			if (api.getCavalosRestantes() == 0) proximoTurnoDoPrimeiro();
			assertTrue("Passo até " + parada, api.moverPara(parada));
		}
	}
}
