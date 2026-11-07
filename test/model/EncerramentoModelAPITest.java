package model;

import static org.junit.Assert.*;
import java.util.List;
import java.util.Random;
import org.junit.After;
import org.junit.Test;

public class EncerramentoModelAPITest {
	private final ModelAPI api = ModelAPI.getInstance();
	private final Contador contador = new Contador();
	private static class Contador implements ObservadorIF {
		int chamadas;
		@Override public void notify(ObservadoIF observado) { chamadas++; }
	}
	@After public void limpar() { api.remove(contador); }
	/** Sorteio fixo para o teste de partida completa pelos controles gráficos. */
	public static void prepararPartida() {
		assertTrue(ModelAPI.getInstance().novaPartidaCompleta(List.of("Ana", "Bia"), List.of("CHAGATAI", "ALTANI"), PecasDoJogo.cadastro(), new Random(42)));
	}
	@Test public void cartasSecretasESelecaoDeCartelaAtualizamObserver() {
		prepararPartida();
		assertFalse(api.mostrarCartaSecreta());
		assertTrue(api.escolherCartaSecreta(0)); assertTrue(api.escolherCartaSecreta(1));
		api.add(contador);
		assertEquals("", api.getCartaSecretaAtual());
		assertTrue(api.mostrarCartaSecreta());
		assertFalse(api.getCartaSecretaAtual().isEmpty());
		assertFalse(api.mostrarCartaSecreta());
		assertEquals(1, contador.chamadas);
		assertTrue(api.ocultarCartaSecreta());
		assertEquals("", api.getCartaSecretaAtual());
		assertTrue(api.selecionarCartela(1));
		assertEquals(1, api.getJogadorCartela());
		assertFalse(api.selecionarCartela(99));
		assertFalse(api.selecionarCartela(1));
		assertTrue(api.mostrarCartaSecreta());
		assertTrue(api.ativarColuna("AMARELA"));
		assertTrue(api.terminarTurno());
		assertFalse(api.isCartaSecretaVisivel());
		assertEquals("", api.getCartaSecretaAtual());
		assertEquals(1, api.getJogadorCartela());
		assertTrue(api.getVencedores().isEmpty());
		assertEquals("", api.getCartaSecretaFinal(0));
	}
	@Test public void progressoNaoPontuaNemAlteraAsCartas() {
		prepararPartida(); assertTrue(api.escolherCartaSecreta(0)); assertTrue(api.escolherCartaSecreta(0));
		api.add(contador);
		List<String> cartas = api.getCartasAbertas();
		assertEquals(4, api.getObjetivosAbertosComProgresso().size());
		assertEquals(4, api.getObjetivosAbertosComProgresso().size());
		assertEquals(cartas, api.getCartasAbertas());
		assertEquals(0, api.getVotosJogador(0));
		assertEquals(0, api.getPosicaoKurultai());
		assertEquals(10, api.getLimiteKurultai());
		assertEquals(-1, api.getRodadaFinal());
		assertEquals(0, contador.chamadas);
	}
	@Test public void rotaExplicitaRecusadaPreservaPeaoECavalos() {
		prepararPartida(); assertTrue(api.escolherCartaSecreta(0)); assertTrue(api.escolherCartaSecreta(0));
		assertTrue(api.ativarColuna("AMARELA"));
		api.add(contador);
		assertFalse(api.moverPor(List.of("Kiev Nordeste"), List.of(false)));
		assertFalse(api.moverPor(List.of("Sarai Sudoeste"), List.of(true)));
		assertEquals("Karakorum", api.getPosicaoPeao(0, 0));
		assertEquals(2, api.getCavalosRestantes());
		assertTrue(api.podeMoverPor(List.of("Sarai Sudoeste", "Moscou Nordeste"), List.of(false, false)));
		assertEquals(0, contador.chamadas);
		assertTrue(api.moverPor(List.of("Sarai Sudoeste", "Moscou Nordeste"), List.of(false, false)));
		assertEquals("Moscou Nordeste", api.getPosicaoPeao(0, 0));
		assertEquals(0, api.getCavalosRestantes());
		assertEquals(1, contador.chamadas);
	}
	@Test public void rotaEscolheSaltarOuVisitarYurtProprio() {
		for (boolean saltar : List.of(true, false)) {
			assertTrue(PreparacaoCompletaTest.iniciarPadrao(List.of("Ana", "Bia"), List.of("TOLUI", "ALTANI")));
			assertTrue(api.ativarColuna("VERDE"));
			assertTrue(api.pegarTributo("Estepe Central")); assertTrue(api.construirYurt("Sarai Sudoeste"));
			assertTrue(api.terminarTurno()); assertTrue(api.ativarColuna("VERDE")); assertTrue(api.terminarTurno());
			assertTrue(api.ativarColuna("BRANCA"));
			assertTrue(api.moverPor(List.of("Sarai Sudoeste", "Moscou Nordeste"), List.of(saltar, false)));
			assertEquals(saltar ? 1 : 0, api.getCavalosRestantes());
		}
	}

}
