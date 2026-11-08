package model;

import static org.junit.Assert.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.Test;

public class ComponentesKhanTest {
	/** Objetivo: escolhas iniciais. Entrada: duas cartas por pessoa. Saída esperada: início bloqueado até ambos escolherem. */
	@Test public void preparacaoExigeEscolhaDeTodosESemTrocaPosterior() {
		Cenario c = new Cenario(Herdeiro.CHAGATAI, 20);
		assertEquals(4, c.componentes.getCartasAbertas().size());
		assertFalse(c.partida.iniciar());
		Carta carta = c.componentes.cartasDe(c.jogador()).get(0);
		assertEquals(2, c.componentes.cartasDe(c.jogador()).size());
		assertFalse(c.componentes.escolherCartaSecreta(c.jogador(), CartasOficiais.secretas().get(0)));
		assertTrue(c.componentes.escolherCartaSecreta(c.jogador(), carta));
		assertFalse(c.componentes.escolherCartaSecreta(c.jogador(), carta));
		assertFalse(c.partida.iniciar());
		Jogador outro = c.base.jogadores.get(1);
		assertTrue(c.componentes.escolherCartaSecreta(outro, c.componentes.cartasDe(outro).get(0)));
		assertTrue(c.partida.iniciar());
		assertEquals(2, c.componentes.getDescarteCartas().size());
		assertEquals(List.of(carta), c.componentes.cartasDe(c.jogador()));
	}
	/** Objetivo: compra paga corretamente. Entrada: uma moeda física e uma virtual. Saída esperada: duas gastas, uma devolvida ao banco. */
	@Test public void compraUsaVirtualPrimeiroESoAtivaNoTurnoSeguinte() {
		Cenario c = new Cenario(Herdeiro.CHAGATAI, 20).iniciar();
		assertTrue(c.partida.ativarColuna(Coluna.VERDE));
		int banco = c.partida.getEstoque(TipoTributo.MOEDA);
		int cavalos = c.partida.getTurno().getCavalos();
		Melhoria melhoria = c.componentes.getMercado().get(0);
		assertTrue(c.componentes.comprarMelhoria(0));
		assertEquals(0, c.jogador().contarTributo(TipoTributo.MOEDA));
		assertEquals(0, c.partida.getTurno().getRecursoVirtual(TipoTributo.MOEDA));
		assertEquals(banco + 1, c.partida.getEstoque(TipoTributo.MOEDA));
		assertEquals(4, c.componentes.getMercado().size());
		assertEquals(cavalos, c.partida.getTurno().getCavalos());
		assertFalse(c.inventario().podeAtivar(melhoria, 1));
		assertTrue(c.inventario().podeAtivar(melhoria, 3));
	}
	/** Objetivo: comprar antes de ativar não antecipa efeito. Entrada: compra de cavalos, depois ativação. Saída esperada: só cavalos impressos. */
	@Test public void compraAntesDaAtivacaoNaoRecebeBeneficioNoMesmoTurno() {
		Cenario c = new Cenario(Herdeiro.CHAGATAI, 20).iniciar();
		assertEquals(1, c.partida.receberTributoDoEstoque(TipoTributo.MOEDA, 1));
		assertTrue(c.componentes.comprarMelhoria(0));
		assertTrue(c.partida.ativarColuna(Coluna.VERDE));
		assertEquals(c.jogador().getHerdeiro().getAcoes(Coluna.VERDE).getCavalos(), c.partida.getTurno().getCavalos());
	}
	/** Objetivo: recusa atômica. Entrada: compra sem moedas, índice inválido e coluna cheia. Saída esperada: nenhuma peça ou moeda perdida. */
	@Test public void compraInvalidaPreservaPecasERecursos() {
		Cenario c = new Cenario(Herdeiro.CHAGATAI, 20).iniciar();
		List<Melhoria> mercado = c.componentes.getMercado();
		assertFalse(c.componentes.comprarMelhoria(0));
		assertFalse(c.componentes.comprarMelhoria(-2));
		assertEquals(mercado, c.componentes.getMercado());
		assertEquals(1, c.jogador().contarTributo(TipoTributo.MOEDA));
		for (int n = 0; n < 4; n++) ObjetivosTest.adicionar(c.inventario(), Coluna.VERDE, Melhoria.Simbolo.MAO);
		c.partida.receberTributoDoEstoque(TipoTributo.MOEDA, 1);
		assertFalse(c.componentes.comprarMelhoria(0));
		assertEquals(2, c.jogador().contarTributo(TipoTributo.MOEDA));
		assertEquals(mercado, c.componentes.getMercado());
	}
	/** Objetivo: recompensa de linha obrigatória. Entrada: completar três cores. Saída esperada: bloqueio até escolher branca gratuita. */
	@Test public void linhaCompletaExigeMelhoriaBrancaGratuita() {
		Cenario c = new Cenario(Herdeiro.CHAGATAI, 20).iniciar();
		ObjetivosTest.adicionar(c.inventario(), Coluna.VERMELHA, Melhoria.Simbolo.MAO);
		ObjetivosTest.adicionar(c.inventario(), Coluna.AMARELA, Melhoria.Simbolo.MAO);
		assertTrue(c.partida.ativarColuna(Coluna.VERDE)); assertTrue(c.componentes.comprarMelhoria(0));
		assertTrue(c.componentes.temEscolhaPendente(c.jogador()));
		assertFalse(c.partida.terminarTurno());
		assertFalse(c.componentes.escolherMelhoriaGratuita(99));
		assertTrue(c.componentes.escolherMelhoriaGratuita(-1));
		assertEquals(1, c.inventario().contarMelhorias(Coluna.BRANCA));
		assertFalse(c.componentes.temEscolhaPendente(c.jogador()));
		assertTrue(c.partida.terminarTurno());
	}
	/** Objetivo: Jochi não ganha duas vezes a primeira branca. Entrada: melhoria inicial e primeira linha. Saída esperada: nenhuma escolha. */
	@Test public void jochiRecebeMelhoriaAleatoriaENaoDuplicaPrimeiraLinha() {
		Cenario c = new Cenario(Herdeiro.JOCHI, 20).iniciar();
		assertEquals(1, c.inventario().contarMelhorias(Coluna.BRANCA));
		assertEquals(14, c.componentes.melhoriasNaReserva());
		for (Coluna coluna : new Coluna[] { Coluna.VERMELHA, Coluna.VERDE, Coluna.AMARELA }) ObjetivosTest.adicionar(c.inventario(), coluna, Melhoria.Simbolo.MAO);
		assertFalse(c.componentes.temEscolhaPendente(c.jogador()));
		assertFalse(c.componentes.escolherMelhoriaGratuita(0));
	}
	/** Objetivo: esgotamento não bloqueia turno. Entrada: última compra fecha linha sem peças restantes. Saída esperada: sem escolha impossível. */
	@Test public void reservaEsgotadaNaoBloqueiaRecompensaImpossivel() {
		Cenario c = new Cenario(Herdeiro.CHAGATAI, 5).iniciar();
		// Quatro melhorias compradas por dois jogadores; a quinta fecha a primeira linha de Azul.
		c.partida.receberTributoDoEstoque(TipoTributo.MOEDA, 3);
		assertTrue(c.componentes.comprarMelhoria(0)); assertTrue(c.componentes.comprarMelhoria(0));
		PartidaTest.turnoVazio(c.partida);
		c.partida.receberTributoDoEstoque(TipoTributo.MOEDA, 3);
		assertTrue(c.componentes.comprarMelhoria(0)); assertTrue(c.componentes.comprarMelhoria(0));
		PartidaTest.turnoVazio(c.partida);
		ObjetivosTest.adicionar(c.inventario(), Coluna.VERMELHA, Melhoria.Simbolo.MAO);
		ObjetivosTest.adicionar(c.inventario(), Coluna.AMARELA, Melhoria.Simbolo.MAO);
		c.partida.receberTributoDoEstoque(TipoTributo.MOEDA, 2);
		assertTrue(c.componentes.comprarMelhoria(0));
		assertTrue(c.componentes.getMercado().isEmpty());
		assertFalse(c.componentes.temEscolhaPendente(c.jogador()));
		assertFalse(c.componentes.comprarMelhoria(-1));
	}
	/** Objetivo: ícones expostos antes da reposição. Entrada: mercado com 5,4,3,2,1 peças. Saída esperada: moeda só até 3; espada só até 1. */
	@Test public void khanConcedeApenasIconesExpostosAntesDeRepor() {
		for (int compras = 0; compras <= 4; compras++) {
			Cenario c = new Cenario(Herdeiro.CHAGATAI, 20).iniciar();
			c.partida.receberTributoDoEstoque(TipoTributo.MOEDA, 8);
			for (int n = 0; n < compras; n++) assertTrue(c.componentes.comprarMelhoria(0));
			int moedas = c.jogador().contarTributo(TipoTributo.MOEDA);
			assertTrue(c.partida.ativarColuna(Coluna.VERMELHA));
			assertTrue(c.partida.moverKhanParaMelhorias());
			assertEquals("Moedas após " + compras + " compras", moedas + (compras >= 2 ? 1 : 0), c.jogador().contarTributo(TipoTributo.MOEDA));
			assertEquals("Espadas após " + compras + " compras", compras >= 4 ? 1 : 0, c.jogador().contarTributo(TipoTributo.ESPADA));
			assertEquals(5, c.componentes.getMercado().size());
		}
	}
	/** Objetivo: sincronizar peças e contadores. Entrada: escolha de tesouro visível e quatro ataques. Saída esperada: transferência e nova cidade abastecida. */
	@Test public void ataqueEscolheTipoEConquistaAbasteceNovaCidade() {
		Cenario c = new Cenario(Herdeiro.CHAGATAI, 20).iniciar();
		Cidade cidade = c.base.revelada(); TipoTesouro tipo = c.componentes.tesourosEm(cidade).get(2);
		c.partida.receberTributoDoEstoque(TipoTributo.ESPADA, 7);
		assertTrue(c.partida.atacarCidade(cidade.getNome(), tipo));
		assertEquals(1, c.inventario().contarTesouro(tipo));
		assertEquals(3, c.componentes.tesourosEm(cidade).size());
		for (int n = 0; n < 3; n++) assertTrue(c.partida.atacarCidade(cidade.getNome()));
		assertTrue(cidade.isConquistada());
		assertEquals(4, c.inventario().totalTesouros());
		assertEquals(24, c.componentes.tesourosNaReserva());
		for (Cidade outra : c.base.reveladas()) assertEquals(4, c.componentes.tesourosEm(outra).size());
	}
	/** Objetivo: recusar tipo indisponível. Entrada: cidade com quatro peças e tipo ausente. Saída esperada: recursos e contador intactos. */
	@Test public void ataqueComTipoAusenteNaoGastaEspada() {
		Cenario c = new Cenario(Herdeiro.CHAGATAI, 20).iniciar(); Cidade cidade = c.base.revelada();
		TipoTesouro ausente = null;
		for (TipoTesouro tipo : TipoTesouro.values()) if (!c.componentes.tesourosEm(cidade).contains(tipo)) ausente = tipo;
		assertNotNull(ausente); c.partida.receberTributoDoEstoque(TipoTributo.ESPADA, 1);
		assertFalse(c.partida.atacarCidade(cidade.getNome(), ausente));
		assertEquals(1, c.jogador().contarTributo(TipoTributo.ESPADA));
		assertEquals(4, cidade.getTesouros()); assertEquals(0, c.inventario().totalTesouros());
	}
	/** Objetivo: entrega usa virtual primeiro e fixa Altani. Entrada: três tipos em K, um virtual. Saída esperada: dois físicos descartados, peão fixo. */
	@Test public void entregaNaCapitalUsaTesouroVirtualEFixaPeao() {
		Cenario c = new Cenario(Herdeiro.ALTANI, 20).iniciar();
		c.inventario().receberTesouro(TipoTesouro.FERRO); c.inventario().receberTesouro(TipoTesouro.PELES); c.inventario().receberTesouro(TipoTesouro.GADO);
		c.partida.getTurno().adicionarTesouroVirtual(TipoTesouro.FERRO);
		assertTrue(c.componentes.entregarTesouros("K", pedidoCapital()));
		assertEquals(1, c.inventario().contarTesouro(TipoTesouro.FERRO));
		assertEquals(0, c.partida.getTurno().getTesouroVirtual(TipoTesouro.FERRO));
		assertEquals(2, c.componentes.tesourosDescartados());
		assertEquals(2, c.jogador().getVotos()); assertEquals(2, c.partida.getPosicaoKurultai());
		assertFalse(c.partida.escolherPeao(1));
	}
	/** Objetivo: entrega recusada é atômica. Entrada: falta gado e destino não visitado. Saída esperada: ferro e virtual preservados. */
	@Test public void entregaInvalidaPreservaTodosOsRecursos() {
		Cenario c = new Cenario(Herdeiro.CHAGATAI, 20).iniciar();
		c.inventario().receberTesouro(TipoTesouro.FERRO); c.partida.getTurno().adicionarTesouroVirtual(TipoTesouro.PELES);
		assertFalse(c.componentes.entregarTesouros("K", pedidoCapital()));
		assertFalse(c.componentes.entregarTesouros("D", Map.of(TipoTesouro.FERRO, 2)));
		assertEquals(1, c.inventario().contarTesouro(TipoTesouro.FERRO));
		assertEquals(1, c.partida.getTurno().getTesouroVirtual(TipoTesouro.PELES));
		assertEquals(0, c.componentes.tesourosDescartados()); assertEquals(0, c.jogador().getVotos());
	}
	/** Objetivo: bônus é exclusivo e usável uma vez. Entrada: duas entregas na capital e duas tentativas de usar moedas. Saída esperada: uma peça. */
	@Test public void bonusEExclusivoEUtilizadoUmaUnicaVez() {
		Cenario c = new Cenario(Herdeiro.CHAGATAI, 20).iniciar();
		for (int n = 0; n < 2; n++) {
			for (TipoTesouro tipo : pedidoCapital().keySet()) c.inventario().receberTesouro(tipo);
			assertTrue(c.componentes.entregarTesouros("K", pedidoCapital()));
		}
		assertEquals(1, c.inventario().getBonus().size());
		assertTrue(c.componentes.usarBonus(TipoBonus.MOEDAS));
		assertFalse(c.componentes.usarBonus(TipoBonus.MOEDAS));
		assertEquals(3, c.jogador().contarTributo(TipoTributo.MOEDA));
	}
	/** Objetivo: escolha de tesouro obrigatória. Entrada: bônus de três peças. Saída esperada: bloqueio, uma guardada e duas descartadas. */
	@Test public void bonusDeTesouroBloqueiaTurnoAteEscolher() {
		Cenario c = new Cenario(Herdeiro.CHAGATAI, 20).iniciar();
		c.inventario().receberBonus(TipoBonus.TESOURO);
		assertTrue(c.partida.ativarColuna(Coluna.AMARELA));
		assertTrue(c.componentes.usarBonus(TipoBonus.TESOURO));
		assertEquals(3, c.componentes.escolhasDeTesouro(c.jogador()).size());
		assertFalse(c.partida.terminarTurno());
		assertFalse(c.componentes.escolherTesouroDoBonus(null));
		assertTrue(c.componentes.escolherTesouroDoBonus(c.componentes.escolhasDeTesouro(c.jogador()).get(0)));
		assertEquals(1, c.inventario().totalTesouros()); assertEquals(2, c.componentes.tesourosDescartados());
		assertTrue(c.partida.terminarTurno());
	}
	/** Objetivo: tesouro virtual só nasce uma vez na ativação. Entrada: melhoria antiga e várias ações. Saída esperada: saldo um; zero no próximo turno. */
	@Test public void tesouroVirtualNaoSeRepeteNosCallbacksEExpira() {
		Cenario c = new Cenario(Herdeiro.CHAGATAI, 20).iniciar();
		ObjetivosTest.adicionar(c.inventario(), Coluna.VERDE, Melhoria.Simbolo.FERRO);
		assertTrue(c.partida.ativarColuna(Coluna.VERDE));
		assertEquals(1, c.partida.getTurno().getTesouroVirtual(TipoTesouro.FERRO));
		assertTrue(c.partida.concluirAcaoComplementar(false)); assertTrue(c.partida.concluirAcaoComplementar(false));
		assertEquals(1, c.partida.getTurno().getTesouroVirtual(TipoTesouro.FERRO));
		assertTrue(c.partida.terminarTurno()); assertEquals(0, c.partida.getTurno().getTesouroVirtual(TipoTesouro.FERRO));
	}
	/** Objetivo: carta aberta é exclusiva e não reposta. Entrada: três espadas físicas e uma virtual. Saída esperada: voto uma única vez. */
	@Test public void cartasAbertasPontuamUmaVezSemReposicao() {
		Cenario c = new Cenario(Herdeiro.CHAGATAI, 20).iniciar();
		// Escolhe uma semente cujo conjunto inclui a carta de quatro espadas.
		for (int semente = 0; semente < 100 && c.componentes.getCartasAbertas().stream().noneMatch(a -> a.getNome().equals("Quatro espadas")); semente++) {
			c = new Cenario(Herdeiro.CHAGATAI, 20, semente).iniciar();
		}
		assertTrue(c.componentes.getCartasAbertas().stream().anyMatch(a -> a.getNome().equals("Quatro espadas")));
		c.partida.receberTributoDoEstoque(TipoTributo.ESPADA, 3);
		ObjetivosTest.adicionar(c.inventario(), Coluna.VERDE, Melhoria.Simbolo.ESPADA);
		assertTrue(c.partida.ativarColuna(Coluna.VERDE));
		assertEquals(1, c.jogador().getVotos()); assertEquals(1, c.partida.getPosicaoKurultai());
		assertEquals(3, c.componentes.getCartasAbertas().size());
		assertTrue(c.partida.concluirAcaoComplementar(false)); assertEquals(1, c.jogador().getVotos());
		assertEquals(3, c.jogador().contarTributo(TipoTributo.ESPADA));
	}
	/** Objetivo: proibir ações fora da fase. Entrada: compra/entrega antes do início. Saída esperada: falso e mercado intacto. */
	@Test public void componentesRecusamAcoesDurantePreparacao() {
		Cenario c = new Cenario(Herdeiro.CHAGATAI, 20);
		assertFalse(c.componentes.comprarMelhoria(0));
		assertFalse(c.componentes.entregarTesouros("K", pedidoCapital()));
		assertFalse(c.componentes.usarBonus(TipoBonus.MOEDAS));
		assertEquals(5, c.componentes.getMercado().size());
	}
	/** Objetivo: configuração inválida não altera preparação. Entrada: só quatro melhorias. Saída esperada: moedas e cidades intactas. */
	@Test public void configuracaoInvalidaNaoAlteraJogadoresNemCidades() {
		PartidaTest.Cenario base = PartidaTest.padrao();
		List<Melhoria> pecas = new ArrayList<>();
		for (int n = 0; n < 4; n++) pecas.add(new Melhoria(Coluna.VERDE, Melhoria.Simbolo.MAO));
		ComponentesKhan componentes = new ComponentesKhan(pecas, List.of(), new Random(1));
		assertThrows(IllegalArgumentException.class, () -> base.criar(componentes));
		assertEquals(0, base.azul().contarTributo(TipoTributo.MOEDA));
		assertTrue(base.reveladas().isEmpty());
		assertNull(base.tabuleiro.posicaoDe(base.azul().getPeao(0)));
	}
	/** Objetivo: bônus de voto imediato. Entrada: primeira entrega na dupla visitada. Saída esperada: voto da entrega mais voto do bônus. */
	@Test public void bonusDeVotoEImediatoENaoPodeSerReutilizado() {
		Cenario c = new Cenario(Herdeiro.CHAGATAI, 20).iniciar();
		c.base.tabuleiro.posicionarPeao(c.jogador().getPeao(0), c.base.tabuleiro.getParada("D"));
		assertTrue(c.partida.escolherPeao(0));
		c.inventario().receberTesouro(TipoTesouro.FERRO); c.inventario().receberTesouro(TipoTesouro.FERRO);
		assertTrue(c.componentes.entregarTesouros("D", Map.of(TipoTesouro.FERRO, 2)));
		assertEquals(2, c.jogador().getVotos()); assertEquals(2, c.partida.getPosicaoKurultai());
		assertEquals(Boolean.TRUE, c.inventario().getBonus().get(TipoBonus.VOTO));
		assertFalse(c.componentes.usarBonus(TipoBonus.VOTO));
	}
	/** Objetivo: bônus de ações e estoque finito. Entrada: ações extras e banco sem espadas. Saída esperada: ações somadas sem fabricar espada. */
	@Test public void bonusSomamAcoesERespeitamBancoFinito() {
		Cenario c = new Cenario(Herdeiro.CHAGATAI, 20).iniciar();
		c.inventario().receberBonus(TipoBonus.MOVIMENTO_E_TRIBUTO); c.inventario().receberBonus(TipoBonus.TRIBUTOS);
		assertTrue(c.componentes.usarBonus(TipoBonus.MOVIMENTO_E_TRIBUTO));
		assertTrue(c.componentes.usarBonus(TipoBonus.TRIBUTOS));
		assertEquals(3, c.partida.getTurno().getCavalos()); assertEquals(3, c.partida.getTurno().getMaos());
		int recebidas = c.partida.receberTributoDoEstoque(TipoTributo.ESPADA, 100);
		c.inventario().receberBonus(TipoBonus.ESPADAS);
		assertTrue(c.componentes.usarBonus(TipoBonus.ESPADAS));
		assertEquals(recebidas, c.jogador().contarTributo(TipoTributo.ESPADA));
		assertEquals(0, c.partida.getEstoque(TipoTributo.ESPADA));
	}
	static Map<TipoTesouro, Integer> pedidoCapital() { return Map.of(TipoTesouro.FERRO, 1, TipoTesouro.PELES, 1, TipoTesouro.GADO, 1); }
	static final class Cenario {
		final PartidaTest.Cenario base;
		final ComponentesKhan componentes;
		final Partida partida;
		Cenario(Herdeiro herdeiro, int melhorias) { this(herdeiro, melhorias, 42); }
		Cenario(Herdeiro herdeiro, int melhorias, int semente) {
			base = new PartidaTest.Cenario(herdeiro, Herdeiro.OGEDEI);
			List<Melhoria> pecas = new ArrayList<>();
			for (int n = 0; n < melhorias; n++) pecas.add(new Melhoria(Coluna.VERDE, Melhoria.Simbolo.CAVALOS));
			List<Conselheiro> conselheiros = List.of(
				new Conselheiro(base.tabuleiro.getKarakorum(), pedidoCapital(), TipoBonus.MOEDAS),
				new Conselheiro(base.tabuleiro.getParada("D"), Map.of(TipoTesouro.FERRO, 2), TipoBonus.VOTO));
			componentes = new ComponentesKhan(pecas, conselheiros, new Random(semente));
			base.criar(componentes); partida = base.partida;
		}
		Cenario iniciar() {
			for (Jogador jogador : base.jogadores) assertTrue(componentes.escolherCartaSecreta(jogador, componentes.cartasDe(jogador).get(0)));
			for (Jogador jogador : base.jogadores) if (!componentes.opcoesMelhoriaInicial(jogador).isEmpty()) assertTrue(componentes.escolherMelhoriaInicial(jogador, 0));
			assertTrue(partida.iniciar()); return this;
		}
		Jogador jogador() { return base.azul(); }
		Inventario inventario() { return componentes.inventarioDe(jogador()); }
	}
}
