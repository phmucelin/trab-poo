package model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

/**
 * Testes estruturais do mapa montado por MapaOficial. Eles verificam
 * o que o manual e o tabuleiro oficial garantem (contagens, conectividade,
 * simetria).
 */
public class MapaOficialTest {

	private Tabuleiro tab;

	@Before
	public void setUp() {
		tab = MapaOficial.criar();
	}

	/**
	 * Objetivo: o mapa tem 9 cidades, 3 por região, cada uma adjacente a pelo
	 * menos uma parada da sua própria região.
	 * Entrada: o tabuleiro montado por MapaOficial.criar().
	 * Saída esperada: 9 cidades, 3 em cada região, todas com uma parada
	 * adjacente da mesma região.
	 */
	@Test
	public void noveCidadesTresPorRegiao() {
		assertEquals(9, tab.getCidades().size());
		for (Regiao r : Regiao.values()) {
			int n = 0;
			for (Cidade c : tab.getCidades()) {
				if (c.getRegiao() == r) {
					n++;
					boolean temParadaDaRegiao = false;
					for (Parada p : c.getParadasAdjacentes()) {
						if (p.getRegiao() == r) temParadaDaRegiao = true;
					}
					assertTrue(c + " sem parada na sua região", temParadaDaRegiao);
				}
			}
			assertEquals(r + " deveria ter 3 cidades", 3, n);
		}
	}

	/**
	 * Objetivo: existem exatamente 7 paradas de conselheiro (6 duplas +
	 * Karakorum) e Karakorum não pertence a região nenhuma.
	 * Entrada: as paradas de conselheiro do tabuleiro oficial.
	 * Saída esperada: 7 paradas, 6 delas duplas, incluindo Karakorum, que tem
	 * região nula.
	 */
	@Test
	public void seteConselheirosIncluindoKarakorum() {
		Set<Parada> cons = tab.paradasDeConselheiro();
		assertEquals(7, cons.size());
		assertNotNull(tab.getKarakorum());
		assertTrue(cons.contains(tab.getKarakorum()));
		assertEquals(null, tab.getKarakorum().getRegiao());
		int duplas = 0;
		for (Parada p : cons) {
			if (p.isDupla()) duplas++;
		}
		assertEquals(6, duplas);
	}

	/**
	 * Objetivo: todas as rotas são simétricas e nenhuma parada é isolada.
	 * Entrada: cada parada do tabuleiro oficial e as suas vizinhas.
	 * Saída esperada: toda parada tem ao menos 1 vizinha, e se A é vizinha de
	 * B, B também é vizinha de A.
	 */
	@Test
	public void rotasSimetricasESemParadaIsolada() {
		for (Parada p : tab.getParadas()) {
			Set<Parada> viz = tab.vizinhas(p);
			assertTrue(p + " está isolada", viz.size() >= 1);
			for (Parada q : viz) {
				assertTrue("Rota assimétrica " + p + " -> " + q, tab.saoVizinhas(q, p));
			}
		}
	}

	/**
	 * Objetivo: o mapa é conexo: partindo de Karakorum (onde todos os peões
	 * começam) é possível chegar a qualquer parada por rotas.
	 * Entrada: uma busca em largura pelas rotas, a partir de Karakorum.
	 * Saída esperada: a busca visita todas as paradas do tabuleiro.
	 */
	@Test
	public void todasAsParadasAlcancaveisDeKarakorum() {
		Set<Parada> visitadas = new HashSet<>();
		Deque<Parada> fila = new ArrayDeque<>();
		fila.add(tab.getKarakorum());
		visitadas.add(tab.getKarakorum());
		while (!fila.isEmpty()) {
			Parada p = fila.poll();
			for (Parada q : tab.vizinhas(p)) {
				if (visitadas.add(q)) fila.add(q);
			}
		}
		assertEquals(tab.getParadas().size(), visitadas.size());
	}

	/**
	 * Objetivo: há 4 províncias do Khan, cada uma apontando para exatamente 2
	 * vizinhas; e, na preparação, toda província tem 1 peça de tributo.
	 * Entrada: as províncias do tabuleiro oficial, logo depois de montado.
	 * Saída esperada: 4 províncias do Khan com 2 vizinhas cada, e todas as
	 * províncias com 1 peça e ao menos 1 parada adjacente.
	 */
	@Test
	public void quatroProvinciasDoKhanETributoInicial() {
		Set<Provincia> khan = tab.provinciasDoKhan();
		assertEquals(4, khan.size());
		for (Provincia p : khan) {
			assertEquals(p + " deve apontar para 2 vizinhas", 2, p.getVizinhasDoKhan().size());
		}
		for (Provincia p : tab.getProvincias()) {
			assertEquals(p + " deveria começar com 1 peça", 1, p.getPecas());
			assertTrue(p + " sem parada adjacente", p.getParadasAdjacentes().size() >= 1);
		}
	}
}
