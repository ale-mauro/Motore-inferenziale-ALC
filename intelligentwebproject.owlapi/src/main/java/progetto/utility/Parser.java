package progetto.utility;

import java.util.*;
import java.util.stream.Stream;

import javax.swing.JOptionPane;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.expression.ShortFormEntityChecker;
import org.semanticweb.owlapi.manchestersyntax.renderer.ManchesterOWLSyntaxPrefixNameShortFormProvider;
import org.semanticweb.owlapi.manchestersyntax.renderer.ParserException;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.BidirectionalShortFormProvider;
import org.semanticweb.owlapi.util.BidirectionalShortFormProviderAdapter;
import org.semanticweb.owlapi.util.ShortFormProvider;
import org.semanticweb.owlapi.util.mansyntax.ManchesterOWLSyntaxParser;

public class Parser {
	/*
	 * Effettua il parsin di una stringa in un concetto OWL
	 * Effettua il parsing di una tbox in un singolo concetto OWL
	 * */

	
    public static OWLClassExpression strToConcept(Ontology ontology, String expr){
    	/*
    	 *
	     * Effettua il parsing di una stringa in Manchester Syntax presa in input,
	     * convertendola in un concetto complesso (OWLClassExpression).
         * Converte l'espressione in un concetto secondo il tipo di dato OWLClassExpression.
         */
    	
    	OWLOntology o = ontology.getOntology();
    	OWLOntologyManager manager = ontology.getManager();
    	
    	OWLClassExpression concept = null;
        ManchesterOWLSyntaxParser parser = OWLManager.createManchesterParser();
        
        parser.setDefaultOntology(o);

        // checker necessario per mappare le stringhe entità con le entità effettive
        Set<OWLOntology> ontologies = Collections.singleton(o);
        ShortFormProvider sfp = new ManchesterOWLSyntaxPrefixNameShortFormProvider(manager.getOntologyFormat(o));
        BidirectionalShortFormProvider shortFormProvider = new BidirectionalShortFormProviderAdapter(ontologies, sfp);
        ShortFormEntityChecker checker = new ShortFormEntityChecker(shortFormProvider);
        parser.setOWLEntityChecker(checker);
        
        try {
        	concept = parser.parseClassExpression(expr);
        	concept = concept.getNNF();
        }catch(ParserException pe) {
        	JOptionPane.showMessageDialog(null, "Errore nel parsing \n"+ pe.getMessage(), "ERROR", JOptionPane.ERROR_MESSAGE, null);
        }
        
        return concept;
    }
    
    public static OWLClassExpression tboxToConcept(List<OWLAxiom> tbox) {
    	/*
    	 * Nel caso in cui si effettua un tableaux con una TBox non vuota, bisogna trasformare la TBox in un concetto affinché tutti gli individui soddisfino
    	 * sia il concetto C, sia il concetto C' (ovvero la TBox).
    	 * Tale TBox C' sarà formata da una congiunzione di tutti i concetti della tbox.
    	 * */
    	//-------------------------------------------------------------VARIABILI
        OWLSubClassOfAxiom subClassAx;
        OWLEquivalentClassesAxiom equivClassAx;
        OWLClassExpression subClass, superClass, concept = null, operand = null;
        OWLClassExpression subClassConj = null, equivConj = null, domRangeConj = null;
        OWLDataFactory df = OWLManager.getOWLDataFactory();
        //---------------------------------------------------------------CORPUS
        /*
         * Bisogna vedere ogni assioma della TBox di che tipo è. I tipi sono:
         * SubClassOf (Concept Inclusion) (Woman SubClassOf Parent) (trasforma in not Woman OR Parent)
         * EquivalentClasses (Mother = Woman AND Parent)
         * Dominio di un ruolo (hasWife domain Man)
         * Codominio di un ruolo (hasWife Range Woman)
         * */
        for(OWLAxiom ax: tbox) {
            // caso SubClassOf
            if(ax instanceof OWLSubClassOfAxiom) {
                subClassAx = (OWLSubClassOfAxiom) ax;

                subClass = subClassAx.getSubClass();
                superClass = subClassAx.getSuperClass();
                
                subClass = subClass.getComplementNNF();
                operand = df.getOWLObjectUnionOf(Stream.of(subClass, superClass));

                if(subClassConj != null) {
                    subClassConj = df.getOWLObjectIntersectionOf(Stream.of(subClassConj, operand));
                } else {
                    subClassConj = operand;
                }
            }

            // caso EquivalentClasses
            if(ax instanceof OWLEquivalentClassesAxiom) {
                equivClassAx = (OWLEquivalentClassesAxiom) ax;

                for(OWLSubClassOfAxiom sca: equivClassAx.asOWLSubClassOfAxioms()) {
                    
                    subClass = sca.getSubClass();
                    superClass = sca.getSuperClass();

                    subClass = subClass.getComplementNNF();
                    operand = df.getOWLObjectUnionOf(Stream.of(subClass, superClass));

                    if(equivConj != null) {
                        equivConj = df.getOWLObjectIntersectionOf(Stream.of(equivConj, operand));
                    } else {
                        equivConj = operand;
                    }
                }
            }

            /* 
             * gestione dominio di un ruolo (Exists R.T subclassOf A) DIVENTA (ForAll R.Bottom or A) 
             * Si estrae l'assioma dominio e dunque l'assioma di sottoclasse
             * si ottengono sublcass e superclass (sublcass: Exists R.T) (superclass: A)
             * E si fa l'unione del complemento della sottoclasse (ForAll) con la superclasse (A)
             */
            if(ax instanceof OWLObjectPropertyDomainAxiom) {
                OWLObjectPropertyDomainAxiom domainAx = (OWLObjectPropertyDomainAxiom) ax;
                subClassAx = domainAx.asOWLSubClassOfAxiom();
                subClass = subClassAx.getSubClass();
                superClass = subClassAx.getSuperClass();
                subClass = subClass.getComplementNNF();
                operand = df.getOWLObjectUnionOf(Stream.of(subClass, superClass));

                if(domRangeConj != null) {
                    domRangeConj = df.getOWLObjectIntersectionOf(Stream.of(domRangeConj, operand));
                } else {
                    domRangeConj = operand;
                }
            }

            /* 
             * gestione codominio di un ruolo (T subclassOf Forall R.B) DIVENTA (ForAll R.Bottom OR ForAll R.B)
             * Il codominio vuol dire che o non ha archi di tipo R, oppure se ce li ha tutti devono finire in B)
             * 
             * si prende l'assioma codominio (range) (Range(eats, Food)) (Il codominio di eats è food)
             * si recupera la property (eats)
             * Si recupera la subclass (T), PER LA TAUTOLOGIA (?) (T==Exists R.T) DIVENTA (Exists R.T) e Si ottiene il complemento (Forall R.Bottom)
             * 
             * Si recupera la superclass (ForAll R.B)
             * Si fa l'or ottenendo (ForAll R.Bottom OR ForAll R.B) (che intuitivamente vuol dire, o non ha archi di tipo R, oppure se ce li ha tutti devono finire in B)
             * 
             */
            if(ax instanceof OWLObjectPropertyRangeAxiom) {
                OWLObjectPropertyRangeAxiom rangeAx = (OWLObjectPropertyRangeAxiom) ax;
                OWLObjectPropertyExpression prop = rangeAx.getProperty();
                subClass = df.getOWLObjectSomeValuesFrom(prop, df.getOWLThing());
                //subClass = df.getOWLObjectAllValuesFrom(prop, df.getOWLThing());
                
                subClass = subClass.getComplementNNF();
                superClass = df.getOWLObjectAllValuesFrom(prop, rangeAx.getRange());
                operand = df.getOWLObjectUnionOf(Stream.of(subClass, superClass));
                if(domRangeConj != null) {
                    domRangeConj = df.getOWLObjectIntersectionOf(Stream.of(domRangeConj, operand));
                } else {
                    domRangeConj = operand;
                }
            }
        }
        
        List<OWLClassExpression> operands = new LinkedList<>();

        if(subClassConj != null) operands.add(subClassConj);
        if(equivConj != null) operands.add(equivConj);
        if(domRangeConj != null) operands.add(domRangeConj);

        if(operands.size() > 1) {
            concept = df.getOWLObjectIntersectionOf(operands.stream());
        } else {
            concept = operands.get(0);
        }
        
        return concept;
    }

}
