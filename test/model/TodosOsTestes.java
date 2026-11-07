package model;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/** Suíte JUnit 4 do Model (1ª iteração) e da fachada pública (2ª iteração). */
@RunWith(Suite.class)
@Suite.SuiteClasses({
	JogadorTest.class,
	CidadeProvinciaTest.class,
	TabuleiroTest.class,
	MapaOficialTest.class,
	MovimentoTest.class,
	MovimentoOpcionalTest.class,
	PartidaTest.class,
	MovimentoPartidaTest.class,
	IntegracaoPartidaTest.class,
	ConsultasPartidaTest.class,
	BaralhoTest.class,
	ObjetivosTest.class,
	InventarioTest.class,
	ComponentesKhanTest.class,
	PontuacaoTest.class,
	ModelAPITest.class,
	PreparacaoCompletaTest.class,
	AcoesModelAPITest.class,
	MelhoriasModelAPITest.class,
	EntregasModelAPITest.class,
	EncerramentoModelAPITest.class,
	JochiTest.class,
	PecasDoJogoTest.class,
	view.PreparacaoInterfaceTest.class,
	view.AcoesInterfaceTest.class,
	view.MelhoriasInterfaceTest.class,
	view.EntregasInterfaceTest.class,
	view.PartidaCompletaInterfaceTest.class,
	view.RotaInterfaceTest.class,
	view.CatalogoImagensTest.class
})
public class TodosOsTestes {
}
