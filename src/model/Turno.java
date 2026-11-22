package model;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Estado das ações do jogador da vez. Somente Partida executa as jogadas. */
final class Turno {
	private final int numero;
	private Peao peaoAtivo;
	private Coluna coluna;
	private int cavalos;
	private int maos;
	private boolean khanPendente;
	private boolean recuperarFichas;
	private boolean peaoAgiu;
	private boolean habilidadeUsada;
	/** Liga na primeira ação aceita do jogador da vez; antes disso a partida pode ser salva. */
	private boolean iniciado;
	private final Set<Parada> visitadas = new HashSet<>();
	private final Map<Cidade, Integer> ataquesPorCidade = new HashMap<>();
	private final Map<TipoTributo, Integer> virtuais = new EnumMap<>(TipoTributo.class);
	private final Map<TipoTesouro, Integer> tesourosVirtuais = new EnumMap<>(TipoTesouro.class);

	Turno(int numero, Peao peao, Parada posicaoInicial) {
		this.numero = numero;
		peaoAtivo = peao;
		visitadas.add(posicaoInicial);
		for (TipoTributo tipo : TipoTributo.values()) virtuais.put(tipo, 0);
	}

	int getNumero() { return numero; }
	Peao getPeaoAtivo() { return peaoAtivo; }
	Coluna getColuna() { return coluna; }
	int getCavalos() { return cavalos; }
	int getMaos() { return maos; }
	boolean isKhanPendente() { return khanPendente; }
	boolean recuperaFichas() { return recuperarFichas; }
	boolean peaoJaAgiu() { return peaoAgiu; }
	boolean habilidadeUsada() { return habilidadeUsada; }
	boolean foiIniciado() { return iniciado; }
	void marcarInicio() { iniciado = true; }
	int getRecursoVirtual(TipoTributo tipo) { return virtuais.getOrDefault(tipo, 0); }
	int getTesouroVirtual(TipoTesouro tipo) { return tesourosVirtuais.getOrDefault(tipo, 0); }
	void adicionarTesouroVirtual(TipoTesouro tipo) {
		if (tipo == null) throw new IllegalArgumentException("Tipo de tesouro obrigatório");
		tesourosVirtuais.put(tipo, getTesouroVirtual(tipo) + 1);
	}
	int usarTesouroVirtual(TipoTesouro tipo, int quantidade) {
		if (tipo == null || quantidade < 0) throw new IllegalArgumentException("Tesouro e quantidade válidos obrigatórios");
		int usadas = Math.min(quantidade, getTesouroVirtual(tipo));
		tesourosVirtuais.put(tipo, getTesouroVirtual(tipo) - usadas);
		return usadas;
	}
	Set<Parada> getParadasVisitadas() {
		return Collections.unmodifiableSet(new HashSet<>(visitadas));
	}

	void escolherPeao(Peao peao, Parada posicao) {
		peaoAtivo = peao;
		visitadas.clear();
		visitadas.add(posicao);
	}

	void ativar(Coluna coluna, Herdeiro.AcoesColuna impressas, EfeitosColuna extras) {
		this.coluna = coluna;
		cavalos += impressas.getCavalos();
		maos += impressas.getMaos();
		khanPendente = impressas.temKhan();
		recuperarFichas = impressas.recuperaFichas();
		if (impressas.temMoedaVirtual()) virtuais.put(TipoTributo.MOEDA, getRecursoVirtual(TipoTributo.MOEDA) + 1);
		adicionarEfeitos(extras);
	}

	/** Também usado pela parte 2 ao conceder um bônus de ações. */
	void adicionarEfeitos(EfeitosColuna extras) {
		cavalos += extras.getCavalos();
		maos += extras.getMaos();
		for (Map.Entry<TipoTributo, Integer> recurso : extras.getRecursosVirtuais().entrySet()) {
			virtuais.put(recurso.getKey(), getRecursoVirtual(recurso.getKey()) + recurso.getValue());
		}
	}

	void registrarAcaoDoPeao() { peaoAgiu = true; }
	void registrarMovimento(int custo, Set<Parada> percorridas) {
		cavalos -= custo;
		visitadas.addAll(percorridas);
		registrarAcaoDoPeao();
	}
	void gastarMao() { maos--; }
	void resolverKhan() { khanPendente = false; }
	void usarHabilidade() { habilidadeUsada = true; }
	void gastarCavalo() { cavalos--; }
	int usarVirtual(TipoTributo tipo, int quantidade) {
		int usadas = Math.min(quantidade, getRecursoVirtual(tipo));
		virtuais.put(tipo, getRecursoVirtual(tipo) - usadas);
		return usadas;
	}
	int ataquesNaCidade(Cidade cidade) { return ataquesPorCidade.getOrDefault(cidade, 0); }
	void registrarAtaque(Cidade cidade) { ataquesPorCidade.put(cidade, ataquesNaCidade(cidade) + 1); }
}
