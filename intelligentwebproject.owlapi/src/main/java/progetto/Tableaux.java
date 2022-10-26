package progetto;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLClassExpressionVisitor;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLObjectAllValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectUnionOf;

import javafx.util.Pair;
import progetto.utility.LazyUnfolding;
import progetto.utility.Node;

public class Tableaux {
	
	private RDFWriter rdfWriter = new RDFWriter(); //Scrive il file RDF
	private GraphWriter graphWriter = new GraphWriter(); //Scrive il grafo risultante dall'algoritmo del tableaux
	
	private OWLIndividual individual;
	private OWLDataFactory df = OWLManager.getOWLDataFactory(); //data factory
	private OWLClassExpression translatedTbox; //Translated Tg nel caso di lazy unfolding, serve perché sarà aggiunta ai concetti del nodo in caso di esistenziale
//	private  List<OWLAxiom> Tu; //serve per applicare la regola di lazy unfolding (per aggiungere nuovi assiomi a partire dalla Tu).
	private LazyUnfolding LU;
	
	
	public Tableaux(Node startNode, OWLClassExpression translatedTbox, /*List<OWLAxiom> Tu*/ LazyUnfolding LU) {
		this.individual = startNode.getIndividual();
		this.translatedTbox = translatedTbox;
		this.LU = LU;
//		this.Tu = Tu;
		
		//INIZIALIZZAZIONE GRAFO e RDF
		rdfWriter.initRDF();
		graphWriter.initGraph();
		
		graphWriter.addStartNode(startNode);
		graphWriter.addLabelToNode(startNode, startNode.getLabelToSatisfy()," INPUT CONCEPT AND TRANSLATED TBOX");
		
	}
		
	
	
	public boolean tableauxAlgorithm(Node node) {
		/*
		 * Input: x Nodo (che contiene individuo ed etichette associate)
		 * Output: clash-free oppure no
		 *
		 * */
		
		if(node.isBlocked()) {
			graphWriter.setBlocked(node, node.getBlockingNode());
			return true;
		}
		
		
		node = applyAndExhaustively(node);
		Pair<OWLClassExpression, OWLClassExpression> clashAND = possibleClash(node);
		if (clashAND != null) {
			String label = graphWriter.labelToString(node.getLabelToSatisfy()).replace("\n", ", ");
			rdfWriter.addRDFTriple(node, "lables", label);
			
			graphWriter.setClash(node, clashAND);
			return false;
		}
		
		Boolean isClashed = applyOrExhaustively(node);
	    if (isClashed != null) {
	    	return isClashed;
	    }
	    
	    boolean lazyUnfolding = isAppliedLazyUnfolding(node);
	    if(lazyUnfolding) {
	    	graphWriter.addArc(node, node, "LazyUnfolding");
	    	graphWriter.addLabelToNode(node, node.getLabelToSatisfy(), "LU");
	    	node.setIfBlocked();
	    	if(possibleClash(node) == null)
	    		return tableauxAlgorithm(node);
	    }
	    
	    boolean isClashFree;
	    isClashFree = applyExistentialRule(node);
		if(!isClashFree)
			return false;
		
	    return true;		
	}
	
	
	private Node applyAndExhaustively(Node node) {
		/*
		 * APPLICA LA REGOLA DELL'AND
		 * Rimuove tutti gli AND che non sono conformi alla regola dell'AND (ossia quelli in cui il nodo soddisfa tutti i congiunti)
		 * 
		 * Controlla se ci sono assiomi di tipo IntersectionOf
		 * Nel caso positivo, aggiunge all'individuo node i concetti "separati" dell'and attraverso il metodo addExpressionToConcept
		 * 
		 * Scrive poi la label del nodo nel grafo.
		 * */
		
	    Set<OWLAxiom> nodeConcepts = node.getLabelToSatisfy();
	    Set<OWLAxiom> nodeConceptsTMP = new TreeSet<OWLAxiom>(nodeConcepts);
	    
	  //RIMUOVE TUTTI GLI AND NON APPLICABILI (OSSIA QUELLI IN CUI IL NODO SODDISFA GIà TUTTI I CONGIUNTI)
	    for (OWLAxiom axiom : nodeConceptsTMP){
	    	OWLClassExpression classExpression = null;
	    	
            if (axiom instanceof OWLClassAssertionAxiom){
                classExpression = ((OWLClassAssertionAxiom) axiom).getClassExpression();

                if(classExpression instanceof OWLObjectIntersectionOf){
                	
                	if(containsAllConjunct(node, classExpression)){
                		nodeConcepts.remove(axiom);
                		graphWriter.addArc(node, node, "AND already satisfied");
            	        graphWriter.addLabelToNode(node, node.getLabelToSatisfy(),"");
                	}
                }
            }
	    }
	    
	    nodeConcepts = node.getLabelToSatisfy();
	    nodeConceptsTMP = new TreeSet<OWLAxiom>(nodeConcepts);
	    
	    boolean isAppliedANDRule = false;
	   	    
		/*
		 * Scorre i concetti e se trova un intersezione, aggiunge i congiunti ai concetti del nodo.
		 * */
		
        for (OWLAxiom axiom : nodeConceptsTMP){
        	OWLClassExpression classExpression = null;
            if (axiom instanceof OWLClassAssertionAxiom){
            	classExpression = ((OWLClassAssertionAxiom) axiom).getClassExpression();

                if(classExpression instanceof OWLObjectIntersectionOf){
                	node.getLabelToSatisfy().remove(axiom);
                	isAppliedANDRule = node.addANDConjunctsToLabel(classExpression, df, individual);
                }
            }
        }
        
        if(isAppliedANDRule) {
	        graphWriter.addArc(node, node, "And");
	        graphWriter.addLabelToNode(node, node.getLabelToSatisfy(),"");
        }
        
		return node; //è anche superfluo, però serve a far capire che il nodo è possibilmente modificato.
	}

	private Boolean applyOrExhaustively(Node node) {
		/*
		 * Il metodo rimuove tutti gli OR non applicabili secondo la regola (OSSIA QUELLI IN CUI IL NODO SODDISFA GIà ALMENO UN DISGIUNTO) 
		 * 
		 * Il metodo scorre tutti gli assiomi del nodo "node"
		 * Quando trova un assioma di tipo OR (che sarà sicuramente applicabile), crea un nuovo nodo PER OGNI OPERANDO DELL'OR (MA SI PROCEDE CON UN SINGOLO NODO ALLA VOLTA)!!
		 * Si controlla se il nuovo nodo creato è clash free oppure no
		 * 
		 * 
		 * 
	     * Nell'applicare l'OR, si "tenta" un "sottoconcetto" dell'OR alla volta.
	     * Per questo motivo c'è bisogno di un nuovo nodo.
	     * 
	     * Il nuovo nodo sarà uguale al nodo corrente con l'aggiunta del sottoconcetto dell'OR
	     * */
						
		Set<OWLAxiom> nodeConcepts = node.getLabelToSatisfy();
		Set<OWLAxiom> nodeConceptsTMP = new TreeSet<OWLAxiom>(nodeConcepts);
	    Pair<OWLClassExpression, OWLClassExpression> clash = null;
	    boolean isNewNodeClashFree = true;
	    
	    //RIMUOVE TUTTI GLI OR NON APPLICABILI (OSSIA QUELLI IN CUI IL NODO SODDISFA GIà ALMENO UN DISGIUNTO)
	    for (OWLAxiom axiom : nodeConceptsTMP){
	    	OWLClassExpression classExpression = null;
	    	
            if (axiom instanceof OWLClassAssertionAxiom){
                classExpression = ((OWLClassAssertionAxiom) axiom).getClassExpression();

                if(classExpression instanceof OWLObjectUnionOf){
                	List<OWLClassExpression> operandList = ((OWLObjectUnionOf) classExpression).getOperandsAsList();
                	
                	if(!operandList.isEmpty() && nodeContainsAtLeastOnDisjunct(node, operandList)){
                		nodeConcepts.remove(axiom);
                		graphWriter.addArc(node, node, "OR already satisfied");
            	        graphWriter.addLabelToNode(node, node.getLabelToSatisfy(),"");
                	}
                }
            }
	    }
	    
	    String label = graphWriter.labelToString(node.getLabelToSatisfy()).replace("\n", ", ");
		rdfWriter.addRDFTriple(node, "lables", label);
	    
	    /*
	     * Scorre tutti gli assiomi alla ricerca di un OR (ObjectUnionOf)
	     * Quando lo trova, siccome è sicuramente applicabile, crea un nuovo nodo per ogni operando dell'or
	     * Solo un nuovo nodo alla volta continuerà la computazione
	     * */
	    nodeConceptsTMP = new TreeSet<OWLAxiom>(nodeConcepts);
	    for (OWLAxiom axiom : nodeConceptsTMP){
	    	OWLClassExpression classExpression = null;
	    	
            if (axiom instanceof OWLClassAssertionAxiom){
                classExpression = ((OWLClassAssertionAxiom) axiom).getClassExpression();

                if(classExpression instanceof OWLObjectUnionOf){
                	List<OWLClassExpression> operandList = ((OWLObjectUnionOf) classExpression).getOperandsAsList();
                	
                	/*
                	 * La variabile ret è una lista che conterrà i valori di ritorno dell'algoritmo del tableaux applicato ai figli
                	 * Se un figlio ritorna true all'algoritmo del tableaux, allora è clash free e il metodo può terminare (e terminerà anche il programma)
                	 * Altrimenti, l'algoritmo del tableaux ritornerà falso, e questo significa che il figlio contiene il clash. Quindi, si dovrà passare ad analizzare
                	 * il prossimo figlio a cui sarà aggiunto l'altro disgiunto.
                	 * */
            		List<Boolean> ret = new LinkedList<>();
            		
            		for (OWLClassExpression operand : operandList) {
                		/*
                		 * Si crea un nuovo nodo che soddisferà tutti i concetti del padre più un disgiunto	
                		 * L'OR è sicuramente applicabile, perché tutti quelli non applicabili sono stati scartati		
                		 */
                		Node newNode = new Node(individual); //si crea nuovo nodo per l'OR
                		newNode.setParent(node);
                		newNode.setLabelToSatisfy(new TreeSet<OWLAxiom>(nodeConcepts));
                		if(!node.getAxiomAlreadyAppliedLU().isEmpty()) 
                			newNode.setAxiomAlreadyAppliedLU(node.getAxiomAlreadyAppliedLU());
                		
                		newNode.addExpressionToLabel(operand, df, individual);
                		
                		newNode.getLabelToSatisfy().remove(axiom);
            			graphWriter.addArc(newNode.getParent(), newNode, "Or");
            	        graphWriter.addLabelToNode(newNode, newNode.getLabelToSatisfy(),"");
            	        
            	        rdfWriter.addRDFTriple(node, "orEdge", newNode);
            	                    	        
            	        clash = possibleClash(newNode);
            	        isNewNodeClashFree = (clash == null);
            	                        	        
            	        if (!isNewNodeClashFree) { 
            	        	graphWriter.setClash(newNode, clash);
            	        	ret.add(false);
            	        	
            	        	String label2 = graphWriter.labelToString(newNode.getLabelToSatisfy()).replace("\n", ", ");
            	    		rdfWriter.addRDFTriple(newNode, "lables", label2);
            	        }
            	        else {
            	        	/* se il nuovo nodo non ha nessun clash
            	        	 * devo continuare la computazione sul newNode, quindi devo applicare nuovamente AND, OR ecc...
            	        	 * (Si richiama, dunque, l'algoritmo del tableaux)
            	        	 */
            	        	if(tableauxAlgorithm(newNode)) {
            	        		return true;
            	        	}
            	        	else
            	        		ret.add(false);
            	        }
            		}
            		/*
            		 * Inutile fare controllo:
            		 * if (ret.isEmpty()) return true;
            		 * in quanto ret conterrà per forza un elemento (che tral'altro sarà sempre false quindi potrei fare return false
            		 * */
            		boolean returnValue = false;
            		for(boolean el : ret) {
            			returnValue = returnValue || el;
            		}
            		return returnValue;
                }
            }
        }
	    return null;
	}
	

	private boolean applyExistentialRule(Node node) {
		/*
		 * Il metodo scorre tutti gli assiomi del nodo "node"
		 * Quando trova un assioma del tipo EXISTS, crea un nuovo nodo (che soddisferà tutti i concetti del padre + l'exists)
		 * Aggiunge la proprietà e il filler ai concetti del nodo (e rimuove l'assioma exists completo [leva exists eats.food e mette la proprietà eats e il filler food])
		 * 
		 * Se viene applicata la regola esistenziale, bisogna soddisfare tutti gli universali che quantificano sulla stessa proprietà
		 * 
		 * Si controlla quindi se il nuovo nodo creato (che soddisfa l'esiste + tutti i rispettivi universali), è clash free oppure no
		 * - se è clash free, bisogna continuare la computazione sul nodo (perché potrebbe essere Exists eats.(food and person), il nuovo nodo deve soddisfare food and person, e quindi si continua la computazione
		 * - se contiene un clash, allora si continuerà l'esaminazione degli altri assiomi (alla ricerca di un altro EXISTS)
		 * 
		 * 
		 * Crea un nuovo nodo per ogni esistenziale.
		 * Se non ci sono esistenziali, il metodo ritorna true,
		 * Se ci sono esistenziali e ALMENO un esistenziale è clash free, ritorna true
		 * Altrimenti, se tutti i nuovi nodi contengono un clash, ritorna false.
		 * 
		 * Se non c'è nessuna regola applicata, tutti i perogni sono soddisfatti vacuamente,
		 * sono lasciati scritti nelle label del nodo per indicare che sono soddisfatti vacuamente
		 * */
		
		Set<OWLAxiom> nodeConcepts = node.getLabelToSatisfy();
	    Set<OWLAxiom> nodeConceptsTMP = new TreeSet<OWLAxiom>(nodeConcepts);
	    boolean isAppliedExistsRule = false;
	    Pair<OWLClassExpression, OWLClassExpression> clash = null;
	    boolean isClashFree = true;
	    OWLClassExpression classExpression = null;
	    
	    List<Boolean> ret = new LinkedList<>();
	    
	    for (OWLAxiom axiom : nodeConceptsTMP){
            if (axiom instanceof OWLClassAssertionAxiom){
                classExpression = ((OWLClassAssertionAxiom) axiom).getClassExpression();

                if (classExpression instanceof OWLObjectSomeValuesFrom){
                	Node newNode = new Node(individual); //si crea nuovo nodo per l'esistenziale
                	newNode.setParent(node);
                	newNode.setLabelToSatisfy(new TreeSet<OWLAxiom>()); //IL NON NODO PRENDE I CONCETTI DEL PADRE, MA SOLAMENTE FILLER E PROPERTY DELL'ESISTENZIALE
//                	newNode.setAxiomAlreadyAppliedLU(node.getAxiomAlreadyAppliedLU());
                	
                	OWLObjectPropertyExpression property = newNode.addExistPropertyToLabel(classExpression, translatedTbox, df, individual);
            		
            		isAppliedExistsRule = (property != null);
            		
            		if (isAppliedExistsRule) {
            			newNode.getLabelToSatisfy().remove(axiom);
            			            			
            			graphWriter.addArc(newNode.getParent(), newNode, "EXISTS"+property.toString());
            	        graphWriter.addLabelToNode(newNode, newNode.getLabelToSatisfy(),"");
            	        
            			rdfWriter.addRDFTriple(node, "existentialRule", newNode);
            			
            	        // applica UNIVERSALE esaustivamente sul nuovo nodo
            	        applyUniversalRule(node, newNode);
            	        
            	        //Dopo l'applicazione della regola dell'universale, si controlla se il nuovo nodo generato può essere bloccato oppure no
            	        node.setIfBlocked();
            	        
            	        clash = possibleClash(newNode);
            	        isClashFree = (clash == null);
            	        if (!isClashFree) {
            	        	graphWriter.setClash(newNode, clash);
            	        	ret.add(false);
            	        	
            	        	String label2 = graphWriter.labelToString(newNode.getLabelToSatisfy()).replace("\n", ", ");
            	    		rdfWriter.addRDFTriple(newNode, "lables", label2);
            	        }
            	        else {
            	        	/*
            	        	 * se non c'è nessun clash si continua la computazione sul newNode, quindi devo applicare nuovamente AND, OR ecc...
            	        	 * Si richiama, dunque, l'algoritmo del tableaux
            	        	*/
            	        	if(tableauxAlgorithm(newNode)) {
            	        		ret.add(true); // è inutile questa operazione, però vabbè
            	        		return true;
            	        	}
            	        	else
            	        		ret.add(false);
            	        }
            	        
            		}
                }
            }
        }
	    if (ret.isEmpty()) {
	    	//Se si arriva qua, significa che il nodo ritornerà true e quindi il concetto è soddisfacibile
    		graphWriter.setClashFree(node);
	    	return true;
	    }
	    boolean returnValue = false;
		for(boolean el : ret) {
			returnValue = returnValue || el;
		}
		return returnValue;
		
//		return isClashFree;
	}
	
	private void applyUniversalRule(Node node, Node newNode) {
		/*
		 * Scorre tutti gli assiomi del nodo,
		 * Se trova un assioma del tipo AllValuesFrom, aggiunge al nodo tutti i concetti filler se la proprietà è la stessa
		 * */
		Set<OWLAxiom> nodeConcepts = node.getLabelToSatisfy();
	    Set<OWLAxiom> nodeConceptsTMP = new TreeSet<OWLAxiom>(nodeConcepts);
	    OWLClassExpression classExpression = null;
	    boolean isAdded = false;
	    
		for (OWLAxiom axiom : nodeConceptsTMP) {
            if (axiom instanceof OWLClassAssertionAxiom){ 
                classExpression = ((OWLClassAssertionAxiom) axiom).getClassExpression();
                
                if (classExpression instanceof OWLObjectAllValuesFrom){
                	isAdded = newNode.addUniversalPropertyToLabel(axiom, classExpression, df, individual, graphWriter);
            		if(isAdded) {
            	        graphWriter.addArc(newNode, newNode, "FORALL ");
            	        graphWriter.addLabelToNode(newNode, newNode.getLabelToSatisfy(),"");
            		}
                }
            }
        }
	}
	
	private boolean isAppliedLazyUnfolding(Node node) {
		/*
		 * L'applicazione del lazy unfolding avviene dopo l'applicazione esaustiva degli AND e degli OR
		 * Sono state generate Tu e Tg a partire dalla TBox
		 * Tg è stata convertita in un unico concetto
		 * 
		 * A questo punto, bisogna applicare le seguenti regole a Tu.
		 * Sia A un concetto del nodo
		 * Se A == C appartiene alla Tu e C non appartiene ai concetti del nodo, si aggiunge C ai concetti del Nodo.
		 * 
		 * Sia not A un concetto del nodo
		 * Se A == C appartiene alla tu e not C non appartiene ai concetti del nodo, si aggiunge not C ai concetti del nodo
		 * 
		 * Sia A un concetto del nodo
		 * Se A subclassOf C appartiene alla Tu e C non appartiene ai concetti del nodo, allora C è aggiunta ai concetti del nodo
		 * 
		 * 
		 * Questo metodo si occupa di aggiungere i nuovi concetti C al nodo
		 * Scorre tutti i concetti A, e verifica se nella Tu ci sta uno tra A==C, e A subclassOF C (attraverso il metodo applyLazyUnfoldingRule
		 * */
		
//		if(node.isAppliedLU()) {
//			return false;
//		}
		
		Set<OWLAxiom> nodeConcepts = node.getLabelToSatisfy();
        
        
        
        boolean isAppliedRuleForIteration;
        boolean isAppliedRule = false;
        
        do {
            isAppliedRuleForIteration = false;
            Set<OWLAxiom> nodeConceptsTMP = new TreeSet<OWLAxiom>(nodeConcepts);
            Set<OWLAxiom> axiomAlreadyAppliedLU = node.getAxiomAlreadyAppliedLU();
           	nodeConceptsTMP.removeAll(axiomAlreadyAppliedLU);

            for(OWLAxiom axiom: nodeConceptsTMP) {
                if(axiom instanceof OWLClassAssertionAxiom) {
                	OWLClassAssertionAxiom classAssertion = (OWLClassAssertionAxiom) axiom;
                	
                	OWLClassAssertionAxiom addedLUAxiom = LU.applyLazyUnfoldingRule(classAssertion,node, individual);
                	if(addedLUAxiom != null) {
                        isAppliedRuleForIteration = true;
                        isAppliedRule = true;
                        node.addAxiomAlreadyAppliedLU(axiom);
                    }
                }
            }
        } while(isAppliedRuleForIteration); //se isAppliedRule = true, significa che sono stati aggiunti nuovi concetti al nodo e quindi si ripete il while alla ricerca degli assiomi A==C ecc... da lazy unfoldare
       
        //Se sono stati aggiunti nuovi concetti al nodo, bisogna richiamare l'algoritmo del tableaux, altrimenti 
        if(isAppliedRule) {
        	node.setAppliedLU(true);
        	return true;
        }
        else
        	return false;
	}
	
	
	private Pair<OWLClassExpression, OWLClassExpression> possibleClash(Node node) {
		/*
		 * Controlla se un nodo è clash free oppure no.
		 * Nel caso in cui è presente un clash, ritorna una coppia contenente le espressioni che hanno causato il clash
		 * ritorna null se non ci sono clash
		 * 
		 * Per controllare se c'è un clash, si scorre doppiamente la lista degli assiomi del nodo
		 * Se un assioma è uguale al complemento di un altro assioma, allora ritorna il clash.
		 * */
		Set<OWLAxiom> nodeLabels = node.getLabelToSatisfy();
		OWLClassExpression firstExpression, secondExpression;
		Pair<OWLClassExpression, OWLClassExpression> clash = null;
		
		for(OWLAxiom firstAxiom : nodeLabels) {
			
			if (firstAxiom instanceof OWLClassAssertionAxiom){
                firstExpression = ((OWLClassAssertionAxiom) firstAxiom).getClassExpression();
                
                if(firstExpression instanceof OWLClass) {
                    if(firstExpression.isOWLNothing()) {
                        return new Pair<OWLClassExpression, OWLClassExpression>(firstExpression, firstExpression);
                    }
                }
                
                for(OWLAxiom secondAxiom : nodeLabels) {
                	if(firstAxiom.equals(secondAxiom))
                		continue;
                	
                	if (secondAxiom instanceof OWLClassAssertionAxiom){
                        secondExpression = ((OWLClassAssertionAxiom) secondAxiom).getClassExpression();
                        if(secondExpression.equals(firstExpression.getObjectComplementOf())) {
                        	clash = new Pair<OWLClassExpression, OWLClassExpression>(firstExpression, secondExpression);
                        	return clash;
                        }
                        
                	}
    				
    			}
			}
			
		}
		return null;
	}
	
	private boolean nodeContainsAtLeastOnDisjunct(Node node, List<OWLClassExpression> operandList) {
		/*
		 * Input: La lista di operandi di un assioma OR
		 * 
		 * Ritorna true o false a seconda se l'OR è applicabile oppure no, secondo la regola
		 * (ossia, l'OR è applicabile se il nodo corrente NON contiene già almeno uno dei disgiunti)
		 * */
		 OWLClassAssertionAxiom disjuntAxiom;
		 Set<OWLAxiom> nodeConcepts = node.getLabelToSatisfy();
		 
		 for (OWLClassExpression disjunct : operandList) {
			 disjuntAxiom = df.getOWLClassAssertionAxiom(disjunct, individual);
			 
			 if(nodeConcepts.contains(disjuntAxiom))
				 return true;
		 }
		
		return false;
	}
	
	private boolean containsAllConjunct(Node node, OWLClassExpression classExpression) {
		/*
		 * Input: La classExpression corrispondente dell'AND
		 * 
		 * Ritorna true o false a seconda se l'AND è applicabile oppure no, secondo la regola
		 * (ossia, l'AND è applicabile se il nodo corrente NON contiene già tutti i congiunti dell'AND)
		 * */
		 Set<OWLAxiom> nodeConcepts = node.getLabelToSatisfy();
		 List<Boolean> ret = new LinkedList<>();
		 classExpression.accept(new OWLClassExpressionVisitor() {
	            @Override
	            public void visit(OWLObjectIntersectionOf oi) {
	            	boolean containsAllConjuncts = true;
	                for (OWLClassExpression ce : oi.asConjunctSet()) {
	                    OWLClassAssertionAxiom classAssertion = df.getOWLClassAssertionAxiom(ce, individual);
	                    
	                    if (nodeConcepts.contains(classAssertion)) 
	                    	containsAllConjuncts = (containsAllConjuncts && true);
	                    else
	                    	containsAllConjuncts = false;
	                }
	                ret.add(containsAllConjuncts);
	            }
	        });
		 if (ret.size()==1)
			 return ret.get(0);
		 else
			 return true;
	}

	
	public GraphWriter getGraph() {
		return this.graphWriter;
	}
	
	public RDFWriter getRDF() {
		return this.rdfWriter;
	}
}
