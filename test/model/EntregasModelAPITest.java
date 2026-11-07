package model;

import static org.junit.Assert.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.After;
import org.junit.Test;

/** Integração por jogadas públicas, com sorteios reproduzíveis e cadastro sintético. */
public class EntregasModelAPITest {
	private final ModelAPI api = ModelAPI.getInstance();
	private final Contador contador = new Contador();
	private static class Contador implements ObservadorIF {
		int chamadas;
		@Override public void notify(ObservadoIF observado) { chamadas++; }
	}
	@After public void limpar() { api.remove(contador); }
	public static Map<String, Integer> ofertaCapital() { return Map.of("FERRO", 1, "PELES", 1, "GADO", 1); }

	/** Cenário compartilhado com a interface: três tesouros obtidos pela habilidade de Ogedei. */
	public static void prepararEntregaCapital(int semente) {
		ModelAPI api = ModelAPI.getInstance();
		assertTrue(api.novaPartidaCompleta(List.of("Ana", "Bia"), List.of("OGEDEI", "ALTANI"), PreparacaoCompletaTest.cadastro(), new Random(semente)));
		assertTrue(api.escolherCartaSecreta(0));
		assertTrue(api.escolherCartaSecreta(0));
		assertTrue(api.ativarColuna("VERDE"));
		coletarPedido();
	}
	private static void coletarPedido() {
		ModelAPI api = ModelAPI.getInstance();
		int indice = 0;
		for (String tipo : List.of("FERRO", "PELES", "GADO")) {
			if (indice++ > 0) proximoVerde();
			String cidade = api.getCidades().stream().filter(c -> api.getTesourosCidade(c).contains(tipo)
					&& api.podePegarTesouroComMoeda(c, tipo)).findFirst().orElseThrow();
			assertTrue(api.pegarTesouroComMoeda(cidade, tipo));
		}
	}
	private static void finalizar() {
		ModelAPI api = ModelAPI.getInstance();
		if (api.isKhanPendente()) assertTrue(api.moverKhan(api.getDestinosDoKhan().get(0)));
		assertTrue(api.terminarTurno());
	}
	private static void passarOutroJogador() {
		ModelAPI api = ModelAPI.getInstance();
		assertTrue(api.ativarColuna(api.getColunasDisponiveis().get(0)));
		finalizar();
	}
	private static void proximoVerde() {
		ModelAPI api = ModelAPI.getInstance();
		finalizar(); passarOutroJogador();
		assertTrue(api.ativarColuna("BRANCA"));
		finalizar(); passarOutroJogador();
		assertTrue(api.ativarColuna("VERDE"));
	}

	@Test public void entregaPagaPedidoPontuaEReservaBonusUmaVez() {
		prepararEntregaCapital(12);
		assertEquals("TESOURO", api.getBonusConselheiro("Karakorum"));
		assertEquals(ofertaCapital(), api.getPedidoConselheiro("Karakorum"));
		assertTrue(api.isConselheiroVisitado("Karakorum"));
		api.add(contador);
		assertTrue(api.podeEntregarTesouros("Karakorum", ofertaCapital()));
		assertTrue(api.podeEntregarTesouros("Karakorum", ofertaCapital()));
		assertEquals(0, contador.chamadas);
		assertFalse(api.entregarTesouros("Karakorum", Map.of("FERRO", 2)));
		assertTrue(api.entregarTesouros("Karakorum", ofertaCapital()));
		assertEquals(1, contador.chamadas);
		assertEquals(2, api.getVotosJogador(0));
		assertEquals(List.of("TESOURO"), api.getBonusJogador(0));
		assertNull(api.getBonusConselheiro("Karakorum"));
		for (String tipo : ofertaCapital().keySet()) assertEquals(0, api.getTesourosJogador(0, tipo));
		assertFalse(api.entregarTesouros("Karakorum", ofertaCapital()));
		assertEquals(1, contador.chamadas);
		api.remove(contador);
		proximoVerde(); coletarPedido();
		assertTrue(api.entregarTesouros("Karakorum", ofertaCapital()));
		assertEquals(List.of("TESOURO"), api.getBonusJogador(0));
		assertEquals(4, api.getVotosJogador(0));
	}

	@Test public void bonusDeTesouroBloqueiaTurnoENotificaEscolha() {
		prepararEntregaCapital(12);
		assertTrue(api.entregarTesouros("Karakorum", ofertaCapital()));
		api.add(contador);
		assertTrue(api.podeUsarBonus("TESOURO"));
		assertTrue(api.podeUsarBonus("TESOURO"));
		assertFalse(api.isBonusUsado(0, "TESOURO"));
		assertEquals(0, contador.chamadas);
		assertTrue(api.usarBonus("TESOURO"));
		assertEquals(1, contador.chamadas);
		assertTrue(api.isBonusUsado(0, "TESOURO"));
		assertEquals(3, api.getOpcoesTesouroDoBonus().size());
		assertTrue(api.isTesouroDoBonusPendente());
		assertFalse(api.terminarTurno());
		assertFalse(api.usarBonus("TESOURO"));
		assertFalse(api.escolherTesouroDoBonus("INVALIDO"));
		String tipo = api.getOpcoesTesouroDoBonus().get(0);
		assertTrue(api.escolherTesouroDoBonus(tipo));
		assertEquals(1, api.getTesourosJogador(0, tipo));
		assertFalse(api.isTesouroDoBonusPendente());
		assertTrue(api.getOpcoesTesouroDoBonus().isEmpty());
		assertFalse(api.escolherTesouroDoBonus(tipo));
		assertTrue(api.podeTerminarTurno());
		assertEquals(2, contador.chamadas);
	}

	@Test public void bonusDeRecursosEAcoesSoPodemSerUsadosUmaVez() {
		String[] bonus = { "TRIBUTOS", "MOVIMENTO_E_TRIBUTO", "MOEDAS", "YURTS", "ESPADAS" };
		int[] sementes = { 0, 1, 5, 7, 8 };
		for (int i = 0; i < bonus.length; i++) {
			prepararEntregaCapital(sementes[i]);
			assertEquals(bonus[i], api.getBonusConselheiro("Karakorum"));
			assertTrue(api.entregarTesouros("Karakorum", ofertaCapital()));
			int cavalos = api.getCavalosRestantes(), maos = api.getMaosRestantes();
			int moedas = api.getTributosJogador(0, "MOEDA"), yurts = api.getTributosJogador(0, "YURT"), espadas = api.getTributosJogador(0, "ESPADA");
			assertTrue(api.usarBonus(bonus[i]));
			assertTrue(api.isBonusUsado(0, bonus[i]));
			assertFalse(api.usarBonus(bonus[i]));
			switch (bonus[i]) {
			case "TRIBUTOS": assertEquals(maos + 2, api.getMaosRestantes()); break;
			case "MOVIMENTO_E_TRIBUTO": assertEquals(cavalos + 3, api.getCavalosRestantes()); assertEquals(maos + 1, api.getMaosRestantes()); break;
			case "MOEDAS": assertEquals(moedas + 2, api.getTributosJogador(0, "MOEDA")); break;
			case "YURTS": assertEquals(yurts + 2, api.getTributosJogador(0, "YURT")); break;
			case "ESPADAS": assertEquals(espadas + 2, api.getTributosJogador(0, "ESPADA")); break;
			}
		}
	}

	@Test public void votoAutomaticoNaoPodeSerReutilizado() {
		prepararEntregaCapital(9);
		assertTrue(api.entregarTesouros("Karakorum", ofertaCapital()));
		assertEquals(3, api.getVotosJogador(0));
		assertTrue(api.isBonusUsado(0, "VOTO"));
		assertFalse(api.podeUsarBonus("VOTO"));
		assertFalse(api.usarBonus("VOTO"));
	}

	@Test public void ofertasInvalidasOuSemAlcanceNaoAlteramEstado() {
		prepararEntregaCapital(12);
		api.add(contador);
		assertFalse(api.entregarTesouros("Conselheiro Norte", ofertaCapital()));
		assertFalse(api.entregarTesouros(null, ofertaCapital()));
		assertFalse(api.entregarTesouros("Karakorum", null));
		for (Map<String, Integer> invalida : List.of(Map.of("FERRO", -1), Map.of("FERRO", 4), Map.of("INVALIDO", 1), Map.of("FERRO", 1, "ferro", 1))) {
			assertFalse(api.podeEntregarTesouros("Karakorum", invalida));
			assertFalse(api.entregarTesouros("Karakorum", invalida));
		}
		Map<String, Integer> comNull = new HashMap<>(); comNull.put("FERRO", null);
		assertFalse(api.entregarTesouros("Karakorum", comNull));
		for (String tipo : ofertaCapital().keySet()) assertEquals(1, api.getTesourosJogador(0, tipo));
		assertEquals(0, api.getVotosJogador(0));
		assertEquals(0, contador.chamadas);
		assertFalse(api.usarBonus("TESOURO"));
		assertFalse(api.usarBonus(null));
		assertEquals(0, contador.chamadas);
	}
}
