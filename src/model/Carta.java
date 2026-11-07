package model;

/** Conselheiro aberto ou secreto; o objetivo define a forma de pontuar. */
final class Carta {
	private final String nome;
	private final Objetivo objetivo;
	Carta(String nome, Objetivo objetivo) {
		if (nome == null || nome.isBlank() || objetivo == null) throw new IllegalArgumentException("Nome e objetivo obrigatórios");
		this.nome = nome;
		this.objetivo = objetivo;
	}
	String getNome() { return nome; }
	String getId() {
		String slug = java.text.Normalizer.normalize(nome, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "")
				.toUpperCase(java.util.Locale.ROOT).replaceAll("[^A-Z0-9]+", "_");
		return (isSecreta() ? "SECRETA_" : "ABERTA_") + slug;
	}
	Objetivo getObjetivo() { return objetivo; }
	boolean isSecreta() { return objetivo.isSecreto(); }
	@Override public String toString() { return nome; }
}
