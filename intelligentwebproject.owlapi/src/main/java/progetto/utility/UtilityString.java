package progetto.utility;

public class UtilityString {
	
	/*
	 * Questa classe è di aiuto alla classe GraphWriter nel metodo labelToString che richiama expressionToNaturalLangauge per convertire un'espressione in linguaggio naturale
	 * (ObjectIntersectionOf(Person, Food) in linguaggio naturale Person and Food
	 * 
	 * Si utilizza perché per visitare gli operatori di espressioni come AND oppure OR, si usa un visitator di cui si fa l'override di un metodo che non permette
	 * di modificare variabili (vuole solo variabili final or effectively final)
	 * 
	 * if(C instanceof OWLObjectIntersectionOf){
            C.accept(new OWLClassExpressionVisitor() {
                @Override
                public void visit(OWLObjectIntersectionOf oi) {
                
                	... QUI VORREI MODIFICARE UNA STRINGA, MA NON POSSO PERCHé SE NO NON SAREBBE FINAL E QUINDI FACCIO
                	UtilityString.setString(miaStringa);
                	
            	}
        	});
    	}
    	
	 * */

	private String stringa;
	
	public UtilityString(String s) {
		this.stringa = s;
	}
	
	public String getString() {
		return stringa;
	}
	
	public void setString(String stringa) {
		this.stringa = stringa;
	}
	
}
