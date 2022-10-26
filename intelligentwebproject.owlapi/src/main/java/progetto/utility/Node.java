package progetto.utility;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLClassExpressionVisitor;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLObjectAllValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;

import progetto.GraphWriter;


public class Node {
	private OWLIndividual x; //rappresenta l'individuo inteso in OWL
	
    private Set<OWLAxiom> label; //Concept to Satisfy
    
    private Node parent = null; //nodo che "genera" this    
    
    private boolean isBlocked = false; //dice se un nodo è bloccato dal blocking
    private Node blockingNode = null; //nodo che eventualmente blocca this
    
    private boolean isAppliedLU; //dice se è stato applicato il lazy unfolding su questo nodo
    private Set<OWLAxiom> axiomAlreadyAppliedLU; //Assiomi che sono stati applicati il lazy unfolding (e quindi sono state aggiunte le definizioni degli assiomi aggiunti)
    
    private Integer id = 0; //rappresenta l'ID del nodo (l'ID del nodo non viene assegnato alla sua creazione, ma quando si fa il getId).
    private static Integer staticCounterID = 0; //è un contatore statico per assegnare gli ID ai nodi
    
    
    
//    ------------------------------------------------------------------------Constructor
    public Node(OWLIndividual x){
        this.label = new TreeSet <OWLAxiom>();
        this.x = x;
        this.axiomAlreadyAppliedLU = new TreeSet <OWLAxiom>();
    }
    
//    -------------------------------------------------------------------------SETTER
    public void setAppliedLU(boolean value){
        isAppliedLU = value;
    }

    public void setLabelToSatisfy(Set<OWLAxiom> structure){
        this.label = structure;
    }
    
    public void setBlocked(boolean blocked) {
        this.isBlocked = blocked;
    }

    public void setParent(Node parent) {
        this.parent = parent;
    }
    
    public void setId(Integer id) {
    	this.id = id;
    }
    
    public void setAxiomAlreadyAppliedLU(Set<OWLAxiom> axiomAppliedLU) {
		this.axiomAlreadyAppliedLU = axiomAppliedLU;
	}
    
    public void addAxiomAlreadyAppliedLU(OWLAxiom axiomAppliedLU) {
		this.axiomAlreadyAppliedLU.add(axiomAppliedLU);
	}

//  -------------------------------------------------------------------------GETTER
    public boolean isAppliedLU(){
        return isAppliedLU;
    }
    
    public Set<OWLAxiom> getLabelToSatisfy(){
        return label;
    }
    
    public boolean isBlocked() {
        return this.isBlocked;
    }

    public Node getParent() {
        return parent;
    }
    
    public OWLIndividual getIndividual(){
        return x;
    }

    public Integer getId() {
        if(id == 0) {
            staticCounterID++;
            this.setId(staticCounterID);
        }
        return id;
    }
    
    public String getName() {
    	return "X_" + getId();
    }
    
	public Node getBlockingNode() {
		return this.blockingNode;
	}
	
	public Set<OWLAxiom> getAxiomAlreadyAppliedLU() {
		return this.axiomAlreadyAppliedLU;
	}


    
//-------------------------------------------------------------------------------------------------AGGIUNGERE CONCETTI AI NODI
    
    public boolean addExpressionToLabel(OWLClassExpression C, OWLDataFactory df, OWLIndividual individual) {
    	Set<OWLAxiom> nodeConcepts = this.getLabelToSatisfy();
    	boolean isAdded = false;
    	
    	OWLAxiom a = df.getOWLClassAssertionAxiom(C, individual);    	
    	if(!nodeConcepts.contains(a)) {
    		isAdded = nodeConcepts.add(a);
    	}
    	
    	return isAdded;
	}
    
    
	public boolean addANDConjunctsToLabel(OWLClassExpression classExpression, OWLDataFactory df, OWLIndividual individual) {
		/*
		 * Visita tutti i congiunti dell'Intersezione (AND).
		 * Se il nodo corrente non contiene gli assiomi dei congiunti, gli li aggiunge tutti.
		 * */
		
		Set<OWLAxiom> nodeConcepts = this.getLabelToSatisfy();
		int oldSize = nodeConcepts.size();
		
		classExpression.accept(new OWLClassExpressionVisitor() {
            @Override
            public void visit(OWLObjectIntersectionOf oi) {
                for (OWLClassExpression ce : oi.asConjunctSet()) {
                    OWLClassAssertionAxiom classAssertion = df.getOWLClassAssertionAxiom(ce, individual);
                    
                    if (!nodeConcepts.contains(classAssertion)) 
                    	nodeConcepts.add(classAssertion);
                }
            }
        });
		
		int newSize = nodeConcepts.size();
		return newSize > oldSize;
	}
	
	public OWLObjectPropertyExpression addExistPropertyToLabel(OWLClassExpression classExpression,OWLClassExpression translatedTbox, OWLDataFactory df, OWLIndividual individual) {
		/*
		 * Visit il FILLER dell'EXISTS e aggiunge il filler alle label del nodo
		 * Aggiunge anche la property in quanto serve per il FORALL
		 * Se l'exists è EXISTS eats.Food, 
    	 * -la property è eats 
    	 * -il filler è Food
		 * */
		
		Set<OWLAxiom> nodeConcepts = this.getLabelToSatisfy();
		List<OWLObjectPropertyExpression> ret = new LinkedList<>();
		
		classExpression.accept(new OWLClassExpressionVisitor() {
            @Override
            public void visit(OWLObjectSomeValuesFrom svf) {
            	/*
            	 * Se l'exists è EXISTS eats.Food, 
            	 * -la property è eats 
            	 * -il filler è Food
            	 * */
            	OWLObjectPropertyExpression property = svf.getProperty();
            	OWLClassExpression filler = svf.getFiller(); 
                ret.add(property);
            	//Ottengo gli assiomi corrispondenti
            	OWLObjectPropertyAssertionAxiom propertyAxiom = df.getOWLObjectPropertyAssertionAxiom(property, individual, individual);
                OWLClassAssertionAxiom fillerAxiom = df.getOWLClassAssertionAxiom(filler, individual);
            	
                nodeConcepts.add(fillerAxiom);
                nodeConcepts.add(propertyAxiom);
                
                if(translatedTbox != null) {
                    // la regola dell'esistenziale è stata applicata e la tbox (Tg in caso di lazy unfolding) non è vuota: la si aggiunge nel nuovo individuo
                    OWLClassAssertionAxiom tboxAss = df.getOWLClassAssertionAxiom(translatedTbox,individual);
                    nodeConcepts.add(tboxAss);
                }
            }
        });
		if(ret.size() == 1) 
			return ret.get(0);
		else
			return null;
	}
	
	public boolean addUniversalPropertyToLabel(OWLAxiom axiom, OWLClassExpression classExpression, OWLDataFactory df, OWLIndividual individual, GraphWriter graphWriter) {
		/*
		 * controlla la property della classExpression (Che è di tipo All Values From)
		 * Se il nodo contiene quella property, aggiunge la class Assertion (e rimuove l'assioma)
		 * 
		 * Se il forall è FORALL_eats.Food, 
    	 * -la property è eats 
    	 * -il filler è Food
		 * 
		 * */
		
		Set<OWLAxiom> nodeConcepts = this.getLabelToSatisfy();
		List<Boolean> ret = new LinkedList<>();
		classExpression.accept(new OWLClassExpressionVisitor() {
            @Override
            public void visit(OWLObjectAllValuesFrom avf) {
            	 OWLClassExpression filler = avf.getFiller();
                 OWLObjectPropertyExpression property = avf.getProperty();
                 
                 //Ottengo gli assiomi corrispondenti
                 OWLObjectPropertyAssertionAxiom propertyAxiom = df.getOWLObjectPropertyAssertionAxiom(property, individual, individual);
                 OWLClassAssertionAxiom fillerAxiom = df.getOWLClassAssertionAxiom(filler, individual);
                 
                 //se il nodo contiene la property, allor aaggiunge i filler
                 if (nodeConcepts.contains(propertyAxiom)) {
                	 nodeConcepts.remove(axiom);
                	 if(nodeConcepts.add(fillerAxiom))
                		 ret.add(true);
                 }
            }
		});
		if(!ret.isEmpty())
			return true;
		else 
			return false;
	}
	
	//------------------------------------------------------------------------------------------BLOCKING
	
	public void setIfBlocked() {
		/*
		 * La regola del blocking è la seguente:
		 * (Label(x_j) included label(x_i) e i < j, allora il nodo j è bloccato
		 * 
		 * Quindi, si devono controllare tutti i parent dei nodi.
		 * È necessario che un antenato NON contenga almeno un assioma del nodo corrente, che ritorna false 
		 * (l'antenato deve contenere TUTTI gli assiomi del nodo corrente per ritornare true)
		 * */
		Node currentNode = this;
		Node ancestorNode = this.getParent();
        Set<OWLAxiom> ancestorLabel; 
        Set<OWLAxiom> nodeLabel;
        
        boolean blocked = false;
        
        while(ancestorNode != null && !blocked) {
        	boolean containsAllAncestorAxiom = true;
        	nodeLabel = currentNode.getLabelToSatisfy();
        	ancestorLabel = ancestorNode.getLabelToSatisfy();
        	
        	for (OWLAxiom nodeAxiom : nodeLabel) {
        		
        		if(ancestorLabel.contains(nodeAxiom)) 
        			containsAllAncestorAxiom = containsAllAncestorAxiom && true;
        		else 
        			containsAllAncestorAxiom = false;
        	}
        	
        	if (containsAllAncestorAxiom) {
        		blocked = true;
        		this.blockingNode=ancestorNode;
        	}
        	
        	ancestorNode = ancestorNode.getParent();
       }
        
       this.isBlocked = blocked;
	}

}