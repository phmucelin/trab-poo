package model;

/** As sete peças de bônus; cada peça só pode ser utilizada uma vez. */
enum TipoBonus {
	VOTO("1 voto, aplicado automaticamente na primeira entrega"),
	ESPADAS("Até 2 espadas do estoque"), MOEDAS("Até 2 moedas do estoque"),
	YURTS("Até 2 tributos de yurt do estoque"),
	MOVIMENTO_E_TRIBUTO("3 cavalos e 1 mão adicionais neste turno"),
	TRIBUTOS("2 mãos adicionais neste turno"),
	TESOURO("Escolha 1 entre até 3 tesouros da reserva");

	private final String descricao;
	TipoBonus(String descricao) { this.descricao = descricao; }
	String getDescricao() { return descricao; }
}
