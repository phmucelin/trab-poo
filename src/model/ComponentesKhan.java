package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Parte 2: coordena cartas, tesouros, melhorias, entregas, bônus e votos.
 * As peças de melhoria e os pedidos de cada parada entram como configuração:
 * o manual escaneado não permite transcrever todas as cores do material físico.
 * Uma instância pertence a uma única partida.
 */
final class ComponentesKhan implements ComponentesPartida {
	private final Baralho abertas;
	private final Baralho secretas;
	private final List<Carta> cartasAbertas = new ArrayList<>();
	private final Map<Jogador, Inventario> inventarios = new HashMap<>();
	private final Set<Jogador> escolheramCarta = new HashSet<>();
	private final Map<Jogador, List<Melhoria>> escolhasIniciais = new HashMap<>();
	private final Map<Jogador, Integer> linhasPremiadas = new HashMap<>();
	private final List<Melhoria> pilhaMelhorias;
	/** Índice zero é a peça mais à direita; espaços vagos ficam à esquerda. */
	private final List<Melhoria> mercado = new ArrayList<>();
	private final Map<Parada, Conselheiro> conselheiros = new HashMap<>();
	private final List<TipoTesouro> pilhaTesouros = new ArrayList<>();
	private final List<TipoTesouro> descarteTesouros = new ArrayList<>();
	private final Map<Cidade, List<TipoTesouro>> tesourosCidades = new HashMap<>();
	private final Map<Jogador, List<TipoTesouro>> escolhasTesouro = new HashMap<>();
	private Partida partida;
	private int turnoAtivado = -1;
	private boolean pontuou;
	private final Map<Jogador, Integer> votosInfluenciaFinal = new HashMap<>();
	private final Map<Jogador, Integer> votosSecretosFinal = new HashMap<>();

	ComponentesKhan(List<Melhoria> melhorias, List<Conselheiro> conselheiros, Random random) {
		if (melhorias == null || conselheiros == null || random == null
				|| melhorias.stream().anyMatch(m -> m == null) || new HashSet<>(melhorias).size() != melhorias.size()) {
			throw new IllegalArgumentException("Peças, conselheiros e sorteio válidos obrigatórios");
		}
		Set<TipoBonus> bonus = new HashSet<>();
		for (Conselheiro conselheiro : conselheiros) {
			if (conselheiro == null || conselheiro.getBonus() == null || !bonus.add(conselheiro.getBonus())
					|| this.conselheiros.putIfAbsent(conselheiro.getParada(), conselheiro) != null) {
				throw new IllegalArgumentException("Conselheiros e peças de bônus não podem se repetir");
			}
		}
		abertas = new Baralho(CartasOficiais.abertas(), random);
		secretas = new Baralho(CartasOficiais.secretas(), random);
		for (int i = 0; i < 4; i++) cartasAbertas.add(abertas.comprarCarta());
		pilhaMelhorias = new ArrayList<>(melhorias);
		Collections.shuffle(pilhaMelhorias, random);
		for (TipoTesouro tipo : TipoTesouro.values()) for (int i = 0; i < 8; i++) pilhaTesouros.add(tipo);
		Collections.shuffle(pilhaTesouros, random);
	}

	@Override public void validarPreparacao(Tabuleiro tabuleiro, List<Jogador> jogadores) {
		if (this.partida != null) throw new IllegalStateException("Componentes já vinculados");
		for (Parada parada : conselheiros.keySet()) {
			if (!tabuleiro.existeParada(parada.getNome()) || tabuleiro.getParada(parada.getNome()) != parada) {
				throw new IllegalArgumentException("Conselheiro de outro tabuleiro");
			}
		}
		int necessarias = jogadores.stream().anyMatch(j -> j.getHerdeiro() == Herdeiro.JOCHI) ? 7 : 5;
		if (pilhaMelhorias.size() < necessarias) throw new IllegalArgumentException("Faltam peças para preparação de melhorias");
		int tesourosNecessarios = 0;
		for (Cidade cidade : tabuleiro.getCidades()) {
			tesourosNecessarios += cidade.isRevelada() ? cidade.getTesouros() : Cidade.TESOUROS_AO_REVELAR;
		}
		// Reserva também as três peças do único bônus de tesouro.
		if (tesourosNecessarios > 37) throw new IllegalArgumentException("Cidades excedem a reserva de tesouros do jogo");
	}
	@Override public void preparar(Partida partida) {
		if (partida == null || this.partida != null) throw new IllegalStateException("Componentes já vinculados ou partida ausente");
		this.partida = partida;
		for (Jogador jogador : partida.getJogadores()) {
			Inventario inventario = new Inventario();
			inventarios.put(jogador, inventario);
			secretas.distribuir(jogador, 2);
			linhasPremiadas.put(jogador, jogador.getHerdeiro() == Herdeiro.JOCHI ? 1 : 0);
			if (jogador.getHerdeiro() == Herdeiro.JOCHI) escolhasIniciais.put(jogador,
					List.of(retirarTopoMelhoria(), retirarTopoMelhoria()));
		}
		reporMercado();
		abastecerCidadesReveladas();
	}
	Inventario inventarioDe(Jogador jogador) {
		Inventario inventario = inventarios.get(jogador);
		if (inventario == null) throw new IllegalArgumentException("Jogador não pertence a estes componentes");
		return inventario;
	}
	List<Carta> getCartasAbertas() { return List.copyOf(cartasAbertas); }
	List<Carta> cartasDe(Jogador jogador) { return secretas.cartasDe(jogador); }
	List<Carta> getDescarteCartas() { return secretas.getDescarte(); }
	List<Melhoria> getMercado() { return List.copyOf(mercado); }
	List<TipoTesouro> tesourosEm(Cidade cidade) { return List.copyOf(tesourosCidades.getOrDefault(cidade, List.of())); }
	List<TipoTesouro> escolhasDeTesouro(Jogador jogador) { return List.copyOf(escolhasTesouro.getOrDefault(jogador, List.of())); }
	int tesourosNaReserva() { return pilhaTesouros.size(); }
	int tesourosDescartados() { return descarteTesouros.size(); }
	int melhoriasNaReserva() { return pilhaMelhorias.size(); }

	boolean escolherCartaSecreta(Jogador jogador, Carta carta) {
		if (partida == null || partida.getFase() != Fase.PREPARACAO || escolheramCarta.contains(jogador)
				|| !secretas.cartasDe(jogador).contains(carta)) return false;
		for (Carta outra : secretas.cartasDe(jogador)) if (outra != carta) secretas.descartar(outra);
		escolheramCarta.add(jogador);
		return true;
	}
	boolean cartaEscolhida(Jogador jogador) { return escolheramCarta.contains(jogador); }
	List<Melhoria> opcoesMelhoriaInicial(Jogador jogador) { return escolhasIniciais.getOrDefault(jogador, List.of()); }
	boolean escolherMelhoriaInicial(Jogador jogador, int indice) {
		List<Melhoria> opcoes = escolhasIniciais.get(jogador);
		if (partida == null || partida.getFase() != Fase.PREPARACAO || opcoes == null || indice < 0 || indice >= opcoes.size()) return false;
		if (!inventarioDe(jogador).adicionarMelhoria(opcoes.get(indice), Coluna.BRANCA, 0)) return false;
		// A peça rejeitada volta ao fundo da pilha fechada, preservando as 44 peças.
		pilhaMelhorias.add(0, opcoes.get(1 - indice));
		escolhasIniciais.remove(jogador);
		return true;
	}

	@Override public boolean preparacaoConcluida() {
		return partida != null && escolheramCarta.size() == inventarios.size() && escolhasIniciais.isEmpty();
	}

	@Override public EfeitosColuna efeitosDasMelhorias(Jogador jogador, Coluna coluna, int numeroTurno) {
		Inventario inventario = inventarioDe(jogador);
		int cavalos = 0, maos = 0;
		Map<TipoTributo, Integer> tributos = new EnumMap<>(TipoTributo.class);
		for (Melhoria melhoria : inventario.getMelhorias(coluna)) {
			if (!inventario.podeAtivar(melhoria, numeroTurno)) continue;
			EfeitosColuna efeito = melhoria.getEfeitos();
			cavalos += efeito.getCavalos(); maos += efeito.getMaos();
			for (Map.Entry<TipoTributo, Integer> item : efeito.getRecursosVirtuais().entrySet()) tributos.merge(item.getKey(), item.getValue(), Integer::sum);
		}
		return new EfeitosColuna(cavalos, maos, tributos);
	}
	@Override public void aposAcao(Partida partida) {
		exigirPartida(partida);
		if (!emAndamento()) return;
		Turno turno = partida.getTurno();
		Jogador jogador = partida.getJogadorDaVez();
		Inventario inventario = inventarioDe(jogador);
		if (turno.getColuna() != null && turnoAtivado != turno.getNumero()) {
			turnoAtivado = turno.getNumero();
			for (Melhoria melhoria : inventario.getMelhorias(turno.getColuna())) {
				if (inventario.podeAtivar(melhoria, turno.getNumero()) && melhoria.getTesouroVirtual() != null) {
					turno.adicionarTesouroVirtual(melhoria.getTesouroVirtual());
				}
			}
		}
		Iterator<Carta> it = cartasAbertas.iterator();
		while (it.hasNext()) {
			Carta carta = it.next();
			if (carta.getObjetivo().objetivoCumprido(jogador, partida, inventario)) {
				jogador.adicionarVotos(1);
				abertas.descartar(carta);
				it.remove();
			}
		}
	}

	@Override public boolean podeReceberTesouro(Cidade cidade, TipoTesouro tipo) {
		List<TipoTesouro> pecas = tesourosCidades.get(cidade);
		return pecas != null && !pecas.isEmpty() && pecas.size() == cidade.getTesouros()
				&& (tipo == null || pecas.contains(tipo));
	}
	@Override public void receberTesouro(Jogador jogador, Cidade cidade) { receberTesouro(jogador, cidade, null); }
	@Override public void receberTesouro(Jogador jogador, Cidade cidade, TipoTesouro tipo) {
		List<TipoTesouro> pecas = tesourosCidades.get(cidade);
		if (pecas == null || pecas.size() != cidade.getTesouros() + 1 || (tipo != null && !pecas.contains(tipo))) {
			throw new IllegalStateException("Contador e peças de tesouro da cidade divergiram");
		}
		TipoTesouro recebido = tipo == null ? pecas.remove(0) : pecas.remove(pecas.indexOf(tipo));
		inventarioDe(jogador).receberTesouro(recebido);
		abastecerCidadesReveladas();
	}
	private void abastecerCidadesReveladas() {
		List<Cidade> cidades = new ArrayList<>(partida.getTabuleiro().getCidades());
		cidades.sort(Comparator.comparing(Cidade::getNome));
		for (Cidade cidade : cidades) {
			if (!cidade.isRevelada() || tesourosCidades.containsKey(cidade)) continue;
			if (cidade.getTesouros() > pilhaTesouros.size()) throw new IllegalStateException("Reserva insuficiente para revelar cidade");
			List<TipoTesouro> pecas = new ArrayList<>();
			for (int i = 0; i < cidade.getTesouros(); i++) pecas.add(retirarTopoTesouro());
			tesourosCidades.put(cidade, pecas);
		}
	}

	/** Índice -1 compra do topo; demais índices vêm de getMercado(). */
	boolean podeComprarMelhoria(int indice) {
		Melhoria melhoria = consultarMelhoria(indice);
		return melhoria != null && podeComprarMelhoria(indice, melhoria.getCor());
	}
	boolean podeComprarMelhoria(int indice, Coluna destino) {
		if (!emAndamento() || destino == null) return false;
		Melhoria melhoria = consultarMelhoria(indice);
		Jogador jogador = partida.getJogadorDaVez();
		return melhoria != null && (jogador.getHerdeiro() == Herdeiro.JOCHI || destino == melhoria.getCor())
				&& inventarioDe(jogador).cabeMelhoria(destino)
				&& jogador.contarTributo(TipoTributo.MOEDA) + partida.getTurno().getRecursoVirtual(TipoTributo.MOEDA) >= 2;
	}
	boolean comprarMelhoria(int indice) {
		Melhoria melhoria = consultarMelhoria(indice);
		return melhoria != null && comprarMelhoria(indice, melhoria.getCor());
	}
	boolean comprarMelhoria(int indice, Coluna destino) {
		if (!podeComprarMelhoria(indice, destino)) return false;
		Jogador jogador = partida.getJogadorDaVez();
		Melhoria melhoria = retirarMelhoria(indice);
		partida.gastarTributo(TipoTributo.MOEDA, 2);
		inventarioDe(jogador).adicionarMelhoria(melhoria, destino, partida.getTurno().getNumero(), jogador.getHerdeiro() == Herdeiro.JOCHI);
		partida.concluirAcaoComplementar(false);
		return true;
	}

	boolean podeEscolherMelhoriaGratuita(int indice) {
		return emAndamento() && temMelhoriaPendente(partida.getJogadorDaVez()) && consultarMelhoria(indice) != null
				&& inventarioDe(partida.getJogadorDaVez()).cabeMelhoria(Coluna.BRANCA);
	}
	boolean escolherMelhoriaGratuita(int indice) {
		if (!podeEscolherMelhoriaGratuita(indice)) return false;
		Melhoria melhoria = retirarMelhoria(indice);
		inventarioDe(partida.getJogadorDaVez()).adicionarMelhoria(melhoria, Coluna.BRANCA, partida.getTurno().getNumero());
		linhasPremiadas.merge(partida.getJogadorDaVez(), 1, Integer::sum);
		partida.concluirAcaoComplementar(false);
		return true;
	}

	boolean temMelhoriaPendente(Jogador jogador) {
		Inventario inventario = inventarioDe(jogador);
		return inventario.cabeMelhoria(Coluna.BRANCA) && inventario.linhasCompletas() > linhasPremiadas.getOrDefault(jogador, 0)
				&& (!mercado.isEmpty() || !pilhaMelhorias.isEmpty());
	}
	@Override public boolean temEscolhaPendente(Jogador jogador) {
		return escolhasTesouro.containsKey(jogador) || temMelhoriaPendente(jogador);
	}
	@Override public boolean moverKhanParaMelhorias(Partida partida) {
		exigirPartida(partida);
		if (!emAndamento() || !partida.getTurno().isKhanPendente()
				|| Partida.AREA_MELHORIAS.equals(partida.getPosicaoKhan())) return false;
		// Da direita para a esquerda: vazio, espada, vazio, moeda, vazio (p. 9).
		if (mercado.size() < 4) partida.receberTributoDoEstoque(TipoTributo.MOEDA, 1);
		if (mercado.size() < 2) partida.receberTributoDoEstoque(TipoTributo.ESPADA, 1);
		reporMercado();
		return true;
	}
	private void reporMercado() { while (mercado.size() < 5 && !pilhaMelhorias.isEmpty()) mercado.add(retirarTopoMelhoria()); }
	private Melhoria consultarMelhoria(int indice) {
		if (indice == -1) return pilhaMelhorias.isEmpty() ? null : pilhaMelhorias.get(pilhaMelhorias.size() - 1);
		return indice >= 0 && indice < mercado.size() ? mercado.get(indice) : null;
	}
	private Melhoria retirarMelhoria(int indice) { return indice == -1 ? retirarTopoMelhoria() : mercado.remove(indice); }
	private Melhoria retirarTopoMelhoria() { return pilhaMelhorias.remove(pilhaMelhorias.size() - 1); }
	private TipoTesouro retirarTopoTesouro() { return pilhaTesouros.remove(pilhaTesouros.size() - 1); }

	List<Conselheiro> getConselheiros() {
		List<Conselheiro> lista = new ArrayList<>(conselheiros.values());
		lista.sort(Comparator.comparing(c -> c.getParada().getNome()));
		return List.copyOf(lista);
	}
	Conselheiro getConselheiro(String nome) {
		for (Conselheiro conselheiro : conselheiros.values()) if (conselheiro.getParada().getNome().equals(nome)) return conselheiro;
		return null;
	}

	boolean podeEntregarTesouros(String nome, Map<TipoTesouro, Integer> oferta) {
		if (!emAndamento() || !partida.getTabuleiro().existeParada(nome)) return false;
		Parada parada = partida.getTabuleiro().getParada(nome);
		Conselheiro conselheiro = conselheiros.get(parada);
		if (conselheiro == null || !partida.getTurno().getParadasVisitadas().contains(parada) || !conselheiro.aceita(oferta)) return false;
		Inventario inventario = inventarioDe(partida.getJogadorDaVez());
		for (Map.Entry<TipoTesouro, Integer> item : oferta.entrySet()) {
			if (inventario.contarTesouro(item.getKey()) + partida.getTurno().getTesouroVirtual(item.getKey()) < item.getValue()) return false;
		}
		return true;
	}
	boolean entregarTesouros(String nome, Map<TipoTesouro, Integer> oferta) {
		if (!podeEntregarTesouros(nome, oferta)) return false;
		Jogador jogador = partida.getJogadorDaVez();
		Inventario inventario = inventarioDe(jogador);
		Map<TipoTesouro, Integer> fisicos = new EnumMap<>(TipoTesouro.class);
		for (Map.Entry<TipoTesouro, Integer> item : oferta.entrySet()) {
			int n = item.getValue() - partida.getTurno().usarTesouroVirtual(item.getKey(), item.getValue());
			fisicos.put(item.getKey(), n);
			for (int i = 0; i < n; i++) descarteTesouros.add(item.getKey());
		}
		inventario.gastarTesouros(fisicos);
		Conselheiro conselheiro = conselheiros.get(partida.getTabuleiro().getParada(nome));
		jogador.adicionarVotos(conselheiro.getVotos());
		TipoBonus bonus = conselheiro.retirarBonus();
		if (bonus != null) {
			inventario.receberBonus(bonus);
			if (bonus == TipoBonus.VOTO) { inventario.usarBonus(bonus); jogador.adicionarVotos(1); }
		}
		partida.concluirAcaoComplementar(true);
		return true;
	}
	boolean podeUsarBonus(TipoBonus bonus) {
		if (!emAndamento() || bonus == null || bonus == TipoBonus.VOTO) return false;
		Jogador jogador = partida.getJogadorDaVez();
		return inventarioDe(jogador).podeUsarBonus(bonus)
				&& (bonus != TipoBonus.TESOURO || (!pilhaTesouros.isEmpty() && !escolhasTesouro.containsKey(jogador)));
	}
	boolean usarBonus(TipoBonus bonus) {
		if (!podeUsarBonus(bonus)) return false;
		Jogador jogador = partida.getJogadorDaVez();
		Inventario inventario = inventarioDe(jogador);
		inventario.usarBonus(bonus);
		switch (bonus) {
		case ESPADAS: partida.receberTributoDoEstoque(TipoTributo.ESPADA, 2); break;
		case MOEDAS: partida.receberTributoDoEstoque(TipoTributo.MOEDA, 2); break;
		case YURTS: partida.receberTributoDoEstoque(TipoTributo.YURT, 2); break;
		case MOVIMENTO_E_TRIBUTO: partida.getTurno().adicionarEfeitos(new EfeitosColuna(3, 1, Map.of())); break;
		case TRIBUTOS: partida.getTurno().adicionarEfeitos(new EfeitosColuna(0, 2, Map.of())); break;
		case TESOURO:
			List<TipoTesouro> escolha = new ArrayList<>();
			int n = Math.min(3, pilhaTesouros.size());
			for (int i = 0; i < n; i++) escolha.add(retirarTopoTesouro());
			escolhasTesouro.put(jogador, escolha);
			break;
		default: throw new IllegalStateException("Bônus desconhecido");
		}
		partida.concluirAcaoComplementar(false);
		return true;
	}
	boolean escolherTesouroDoBonus(TipoTesouro tipo) {
		if (!emAndamento()) return false;
		Jogador jogador = partida.getJogadorDaVez();
		List<TipoTesouro> escolha = escolhasTesouro.get(jogador);
		if (escolha == null || !escolha.contains(tipo)) return false;
		escolha.remove(tipo);
		descarteTesouros.addAll(escolha);
		escolhasTesouro.remove(jogador);
		inventarioDe(jogador).receberTesouro(tipo);
		partida.concluirAcaoComplementar(false);
		return true;
	}

	@Override public void pontuarFimDeJogo(Partida partida) {
		exigirPartida(partida);
		if (pontuou || partida.getFase() != Fase.ENCERRADA) return;
		for (Jogador jogador : partida.getJogadores()) {
			Inventario inventario = inventarioDe(jogador);
			int influencia = Pontuacao.influencia(jogador, partida.getTabuleiro(), inventario);
			int secretos = 0;
			for (Carta carta : secretas.cartasDe(jogador)) secretos += carta.getObjetivo().votos(jogador, partida, inventario);
			votosInfluenciaFinal.put(jogador, influencia);
			votosSecretosFinal.put(jogador, secretos);
			jogador.adicionarVotos(influencia + secretos);
		}
		pontuou = true;
	}
	int getVotosInfluenciaFinal(Jogador jogador) { return votosInfluenciaFinal.getOrDefault(jogador, 0); }
	int getVotosSecretosFinal(Jogador jogador) { return votosSecretosFinal.getOrDefault(jogador, 0); }
	List<Jogador> getVencedores() { return partida == null ? List.of() : Pontuacao.vencedores(partida, inventarios); }
	private boolean emAndamento() { return partida != null && partida.getFase() == Fase.EM_ANDAMENTO; }
	private void exigirPartida(Partida partida) {
		if (this.partida == null || this.partida != partida) throw new IllegalArgumentException("Partida não pertence a estes componentes");
	}
}
