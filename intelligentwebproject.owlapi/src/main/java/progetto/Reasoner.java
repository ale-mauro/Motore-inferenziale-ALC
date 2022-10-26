package progetto;



import javafx.util.Pair;
import progetto.utility.LazyUnfolding;
import progetto.utility.Node;
import progetto.utility.Ontology;
import progetto.utility.Parser;

import java.util.*;

//Provides a point of convenience for creating an OWLOntologyManager with commonly required features (such as an RDF parser for example).
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;


public class Reasoner {
	
    public Pair<Boolean,Long> reasoning(Ontology ontology, String conceptText) {

    	/*
    	 * Ottiene l'ontology ed ottiene la TBox e traduce il concetto in input da stringa a OWLClassExpression
    	 * Divide la TBox in Tu e Tg (perché si deve applicare il lazy unfolding)
    	 * Traduce la Tg in un concetto C' (nel caso di TBox non vuota) (senza lazy unfolding si dovrebbe tradurre l'intera Tbox)
    	 * Crea il Tableaux e il primo nodo (che deve soddisfare il concetto C e la Tg C')
    	 * Chiama l'algoritmo del tableaux sul primo nodo.
    	 * */
    	
    	List<OWLAxiom> tbox = ontology.getTbox();
    	OWLClassExpression C = Parser.strToConcept(ontology, conceptText);
    	if (C==null) {
    		return null;
    	}
    	
        
    	
        List<OWLAxiom> Tg = null;
        List<OWLAxiom> Tu = null;
        OWLClassExpression translatedTg = null;
        
        //recupera Tg e Tu e traduce Tg in concetto
        if (tbox != null && !tbox.isEmpty()) {
        	LazyUnfolding LU = new LazyUnfolding();
	    	Pair<List<OWLAxiom>, List<OWLAxiom>> tboxLazyUnfolding = LU.getLazyUnfoldingPartition(tbox); 
	    	Tu = tboxLazyUnfolding.getKey();
	        Tg = tboxLazyUnfolding.getValue();
	        if(Tg != null && !Tg.isEmpty()) 
                translatedTg = Parser.tboxToConcept(Tg);
        
	        
	        

        //Creazione nodo iniziale del tableaux. (C e TranslatedTg sono i concetti che deve soddisfare (concetto e Tg))
        Node startNode = createFirstNode(C, translatedTg);
                
        //Chiamata al tableaux 
        long startTime = System.nanoTime(); 
        Tableaux tableaux = new Tableaux(startNode, translatedTg, LU);
        boolean isSatisfiabile = tableaux.tableauxAlgorithm(startNode);
        long stopTime = System.nanoTime();
        
        long reasoningTime = (stopTime - startTime) / 1000000;
        
        GraphWriter graphWriter = tableaux.getGraph();
        graphWriter.renderGraph("result/tableau_graph");
        
        RDFWriter rdfWriter = tableaux.getRDF();
        rdfWriter.renderRDF("result/tableau_rdf");
        
        Pair<Boolean,Long> isSatisfiable_reasoningTime = new Pair<Boolean,Long>(isSatisfiabile, reasoningTime);
        
        return isSatisfiable_reasoningTime;
        
        }
        else
        	return null;
       
    }
    
    private Node createFirstNode(OWLClassExpression C, OWLClassExpression translatedTbox) {
    	/*
    	 * Crea il primo nodo del tableaux che deve soddisfare il concetto C e la Tg tradotta.
    	 */
    	
    	OWLDataFactory dataFactory = OWLManager.getOWLDataFactory();
    	
    	OWLIndividual x0 = dataFactory.getOWLAnonymousIndividual(); // Anonymous individuals are analogous to blank nodes in RDF
        Node node = new Node(x0);
        Set<OWLAxiom> nodeConcepts = node.getLabelToSatisfy();
        
        nodeConcepts.add(dataFactory.getOWLClassAssertionAxiom(C, x0));
        if (translatedTbox != null) 
        	nodeConcepts.add(dataFactory.getOWLClassAssertionAxiom(translatedTbox, x0));
        
        node.setLabelToSatisfy(nodeConcepts);
		return node;
	}
    
}
