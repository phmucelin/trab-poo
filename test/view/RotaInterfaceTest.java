package view;

import static org.junit.Assert.*;
import java.util.List;
import javax.swing.SwingUtilities;
import org.junit.Test;
import model.ModelAPI;
import model.PreparacaoCompletaTest;

public class RotaInterfaceTest {
	@Test public void tracaRotaPassandoPorParadaOcupadaEExecutaNoMapa() throws Exception {
		SwingUtilities.invokeAndWait(new Runnable() {
			@Override public void run() {
				ModelAPI api = ModelAPI.getInstance();
				assertTrue(PreparacaoCompletaTest.iniciarPadrao(List.of("Ana", "Bia"), List.of("ALTANI", "TOLUI")));
				assertTrue(api.ativarColuna("VERDE"));
				assertTrue(api.moverPara("Sarai Sudoeste")); assertTrue(api.terminarTurno());
				assertTrue(api.ativarColuna("VERDE"));
				PainelTabuleiro mapa = new PainelTabuleiro();
				mapa.setSize(PainelTabuleiro.LARGURA, PainelTabuleiro.ALTURA);
				try {
					mapa.setModoRota(true);
					PartidaCompletaInterfaceTest.clicar(mapa, CoordenadasMapa.daParada("Sarai Sudoeste"));
					assertFalse(mapa.isRotaValida());
					PartidaCompletaInterfaceTest.clicar(mapa, CoordenadasMapa.daParada("Moscou Nordeste"));
					assertEquals(List.of("Sarai Sudoeste", "Moscou Nordeste"), mapa.getRotaPlanejada());
					assertTrue(mapa.isRotaValida());
					assertEquals("Karakorum", api.getPosicaoPeao(1, 0));
					PartidaCompletaInterfaceTest.clicar(mapa, CoordenadasMapa.daParada("Moscou Nordeste"));
					assertEquals("Moscou Nordeste", api.getPosicaoPeao(1, 0));
					assertEquals(0, api.getCavalosRestantes());
					assertTrue(mapa.getRotaPlanejada().isEmpty());
				} finally { api.remove(mapa); }
			}
		});
	}
}
