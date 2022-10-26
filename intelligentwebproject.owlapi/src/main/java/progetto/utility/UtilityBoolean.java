package progetto.utility;

public class UtilityBoolean {

	/*
	 * Questa classe è di aiuto nel partizionamento della TBox in Tu e Tg (in particolare nel metodo isAcyclicalConcept).
	 * 
	 * Si utilizza perché siccome isAcyclical è un metodo ricorsivo, si cerca di evitare di perdere dei valori booleani. Pertanto si conserva il valore booleano in questa classe.
	 * 
	 * 
	 * */
	private Boolean booleano;
	
	public UtilityBoolean() {
	}
	
	public UtilityBoolean(Boolean b) {
		this.booleano = b;
	}
	
	public Boolean getValue() {
		return booleano;
	}
	
	public void setValue(Boolean booleano) {
		this.booleano = booleano;
	}
	
}
