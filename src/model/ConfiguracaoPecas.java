package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/** Cadastro explícito das peças físicas; nunca deduz cores ou pedidos ausentes. */
final class ConfiguracaoPecas {
	private ConfiguracaoPecas() { }

	/** Linhas UTF-8 separadas por ponto e vírgula; ver docs/iteracao3.md. */
	static ComponentesKhan criar(List<String> linhas, Tabuleiro tabuleiro, Random random) {
		if (linhas == null) throw new IllegalArgumentException("Informe o cadastro das peças.");
		List<Melhoria> melhorias = new ArrayList<>();
		Map<Melhoria.Simbolo, Integer> totais = new EnumMap<>(Melhoria.Simbolo.class);
		List<Conselheiro> conselheiros = new ArrayList<>();
		Set<String> paradas = new HashSet<>();
		List<TipoBonus> bonus = new ArrayList<>(List.of(TipoBonus.values()));
		Collections.shuffle(bonus, random);
		for (int i = 0; i < linhas.size(); i++) {
			String linha = linhas.get(i);
			if (linha == null) throw new IllegalArgumentException("Linha " + (i + 1) + " inválida.");
			linha = linha.strip();
			if (linha.isEmpty() || linha.startsWith("#")) continue;
			String[] campos = linha.split(";", -1);
			for (int c = 0; c < campos.length; c++) campos[c] = campos[c].strip();
			try {
				if (campos.length == 4 && campos[0].equals("MELHORIA")) {
					Coluna cor = Coluna.valueOf(campos[1]);
					Melhoria.Simbolo simbolo = Melhoria.Simbolo.valueOf(campos[2]);
					int quantidade = Integer.parseInt(campos[3]);
					if (quantidade < 1 || quantidade > 44 || melhorias.size() + quantidade > 44) {
						throw new IllegalArgumentException("Quantidade de melhorias inválida.");
					}
					for (int n = 0; n < quantidade; n++) melhorias.add(new Melhoria(cor, simbolo));
					totais.merge(simbolo, quantidade, Integer::sum);
				} else if (campos.length == 3 && campos[0].equals("CONSELHEIRO")) {
					if (!paradas.add(campos[1]) || paradas.size() > bonus.size()) {
						throw new IllegalArgumentException("Conselheiro repetido ou excedente.");
					}
					Map<TipoTesouro, Integer> pedido = new EnumMap<>(TipoTesouro.class);
					if (!campos[2].equals("QUAISQUER")) {
						for (String item : campos[2].split(",", -1)) {
							String[] par = item.strip().split(":", -1);
							if (par.length != 2 || pedido.put(TipoTesouro.valueOf(par[0].strip()),
									Integer.parseInt(par[1].strip())) != null) {
								throw new IllegalArgumentException("Pedido inválido ou repetido.");
							}
						}
					}
					conselheiros.add(new Conselheiro(tabuleiro.getParada(campos[1]), pedido, bonus.get(conselheiros.size())));
				} else throw new IllegalArgumentException("Use MELHORIA ou CONSELHEIRO no formato documentado.");
			} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException("Linha " + (i + 1) + ": " + e.getMessage(), e);
			}
		}
		for (Melhoria.Simbolo simbolo : Melhoria.Simbolo.values()) {
			int esperado;
			switch (simbolo) {
			case MOEDA: case ESPADA: case YURT: esperado = 8; break;
			case MAO: esperado = 6; break;
			case CAVALOS: esperado = 9; break;
			default: esperado = 1;
			}
			if (totais.getOrDefault(simbolo, 0) != esperado) {
				throw new IllegalArgumentException("O cadastro deve conter " + esperado + " melhoria(s) de " + simbolo + ".");
			}
		}
		if (conselheiros.size() != tabuleiro.paradasDeConselheiro().size()) {
			throw new IllegalArgumentException("Cadastre um pedido para cada uma das sete paradas de conselheiro.");
		}
		return new ComponentesKhan(melhorias, conselheiros, random);
	}
}
