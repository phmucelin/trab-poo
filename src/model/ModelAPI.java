package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.EnumMap;
import java.util.Random;

/**
 * Fachada pública do Model (padrões Facade e Singleton, INF1636 cap. 17).
 *
 * É a única classe do pacote model visível para a View e o Controller. Todas
 * as outras continuam com visibilidade de pacote. A fachada só recebe e
 * devolve tipos simples e coleções de tipos simples, para que
 * nenhuma classe interna do Model vaze para fora do pacote.
 *
 * Convenções do contrato:
 * - paradas, cidades e províncias são identificadas pelo nome usado no mapa;
 * - jogadores e peões são identificados por índices (ordem de jogo; 0 ou 1);
 * - herdeiros, colunas, cores, tipos e regiões são o nome da constante em
 *   maiúsculas (por exemplo "ALTANI", "VERMELHA", "AZUL", "DUPLA", "RUSSIA");
 * - ações devolvem false quando a regra não permite, sem alterar nada;
 * - toda ação bem-sucedida notifica os observadores registrados.
 */
public final class ModelAPI implements ObservadoIF {

	/** Local do Khan fora das províncias: a área de melhorias (manual, p. 9). */
	public static final String AREA_MELHORIAS = Partida.AREA_MELHORIAS;

	private static ModelAPI instancia = null;

	private final List<ObservadorIF> observadores = new ArrayList<>();
	private Partida partida;
	private ComponentesKhan componentesKhan;
	private int jogadorPreparando = -1;
	private String erroConfiguracao = "";
	private boolean cartaSecretaVisivel;
	private int jogadorCartela = -1;
	private boolean janelaCartasAberta;
	private boolean janelaCartelaAberta;

	private ModelAPI() {
	}


	public static ModelAPI getInstance() {
		if (instancia == null) {
			instancia = new ModelAPI();
		}
		return instancia;
	}

	// ============================================================ Observer

	@Override
	public void add(ObservadorIF o) {
		if (o != null && !observadores.contains(o)) {
			observadores.add(o);
		}
	}

	@Override
	public void remove(ObservadorIF o) {
		observadores.remove(o);
	}

	private void notificar() {
		for (ObservadorIF o : new ArrayList<>(observadores)) {
			o.notify(this);
		}
	}

	// ============================================================ partida

	/** Nomes aceitos em novaPartida, na ordem do manual. */
	public List<String> getHerdeiros() {
		List<String> nomes = new ArrayList<>();
		for (Herdeiro h : Herdeiro.values()) {
			nomes.add(h.name());
		}
		return nomes;
	}

	/** Prepara uma partida completa com o cadastro embutido; as escolhas iniciam os turnos. */
	public boolean novaPartida(List<String> nomes, List<String> herdeiros) {
		return novaPartidaCompleta(nomes, herdeiros, PecasDoJogo.cadastro(), new Random());
	}

	/** Cria a preparação completa sem iniciar turnos nem escolher cartas pelo jogador. */
	boolean novaPartidaCompleta(List<String> nomes, List<String> herdeiros, List<String> pecas) {
		return novaPartidaCompleta(nomes, herdeiros, pecas, new Random());
	}

	/** Sorteio injetável para validar a integração com cenários reproduzíveis. */
	boolean novaPartidaCompleta(List<String> nomes, List<String> herdeiros, List<String> pecas, Random random) {
		erroConfiguracao = "";
		if (nomes == null || herdeiros == null || nomes.size() != herdeiros.size()
				|| nomes.size() < 2 || nomes.size() > Cor.values().length) {
			erroConfiguracao = "Informe de 2 a 5 jogadores e seus herdeiros.";
			return false;
		}
		try {
			List<Jogador> jogadores = new ArrayList<>();
			for (int i = 0; i < nomes.size(); i++) {
				Herdeiro h = herdeiro(herdeiros.get(i));
				if (h == null) throw new IllegalArgumentException("Herdeiro inválido.");
				jogadores.add(new Jogador(nomes.get(i), Cor.values()[i], h));
			}
			Tabuleiro tabuleiro = MapaOficial.criar();
			if (random == null) throw new IllegalArgumentException("Sorteio obrigatório.");
			ComponentesKhan componentes = ConfiguracaoPecas.criar(pecas, tabuleiro, random);
			Partida nova = new Partida(tabuleiro, jogadores, componentes, random);
			cartaSecretaVisivel = false;
			janelaCartasAberta = false;
			janelaCartelaAberta = false;
			jogadorCartela = 0;
			partida = nova;
			componentesKhan = componentes;
			jogadorPreparando = 0;
		} catch (IllegalArgumentException e) {
			erroConfiguracao = e.getMessage();
			return false;
		}
		notificar();
		return true;
	}

	public String getErroConfiguracao() { return erroConfiguracao; }

	public boolean isPreparacao() {
		return partida != null && partida.getFase() == Fase.PREPARACAO;
	}

	/** -1 quando não há escolha de carta secreta pendente. */
	public int getJogadorPreparando() { return jogadorPreparando; }

	/** Somente as opções do jogador que está realizando a preparação. */
	public List<String> getOpcoesCartaSecreta() {
		List<String> opcoes = new ArrayList<>();
		if (isPreparacao() && componentesKhan != null && jogadorPreparando >= 0 && !componentesKhan.cartaEscolhida(jogador(jogadorPreparando))) {
			for (Carta carta : componentesKhan.cartasDe(jogador(jogadorPreparando))) {
				opcoes.add(carta.getNome() + ": " + carta.getObjetivo().getDescricao());
			}
		}
		return opcoes;
	}

	/** Confirma uma escolha; a última encerra a preparação antes da notificação. */
	public boolean escolherCartaSecreta(int indice) {
		if (!isPreparacao() || componentesKhan == null || jogadorPreparando < 0) return false;
		Jogador jogador = jogador(jogadorPreparando);
		List<Carta> cartas = componentesKhan.cartasDe(jogador);
		if (indice < 0 || indice >= cartas.size()
				|| !componentesKhan.escolherCartaSecreta(jogador, cartas.get(indice))) return false;
		avancarPreparacao();
		notificar();
		return true;
	}

	private void avancarPreparacao() {
		while (jogadorPreparando < getNumJogadores() && componentesKhan.cartaEscolhida(jogador(jogadorPreparando))
				&& componentesKhan.opcoesMelhoriaInicial(jogador(jogadorPreparando)).isEmpty()) jogadorPreparando++;
		if (jogadorPreparando == getNumJogadores()) {
			if (!partida.iniciar()) throw new IllegalStateException("Preparação incompleta.");
			jogadorPreparando = -1;
		}
	}
	public boolean isEscolhaMelhoriaInicialPendente() {
		return isPreparacao() && jogadorPreparando >= 0 && componentesKhan.cartaEscolhida(jogador(jogadorPreparando))
				&& !componentesKhan.opcoesMelhoriaInicial(jogador(jogadorPreparando)).isEmpty();
	}
	public List<String> getOpcoesMelhoriaInicial() {
		List<String> opcoes = new ArrayList<>();
		if (isEscolhaMelhoriaInicialPendente()) {
			for (Melhoria melhoria : componentesKhan.opcoesMelhoriaInicial(jogador(jogadorPreparando))) {
				opcoes.add(melhoria.getCor().name() + ": " + melhoria.getDescricao());
			}
		}
		return opcoes;
	}
	public boolean escolherMelhoriaInicial(int indice) {
		if (!isEscolhaMelhoriaInicialPendente() || !componentesKhan.escolherMelhoriaInicial(jogador(jogadorPreparando), indice)) return false;
		avancarPreparacao();
		notificar();
		return true;
	}

	public List<String> getCartasAbertas() {
		List<String> cartas = new ArrayList<>();
		if (componentesKhan != null) {
			for (Carta carta : componentesKhan.getCartasAbertas()) {
				cartas.add(carta.getNome() + ": " + carta.getObjetivo().getDescricao());
			}
		}
		return cartas;
	}

	public boolean temPartida() {
		return partida != null;
	}

	public boolean isPartidaEncerrada() {
		return partida != null && partida.getFase() == Fase.ENCERRADA;
	}

	public int getRodada() {
		return partida == null ? 0 : partida.getRodada();
	}

	// ============================================================ mapa

	/** Nomes de todas as paradas, em ordem alfabética. */
	public List<String> getParadas() {
		List<String> nomes = new ArrayList<>();
		if (partida != null) {
			for (Parada p : partida.getTabuleiro().getParadas()) {
				nomes.add(p.getNome());
			}
		}
		Collections.sort(nomes);
		return nomes;
	}

	/** "SIMPLES", "DUPLA" ou "KARAKORUM"; null se a parada não existir. */
	public String getTipoParada(String parada) {
		Parada p = parada(parada);
		return p == null ? null : p.getTipo().name();
	}

	/** "RUSSIA", "CHINA" ou "PERSIA"; null para Karakorum ou parada inexistente. */
	public String getRegiaoParada(String parada) {
		Parada p = parada(parada);
		return p == null || p.getRegiao() == null ? null : p.getRegiao().name();
	}

	/** Paradas ligadas por rota à parada informada, em ordem alfabética. */
	public List<String> getVizinhas(String parada) {
		List<String> nomes = new ArrayList<>();
		Parada p = parada(parada);
		if (p != null) {
			for (Parada v : partida.getTabuleiro().vizinhas(p)) {
				nomes.add(v.getNome());
			}
		}
		Collections.sort(nomes);
		return nomes;
	}

	public List<String> getCidades() {
		List<String> nomes = new ArrayList<>();
		if (partida != null) {
			for (Cidade c : partida.getTabuleiro().getCidades()) {
				nomes.add(c.getNome());
			}
		}
		Collections.sort(nomes);
		return nomes;
	}

	/** Paradas do anel ao redor da cidade, em ordem alfabética. */
	public List<String> getParadasDaCidade(String cidade) {
		List<String> nomes = new ArrayList<>();
		Cidade c = cidade(cidade);
		if (c != null) {
			for (Parada p : c.getParadasAdjacentes()) {
				nomes.add(p.getNome());
			}
		}
		Collections.sort(nomes);
		return nomes;
	}

	public boolean isCidadeRevelada(String cidade) {
		Cidade c = cidade(cidade);
		return c != null && c.isRevelada();
	}

	// ============================================================ jogadores

	public int getNumJogadores() {
		return partida == null ? 0 : partida.getJogadores().size();
	}

	public int getJogadorDaVez() {
		return partida == null ? -1 : partida.getJogadores().indexOf(partida.getJogadorDaVez());
	}

	public String getNomeJogador(int jogador) {
		return jogador(jogador).getNome();
	}

	/** Nome da constante de Cor: "AZUL", "VERDE", "VERMELHO", "AMARELO" ou "PRETO". */
	public String getCorJogador(int jogador) {
		return jogador(jogador).getCor().name();
	}

	public String getHerdeiroJogador(int jogador) {
		return jogador(jogador).getHerdeiro().name();
	}

	/** 2 para Altani, 1 para os demais. */
	public int getNumPeoes(int jogador) {
		return jogador(jogador).getPeoes().size();
	}

	/** Nome da parada onde está o peão (índice 0, ou 1 no caso de Altani). */
	public String getPosicaoPeao(int jogador, int peao) {
		Jogador j = jogador(jogador);
		if (peao < 0 || peao >= j.getPeoes().size()) {
			throw new IllegalArgumentException("Peão inexistente: " + peao);
		}
		return partida.getTabuleiro().posicaoDe(j.getPeao(peao)).getNome();
	}

	// ============================================================ turno

	/** Colunas cuja ficha de ativação o jogador da vez ainda pode usar. */
	public List<String> getColunasDisponiveis() {
		List<String> nomes = new ArrayList<>();
		if (emAndamento()) {
			Jogador j = partida.getJogadorDaVez();
			for (Coluna c : Coluna.values()) {
				if (j.temFichaDisponivel(c)) {
					nomes.add(c.name());
				}
			}
		}
		return nomes;
	}

	/** Ações impressas na coluna da cartela do jogador da vez (para exibição). */
	public String getDescricaoColuna(String coluna) {
		Coluna c = coluna(coluna);
		if (!emAndamento() || c == null) {
			return "";
		}
		return partida.getJogadorDaVez().getHerdeiro().getAcoes(c).toString();
	}

	/** Coluna ativada neste turno; null enquanto nenhuma foi ativada. */
	public String getColunaAtivada() {
		if (!emAndamento() || partida.getTurno().getColuna() == null) {
			return null;
		}
		return partida.getTurno().getColuna().name();
	}

	/** Coloca a ficha na coluna e libera as ações dela. Uma por turno. */
	public boolean ativarColuna(String coluna) {
		Coluna c = coluna(coluna);
		if (!emAndamento() || c == null || !partida.ativarColuna(c)) {
			return false;
		}
		notificar();
		return true;
	}

	public int getCavalosRestantes() {
		return emAndamento() ? partida.getTurno().getCavalos() : 0;
	}

	/** Índice do peão que age neste turno (sempre 0, exceto para Altani). */
	public int getPeaoAtivo() {
		if (!emAndamento()) {
			return -1;
		}
		return partida.getJogadorDaVez().getPeoes().indexOf(partida.getTurno().getPeaoAtivo());
	}

	/** Altani pode trocar de peão até a primeira ação que depende de posição. */
	public boolean podeEscolherPeao() {
		return emAndamento() && partida.getJogadorDaVez().getPeoes().size() > 1
				&& !partida.getTurno().peaoJaAgiu();
	}

	public boolean escolherPeao(int peao) {
		if (!podeEscolherPeao() || !partida.escolherPeao(peao)) {
			return false;
		}
		notificar();
		return true;
	}

	/** Paradas onde o peão ativo pode terminar o movimento com os cavalos restantes. */
	public List<String> getDestinosPossiveis() {
		List<String> nomes = new ArrayList<>();
		if (emAndamento() && getCavalosRestantes() > 0) {
			for (String nome : getParadas()) {
				if (partida.podeMoverPara(nome)) {
					nomes.add(nome);
				}
			}
		}
		return nomes;
	}

	public boolean podeMoverPara(String parada) {
		return emAndamento() && parada != null && partida.podeMoverPara(parada);
	}

	/**
	 * Move o peão ativo pelo caminho mais barato até a parada. Cada parada
	 * custa um cavalo; yurts próprios intermediários são pulados sem custo.
	 */
	public boolean moverPara(String parada) {
		if (!podeMoverPara(parada) || !partida.moverPara(parada)) {
			return false;
		}
		notificar();
		return true;
	}

	/** true quando a coluna ativada obriga a mover o Khan antes de terminar o turno. */
	public boolean isKhanPendente() {
		return emAndamento() && partida.getTurno().isKhanPendente();
	}

	/** Local onde o Khan está (província ou AREA_MELHORIAS); null antes do primeiro movimento. */
	public String getPosicaoKhan() {
		return partida == null ? null : partida.getPosicaoKhan();
	}

	/**
	 * Todos os locais que o Khan pode ocupar: as províncias do Khan, em ordem
	 * alfabética, e AREA_MELHORIAS. São os locais clicáveis do Khan no mapa.
	 */
	public List<String> getLocaisDoKhan() {
		List<String> nomes = new ArrayList<>();
		if (partida != null) {
			for (Provincia p : partida.getTabuleiro().provinciasDoKhan()) {
				nomes.add(p.getNome());
			}
			Collections.sort(nomes);
			nomes.add(AREA_MELHORIAS);
		}
		return nomes;
	}

	/** Locais para onde o Khan pode ir agora: qualquer um de getLocaisDoKhan(), menos o atual. */
	public List<String> getDestinosDoKhan() {
		List<String> nomes = new ArrayList<>();
		if (isKhanPendente()) {
			for (String local : getLocaisDoKhan()) {
				if (!local.equals(partida.getPosicaoKhan())) {
					nomes.add(local);
				}
			}
		}
		return nomes;
	}

	/** Move o Khan para uma província do Khan ou para AREA_MELHORIAS. */
	public boolean moverKhan(String local) {
		if (!isKhanPendente() || local == null) {
			return false;
		}
		boolean moveu = AREA_MELHORIAS.equals(local) ? partida.moverKhanParaMelhorias()
				: partida.moverKhanParaProvincia(local);
		if (!moveu) {
			return false;
		}
		notificar();
		return true;
	}

	/** Exige coluna ativada, Khan resolvido e nenhuma escolha obrigatória pendente. */
	public boolean podeTerminarTurno() {
		return partida != null && partida.podeTerminarTurno();
	}

	public boolean terminarTurno() {
		if (!emAndamento() || !partida.terminarTurno()) {
			return false;
		}
		cartaSecretaVisivel = false;
		if (isPartidaEncerrada()) {
			janelaCartasAberta = false;
			janelaCartelaAberta = false;
		}
		jogadorCartela = getJogadorDaVez();
		notificar();
		return true;
	}

	// ============================================================ ações e recursos

	public boolean isPartidaCompleta() { return componentesKhan != null; }

	public List<String> getProvincias() {
		List<String> nomes = new ArrayList<>();
		if (partida != null) for (Provincia provincia : partida.getTabuleiro().getProvincias()) nomes.add(provincia.getNome());
		Collections.sort(nomes);
		return nomes;
	}

	public String getTipoTributoProvincia(String nome) {
		Provincia provincia = provincia(nome);
		return provincia == null ? null : provincia.getTributo().name();
	}

	public int getPecasProvincia(String nome) {
		Provincia provincia = provincia(nome);
		return provincia == null ? 0 : provincia.getPecas();
	}

	public int getMaosRestantes() { return emAndamento() ? partida.getTurno().getMaos() : 0; }

	public int getTributosJogador(int indice, String tipo) {
		TipoTributo tributo = tributo(tipo);
		return tributo == null ? 0 : jogador(indice).contarTributo(tributo);
	}

	public int getTributosVirtuais(String tipo) {
		TipoTributo tributo = tributo(tipo);
		return !emAndamento() || tributo == null ? 0 : partida.getTurno().getRecursoVirtual(tributo);
	}

	public int getYurtsDisponiveis(int indice) { return jogador(indice).getYurtsDisponiveis(); }
	public int getVotosJogador(int indice) { return jogador(indice).getVotos(); }

	/** Cores das peças construídas, incluindo peças repetidas em paradas duplas. */
	public List<String> getYurtsParada(String nome) {
		List<String> cores = new ArrayList<>();
		Parada parada = parada(nome);
		if (parada != null) for (Cor cor : parada.getYurts()) cores.add(cor.name());
		return cores;
	}

	public int getTesourosRestantesCidade(String nome) {
		Cidade cidade = cidade(nome);
		return cidade == null ? 0 : cidade.getTesouros();
	}

	public String getConquistadorCidade(String nome) {
		Cidade cidade = cidade(nome);
		return cidade == null || cidade.getConquistadaPor() == null ? null : cidade.getConquistadaPor().name();
	}

	/** Peças físicas restantes, com repetições; vazia na demonstração sem tipos. */
	public List<String> getTesourosCidade(String nome) {
		List<String> tipos = new ArrayList<>();
		Cidade cidade = cidade(nome);
		if (componentesKhan != null && cidade != null) {
			for (TipoTesouro tipo : componentesKhan.tesourosEm(cidade)) tipos.add(tipo.name());
		}
		return tipos;
	}

	public int getTesourosJogador(int indice, String nome) {
		TipoTesouro tipo = tesouro(nome);
		return componentesKhan == null || tipo == null ? 0 : componentesKhan.inventarioDe(jogador(indice)).contarTesouro(tipo);
	}

	public int getTotalTesourosRecebidos(int indice) { return partida.getTesourosRecebidos(jogador(indice)); }

	public boolean podeConstruirYurt(String nome) { return emAndamento() && partida.podeConstruirYurt(nome); }
	public boolean construirYurt(String nome) {
		if (!emAndamento() || !partida.construirYurt(nome)) return false;
		notificar();
		return true;
	}

	public boolean podePegarTributo(String nome) { return emAndamento() && partida.podePegarTributo(nome); }
	public boolean pegarTributo(String nome) {
		if (!emAndamento() || !partida.pegarTributo(nome)) return false;
		notificar();
		return true;
	}

	/** null escolhe automaticamente; nome desconhecido é recusado, sem substituição. */
	public boolean podeAtacarCidade(String nome, String tipo) {
		return emAndamento() && (tipo == null || tesouro(tipo) != null) && partida.podeAtacarCidade(nome, tesouro(tipo));
	}
	public int getCustoAtaqueCidade(String nome) { return emAndamento() ? partida.getCustoAtaqueCidade(nome) : 0; }
	public boolean atacarCidade(String nome, String tipo) {
		if (!podeAtacarCidade(nome, tipo) || !partida.atacarCidade(nome, tesouro(tipo))) return false;
		notificar();
		return true;
	}

	public boolean podePegarTesouroComMoeda(String nome, String tipo) {
		return emAndamento() && (tipo == null || tesouro(tipo) != null) && partida.podePegarTesouroComMoeda(nome, tesouro(tipo));
	}
	public boolean pegarTesouroComMoeda(String nome, String tipo) {
		if (!podePegarTesouroComMoeda(nome, tipo) || !partida.pegarTesouroComMoeda(nome, tesouro(tipo))) return false;
		notificar();
		return true;
	}
	public boolean podeTrocarCavaloPorEspada() { return emAndamento() && partida.podeTrocarCavaloPorEspada(); }
	public boolean trocarCavaloPorEspada() {
		if (!emAndamento() || !partida.trocarCavaloPorEspada()) return false;
		notificar();
		return true;
	}

	// ============================================================ mercado e cartela

	/** Índice 0 é a peça mais à direita; a pilha fechada nunca revela sua peça. */
	public List<String> getMercadoMelhorias() {
		List<String> mercado = new ArrayList<>();
		if (componentesKhan != null) for (Melhoria melhoria : componentesKhan.getMercado()) {
			mercado.add(melhoria.getCor().name() + ": " + melhoria.getDescricao());
		}
		return mercado;
	}
	public int getMelhoriasNaReserva() { return componentesKhan == null ? 0 : componentesKhan.melhoriasNaReserva(); }

	/** Peças da cartela do jogador, na ordem de aquisição dentro da coluna. */
	public List<String> getMelhoriasJogador(int indice, String nomeColuna) {
		List<String> pecas = new ArrayList<>();
		Coluna coluna = coluna(nomeColuna);
		if (componentesKhan == null || coluna == null) return pecas;
		Inventario inventario = componentesKhan.inventarioDe(jogador(indice));
		for (Melhoria melhoria : inventario.getMelhorias(coluna)) {
			pecas.add(melhoria.getDescricao() + (inventario.podeAtivar(melhoria, partida.getTurno().getNumero())
					? "" : " (a partir do próximo turno)"));
		}
		return pecas;
	}
	public boolean isMelhoriaGratuitaPendente() {
		return emAndamento() && componentesKhan != null && componentesKhan.temMelhoriaPendente(partida.getJogadorDaVez());
	}
	/** -1 é a compra às cegas do topo da pilha; 0..4 são posições no mercado. */
	public boolean podeComprarMelhoria(int indice) {
		return emAndamento() && componentesKhan != null && componentesKhan.podeComprarMelhoria(indice);
	}
	public boolean comprarMelhoria(int indice) {
		if (!emAndamento() || componentesKhan == null || !componentesKhan.comprarMelhoria(indice)) return false;
		notificar();
		return true;
	}
	public boolean podeComprarMelhoria(int indice, String destino) {
		return emAndamento() && componentesKhan != null && componentesKhan.podeComprarMelhoria(indice, coluna(destino));
	}
	public boolean comprarMelhoria(int indice, String destino) {
		if (!emAndamento() || componentesKhan == null || !componentesKhan.comprarMelhoria(indice, coluna(destino))) return false;
		notificar();
		return true;
	}

	public boolean podeEscolherMelhoriaGratuita(int indice) {
		return emAndamento() && componentesKhan != null && componentesKhan.podeEscolherMelhoriaGratuita(indice);
	}
	public boolean escolherMelhoriaGratuita(int indice) {
		if (!emAndamento() || componentesKhan == null || !componentesKhan.escolherMelhoriaGratuita(indice)) return false;
		notificar();
		return true;
	}

	// ============================================================ conselheiros e bônus

	public List<String> getTiposTesouro() {
		List<String> tipos = new ArrayList<>();
		for (TipoTesouro tipo : TipoTesouro.values()) tipos.add(tipo.name());
		return tipos;
	}
	public int getTesourosVirtuais(String nome) {
		TipoTesouro tipo = tesouro(nome);
		return !emAndamento() || tipo == null ? 0 : partida.getTurno().getTesouroVirtual(tipo);
	}
	public List<String> getConselheiros() {
		List<String> nomes = new ArrayList<>();
		if (componentesKhan != null) {
			for (Conselheiro conselheiro : componentesKhan.getConselheiros()) {
				nomes.add(conselheiro.getParada().getNome());
			}
		}
		return nomes;
	}
	private Conselheiro conselheiro(String nome) { return componentesKhan == null ? null : componentesKhan.getConselheiro(nome); }
	/** Pedido vazio significa três tesouros quaisquer; nome inexistente também retorna vazio. */
	public Map<String, Integer> getPedidoConselheiro(String nome) {
		Map<String, Integer> pedido = new LinkedHashMap<>();
		Conselheiro conselheiro = conselheiro(nome);
		if (conselheiro != null) {
			for (Map.Entry<TipoTesouro, Integer> item : conselheiro.getPedido().entrySet()) {
				pedido.put(item.getKey().name(), item.getValue());
			}
		}
		return pedido;
	}
	public int getVotosEntrega(String nome) { return conselheiro(nome) == null ? 0 : conselheiro(nome).getVotos(); }
	public String getBonusConselheiro(String nome) {
		Conselheiro conselheiro = conselheiro(nome);
		return conselheiro == null || conselheiro.getBonus() == null ? null : conselheiro.getBonus().name();
	}
	public boolean isConselheiroVisitado(String nome) {
		Conselheiro conselheiro = conselheiro(nome);
		return emAndamento() && conselheiro != null && partida.getTurno().getParadasVisitadas().contains(conselheiro.getParada());
	}
	/** Zeros são omitidos; tipos desconhecidos, repetidos ou quantidades inválidas são recusados. */
	private Map<TipoTesouro, Integer> ofertaTesouros(Map<String, Integer> oferta) {
		if (oferta == null) return null;
		Map<TipoTesouro, Integer> convertida = new EnumMap<>(TipoTesouro.class);
		java.util.Set<TipoTesouro> vistos = new java.util.HashSet<>();
		for (Map.Entry<String, Integer> item : oferta.entrySet()) {
			TipoTesouro tipo = tesouro(item.getKey());
			Integer quantidade = item.getValue();
			if (tipo == null || !vistos.add(tipo) || quantidade == null || quantidade < 0 || quantidade > 3) return null;
			if (quantidade > 0) convertida.put(tipo, quantidade);
		}
		return convertida;
	}
	public boolean podeEntregarTesouros(String nome, Map<String, Integer> oferta) {
		Map<TipoTesouro, Integer> convertida = ofertaTesouros(oferta);
		return emAndamento() && componentesKhan != null && convertida != null && componentesKhan.podeEntregarTesouros(nome, convertida);
	}
	public boolean entregarTesouros(String nome, Map<String, Integer> oferta) {
		Map<TipoTesouro, Integer> convertida = ofertaTesouros(oferta);
		if (!emAndamento() || componentesKhan == null || convertida == null || !componentesKhan.entregarTesouros(nome, convertida)) return false;
		notificar();
		return true;
	}
	public List<String> getBonusJogador(int indice) {
		List<String> bonus = new ArrayList<>();
		if (componentesKhan != null) {
			for (TipoBonus tipo : componentesKhan.inventarioDe(jogador(indice)).getBonus().keySet()) {
				bonus.add(tipo.name());
			}
		}
		return bonus;
	}
	public boolean isBonusUsado(int indice, String nome) {
		TipoBonus tipo = bonus(nome);
		return componentesKhan != null && tipo != null && Boolean.TRUE.equals(componentesKhan.inventarioDe(jogador(indice)).getBonus().get(tipo));
	}
	public String getDescricaoBonus(String nome) { return bonus(nome) == null ? "" : bonus(nome).getDescricao(); }
	private static TipoBonus bonus(String nome) {
		for (TipoBonus tipo : TipoBonus.values()) if (tipo.name().equalsIgnoreCase(nome)) return tipo;
		return null;
	}
	public boolean podeUsarBonus(String nome) { return emAndamento() && componentesKhan != null && componentesKhan.podeUsarBonus(bonus(nome)); }
	public boolean usarBonus(String nome) {
		if (!emAndamento() || componentesKhan == null || !componentesKhan.usarBonus(bonus(nome))) return false;
		notificar();
		return true;
	}
	public List<String> getOpcoesTesouroDoBonus() {
		List<String> opcoes = new ArrayList<>();
		if (emAndamento() && componentesKhan != null) {
			for (TipoTesouro tipo : componentesKhan.escolhasDeTesouro(partida.getJogadorDaVez())) {
				opcoes.add(tipo.name());
			}
		}
		return opcoes;
	}
	public boolean isTesouroDoBonusPendente() { return !getOpcoesTesouroDoBonus().isEmpty(); }
	public boolean escolherTesouroDoBonus(String nome) {
		if (!emAndamento() || componentesKhan == null || !componentesKhan.escolherTesouroDoBonus(tesouro(nome))) return false;
		notificar();
		return true;
	}

	public boolean podeMoverPor(List<String> rota, List<Boolean> saltos) {
		return emAndamento() && partida.podeMoverPor(rota, saltos);
	}
	public boolean moverPor(List<String> rota, List<Boolean> saltos) {
		if (!emAndamento() || !partida.moverPor(rota, saltos)) return false;
		notificar();
		return true;
	}

	// ============================================================ objetivos e encerramento

	public int getPosicaoKurultai() { return partida == null ? 0 : partida.getPosicaoKurultai(); }
	public int getLimiteKurultai() { return partida == null ? 0 : partida.getLimiteKurultai(); }
	/** -1 enquanto o fim ainda não foi disparado. */
	public int getRodadaFinal() { return partida == null ? -1 : partida.getRodadaFinal(); }
	public List<String> getObjetivosAbertosComProgresso() {
		List<String> objetivos = new ArrayList<>();
		if (componentesKhan == null || !emAndamento()) return objetivos;
		Jogador jogador = partida.getJogadorDaVez();
		for (Carta carta : componentesKhan.getCartasAbertas()) objetivos.add(carta.getNome() + ": "
				+ carta.getObjetivo().getDescricao() + " Progresso atual: "
				+ carta.getObjetivo().progresso(jogador, partida, componentesKhan.inventarioDe(jogador)) + ".");
		return objetivos;
	}
	public boolean isCartaSecretaVisivel() { return emAndamento() && componentesKhan != null && cartaSecretaVisivel; }
	public boolean mostrarCartaSecreta() {
		if (!emAndamento() || componentesKhan == null || cartaSecretaVisivel) return false;
		cartaSecretaVisivel = true;
		notificar();
		return true;
	}
	public boolean ocultarCartaSecreta() {
		if (!cartaSecretaVisivel) return false;
		cartaSecretaVisivel = false;
		notificar();
		return true;
	}
	public String getCartaSecretaAtual() {
		if (!isCartaSecretaVisivel()) return "";
		Jogador jogador = partida.getJogadorDaVez();
		Carta carta = componentesKhan.cartasDe(jogador).get(0);
		return carta.getNome() + ": " + carta.getObjetivo().getDescricao() + " Progresso atual: "
				+ carta.getObjetivo().progresso(jogador, partida, componentesKhan.inventarioDe(jogador)) + ".";
	}
	public int getJogadorCartela() { return partida == null ? -1 : jogadorCartela; }
	public boolean selecionarCartela(int indice) {
		if (partida == null || indice < 0 || indice >= getNumJogadores() || indice == jogadorCartela) return false;
		jogadorCartela = indice;
		notificar();
		return true;
	}
	public int getVotosInfluenciaFinal(int indice) {
		return !isPartidaEncerrada() || componentesKhan == null ? 0 : componentesKhan.getVotosInfluenciaFinal(jogador(indice));
	}
	public int getVotosSecretosFinal(int indice) {
		return !isPartidaEncerrada() || componentesKhan == null ? 0 : componentesKhan.getVotosSecretosFinal(jogador(indice));
	}
	public String getCartaSecretaFinal(int indice) {
		if (!isPartidaEncerrada() || componentesKhan == null) return "";
		Carta carta = componentesKhan.cartasDe(jogador(indice)).get(0);
		return carta.getNome() + ": " + carta.getObjetivo().getDescricao();
	}
	public int getTotalTesourosRestantes(int indice) {
		return componentesKhan == null ? 0 : componentesKhan.inventarioDe(jogador(indice)).totalTesouros();
	}
	public int getDistanciaCapital(int indice) { return Pontuacao.distanciaDaCapital(jogador(indice), partida.getTabuleiro()); }
	public List<Integer> getVencedores() {
		List<Integer> indices = new ArrayList<>();
		if (isPartidaEncerrada() && componentesKhan != null) {
			for (Jogador jogador : componentesKhan.getVencedores()) {
				indices.add(partida.getJogadores().indexOf(jogador));
			}
		}
		return indices;
	}

	// Ids das imagens. O Model não conhece arquivos: quem liga id a arquivo é a View.
	public List<String> getIdsCartasAbertas() {
		List<String> ids = new ArrayList<>();
		if (componentesKhan != null) {
			for (Carta carta : componentesKhan.getCartasAbertas()) {
				ids.add(carta.getId());
			}
		}
		return ids;
	}
	public List<String> getIdsOpcoesCartaSecreta() {
		List<String> ids = new ArrayList<>();
		if (isPreparacao() && jogadorPreparando >= 0 && !componentesKhan.cartaEscolhida(jogador(jogadorPreparando))) {
			for (Carta carta : componentesKhan.cartasDe(jogador(jogadorPreparando))) {
				ids.add(carta.getId());
			}
		}
		return ids;
	}
	public List<String> getIdsOpcoesMelhoriaInicial() {
		List<String> ids = new ArrayList<>();
		if (isEscolhaMelhoriaInicialPendente()) {
			for (Melhoria melhoria : componentesKhan.opcoesMelhoriaInicial(jogador(jogadorPreparando))) {
				ids.add(melhoria.getId());
			}
		}
		return ids;
	}
	public String getIdCartaSecretaAtual() {
		return isCartaSecretaVisivel() ? componentesKhan.cartasDe(partida.getJogadorDaVez()).get(0).getId() : null;
	}
	public List<String> getIdsMelhoriasJogador(int indice, String nomeColuna) {
		List<String> ids = new ArrayList<>();
		Coluna coluna = coluna(nomeColuna);
		if (componentesKhan != null && coluna != null) {
			for (Melhoria melhoria : componentesKhan.inventarioDe(jogador(indice)).getMelhorias(coluna)) {
				ids.add(melhoria.getId());
			}
		}
		return ids;
	}
	public List<String> getIdsMercadoMelhorias() {
		List<String> ids = new ArrayList<>();
		if (componentesKhan != null) {
			for (Melhoria melhoria : componentesKhan.getMercado()) {
				ids.add(melhoria.getId());
			}
		}
		return ids;
	}

	// ============================================================ janelas de cartas e cartela
	// O enunciado trata abrir essas janelas como mudança de estado da partida, então
	// o estado fica aqui e as janelas só refletem o que o Observer avisar.

	public boolean isJanelaCartasAberta() { return janelaCartasAberta; }
	public boolean isJanelaCartelaAberta() { return janelaCartelaAberta; }

	public boolean abrirJanelaCartas() {
		if (!emAndamento() || janelaCartasAberta) {
			return false;
		}
		janelaCartasAberta = true;
		notificar();
		return true;
	}

	/** Fechar a janela também esconde a carta secreta. */
	public boolean fecharJanelaCartas() {
		if (!janelaCartasAberta) {
			return false;
		}
		janelaCartasAberta = false;
		cartaSecretaVisivel = false;
		notificar();
		return true;
	}

	public boolean abrirJanelaCartela() {
		if (!emAndamento() || janelaCartelaAberta) {
			return false;
		}
		janelaCartelaAberta = true;
		notificar();
		return true;
	}

	public boolean fecharJanelaCartela() {
		if (!janelaCartelaAberta) {
			return false;
		}
		janelaCartelaAberta = false;
		notificar();
		return true;
	}

	private Provincia provincia(String nome) {
		if (partida != null) for (Provincia provincia : partida.getTabuleiro().getProvincias()) {
			if (provincia.getNome().equals(nome)) return provincia;
		}
		return null;
	}
	private static TipoTributo tributo(String nome) {
		for (TipoTributo tipo : TipoTributo.values()) if (tipo.name().equalsIgnoreCase(nome)) return tipo;
		return null;
	}
	private static TipoTesouro tesouro(String nome) {
		for (TipoTesouro tipo : TipoTesouro.values()) if (tipo.name().equalsIgnoreCase(nome)) return tipo;
		return null;
	}

	// ============================================================ auxiliares

	private boolean emAndamento() {
		return partida != null && partida.getFase() == Fase.EM_ANDAMENTO;
	}

	private Jogador jogador(int indice) {
		if (partida == null || indice < 0 || indice >= partida.getJogadores().size()) {
			throw new IllegalArgumentException("Jogador inexistente: " + indice);
		}
		return partida.getJogadores().get(indice);
	}

	private Parada parada(String nome) {
		if (partida == null || nome == null || !partida.getTabuleiro().existeParada(nome)) {
			return null;
		}
		return partida.getTabuleiro().getParada(nome);
	}

	private Cidade cidade(String nome) {
		if (partida == null || nome == null) {
			return null;
		}
		for (Cidade c : partida.getTabuleiro().getCidades()) {
			if (c.getNome().equals(nome)) {
				return c;
			}
		}
		return null;
	}

	private static Herdeiro herdeiro(String nome) {
		for (Herdeiro h : Herdeiro.values()) {
			if (h.name().equalsIgnoreCase(nome) || h.getNome().equalsIgnoreCase(nome)) {
				return h;
			}
		}
		return null;
	}

	private static Coluna coluna(String nome) {
		for (Coluna c : Coluna.values()) {
			if (c.name().equalsIgnoreCase(nome)) {
				return c;
			}
		}
		return null;
	}
}
