package view;

import static org.junit.Assert.*;
import java.util.List;
import org.junit.Test;
import model.ModelAPI;
import model.PreparacaoCompletaTest;

/** Verifica recursos oficiais reais e o vínculo entre identificadores e a View. */
public class CatalogoImagensTest {
	@Test public void carregaTodasAsImagensMapeadas() {
		CatalogoImagens catalogo = CatalogoImagens.getInstance();
		assertEquals(49, catalogo.getIdentificadores().size()); // 24 cartas, 5 cartelas e 20 tipos de melhoria.
		for (String id : catalogo.getIdentificadores()) {
			assertTrue(id, catalogo.get(id).getWidth() > 0);
			assertSame(catalogo.get(id), catalogo.get(id));
		}
	}
	@Test public void cadastroPadraoTemImagensEJochiEscolheAntesDosTurnos() {
		ModelAPI api = ModelAPI.getInstance();
		assertTrue(api.novaPartida(List.of("Ana", "Bia"), List.of("JOCHI", "ALTANI")));
		assertEquals(2, api.getIdsOpcoesCartaSecreta().size());
		assertTrue(api.escolherCartaSecreta(0));
		assertEquals(0, api.getJogadorPreparando());
		assertTrue(api.isEscolhaMelhoriaInicialPendente());
		assertTrue(api.getOpcoesCartaSecreta().isEmpty());
		assertFalse(api.escolherCartaSecreta(0));
		for (String id : api.getIdsOpcoesMelhoriaInicial()) assertNotNull(CatalogoImagens.getInstance().get(id));
		assertTrue(api.escolherMelhoriaInicial(1));
		assertTrue(api.escolherCartaSecreta(0));
		assertFalse(api.isPreparacao());
		for (String id : api.getIdsCartasAbertas()) assertNotNull(CatalogoImagens.getInstance().get(id));
		for (String id : api.getIdsMercadoMelhorias()) assertNotNull(CatalogoImagens.getInstance().get(id));
		for (String id : api.getIdsMelhoriasJogador(0, "BRANCA")) assertNotNull(CatalogoImagens.getInstance().get(id));
	}
}
