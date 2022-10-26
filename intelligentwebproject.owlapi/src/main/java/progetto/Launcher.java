package progetto;

import javafx.util.Pair;
import progetto.utility.Ontology;
import progetto.view.View;

public class Launcher {
	/*
	 * Legge l'ontologia (file path) e il concetto (stringa) tramite una GUI, 
	 * Crea l'OWLOntology e la TBox (lista di assiomi),
	 * Passa il tutto al Reasoner
	 * 
	 * */
	
	public static void main(String[] args) {
        Reasoner reasoner = new Reasoner();
        View view = new View();
        
        String ontologyFile = view.readOntology();
        
        String conceptText = view.readConcept();
              
        // caricamento Ontologia (e rispettiva tbox)
        Ontology ontology = new Ontology(ontologyFile);
          
        Pair<Boolean,Long> isSatisfiable_reasoningTime = reasoner.reasoning(ontology, conceptText);
        
        view.showResult(isSatisfiable_reasoningTime);
        
//        }
	}
}
