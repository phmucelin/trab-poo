package controller;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import javax.swing.SwingUtilities;

import view.DialogoNovaPartida;
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
	private JanelaJogo janelaJogo;
	private String erroArquivo = "";
	private ConfirmadorNovaPartida confirmador = new DialogoNovaPartida();
	private Runnable encerrador = new Encerrar();

	/** Fecha a aplicação (trocável nos testes). */
	private static class Encerrar implements Runnable {
		@Override
		public void run() {
			System.exit(0);
		}
	}

	/** Roda no fim da fila de eventos, depois que os painéis já se redesenharam. */
	private class AnunciarFim implements Runnable {
		@Override
		public void run() {
			anunciarFim();
		}
	}

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
		abrirJanelaJogo();
		return true;
	}

	private void abrirJanelaJogo() {
		janelaJogo = new JanelaJogo();
		janelaJogo.setVisible(true);
	}

	/** Só antes de o jogador da vez começar a jogar (regra 1 da 4ª iteração). */
	public boolean podeSalvar() {
		return model.podeSalvar();
	}

	/** Motivo da última recusa de salvarPartida ou carregarPartida. */
	public String getErroArquivo() {
		return erroArquivo;
	}

	/** Grava a partida em texto ASCII no arquivo escolhido pelo usuário. */
	public boolean salvarPartida(File arquivo) {
		erroArquivo = "";
		List<String> linhas = model.gerarSalvamento();
		if (linhas.isEmpty()) {
			erroArquivo = "A partida só pode ser salva antes de o jogador da vez começar a jogar.";
			return false;
		}
		try {
			Files.write(arquivo.toPath(), linhas, StandardCharsets.US_ASCII);
		} catch (IOException e) {
			erroArquivo = "Não foi possível gravar o arquivo: " + e.getMessage();
			return false;
		}
		return true;
	}

	/**
	 * Lê uma partida salva. Vinda da JanelaInicial, abre a JanelaJogo; com a
	 * JanelaJogo já aberta, ela se atualiza pelo Observer. Se o arquivo não
	 * servir, a partida atual continua como estava.
	 */
	public boolean carregarPartida(File arquivo) {
		erroArquivo = "";
		List<String> linhas;
		try {
			linhas = Files.readAllLines(arquivo.toPath(), StandardCharsets.US_ASCII);
		} catch (IOException e) {
			erroArquivo = "Não foi possível ler o arquivo (ele precisa ser texto ASCII): " + e.getMessage();
			return false;
		}
		if (!model.restaurarPartida(linhas)) {
			erroArquivo = model.getErroConfiguracao();
			return false;
		}
		if (janelaJogo == null || !janelaJogo.isDisplayable()) {
			abrirJanelaJogo();
		}
		return true;
	}

	/** Troca o diálogo de fim de partida (usado pelos testes). */
	public void setConfirmador(ConfirmadorNovaPartida confirmador) {
		this.confirmador = confirmador;
	}

	/** Troca a ação de encerrar a aplicação (usada pelos testes). */
	public void setEncerrador(Runnable encerrador) {
		this.encerrador = encerrador;
	}

	/**
	 * Término da partida (chamado uma vez só, porque terminarTurno só devolve
	 * true uma vez no turno final): com o vencedor apontado, pergunta se há nova partida.
	 * Sim reabre a janela de escolha dos personagens; não encerra a aplicação.
	 */
	private void anunciarFim() {
		if (!model.isPartidaEncerrada()) {
			return;
		}
		StringBuilder nomes = new StringBuilder();
		List<Integer> vencedores = model.getVencedores();
		for (int jogador : vencedores) {
			if (nomes.length() > 0) nomes.append(", ");
			nomes.append(model.getNomeJogador(jogador));
		}
		String texto = (vencedores.size() > 1 ? "Vitória compartilhada: " : "Vencedor: ") + nomes + ".";
		if (confirmador.confirmar(janelaJogo, texto)) {
			reiniciar();
		} else {
			encerrador.run();
		}
	}

	/** Descarta as janelas da partida que acabou e reabre a escolha dos personagens. */
	private void reiniciar() {
		if (janelaCartas != null) janelaCartas.dispose();
		if (janelaCartela != null) janelaCartela.dispose();
		janelaCartas = null;
		janelaCartela = null;
		if (janelaJogo != null) janelaJogo.dispose();
		janelaJogo = null;
		abrirJanelaInicial();
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
		if (terminou && model.isPartidaEncerrada()) {
			SwingUtilities.invokeLater(new AnunciarFim());
		}
		return terminou;
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
