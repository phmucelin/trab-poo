package controller;

import java.io.File;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import view.JanelaAbertura;
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

	private JanelaJogo janelaJogo;
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

	/**
	 * Primeira janela (chamada por Main): nova partida ou continuar uma
	 * partida salva.
	 */
	public void abrirJanelaAbertura() {
		new JanelaAbertura().setVisible(true);
	}

	/**
	 * Janela de escolha dos personagens: aberta por "Nova partida" na
	 * JanelaAbertura e reaberta quando os jogadores pedem outra partida.
	 */
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
		abrirJanelaJogo();
		return true;
	}

	/**
	 * 4ª iteração: recupera a partida gravada no arquivo escolhido pela
	 * JanelaAbertura. Se o arquivo for inválido, devolve false e a partida em
	 * memória fica como estava; ModelAPI.getErroArquivo() diz o motivo.
	 */
	public boolean carregarPartida(File arquivo) {
		if (!model.carregar(arquivo)) {
			return false;
		}
		abrirJanelaJogo();
		return true;
	}

	/** 4ª iteração: só é aceito antes de o jogador da vez começar a jogar. */
	public boolean salvarPartida(File arquivo) {
		return model.salvar(arquivo);
	}

	private void abrirJanelaJogo() {
		fecharJanelasDaPartida();
		janelaJogo = new JanelaJogo();
		janelaJogo.setVisible(true);
	}

	/** Fecha tabuleiro, cartas e cartela e solta seus observadores. */
	private void fecharJanelasDaPartida() {
		if (janelaCartas != null) janelaCartas.dispose();
		if (janelaCartela != null) janelaCartela.dispose();
		if (janelaJogo != null) janelaJogo.dispose();
		janelaCartas = null;
		janelaCartela = null;
		janelaJogo = null;
		model.removerObservadores();
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
		boolean terminou = model.terminarTurno();
		// 4ª iteração: logo depois de o vencedor aparecer na aba Resultado,
		// pergunta se os jogadores querem outra partida. Só há pergunta quando
		// o tabuleiro foi aberto por este Controlador (não nos testes de painel).
		if (terminou && model.isPartidaEncerrada() && janelaJogo != null && janelaJogo.isDisplayable()) {
			SwingUtilities.invokeLater(new PerguntarNovaPartida());
		}
		return terminou;
	}

	/** Executado depois que os observadores mostraram o resultado. */
	private class PerguntarNovaPartida implements Runnable {
		@Override
		public void run() {
			int resposta = JOptionPane.showConfirmDialog(janelaJogo, textoDoResultado() + "\n\nDesejam iniciar uma nova partida?",
					"Fim da partida", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
			fimDaPartida(resposta == JOptionPane.YES_OPTION);
		}
	}

	/**
	 * Resposta ao diálogo de fim de partida: sim reabre a escolha dos
	 * personagens; não encerra a aplicação.
	 */
	public void fimDaPartida(boolean novaPartida) {
		if (!novaPartida) {
			System.exit(0);
		}
		fecharJanelasDaPartida();
		abrirJanelaInicial();
	}

	private String textoDoResultado() {
		List<Integer> vencedores = model.getVencedores();
		StringBuilder texto = new StringBuilder(vencedores.size() > 1 ? "Vitória compartilhada: " : "Vencedor: ");
		for (int i = 0; i < vencedores.size(); i++) {
			int j = vencedores.get(i);
			if (i > 0) texto.append(", ");
			texto.append(model.getNomeJogador(j)).append(" (").append(model.getVotosJogador(j)).append(" votos)");
		}
		return texto.append('.').toString();
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

	/**
	 * As janelas de cartas e de cartela são criadas uma vez só e reaproveitadas.
	 * Quem decide se ficam visíveis é o Model, pelo Observer.
	 */
	public boolean abrirJanelaCartas() {
		if (!podeAbrirJanelas()) {
			return false;
		}
		if (janelaCartas == null || !janelaCartas.isDisplayable()) {
			janelaCartas = new JanelaCartas();
		}
		boolean abriu = model.abrirJanelaCartas();
		janelaCartas.toFront();
		return abriu;
	}

	public boolean fecharJanelaCartas() {
		return model.fecharJanelaCartas();
	}

	public boolean abrirJanelaCartela() {
		if (!podeAbrirJanelas()) {
			return false;
		}
		if (janelaCartela == null || !janelaCartela.isDisplayable()) {
			janelaCartela = new JanelaCartela();
		}
		boolean abriu = model.abrirJanelaCartela();
		janelaCartela.toFront();
		return abriu;
	}

	public boolean fecharJanelaCartela() {
		return model.fecharJanelaCartela();
	}

	private boolean podeAbrirJanelas() {
		return model.temPartida() && !model.isPreparacao() && !model.isPartidaEncerrada();
	}
}
