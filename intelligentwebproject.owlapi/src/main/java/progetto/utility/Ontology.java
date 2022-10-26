package progetto.utility;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

public class Ontology {
	
	/*
	 * All'istanziazione di una Ontologia, viene creata l'ontologia, la corrispettiva TBox e l'ontology Manager.
	 * Permette di caricare da file l'ontologia di interesse
	 * */
	
	private OWLOntology ontology;
    private List<OWLAxiom> tbox;
    private OWLOntologyManager manager;
//    private List<OWLAxiom> Tg;
//    private List<OWLAxiom> Tu;
//    private List<OWLAxiom> translatedTg;
    
    
    
    public Ontology(String ontologyFile) {
        File file = new File(ontologyFile);
        manager = OWLManager.createOWLOntologyManager();
        
        try {
            this.ontology = manager.loadOntologyFromOntologyDocument(file);
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
        }
        this.tbox = ontology.logicalAxioms().collect(Collectors.toList());
    }
    
    
    
    public OWLOntology getOntology() {
    	return this.ontology;
    }
    
    
    
    public List<OWLAxiom> getTbox(){
    	return this.tbox;
    }
    
    public OWLOntologyManager getManager() {
    	return manager;
    }
}
