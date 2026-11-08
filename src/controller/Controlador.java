package controller;

import java.util.List;
import java.util.Map;

import view.JanelaInicial;
import view.JanelaJogo;
import view.JanelaCartas;
import view.JanelaCartela;

import model.ModelAPI;
import model.ObservadorIF;

/**
 * Controlador da aplicação (Singleton, INF1636 cap. 17).
 *
 * CONTRATO DA 2ª ITERAÇÃO: as assinaturas abaixo foram combinadas entre as
 * três partes e não devem mudar sem avisar o grupo. A View chama somente
 * estes métodos para agir; para ler o estado, os observadores consultam a
 * ModelAPI recebida em notify (callback do padrão Observer).
 *
 * Coordena abertura, preparação, escolhas obrigatórias, consulta de cartas
 * e resultado. O Model valida as regras; as Views observadoras refletem o
 * estado depois dos comandos aceitos.
 */
public final class Controlador {

	private static Controlador instancia = null;

	private JanelaCartas janelaCartas;
	private JanelaCartela janelaCartela;

	private final ModelAPI model = ModelAPI.getInstance();

	private Controlador() {
	}

	public static Controlador getInstance() {
		if (instancia == null) {
			instancia = new Controlador();
		}
		return instancia;
	}

	/** Os painéis chamam no construtor: Controlador.getInstance().registra(this). */
	public void registra(ObservadorIF o) {
		model.add(o);
	}

	public void desregistra(ObservadorIF o) {
		model.remove(o);
	}

	/** Abre a janela de configuração da partida (chamado por Main). */
	public void abrirJanelaInicial() {
		new JanelaInicial().setVisible(true);
	}

	/**
	 * Chamado pela JanelaInicial. Se a partida for criada, abre a JanelaJogo;
	 * se for recusada, devolve false e a JanelaInicial mostra o aviso na
	 * própria janela, sem caixa de diálogo.
	 */
	public boolean novaPartida(List<String> nomes, List<String> herdeiros) {
		if (!model.novaPartida(nomes, herdeiros)) {
			return false;
		}
		new JanelaJogo().setVisible(true);
		return true;
	}

	public boolean escolherMelhoriaInicial(int indice) { return model.escolherMelhoriaInicial(indice); }

	public boolean escolherCartaSecreta(int indice) {
		return model.escolherCartaSecreta(indice);
	}

	/**
	 * Chamados pelo PainelTabuleiro (Parte 2), porque todo deslocamento é feito
	 * clicando no mapa: numa parada (mover o peão), num local do Khan
	 * (província do Khan ou ModelAPI.AREA_MELHORIAS) ou no outro peão de Altani.
	 * Um clique recusado não faz nada: o painel não mostra mensagem.
	 */
	public boolean moverPor(List<String> rota, List<Boolean> saltos) { return model.moverPor(rota, saltos); }

	public boolean paradaClicada(String parada) {
		return model.moverPara(parada);
	}

	public boolean moverKhan(String local) {
		return model.moverKhan(local);
	}

	public boolean escolherPeao(int peao) {
		return model.escolherPeao(peao);
	}

	/** Chamados pelo PainelTurno (Parte 1). */
	public boolean ativarColuna(String coluna) {
		return model.ativarColuna(coluna);
	}

	public boolean terminarTurno() {
		return model.terminarTurno();
	}

	/** Ações da aba lateral; legalidade e custos permanecem no Model. */
	public boolean construirYurt(String parada) { return model.construirYurt(parada); }
	public boolean pegarTributo(String provincia) { return model.pegarTributo(provincia); }
	public boolean atacarCidade(String cidade, String tesouro) { return model.atacarCidade(cidade, tesouro); }
	public boolean pegarTesouroComMoeda(String cidade, String tesouro) { return model.pegarTesouroComMoeda(cidade, tesouro); }
	public boolean trocarCavaloPorEspada() { return model.trocarCavaloPorEspada(); }
	public boolean comprarMelhoria(int indice, String coluna) { return model.comprarMelhoria(indice, coluna); }
	public boolean comprarMelhoria(int indice) { return model.comprarMelhoria(indice); }
	public boolean escolherMelhoriaGratuita(int indice) { return model.escolherMelhoriaGratuita(indice); }

	public boolean entregarTesouros(String parada, Map<String, Integer> oferta) { return model.entregarTesouros(parada, oferta); }
	public boolean usarBonus(String bonus) { return model.usarBonus(bonus); }
	public boolean escolherTesouroDoBonus(String tesouro) { return model.escolherTesouroDoBonus(tesouro); }

	public boolean mostrarCartaSecreta() { return model.mostrarCartaSecreta(); }
	public boolean ocultarCartaSecreta() { return model.ocultarCartaSecreta(); }
	public boolean selecionarCartela(int jogador) { return model.selecionarCartela(jogador); }

	/** Prioridade da sequência de telas; a View aplica após receber o Observer. */
	public String getEtapaPartida() {
		if (model.isPreparacao()) return "PREPARACAO";
		if (model.isPartidaEncerrada()) return "RESULTADO";
		if (model.isTesouroDoBonusPendente()) return "ENTREGAS";
		if (model.isMelhoriaGratuitaPendente()) return "MELHORIAS";
		return "JOGO";
	}

	public boolean abrirJanelaCartas() {
		if (!model.temPartida() || model.isPreparacao() || model.isPartidaEncerrada()) return false;
		if (janelaCartas == null || !janelaCartas.isDisplayable()) janelaCartas = new JanelaCartas();
		boolean abriu = model.abrirJanelaCartas(); janelaCartas.toFront(); return abriu;
	}
	public boolean fecharJanelaCartas() { return model.fecharJanelaCartas(); }
	public boolean abrirJanelaCartela() {
		if (!model.temPartida() || model.isPreparacao() || model.isPartidaEncerrada()) return false;
		if (janelaCartela == null || !janelaCartela.isDisplayable()) janelaCartela = new JanelaCartela();
		boolean abriu = model.abrirJanelaCartela(); janelaCartela.toFront(); return abriu;
	}
	public boolean fecharJanelaCartela() { return model.fecharJanelaCartela(); }

}
