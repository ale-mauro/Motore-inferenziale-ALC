package progetto.utility;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLClassExpressionVisitor;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLObjectAllValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectComplementOf;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectUnionOf;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;

import javafx.util.Pair;

public class LazyUnfolding {
	private OWLDataFactory df = OWLManager.getOWLDataFactory();
	private List<OWLAxiom> Tu = new LinkedList<>();
    private List<OWLAxiom> Tg = new LinkedList<>();
	
    /*
     * La classe Si occupa di :
     * 1) Partizionare la TBox in Tu e Tg; 2) Applicare la regola di lazy unfolding su un nodo
     * 
     * Per fare 1) (Partizionare la TBox in Tu e Tg)
     * fa ausilio di alcuni metodi che controllano:
     * -Se Tu rimane unfoldable aggiungendo concetti di SubClass oppure EquivalentClass
     * -Se la Tu rimane Aciclica (nessun concetto sia definito direttamente o indirettamente in termini di se stesso)
     * -Se A=D è in Tu, allora controlla che il concetto A non compare a sinistra di nessun'altra CGI di Tu
     * -Trasformare i concetti di sottoclasse in modo tale che nel LHS sia presente solo un concetto atomico. 
     */
    
	
	public Pair<List<OWLAxiom>, List<OWLAxiom>> getLazyUnfoldingPartition(List<OWLAxiom> tbox) {
        /* 
         * Tale metodo partiziona la tbox andando a creare due liste di assiomi:
         *  - Tu contenente solo assiomi unfoldable (A subClassOf D;     A Equivalent D) tale che se A Equivalent D è in Tu, allora A non compare a sinistra di nessun'altra GCI di Tu e il grafo delle dipendenze è aciclico
         *  - Tg contenente la restante parte di assiomi
         * 
         * Il tipo di ritorno pair contiene nell'ordine Tu e Tg.
         * 
         * La tbox presenta solo 4 tipologie di assiomi: 
         * 1) OWLEquivalentClassesAxiom,
         * 2) OWLSubClassOfAxiom,
         * 3) OWLObjectPropertyDomainAxiom,
         * 4) OWLObjectPropertyRangeAxiom.
         * 
         * Vengono presi in considerazione soltanto i primi due
         * 
         * 
         * - Si controlla il tipo di Assioma (Subclass oppure Equivalent)
         * 
         * 
         * Per creare Tu (CASO EQUIVALENT CLASS)
         * - Si controlla se il grafo delle dipendenze è aciclico
         * - Si controlla se A non compare a sinistra di nessun'altra GCI
         * 
         * 
         * Per creare Tu (CASO SUBCLASSOF):
         *  - Si trasforma l'assioma in modo da avere solo A a sinistra (ovviamente solo se assioma composto (A AND C SublcassOf D --- A SublcassOf notC OR D)
         *  - Si controlla se il grafo delle dipendenze è aciclico
         *  - Si controlla se A non compare a sinistra di nessun'altra GCI
         * 
         */

        OWLEquivalentClassesAxiom equivalentAxiom;
        OWLSubClassOfAxiom subClassAxiom, modifiedSubClassAx;
        
        for(OWLAxiom axiom : tbox){
        	
        	//Caso equivalentClassesAxiom
            if (axiom instanceof OWLEquivalentClassesAxiom){
                equivalentAxiom = (OWLEquivalentClassesAxiom) axiom;
                
                if(isUnfoldableAddingEquivalentClass(Tu, equivalentAxiom)){
                    Tu.add(equivalentAxiom);
                }
                else {
                    Tg.add(equivalentAxiom);
                } 
            }
            
            //Caso OWLSubClassAxiom
            else if (axiom instanceof OWLSubClassOfAxiom){
                subClassAxiom = (OWLSubClassOfAxiom) axiom;
                if(isUnfoldableAddingSubClass(Tu, subClassAxiom)){
                    OWLClassExpression A = subClassAxiom.getSubClass();
                    
                    //Se nell'assioma di subclass (A SubClass D), A è di tipo A AND C (A and C sublcass D), allora si trasforma in (A subclass notC or D)
                    if(A instanceof OWLObjectIntersectionOf){
                        modifiedSubClassAx = subClassToSingleLHSForm(subClassAxiom);
                        Tu.add(modifiedSubClassAx);
                    }
                    //altrimenti (se A è atomico e NON è un AND), si rimane com'è è si aggiunge A subClass D a Tu.
                    else {
                        Tu.add(subClassAxiom);
                    }
                }
                //altrimenti se la Tu non rimane unfoldable aggiungendo l'assioma di sottoclasse, l'assioma va in Tg
                else {
                    Tg.add(subClassAxiom);
                } 
            }
            
            //Caso nè EquivalentClass, nè SubClass
            else {
            	Tg.add(axiom);
            }
        }

        return new Pair<List<OWLAxiom>, List<OWLAxiom>>(Tu, Tg);
    }
	
	private boolean isUnfoldableAddingEquivalentClass(List<OWLAxiom> Tu, OWLEquivalentClassesAxiom equivClassAx) {

        /* 
         * Input: Assioma del tipo A Equivalent D
         * Si controlla che il grafo delle dipendenze è aciclico 
         * Si controlla che A non compare a sinistra di nessun'altra GCI
         * 
         * PS: CI SI ASPETTA CHE il LHS di un EquivalentClassesAxiom sia un concetto atomico (OWLClass) (A Equivalent D, A DEVE ESSERE CONCETTO ATOMICO)
         * In caso contrario, l'assioma non viene considerato e si ritorna false.
         */        
        
		List<OWLClassExpression> equivParts = equivClassAx.classExpressions().collect(Collectors.toList());
        OWLClassExpression A = equivParts.get(0); //Recupera equivalenza a sinistra
        OWLClassExpression D = equivParts.get(1); //recupera equivalenza a destra
        
        
        //se A NON è un concetto atomico (OWLClass) si ritorna falso
        if(! (A instanceof OWLClass))
        	return false;
        
        
        UtilityBoolean isAcyclical = new UtilityBoolean(true);
        
	    isAcyclicalConcept(Tu, (OWLClass) A, D, isAcyclical); //modifica il valore della variabile isAcyclical
	
	    if(isAcyclical.getValue()){ 
	    	//se il grafo delle dipendenze è aciclico, si controlla che A non compare a sinistra di nient'altro
	    	return checkConceptInOtherLeftSideGCI(Tu, (OWLClass) A);
	    }
	    else {
	    	//se il grafo delle dipendenze NON è aciclico, si ritorna false.
	    	return false;
	    }  
    
    }
	
	
	private boolean isUnfoldableAddingSubClass(List<OWLAxiom> Tu, OWLSubClassOfAxiom subClassAx) {
        
        /* 
         * Controlla che Tu rimane unfoldable se si aggiunge un assioma di SubClass (A subclass D)
         * 
         * Se A è un concetto atomico (A Subclass D), allora si controlla solamente se il grafo delle dipendenze è aciclico e che A non compare a sinistra di nient'altro
         * 
         * Se A è un concetto composto (A AND C Sublcass D) si trasforma l'inclusione in modo da avere a sinistra solo A (A Subclass notC OR D)
         * 
         * Verifica che A o sia un concetto atomico oppure del tipo A ∩ C (con C che può essere 
         * un concetto complesso) trasformando l'inclusione in modo da avere il LHS formato da un 
         * solo concetto atomico A e spostando a destra i restanti congiunti C
         */
		
        OWLClassExpression A = null;
        UtilityBoolean isAcyclical = new UtilityBoolean(false);
        A = subClassAx.getSubClass();
        OWLClassExpression D = subClassAx.getSuperClass();

        // Se A è un concetto atomico, si controlla solo se il grafo delle dipendenze è aciclico e se A non compare a sinistra di nessun'altra GCI
        if(A instanceof OWLClass){
            isAcyclicalConcept(Tu, (OWLClass) A, D, isAcyclical); //modifica il valore di ret
            
            if(!isAcyclical.getValue()) {
                return false;
            }
            return checkConceptInOtherLeftSideGCI(Tu, (OWLClass) A);    

        }
        
        //--------------------------------------------CASO CHECK UNFOLDABLE ADDING SUBCLASS CON 'A' NON CONCETTO ATOMICO MA AND
        /*
         * Se A NON è un concetto atomico, ma è un concetto composto (del tipo A AND C SubclassOf D) [A = A AND C]
         * allora per ogni congiunto dell'AND, si controlla se non è definito direttamente o indirettamente in termini di se stesso.
         * e se ogni congiunto dell'AND non compaia a sinistra di altre GCI 
         * 
         * (SI FANNO I CONTROLLI DI UNFOLDABLE SU OGNI CONGIUNTO)
         */
        else if(A instanceof OWLObjectIntersectionOf){
            A.accept(new OWLClassExpressionVisitor() {
                @Override
                public void visit(OWLObjectIntersectionOf oi) {
                    Set<OWLClassExpression> conjunctSet =  oi.asConjunctSet(); 
                    UtilityBoolean isAcyclic = new UtilityBoolean(false);
                    
                    for (OWLClassExpression andOperand : conjunctSet){
                        
                    	if (andOperand instanceof OWLClass){
                            isAcyclicalConcept(Tu, (OWLClass) andOperand, D, isAcyclic);
                            
                            if(checkConceptInOtherLeftSideGCI(Tu, (OWLClass) andOperand) && isAcyclical.getValue()){
                                isAcyclical.setValue(true);
                            }
                        }
                    }
                }  
            });
            if(isAcyclical.getValue()){
                return true;
            }
        }
        return false;
    }

	
	private void isAcyclicalConcept(List<OWLAxiom> Tu, OWLClass A, OWLClassExpression D, UtilityBoolean isAcyclical) {

        /* 
         * Verifica che nessun concetto sia definito direttamente o indirettamente in termini di se stesso.
         * 
         * PS. non tiene conto dei fake cicle, cioè casi in cui il concetto è sintatticamente ciclico, 
         * ma non semanticamente.
         * 
         * INPUT: Superclasse e sottoclasse di un assioma subclass of oppure equivalenze di un assioma equivalentclass (A subclass D, A equivalent D;  input: A e D)
         * OUTPUT: modifica del valore del parametro dell'UtilityBoolean con true o false a seconda se A è definito direttamente o indirettamente in termini di se stesso 
         * (so che A equiv D, devo vedere se D non contiene A nella sua definizione) (stesso discorso per A subclass D)
         * 
         * D può essere:
         * - Atomico
         * - Equivalent Class
         * - AND
         * - OR
         * - Exists
         * - Forall
         * 
         */

		//SE D concetto atomico
        if(D instanceof OWLClass){
        	
        	//si controlla se D è uguale ad A
            if (D.equals(A)){
                isAcyclical.setValue(false);
            }
            
            //si controlla ogni assioma di equivalenza se l'operando a sinistra è uguale a D. nel caso si fa il controllo tra ciò che c'è a destra ed A
            else {
                for (OWLAxiom axiom: Tu){
                	//si controlla per ogni assioma di equivalenza
                    if(axiom instanceof OWLEquivalentClassesAxiom){
                        OWLEquivalentClassesAxiom equivalentAxiom = (OWLEquivalentClassesAxiom) axiom; 
                        List<OWLClassExpression> equivalentComponents = equivalentAxiom.classExpressions().collect(Collectors.toList());

                        OWLClassExpression leftEquivalentOperand = equivalentComponents.get(0);

                        if(D.equals(leftEquivalentOperand)){
                            OWLClassExpression rightEquivalentOperand = equivalentComponents.get(1);
                            isAcyclicalConcept(Tu, A, rightEquivalentOperand, isAcyclical);
                        }
                    }
                }
            }
        }
        
        //Se D è un AND, si controlla per ogni operando dell'and se A è definito come questo operando
        if(D instanceof OWLObjectIntersectionOf){
            D.accept(new OWLClassExpressionVisitor() {
                @Override
                public void visit(OWLObjectIntersectionOf oi) {
                    for (OWLClassExpression andOperand : oi.getOperandsAsList()) {
                        isAcyclicalConcept(Tu, A, andOperand, isAcyclical);
                        if(!isAcyclical.getValue()){
                            break;
                        }
                    }
                }
            });
        }
        
        //Se D è un OR, si controlla per ogni operando dell'OR se A è definito come ALMENO UNO degli OR
        if(D instanceof OWLObjectUnionOf){
            D.accept(new OWLClassExpressionVisitor() {
                @Override
                public void visit(OWLObjectUnionOf ou) {
                    for (OWLClassExpression orOperand : ou.getOperandsAsList()) {
                        isAcyclicalConcept(Tu, A, orOperand, isAcyclical);
                        if(!isAcyclical.getValue()){
                            break;
                        }
                    }
                }
            });
        }
        
        //Se D è un exists, si controlla per il filler ( Se l'exists è EXISTS eats.Food, -la property è eats  -il filler è Food)
        if(D instanceof OWLObjectSomeValuesFrom){
            D.accept(new OWLClassExpressionVisitor() {
                @Override
                public void visit(OWLObjectSomeValuesFrom svf) {
                    isAcyclicalConcept(Tu, A, svf.getFiller(), isAcyclical);
                }
            });
        }
        
        //Se D è un forall, si controlla per il filler
        if(D instanceof OWLObjectAllValuesFrom){
            D.accept(new OWLClassExpressionVisitor() {
                @Override
                public void visit(OWLObjectAllValuesFrom avf) {
                    isAcyclicalConcept(Tu, A, avf.getFiller(), isAcyclical);
                }
            });
        }
    }
	
    private OWLSubClassOfAxiom subClassToSingleLHSForm(OWLSubClassOfAxiom subClassAx) {

        /*  
         * INPUT: assioma di sottoclasse (A subclass D) in cui 'A' è del tipo A AND C
         *  Il metodo trasforma l'assioma di inclusione in modo tale che nel LHS sia presente solo un concetto atomico. 
         */
        
        OWLClassExpression A = subClassAx.getSubClass();
        OWLClassExpression D = subClassAx.getSuperClass();

        List<OWLSubClassOfAxiom> singleLHSForm = new LinkedList<>();
        
        //A è del tipo A AND C, per ogni congiunto di A si controlla (nuovamente) se è aciclico e se non è presente a sinistra in altre GCI
        //il nuovo A sarà uno degli operandi dell'and (A oppure C) e all'A originale si toglierà questo operando, si farà il complemento e sarà aggiunto in OR nella superclasse
        //(A AND C SublcassOF D diventerà A SubclassOf notC or D)
        A.accept(new OWLClassExpressionVisitor() {
            @Override
            public void visit(OWLObjectIntersectionOf oi) {
                Set<OWLClassExpression> conjunctSet =  oi.asConjunctSet();
                OWLClassExpression newA = null;;
                UtilityBoolean isAcyclic = new UtilityBoolean(false);

                //QUESTO CONTROLLO VIENE FATTO ANCHE IN 'isUnfoldableAddingSubClass' MA QUI SERVE PER PRENDERE IL CONGIUNTO GIUSTO
                //CHE NON SIA A SINSITRA DI UN ALTRA GCI
                for (OWLClassExpression andOperand : conjunctSet){
                    if (andOperand instanceof OWLClass){
                        isAcyclicalConcept(Tu, (OWLClass) andOperand, D, isAcyclic);
                        
                        if(checkConceptInOtherLeftSideGCI(Tu, (OWLClass) andOperand) && isAcyclic.getValue()) {
                            newA = andOperand; 
                            break;
                        }
                    }
                }
                
                //lo sarà sempre perché il controllo viene fatto in 'isUnfoldableAddingSubClass'
                if(newA != null){
                	OWLClassExpression superClass, operand = null;
                	OWLSubClassOfAxiom newSubClassAxiom = null;
                	
                    conjunctSet.remove(newA);
                    operand = df.getOWLObjectIntersectionOf(conjunctSet);  
                    operand = operand.getComplementNNF();
                    
                    superClass = df.getOWLObjectUnionOf(Stream.of(operand, D));
                    newSubClassAxiom = df.getOWLSubClassOfAxiom(newA, superClass);
                    
                    singleLHSForm.add(newSubClassAxiom);
                }
            }
        });
        
        if (!singleLHSForm.isEmpty())
        	return singleLHSForm.get(0);
        else 
        	return null;
    }

    private boolean checkConceptInOtherLeftSideGCI(List<OWLAxiom> Tu, OWLClass A) {

        /*
         * INPUT:
         * Concetto A proveniente da un assioma di equivalenza o sublcass (A subclass D; A equivalent D;    l'input è A)
         * 
         * Il metodo controlla che il concetto atomico A non compaia a sinistra di nessuna altra GCI in Tu. 
         * 
         * 2 casi di GCI in Tu
         * 1) SubClass
         * 2) EquivalentClass
         * 
         * Per ogni subclass (P subclass T), si controlla che A non sia uguale a P
         * Per ogni assioma di equivalenza (P equivalent T) si controlla che A non sia uguale a P
         */
        
        OWLClassExpression P;
        
        for(OWLAxiom axiom : Tu){
        	
            // caso assiomi di sottoclasse (P subclass T; si controlla che A non è uguale a P)
            if (axiom instanceof OWLSubClassOfAxiom){
            	OWLSubClassOfAxiom subClassAxiom = (OWLSubClassOfAxiom) axiom;
            	
                P = subClassAxiom.getSubClass();
                
                if(A.equals(P)){  
                    return false;
                }
            }
            
            // caso assiomi di equicalenza (P equivalent T; si controlla che A non è uguale a P)
            if (axiom instanceof OWLEquivalentClassesAxiom){
            	OWLEquivalentClassesAxiom equivalentAxiom = (OWLEquivalentClassesAxiom) axiom;
                List<OWLClassExpression> equivalentOperands = equivalentAxiom.classExpressions().collect(Collectors.toList());
                
                P = equivalentOperands.get(0); 
                
                if (A.equals(P)){
                    return false;
                }
            }
            
        }
        return true;
    }
    
    
    
    public OWLClassAssertionAxiom applyLazyUnfoldingRule(OWLClassAssertionAxiom classAssertion, Node node, OWLIndividual individual) {

        /*
         * Applica le regole di Lazy Unfolding a partire da un'asserzione. 
         * 
         * Se l'asserzione è del tipo OWLClass, (si ottiene la class expression) si verificano le regole EquivalentClassAxiom (A == C) e  la regola SubClassAXiom (A \sub C)
         * 
         * Invece, se l'asserzione è di tipo ComplementOf A è di tipo notA, si ha (A==C) e si aggiunge not C
         */

		if(Tu == null || Tu.isEmpty()) {
			/*
			 * if(Tu != null && !Tu.isEmpty()) { 
			 * ... (tutto il codice)...
			 * }
			 * else return false;
			 * */
			return null;
		}
//		int oldSize = node.getLabelToSatisfy().size();
		
        Set<OWLAxiom> nodeLabels = node.getLabelToSatisfy();
        boolean isAppliedRule = false;

        OWLClassExpression inputClassExpression = classAssertion.getClassExpression();
        
        if(inputClassExpression instanceof OWLClass) {
        	OWLEquivalentClassesAxiom equivalentAxiom;
        	
            for(OWLAxiom axiom: Tu) {

            	//Caso equivalent Class (A==C, si aggiunge C ai concetti del nodo)
                if(axiom instanceof OWLEquivalentClassesAxiom) {
                    equivalentAxiom = (OWLEquivalentClassesAxiom) axiom; //QUESTO è l'ASSIOMA A==C, DEVO RECUPERARE I SINGOLI MEMBRI DELL'EQUIVALENZA A e C
                    
                    OWLClassExpression firstMember, secondMember;
                    List<OWLClassExpression> equivalentAxiomMembers = equivalentAxiom.classExpressions().collect(Collectors.toList());
                    firstMember = equivalentAxiomMembers.get(0);
                    secondMember = equivalentAxiomMembers.get(1);
                    
                    //Se il primo membro è proprio uguale all'input class expression
                    if(firstMember.equals(inputClassExpression)) {                      	
                        secondMember = secondMember.getNNF();
                        OWLClassAssertionAxiom newClassAssertion = df.getOWLClassAssertionAxiom(secondMember, individual);
                        isAppliedRule = nodeLabels.add(newClassAssertion);
                        
                        if(isAppliedRule) {
//                        	break;
                        	return newClassAssertion;
                        }
                        else
                        	continue;
                    }

                } 
                
                //Caso SubClassAxiom (A SubClass C, si aggiunge C ai concetti del nodo)
                else if(axiom instanceof OWLSubClassOfAxiom) {
                	OWLSubClassOfAxiom subClassAxiom;
                    subClassAxiom = (OWLSubClassOfAxiom) axiom; //QUESTO è L'ASSIOMA A SubClass C, DEVO RECUPERARE LA SOTTOCLASSE A e LA SUPERCLASSE C
                    
                    OWLClassExpression subClass, superClass;
                    subClass = subClassAxiom.getSubClass(); //
                    superClass = subClassAxiom.getSuperClass();

                    if(subClass.equals(inputClassExpression)) {
                    	superClass = superClass.getNNF();
                    	OWLClassAssertionAxiom newClassAssertion = df.getOWLClassAssertionAxiom(superClass, individual);
                    	isAppliedRule = nodeLabels.add(newClassAssertion);
                    	if(isAppliedRule)
//                        	break;
                    		return newClassAssertion;
                        else
                        	continue;
                    }
                }
            }
            

        /*
         * altrimenti, l'espressione in input non è di tipo OWLClass, ma è di tipo OWLObjectComplementOf
         * Sia notA appartenente alle label del nodo
         * Se A == C, allora si aggiunge notC alle label del nodo
         * notA, si ha (A==C) e si aggiunge not C
         */  
        } else if(inputClassExpression instanceof OWLObjectComplementOf) {
        	/*
        	 * L'espressione in input è di tipo notA
        	 * Seccome devo trovare A, trasformo l'espressione in input in A
        	 * */
        	OWLObjectComplementOf complement;
        	
            complement = (OWLObjectComplementOf) inputClassExpression;
            inputClassExpression = complement.getOperand();

            for(OWLAxiom axiom: Tu) {
                if(axiom instanceof OWLEquivalentClassesAxiom) {
                	OWLEquivalentClassesAxiom equivalentAxiom = (OWLEquivalentClassesAxiom) axiom; //QUESTO è l'ASSIOMA A==C, DEVO RECUPERARE I SINGOLI MEMBRI DELL'EQUIVALENZA A e C

                	OWLClassExpression firstMember, secondMember;
                    List<OWLClassExpression> equivalentAxiomMembers = equivalentAxiom.classExpressions().collect(Collectors.toList());
                    firstMember = equivalentAxiomMembers.get(0); //Recupera A
                    secondMember = equivalentAxiomMembers.get(1); //Recupera C

                    if(firstMember.equals(inputClassExpression)) {
                    	secondMember = secondMember.getComplementNNF();
                    	OWLClassAssertionAxiom newClassAssertion = df.getOWLClassAssertionAxiom(secondMember, individual);
                    	isAppliedRule = nodeLabels.add(newClassAssertion);
                    	if(isAppliedRule)
//                        	break;
                    		return newClassAssertion;
                        else
                        	continue;
                    }
                }
            }
            
        }
        return null;
    }

}
