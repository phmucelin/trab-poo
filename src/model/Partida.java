package model;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Coordena as regras do Model: preparação, ações, turnos e encerramento.
 * Recebe jogadores novos e um tabuleiro com a geografia/províncias preparadas.
 * Cartas, tesouros tipados e melhorias entram pelo contrato ComponentesPartida.
 * Com o contrato padrão, executa apenas o subconjunto das partes 1 e 3.
 */
class Partida {
	static final String AREA_MELHORIAS = "AREA_MELHORIAS";
	private static final int[] LIMITES_KURULTAI = { 0, 0, 10, 14, 16, 17 };
	private final Tabuleiro tabuleiro;
	private final List<Jogador> jogadores;
	private final ComponentesPartida componentes;
	private final Map<TipoTributo, Integer> estoque = new EnumMap<>(TipoTributo.class);
	private final Map<Jogador, Integer> tesourosRecebidos = new HashMap<>();
	private final Map<Jogador, Integer> votosRegistrados = new HashMap<>();
	private final Deque<Cidade> cidadesFechadas = new ArrayDeque<>();
	private Fase fase = Fase.PREPARACAO;
	private int indiceJogador;
	private int rodada = 1;
	private int rodadaFinal = -1;
	private int posicaoKurultai;
	private String posicaoKhan;
	private Turno turno;

	Partida(Tabuleiro tabuleiro, List<Jogador> jogadores) {
		this(tabuleiro, jogadores, new ComponentesPartida() { }, new Random());
	}

	Partida(Tabuleiro tabuleiro, List<Jogador> jogadores, ComponentesPartida componentes, Random random) {
		if (tabuleiro == null || tabuleiro.getKarakorum() == null || jogadores == null
				|| jogadores.size() < 2 || jogadores.size() > 5 || componentes == null || random == null) {
			throw new IllegalArgumentException("Informe tabuleiro, 2 a 5 jogadores, componentes e sorteio");
		}
		Set<Cor> cores = new HashSet<>();
		Set<Herdeiro> herdeiros = new HashSet<>();
		for (Jogador jogador : jogadores) {
			if (jogador == null || !cores.add(jogador.getCor()) || !herdeiros.add(jogador.getHerdeiro())) {
				throw new IllegalArgumentException("Cores e herdeiros não podem se repetir");
			}
			if (jogador.getHerdeiro() == Herdeiro.TOLUI && !jogador.temYurtDisponivel()
					&& !tabuleiro.getKarakorum().temYurtDe(jogador.getCor())) {
				throw new IllegalArgumentException("Tolui precisa de seu yurt inicial");
			}
		}
		componentes.validarPreparacao(tabuleiro, jogadores);
		this.tabuleiro = tabuleiro;
		this.jogadores = Collections.unmodifiableList(new ArrayList<>(jogadores));
		this.componentes = componentes;
		prepararEstoque(); // Valida todas as quantidades antes de alterar as peças.
		prepararJogadores();
		prepararCidades(random);
		turno = novoTurno(1);
		componentes.preparar(this);
	}

	private void prepararEstoque() {
		estoque.put(TipoTributo.ESPADA, 10);
		estoque.put(TipoTributo.YURT, 10);
		estoque.put(TipoTributo.MOEDA, 15);
		for (Provincia provincia : tabuleiro.getProvincias()) {
			TipoTributo tipo = provincia.getTributo();
			estoque.put(tipo, estoque.get(tipo) - provincia.getPecas());
		}
		for (int i = 0; i < jogadores.size(); i++) {
			for (TipoTributo tipo : TipoTributo.values()) {
				estoque.put(tipo, estoque.get(tipo) - jogadores.get(i).contarTributo(tipo));
			}
			estoque.put(TipoTributo.MOEDA, estoque.get(TipoTributo.MOEDA) - (i < 2 ? 1 : 2));
		}
		for (int quantidade : estoque.values()) {
			if (quantidade < 0) throw new IllegalArgumentException("Peças físicas excedem o estoque do jogo");
		}
	}

	private void prepararJogadores() {
		for (int i = 0; i < jogadores.size(); i++) {
			Jogador jogador = jogadores.get(i);
			jogador.adicionarTributo(TipoTributo.MOEDA, i < 2 ? 1 : 2);
			for (Peao peao : jogador.getPeoes()) tabuleiro.posicionarPeao(peao, tabuleiro.getKarakorum());
			if (jogador.getHerdeiro() == Herdeiro.TOLUI && !tabuleiro.getKarakorum().temYurtDe(jogador.getCor())) {
				jogador.retirarYurtDoEstoque();
				tabuleiro.getKarakorum().colocarYurt(jogador.getCor(), true);
			}
			tesourosRecebidos.put(jogador, 0);
			votosRegistrados.put(jogador, jogador.getVotos());
			posicaoKurultai += jogador.getVotos();
		}
	}

	private void prepararCidades(Random random) {
		List<Cidade> fechadas = new ArrayList<>();
		int reveladas = 0;
		for (Cidade cidade : tabuleiro.getCidades()) {
			if (cidade.isRevelada()) {
				if (!cidade.isConquistada()) reveladas++;
			} else fechadas.add(cidade);
		}
		fechadas.sort(Comparator.comparing(Cidade::getNome));
		Collections.shuffle(fechadas, random);
		cidadesFechadas.addAll(fechadas);
		while (reveladas < 3 && !cidadesFechadas.isEmpty()) {
			cidadesFechadas.removeFirst().revelar();
			reveladas++;
		}
	}

	boolean iniciar() {
		if (fase != Fase.PREPARACAO || !componentes.preparacaoConcluida()) return false;
		fase = Fase.EM_ANDAMENTO;
		verificarGatilhosDeFim();
		return true;
	}

	Fase getFase() { return fase; }
	Tabuleiro getTabuleiro() { return tabuleiro; }
	List<Jogador> getJogadores() { return jogadores; }
	Jogador getJogadorDaVez() { return jogadores.get(indiceJogador); }
	Turno getTurno() { return turno; }
	int getRodada() { return rodada; }
	int getRodadaFinal() { return rodadaFinal; }
	String getPosicaoKhan() { return posicaoKhan; }
	int getPosicaoKurultai() { return posicaoKurultai; }
	int getLimiteKurultai() { return LIMITES_KURULTAI[jogadores.size()]; }
	int getEstoque(TipoTributo tipo) { return estoque.getOrDefault(tipo, 0); }
	int getTesourosRecebidos(Jogador jogador) { return tesourosRecebidos.getOrDefault(jogador, 0); }
	private boolean emAndamento() { return fase == Fase.EM_ANDAMENTO; }

	boolean ativarColuna(Coluna coluna) {
		Jogador jogador = getJogadorDaVez();
		if (!emAndamento() || coluna == null || turno.getColuna() != null || !jogador.temFichaDisponivel(coluna)) return false;
		EfeitosColuna extras = componentes.efeitosDasMelhorias(jogador, coluna, turno.getNumero());
		if (extras == null) throw new IllegalStateException("Melhorias precisam fornecer efeitos válidos");
		jogador.usarFicha(coluna);
		turno.ativar(coluna, jogador.getHerdeiro().getAcoes(coluna), extras);
		concluirAcao();
		return true;
	}

	boolean escolherPeao(int indice) {
		Jogador jogador = getJogadorDaVez();
		if (!emAndamento() || turno.peaoJaAgiu() || indice < 0 || indice >= jogador.getPeoes().size()) return false;
		Peao peao = jogador.getPeao(indice);
		turno.escolherPeao(peao, tabuleiro.posicaoDe(peao));
		return true;
	}

	boolean podeMoverPara(String destino) { return calcularMovimento(destino) != null; }
	private Movimento.Resultado calcularMovimento(String destino) {
		return emAndamento() ? Movimento.calcular(tabuleiro, turno.getPeaoAtivo(), getJogadorDaVez(), destino, turno.getCavalos()) : null;
	}
	boolean moverPara(String destino) { return executarMovimento(calcularMovimento(destino)); }
	boolean moverPor(List<String> rota) {
		if (!emAndamento()) return false;
		return executarMovimento(Movimento.validarRota(tabuleiro, turno.getPeaoAtivo(), getJogadorDaVez(), rota, turno.getCavalos()));
	}
	/** Cada flag indica se a parada daquele passo deve ser pulada sobre um yurt. */
	boolean podeMoverPor(List<String> rota, List<Boolean> saltarYurts) {
		return emAndamento() && Movimento.validarRota(tabuleiro, turno.getPeaoAtivo(), getJogadorDaVez(), rota, saltarYurts, turno.getCavalos()) != null;
	}
	boolean moverPor(List<String> rota, List<Boolean> saltarYurts) {
		if (!emAndamento()) return false;
		return executarMovimento(Movimento.validarRota(tabuleiro, turno.getPeaoAtivo(), getJogadorDaVez(), rota, saltarYurts, turno.getCavalos()));
	}
	private boolean executarMovimento(Movimento.Resultado caminho) {
		if (caminho == null) return false;
		tabuleiro.posicionarPeao(turno.getPeaoAtivo(), caminho.getDestino());
		turno.registrarMovimento(caminho.getCusto(), caminho.getVisitadas());
		concluirAcao();
		return true;
	}

	/** Consulta sem consumir ações, peças ou alterar a escolha do peão. */
	boolean podePegarTributo(String nome) {
		Provincia provincia = provincia(nome);
		return emAndamento() && provincia != null && turno.getMaos() > 0 && provincia.temPecas()
				&& !Collections.disjoint(provincia.getParadasAdjacentes(), turno.getParadasVisitadas());
	}
	boolean pegarTributo(String nome) {
		if (!podePegarTributo(nome)) return false;
		Provincia provincia = provincia(nome);
		provincia.retirarPeca();
		getJogadorDaVez().adicionarTributo(provincia.getTributo(), 1);
		turno.gastarMao();
		turno.registrarAcaoDoPeao();
		concluirAcao();
		return true;
	}

	/** Consulta capacidade, alcance e recursos sem executar a construção. */
	boolean podeConstruirYurt(String nome) {
		if (!emAndamento() || !tabuleiro.existeParada(nome)) return false;
		Parada parada = tabuleiro.getParada(nome);
		Jogador jogador = getJogadorDaVez();
		boolean alcance = turno.getParadasVisitadas().contains(parada);
		if (!alcance && jogador.getHerdeiro() == Herdeiro.TOLUI) {
			for (Parada visitada : turno.getParadasVisitadas()) {
				if (tabuleiro.saoVizinhas(visitada, parada)) alcance = true;
			}
		}
		return alcance && parada.podeReceberYurt() && jogador.temYurtDisponivel() && recursos(TipoTributo.YURT) >= 1;
	}
	boolean construirYurt(String nome) {
		if (!podeConstruirYurt(nome)) return false;
		Parada parada = tabuleiro.getParada(nome);
		Jogador jogador = getJogadorDaVez();
		gastarTributo(TipoTributo.YURT, 1);
		tabuleiro.colocarYurt(parada, jogador);
		turno.registrarAcaoDoPeao();
		concluirAcao();
		return true;
	}

	/** Consulta custo, alcance e possibilidade de conquista sem retirar tesouro. */
	boolean podeAtacarCidade(String nome) {
		Cidade cidade = cidade(nome);
		Jogador jogador = getJogadorDaVez();
		if (!emAndamento() || cidade == null || !cidade.podeSerAtacada()
				|| Collections.disjoint(cidade.getParadasAdjacentes(), turno.getParadasVisitadas())) return false;
		int custo = getCustoAtaqueCidade(nome);
		return recursos(TipoTributo.ESPADA) >= custo && (cidade.getTesouros() != 1 || jogador.temYurtDisponivel());
	}
	int getCustoAtaqueCidade(String nome) {
		Cidade cidade = cidade(nome);
		return cidade == null ? 0 : turno.ataquesNaCidade(cidade) == 0 ? 1 : 2;
	}
	boolean podeAtacarCidade(String nome, TipoTesouro tipo) {
		return podeAtacarCidade(nome) && componentes.podeReceberTesouro(cidade(nome), tipo);
	}
	boolean atacarCidade(String nome) {
		return atacarCidade(nome, null);
	}
	boolean atacarCidade(String nome, TipoTesouro tipo) {
		if (!podeAtacarCidade(nome, tipo)) return false;
		Cidade cidade = cidade(nome);
		int custo = getCustoAtaqueCidade(nome);
		gastarTributo(TipoTributo.ESPADA, custo);
		turno.registrarAtaque(cidade);
		tomarTesouro(cidade, tipo);
		turno.registrarAcaoDoPeao();
		concluirAcao();
		return true;
	}

	private void tomarTesouro(Cidade cidade, TipoTesouro tipo) {
		Jogador jogador = getJogadorDaVez();
		boolean conquista = cidade.getTesouros() == 1;
		cidade.retirarTesouro(jogador.getCor());
		tesourosRecebidos.put(jogador, getTesourosRecebidos(jogador) + 1);
		if (conquista) {
			jogador.retirarYurtDoEstoque();
			if (!cidadesFechadas.isEmpty()) cidadesFechadas.removeFirst().revelar();
		}
		componentes.receberTesouro(jogador, cidade, tipo);
	}

	boolean moverKhanParaProvincia(String nome) {
		Provincia alvo = provincia(nome);
		if (!emAndamento() || !turno.isKhanPendente() || alvo == null || !alvo.isProvinciaDoKhan()
				|| alvo.getNome().equals(posicaoKhan)) return false;
		abastecer(alvo);
		List<Provincia> vizinhas = new ArrayList<>(alvo.getVizinhasDoKhan());
		vizinhas.sort(Comparator.comparing(Provincia::getNome));
		for (Provincia vizinha : vizinhas) abastecer(vizinha);
		posicaoKhan = alvo.getNome();
		turno.resolverKhan();
		concluirAcao();
		return true;
	}
	private void abastecer(Provincia provincia) {
		TipoTributo tipo = provincia.getTributo();
		if (getEstoque(tipo) > 0 && provincia.colocarPeca()) estoque.put(tipo, getEstoque(tipo) - 1);
	}

	boolean moverKhanParaMelhorias() {
		if (!emAndamento() || !turno.isKhanPendente() || AREA_MELHORIAS.equals(posicaoKhan)
				|| !componentes.moverKhanParaMelhorias(this)) return false;
		posicaoKhan = AREA_MELHORIAS;
		turno.resolverKhan();
		concluirAcao();
		return true;
	}

	boolean podeTrocarCavaloPorEspada() {
		return emAndamento() && getJogadorDaVez().getHerdeiro() == Herdeiro.CHAGATAI
				&& !turno.habilidadeUsada() && turno.getCavalos() > 0 && getEstoque(TipoTributo.ESPADA) > 0;
	}
	boolean trocarCavaloPorEspada() {
		if (!podeTrocarCavaloPorEspada()) return false;
		turno.gastarCavalo();
		turno.usarHabilidade();
		receberTributoDoEstoque(TipoTributo.ESPADA, 1);
		concluirAcao();
		return true;
	}

	boolean pegarTesouroComMoeda(String nome) {
		return pegarTesouroComMoeda(nome, null);
	}
	boolean podePegarTesouroComMoeda(String nome, TipoTesouro tipo) {
		Cidade cidade = cidade(nome);
		return emAndamento() && getJogadorDaVez().getHerdeiro() == Herdeiro.OGEDEI && !turno.habilidadeUsada()
				&& cidade != null && cidade.podeSerAtacada() && cidade.getTesouros() >= 2 && recursos(TipoTributo.MOEDA) >= 1
				&& componentes.podeReceberTesouro(cidade, tipo);
	}
	boolean pegarTesouroComMoeda(String nome, TipoTesouro tipo) {
		if (!podePegarTesouroComMoeda(nome, tipo)) return false;
		Cidade cidade = cidade(nome);
		gastarTributo(TipoTributo.MOEDA, 1);
		turno.usarHabilidade();
		tomarTesouro(cidade, tipo);
		concluirAcao();
		return true;
	}

	private int recursos(TipoTributo tipo) {
		return getJogadorDaVez().contarTributo(tipo) + turno.getRecursoVirtual(tipo);
	}

	/** Auxiliar da parte 2: valida tudo antes de pagar; prioriza recursos virtuais. */
	boolean gastarTributo(TipoTributo tipo, int quantidade) {
		if (!emAndamento() || tipo == null || quantidade <= 0 || recursos(tipo) < quantidade) return false;
		int fisicas = quantidade - turno.usarVirtual(tipo, quantidade);
		getJogadorDaVez().gastarTributo(tipo, fisicas);
		estoque.put(tipo, getEstoque(tipo) + fisicas);
		return true;
	}

	/** Auxiliar da parte 2: entrega até a quantidade disponível de peças físicas. */
	int receberTributoDoEstoque(TipoTributo tipo, int quantidade) {
		if (!emAndamento() || tipo == null || quantidade <= 0) return 0;
		int recebidas = Math.min(quantidade, getEstoque(tipo));
		estoque.put(tipo, getEstoque(tipo) - recebidas);
		getJogadorDaVez().adicionarTributo(tipo, recebidas);
		return recebidas;
	}

	/**
	 * A parte 2 chama após concluir uma jogada própria já validada. Entregas que
	 * usam o peão devem informar true para fixar também a escolha de Altani.
	 * Não chamar de dentro de ComponentesPartida.aposAcao (evita recursão).
	 */
	boolean concluirAcaoComplementar(boolean usaPeao) {
		if (!emAndamento()) return false;
		if (usaPeao) turno.registrarAcaoDoPeao();
		concluirAcao();
		return true;
	}

	/** Consulta: há coluna ativada, o Khan foi resolvido e não há escolha obrigatória pendente. */
	boolean podeTerminarTurno() {
		return emAndamento() && turno.getColuna() != null && !turno.isKhanPendente()
				&& !componentes.temEscolhaPendente(getJogadorDaVez());
	}

	boolean terminarTurno() {
		if (!podeTerminarTurno()) return false;
		if (turno.recuperaFichas()) getJogadorDaVez().recuperarFichas();
		// As cartas já foram avaliadas nas ações. Não abrir novas escolhas aqui.
		sincronizarVotosEGatilhos();
		if (indiceJogador == jogadores.size() - 1) {
			if (rodada == rodadaFinal) {
				fase = Fase.ENCERRADA;
				componentes.pontuarFimDeJogo(this);
				return true;
			}
			rodada++;
		}
		indiceJogador = (indiceJogador + 1) % jogadores.size();
		turno = novoTurno(turno.getNumero() + 1);
		return true;
	}

	private Turno novoTurno(int numero) {
		Peao peao = getJogadorDaVez().getPeao(0);
		return new Turno(numero, peao, tabuleiro.posicaoDe(peao));
	}
	private void concluirAcao() {
		componentes.aposAcao(this);
		sincronizarVotosEGatilhos();
	}
	private void sincronizarVotosEGatilhos() {
		for (Jogador jogador : jogadores) {
			posicaoKurultai += jogador.getVotos() - votosRegistrados.get(jogador);
			votosRegistrados.put(jogador, jogador.getVotos());
		}
		verificarGatilhosDeFim();
	}
	private void verificarGatilhosDeFim() {
		boolean todasConquistadas = !tabuleiro.getCidades().isEmpty();
		for (Cidade cidade : tabuleiro.getCidades()) todasConquistadas &= cidade.isConquistada();
		if (rodadaFinal < 0 && (posicaoKurultai >= getLimiteKurultai() || todasConquistadas)) rodadaFinal = rodada + 1;
	}
	private Provincia provincia(String nome) {
		for (Provincia provincia : tabuleiro.getProvincias()) if (provincia.getNome().equals(nome)) return provincia;
		return null;
	}
	private Cidade cidade(String nome) {
		for (Cidade cidade : tabuleiro.getCidades()) if (cidade.getNome().equals(nome)) return cidade;
		return null;
	}
}
