package progetto.view;

import javax.swing.JFrame;

import javafx.util.Pair;


public class View extends JFrame{
	
	private static final long serialVersionUID = 1L;

	public View(){
        setDefaultCloseOperation(EXIT_ON_CLOSE);
    }
	
	public String readConcept() {
		ConceptPanel c = new ConceptPanel();
		return c.getConcept();
	}
	
	public String readOntology() {
		OntologyPanel o = new OntologyPanel();
		return o.getOntologyPath();
	}
	
	public void showResult(Pair<Boolean,Long> isSatisfiable) {
		new ResultPanel(isSatisfiable);
	}
		
}
