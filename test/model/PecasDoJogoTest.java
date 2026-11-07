package model;

import static org.junit.Assert.*;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

/** Dados conferidos na revisão do EAD e hipótese documentada das multiplicidades. */
public class PecasDoJogoTest {
	@Test public void distribuicaoRespeitaTotaisECoresDosTesouros() {
		Map<String, Integer> cores = new HashMap<>(), simbolos = new HashMap<>();
		Map<String, String> tesouros = new HashMap<>();
		for (String linha : PecasDoJogo.cadastro()) {
			String[] campos = linha.split(";");
			if (!campos[0].equals("MELHORIA")) continue;
			int quantidade = Integer.parseInt(campos[3]);
			cores.merge(campos[1], quantidade, Integer::sum); simbolos.merge(campos[2], quantidade, Integer::sum);
			if (quantidade == 1) tesouros.put(campos[2], campos[1]);
		}
		assertEquals(Map.of("AMARELA",15,"VERMELHA",15,"VERDE",14), cores);
		assertEquals(44, cores.values().stream().mapToInt(Integer::intValue).sum());
		assertEquals(Integer.valueOf(8), simbolos.get("MOEDA")); assertEquals(Integer.valueOf(8), simbolos.get("ESPADA"));
		assertEquals(Integer.valueOf(8), simbolos.get("YURT")); assertEquals(Integer.valueOf(6), simbolos.get("MAO")); assertEquals(Integer.valueOf(9), simbolos.get("CAVALOS"));
		assertEquals(Map.of("GADO","AMARELA","ESPECIARIAS","AMARELA","FERRO","VERMELHA","PELES","VERDE","TECIDO","VERDE"), tesouros);
	}
	@Test public void conselheirosCorrespondemAoTabuleiro() {
		ComponentesKhan componentes = PecasDoJogo.criar(MapaOficial.criar(), new java.util.Random(1));
		assertEquals(Map.of(TipoTesouro.TECIDO,2), componentes.getConselheiro("Conselheiro Norte").getPedido());
		assertEquals(Map.of(TipoTesouro.FERRO,2), componentes.getConselheiro("Conselheiro Nordeste").getPedido());
		assertEquals(Map.of(TipoTesouro.PELES,2), componentes.getConselheiro("Conselheiro Oeste").getPedido());
		assertEquals(Map.of(TipoTesouro.GADO,2), componentes.getConselheiro("Conselheiro Leste").getPedido());
		assertEquals(Map.of(TipoTesouro.ESPECIARIAS,2), componentes.getConselheiro("Conselheiro Sudoeste").getPedido());
		assertTrue(componentes.getConselheiro("Conselheiro Sul").getPedido().isEmpty());
		assertEquals(Map.of(TipoTesouro.FERRO,1,TipoTesouro.PELES,1,TipoTesouro.GADO,1), componentes.getConselheiro("Karakorum").getPedido());
	}
	@Test public void janelasNotificamUmaVezEFecharCartasOcultaSegredo() {
		ModelAPI api = ModelAPI.getInstance();
		assertTrue(api.novaPartida(java.util.List.of("Ana","Bia"),java.util.List.of("ALTANI","OGEDEI")));
		assertFalse(api.abrirJanelaCartas()); assertFalse(api.abrirJanelaCartela());
		PreparacaoCompletaTest.concluirPreparacao();
		final int[] notificacoes = { 0 };
		ObservadorIF observador = new ObservadorIF() { @Override public void notify(ObservadoIF observado) { notificacoes[0]++; } };
		api.add(observador);
		try {
			assertTrue(api.abrirJanelaCartas()); assertFalse(api.abrirJanelaCartas()); assertEquals(1, notificacoes[0]);
			assertTrue(api.mostrarCartaSecreta()); assertNotNull(api.getIdCartaSecretaAtual());
			assertTrue(api.fecharJanelaCartas()); assertFalse(api.isCartaSecretaVisivel()); assertNull(api.getIdCartaSecretaAtual());
			assertFalse(api.fecharJanelaCartas()); assertEquals(3, notificacoes[0]);
			assertTrue(api.abrirJanelaCartela()); assertTrue(api.selecionarCartela(1)); assertTrue(api.fecharJanelaCartela());
			assertEquals(6, notificacoes[0]);
		} finally { api.remove(observador); }
	}
}
