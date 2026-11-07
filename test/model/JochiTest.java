package model;

import static org.junit.Assert.*;
import java.util.List;
import org.junit.Test;

/** Regressões da habilidade impressa na cartela oficial de Jochi. */
public class JochiTest {
	@Test public void escolheUmaEntreDuasAntesDeIniciarESemPerderPecas() {
		ComponentesKhanTest.Cenario c = new ComponentesKhanTest.Cenario(Herdeiro.JOCHI, 20);
		List<Melhoria> opcoes = c.componentes.opcoesMelhoriaInicial(c.jogador());
		assertEquals(2, opcoes.size()); assertNotSame(opcoes.get(0), opcoes.get(1));
		assertEquals(0, c.inventario().contarMelhorias(Coluna.BRANCA));
		for (Jogador jogador : c.base.jogadores) assertTrue(c.componentes.escolherCartaSecreta(jogador, c.componentes.cartasDe(jogador).get(0)));
		assertFalse(c.partida.iniciar());
		assertFalse(c.componentes.escolherMelhoriaInicial(c.jogador(), -1));
		assertEquals(13, c.componentes.melhoriasNaReserva());
		assertTrue(c.componentes.escolherMelhoriaInicial(c.jogador(), 1));
		assertSame(opcoes.get(1), c.inventario().getMelhorias(Coluna.BRANCA).get(0));
		assertEquals(14, c.componentes.melhoriasNaReserva());
		assertEquals(20, c.componentes.melhoriasNaReserva() + c.componentes.getMercado().size() + c.inventario().contarMelhorias(Coluna.BRANCA));
		assertFalse(c.componentes.escolherMelhoriaInicial(c.jogador(), 0));
		assertTrue(c.partida.iniciar());
	}
	@Test public void jochiCompraEmOutraCorSemAnteciparEfeito() {
		ComponentesKhanTest.Cenario c = new ComponentesKhanTest.Cenario(Herdeiro.JOCHI, 20).iniciar();
		c.partida.receberTributoDoEstoque(TipoTributo.MOEDA, 1);
		Melhoria melhoria = c.componentes.getMercado().get(0);
		assertTrue(c.componentes.podeComprarMelhoria(0, Coluna.VERMELHA));
		assertTrue(c.componentes.comprarMelhoria(0, Coluna.VERMELHA));
		assertSame(melhoria, c.inventario().getMelhorias(Coluna.VERMELHA).get(0));
		assertEquals(0, c.jogador().contarTributo(TipoTributo.MOEDA));
		assertFalse(c.inventario().podeAtivar(melhoria, c.partida.getTurno().getNumero()));
	}
	@Test public void outrosHerdeirosNaoPodemIgnorarACor() {
		ComponentesKhanTest.Cenario c = new ComponentesKhanTest.Cenario(Herdeiro.CHAGATAI, 20).iniciar();
		c.partida.receberTributoDoEstoque(TipoTributo.MOEDA, 1);
		assertFalse(c.componentes.comprarMelhoria(0, Coluna.VERMELHA));
		assertFalse(c.componentes.comprarMelhoria(0, Coluna.BRANCA));
		assertEquals(2, c.jogador().contarTributo(TipoTributo.MOEDA));
		assertEquals(5, c.componentes.getMercado().size());
		assertTrue(c.componentes.comprarMelhoria(0, Coluna.VERDE));
	}
	@Test public void colunaCheiaRecusaCompraSemCobrarOuRetirarPeca() {
		ComponentesKhanTest.Cenario c = new ComponentesKhanTest.Cenario(Herdeiro.JOCHI, 20).iniciar();
		c.partida.receberTributoDoEstoque(TipoTributo.MOEDA, 1);
		for (int i = 0; i < 4; i++) ObjetivosTest.adicionar(c.inventario(), Coluna.VERMELHA, Melhoria.Simbolo.MAO);
		List<Melhoria> mercado = c.componentes.getMercado();
		assertFalse(c.componentes.comprarMelhoria(0, Coluna.VERMELHA));
		assertFalse(c.componentes.comprarMelhoria(0, null));
		assertEquals(2, c.jogador().contarTributo(TipoTributo.MOEDA));
		assertEquals(mercado, c.componentes.getMercado());
	}
	@Test public void compraNaBrancaNaoEliminaRecompensaDaSegundaLinha() {
		ComponentesKhanTest.Cenario c = new ComponentesKhanTest.Cenario(Herdeiro.JOCHI, 20).iniciar();
		c.partida.receberTributoDoEstoque(TipoTributo.MOEDA, 1);
		assertTrue(c.componentes.comprarMelhoria(0, Coluna.BRANCA));
		for (Coluna coluna : List.of(Coluna.VERMELHA, Coluna.VERDE, Coluna.AMARELA)) {
			ObjetivosTest.adicionar(c.inventario(), coluna, Melhoria.Simbolo.MAO);
			ObjetivosTest.adicionar(c.inventario(), coluna, Melhoria.Simbolo.MAO);
		}
		assertTrue(c.componentes.temMelhoriaPendente(c.jogador()));
		assertTrue(c.componentes.escolherMelhoriaGratuita(0));
		assertEquals(3, c.inventario().contarMelhorias(Coluna.BRANCA));
		assertFalse(c.componentes.temMelhoriaPendente(c.jogador()));
	}
	@Test public void compraCegaDeJochiRespeitaDestinoECusto() {
		ComponentesKhanTest.Cenario c = new ComponentesKhanTest.Cenario(Herdeiro.JOCHI, 20).iniciar();
		c.partida.receberTributoDoEstoque(TipoTributo.MOEDA, 1);
		int reserva = c.componentes.melhoriasNaReserva();
		assertTrue(c.componentes.comprarMelhoria(-1, Coluna.AMARELA));
		assertEquals(reserva - 1, c.componentes.melhoriasNaReserva());
		assertEquals(1, c.inventario().contarMelhorias(Coluna.AMARELA));
		assertEquals(5, c.componentes.getMercado().size());
	}
}
