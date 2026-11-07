package model;

import static org.junit.Assert.*;
import java.util.List;
import org.junit.After;
import org.junit.Test;

/** Exercita mercado e cartela pela fachada pública usada pela interface. */
public class MelhoriasModelAPITest {
	private final ModelAPI api = ModelAPI.getInstance();
	private final Contador contador = new Contador();
	private static class Contador implements ObservadorIF {
		int chamadas;
		@Override public void notify(ObservadoIF observado) { chamadas++; }
	}
	@After public void limpar() { api.remove(contador); }
	private void iniciar() {
		assertTrue(api.novaPartidaCompleta(List.of("Ana", "Bia"), List.of("OGEDEI", "ALTANI"), PreparacaoCompletaTest.cadastro(), new java.util.Random(42)));
		assertTrue(api.escolherCartaSecreta(0));
		assertTrue(api.escolherCartaSecreta(0));
	}

	@Test public void compraAbertaUsaMoedasVirtuaisNotificaEAtualizaCartela() {
		iniciar();
		assertTrue(api.ativarColuna("VERDE"));
		List<String> mercado = api.getMercadoMelhorias();
		String coluna = mercado.get(0).split(":")[0];
		api.add(contador);
		assertTrue(api.podeComprarMelhoria(0));
		assertTrue(api.podeComprarMelhoria(0));
		assertEquals(0, contador.chamadas);
		assertFalse(api.comprarMelhoria(-2));
		assertFalse(api.comprarMelhoria(99));
		assertEquals(mercado, api.getMercadoMelhorias());
		assertTrue(api.comprarMelhoria(0));
		assertEquals(mercado.subList(1, 5), api.getMercadoMelhorias());
		assertEquals(1, api.getMelhoriasJogador(0, coluna).size());
		assertTrue(api.getMelhoriasJogador(0, coluna).get(0).contains("próximo turno"));
		assertEquals(0, api.getTributosJogador(0, "MOEDA"));
		assertEquals(0, api.getTributosVirtuais("MOEDA"));
		assertEquals(1, contador.chamadas);
		assertFalse(api.comprarMelhoria(0));
		assertEquals(1, contador.chamadas);
		assertTrue(api.terminarTurno());
		assertFalse(api.getMelhoriasJogador(0, coluna).get(0).contains("próximo turno"));
		assertTrue(api.getMelhoriasJogador(1, coluna).isEmpty());
	}

	@Test public void comprarPilhaRetiraSomenteDaReserva() {
		iniciar();
		assertTrue(api.ativarColuna("VERDE"));
		List<String> mercado = api.getMercadoMelhorias();
		int reserva = api.getMelhoriasNaReserva();
		assertTrue(api.podeComprarMelhoria(-1));
		assertTrue(api.comprarMelhoria(-1));
		assertEquals(mercado, api.getMercadoMelhorias());
		assertEquals(reserva - 1, api.getMelhoriasNaReserva());
		int pecas = 0;
		for (String coluna : List.of("VERMELHA", "VERDE", "AMARELA")) pecas += api.getMelhoriasJogador(0, coluna).size();
		assertEquals(1, pecas);
	}

	@Test public void khanRepoeMercadoPelaFachada() {
		iniciar();
		assertTrue(api.ativarColuna("VERDE"));
		assertTrue(api.comprarMelhoria(0));
		assertEquals(4, api.getMercadoMelhorias().size());
		assertTrue(api.terminarTurno());
		assertTrue(api.ativarColuna("VERDE"));
		assertTrue(api.terminarTurno());
		assertTrue(api.ativarColuna("BRANCA"));
		int reserva = api.getMelhoriasNaReserva();
		api.add(contador);
		assertTrue(api.moverKhan(ModelAPI.AREA_MELHORIAS));
		assertEquals(5, api.getMercadoMelhorias().size());
		assertEquals(reserva - 1, api.getMelhoriasNaReserva());
		assertEquals(1, contador.chamadas);
	}

	@Test public void preparacaoNaoPermiteComprarOuPegarGratis() {
		assertTrue(api.novaPartida(List.of("Ana", "Bia"), List.of("JOCHI", "ALTANI")));
		assertEquals(0, api.getMelhoriasJogador(0, "BRANCA").size());
		assertFalse(api.podeComprarMelhoria(0));
		assertFalse(api.comprarMelhoria(-1));
		assertFalse(api.escolherMelhoriaGratuita(0));
		assertEquals(5, api.getMercadoMelhorias().size());
	}

	/** Completa uma linha com compras reais e resolve a recompensa sem cobrar moedas. */
	@Test public void linhaCompletaBloqueiaTurnoAteEscolherBranca() {
		prepararRecompensaPendente();
		assertFalse(api.podeTerminarTurno());
		assertFalse(api.terminarTurno());
		int moedas = api.getTributosJogador(0, "MOEDA");
		api.add(contador);
		assertFalse(api.escolherMelhoriaGratuita(99));
		assertTrue(api.podeEscolherMelhoriaGratuita(0));
		assertTrue(api.podeEscolherMelhoriaGratuita(0));
		assertEquals(0, contador.chamadas);
		assertTrue(api.escolherMelhoriaGratuita(0));
		assertEquals(1, contador.chamadas);
		assertEquals(1, api.getMelhoriasJogador(0, "BRANCA").size());
		assertEquals(moedas, api.getTributosJogador(0, "MOEDA"));
		assertFalse(api.isMelhoriaGratuitaPendente());
		assertFalse(api.escolherMelhoriaGratuita(0));
		if (api.isKhanPendente()) assertTrue(api.moverKhan(api.getDestinosDoKhan().get(0)));
		assertTrue(api.podeTerminarTurno());
	}

	/** Cenário reproduzível compartilhado com o teste de interface da recompensa. */
	public static void prepararRecompensaPendente() {
		new MelhoriasModelAPITest().montarLinha();
	}
	private void montarLinha() {
		iniciar();
		boolean conseguiu = false;
		for (int tentativa = 0; tentativa < 100; tentativa++) {
			assertTrue(api.ativarColuna(tentativa % 2 == 0 ? "VERDE" : "BRANCA"));
			if (api.podePegarTributo("Khan do Nordeste")) assertTrue(api.pegarTributo("Khan do Nordeste"));
			List<String> mercado = api.getMercadoMelhorias();
			int indice = -2;
			for (int i = 0; i < mercado.size(); i++) {
				String coluna = mercado.get(i).split(":")[0];
				if (api.getMelhoriasJogador(0, coluna).isEmpty() && api.podeComprarMelhoria(i)) { indice = i; break; }
			}
			if (indice == -2 && api.podeComprarMelhoria(-1)) indice = -1;
			if (indice >= -1) assertTrue(api.comprarMelhoria(indice));
			if (api.isMelhoriaGratuitaPendente()) { conseguiu = true; break; }
			finalizar();
			assertTrue(api.ativarColuna(api.getColunasDisponiveis().get(0)));
			finalizar();
		}
		assertTrue("Deve ser possível completar uma linha com o cadastro de teste", conseguiu);
	}

	private void finalizar() {
		if (api.isKhanPendente()) {
			String destino = api.getMercadoMelhorias().size() < 5 && api.getDestinosDoKhan().contains(ModelAPI.AREA_MELHORIAS)
					? ModelAPI.AREA_MELHORIAS : api.getDestinosDoKhan().contains("Khan do Nordeste")
					? "Khan do Nordeste" : api.getDestinosDoKhan().get(0);
			assertTrue(api.moverKhan(destino));
		}
		assertTrue(api.terminarTurno());
	}
}
