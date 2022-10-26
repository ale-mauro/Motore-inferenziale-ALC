package progetto;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLClassExpressionVisitor;
import org.semanticweb.owlapi.model.OWLObjectAllValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectComplementOf;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectUnionOf;

import static guru.nidi.graphviz.model.Factory.mutGraph;
import static guru.nidi.graphviz.model.Factory.mutNode;
import static guru.nidi.graphviz.model.Factory.to;

import guru.nidi.graphviz.attribute.Color;
import guru.nidi.graphviz.attribute.Label;
import guru.nidi.graphviz.attribute.Shape;
import guru.nidi.graphviz.attribute.Style;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.engine.GraphvizException;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.model.MutableNode;
import javafx.util.Pair;
import progetto.utility.Node;
import progetto.utility.UtilityString;

public class GraphWriter {

	private HashMap<Node, MutableNode> graphNodes; //Mantiene una corrispondenza tra Node e Mutable Node nel grafo
	
    private MutableGraph graph; //contiene tutti i MUTABLE NODES del grafo	
	
//    
//    
//    
	public void initGraph() {    
		/*
		 * Inizializza il grafo
		 * */
        graph = mutGraph().setDirected(true);
        graphNodes = new HashMap<>();
    }
	
	public void addStartNode(Node node) {
		/*
		 * Aggiunge un nuovo nodo nel grafo 
		 * */
		MutableNode n = mutNode(node.getName()).add(Label.html("<b>start concept</b><br/>"), Color.rgb("1020d0").font());
		graph.add(n);
        
        graphNodes.put(node, n);
	}
	
	public void addArc(Node node, Node child, String rule) {        
    	/*
    	 * Crea un nuovo mutable node "child" e 
    	 * Aggiunge un arco tra il nodo "node" e il nodo "child" con la stringa "rule"
    	*/
    	
        MutableNode parentNode = graphNodes.get(node);
        MutableNode childNode = mutNode(child.getId().toString());
        
        parentNode.addLink(to(childNode).with(Label.of(" " + rule)));
        
        graphNodes.put(child, childNode);
    }
	
	
//	public void addLabelToNode(Node node, Set<OWLAxiom> nodeLabelNotUsed) {
//	
//	/*
//	 * QUESTO METODO MOSTRA SOLAMENTE GLI ASSIOMI DEL NODO CORRENTE CHE NON SONO NEL PADRE (CREA L'ASSIOMA DIFFERENZA)
//	 * Il parametro nodeLabel serve solo per rimanere invariata la chiamata da parte del tableaux
//	 * */
//	Node parent = node.getParent();
//		MutableNode n = graphNodes.get(node);
//        Set<OWLAxiom> nodeStructure = node.getLabelToSatisfy();
//        Set<OWLAxiom> axiomDifference = new TreeSet<>(nodeStructure);
//        MutableNode labelNode;
//
//        if(parent != null){
//            Set<OWLAxiom> parentStructure = parent.getLabelToSatisfy();
//            axiomDifference.removeAll(parentStructure);
//        }
//
//        String nodeLabel = labelToString(axiomDifference);
//
//        
//        labelNode = mutNode(nodeLabel + "\n("+ node.getId()+")").add(Shape.RECTANGLE); 
//        
//        n.addLink(to(labelNode).with(Style.DASHED));
//	}
	
	public void addLabelToNode(Node node, Set<OWLAxiom> nodeLabel, String rule) {
		/*
		 * Crea un rettangolo nel grafo contenente le label (struttura/concetti da soddisfare) del nodo
		 * Aggiunge un arco tra il nodo e il rettangolo Label
		 * */
		MutableNode mutableNode = graphNodes.get(node);
		
		String labelString = labelToString(nodeLabel);
		MutableNode label = mutNode(labelString + "\n("+ node.getId()+")").add(Shape.NOTE).add(Color.rgb("1020d0"));
		
		mutableNode.addLink(to(label).with(Label.of(" " + rule),Style.DASHED));
	}
	
	public void setNodeColor(Node node, String color) {
        if(color.equals("red")){
            graphNodes.get(node).add(Color.RED);
        } 
        else if(color.equals("green")){
            graphNodes.get(node).add(Color.GREEN);
        }
    }
	
	public void setClash(Node node, Pair<OWLClassExpression, OWLClassExpression> clash) {
		/*
		 * Colora il nodo di rosso e crea un rettangolo contenente il clash
		 * Aggiunge un arco tra il nodo e il rettangolo clash
		 * */
//		String clashString = clash.getKey().toString() + " \n " + clash.getValue().toString();
		UtilityString first = new UtilityString("");
		expressionToNaturalLangauge(clash.getKey(), first);
		
		UtilityString second = new UtilityString("");
		expressionToNaturalLangauge(clash.getValue(), second);
		
		String clashString = first.getString() + "\n" + second.getString();
		
		MutableNode mutableNode = graphNodes.get(node).add(Color.RED);
		MutableNode clashNode = mutNode(clashString +"\n(" +node.getId()+")").add(Shape.RECTANGLE).add(Color.RED);
		
		mutableNode.addLink(to(clashNode).with(Label.of("CLASH"), Style.DOTTED));
    }
	
	public void setClashFree(Node node) {
		MutableNode mutableNode = graphNodes.get(node).add(Color.GREEN);
		MutableNode clashFreeNode = mutNode("CLASH FREE" +"\n(" +node.getId()+")").add(Shape.RECTANGLE).add(Color.GREEN);
		
		mutableNode.addLink(to(clashFreeNode).with(Label.of("CLASH FREE"), Style.DOTTED));
	}
	
	public void setBlocked(Node node, Node blockingNode) {
		MutableNode mutableNode = graphNodes.get(node).add(Color.GREEN);
		MutableNode blockedNode = mutNode(node.getId()+" IS BLOCKED FROM " +blockingNode.getId()).add(Shape.RECTANGLE).add(Color.GREEN);
		
		mutableNode.addLink(to(blockedNode).with(Label.of("BLOCKED"), Style.DOTTED));
	}
	
	public void addLoop(Node node, String rule) {
		MutableNode mutableNode = graphNodes.get(node);
		
		mutableNode.addLink(to(mutableNode).with(Label.of(" " + rule)));
	}
	
	public String labelToString(Set<OWLAxiom> set) {
        /*
         * Restituisce le etichette di un nodo in formato stringa (serve sia ad aggiungere la label dei nodi nel grafo, sia ad aggiungere la label dei nodi in rdf)
         */ 
        OWLClassExpression classExpression;
        String labelInString = null;

        for(OWLAxiom axiom : set){
            if (axiom instanceof OWLClassAssertionAxiom){
                classExpression = ((OWLClassAssertionAxiom) axiom).getClassExpression();
                
                UtilityString us = new UtilityString("");
                expressionToNaturalLangauge(classExpression, us); //Cambia il valore di us

                if(labelInString == null){
                    labelInString = us.getString();
                } else {
                    labelInString = labelInString + "\n" + us.getString();
                }
            }
        }
        return labelInString;
    }
	
	//-----------------------------------------------------------------------------METODO AUSILIARI 
	
	private void expressionToNaturalLangauge(OWLClassExpression C, UtilityString labelContainer) {
		
		/*
		 * Converte la class expression (ObjectIntersectionOf(Person, Food) in linguaggio naturale Person and Food 
		 * Crea una stringa contenente le label di un assioma (in questo modo le label del nodo che appaiono nel grafo escono una riga ciascuno)
		 * (QUESTO METODO CAMBIA IL VALORE DELLA STRINGA CONTENUTA NEL LABEL CONTAINER)
		 * 
		 * Casistiche:
		 * 1) ClassExpression di tipo OWLClass (basta prendere il nome del concept (nothing oppure thing [bottom oppure top])
		 * 2) ComplementOf (Si fa ¬ + top or bottom) oppure si fa ¬(espressione al linguaggio naturale)
		 * 3) INTERSECT (AND)
		 * 4) UNIONOF (OR)
		 * 5) SOME VALUE
		 * 6) ALL VALUE
		 * */
		
        String label = null, conceptName = null; 

        if(C instanceof OWLClass){
            label = labelContainer.getString();
            conceptName = getConceptName((OWLClass) C);
            labelContainer.setString(label + conceptName);
        } 
        
        else if (C instanceof OWLObjectComplementOf){
            label = labelContainer.getString();
            OWLClassExpression classExpression = ((OWLObjectComplementOf) C).getOperand();

            if(classExpression instanceof OWLClass){
                conceptName = getConceptName((OWLClass) classExpression);                
                labelContainer.setString(label + "¬" + conceptName);
            } else {
            	UtilityString manageComplementOf =  new UtilityString("");
            	expressionToNaturalLangauge(classExpression, manageComplementOf);
                labelContainer.setString(label + "¬(" + manageComplementOf.getString() + ")");   
            }
        }

        if(C instanceof OWLObjectIntersectionOf){
            C.accept(new OWLClassExpressionVisitor() {
                @Override
                public void visit(OWLObjectIntersectionOf oi) {
                    String label = labelContainer.getString() + "(";
                    labelContainer.setString(label);

                    //Maybe replace with a for loop on operands
                    OWLClassExpression firstIntersectionOperand =  oi.getOperandsAsList().get(0);
                    expressionToNaturalLangauge(firstIntersectionOperand, labelContainer);
                    label = labelContainer.getString() + " ⊓ "; 
                    labelContainer.setString(label);

                    OWLClassExpression secondIntersectionOperand =  oi.getOperandsAsList().get(1);
                    expressionToNaturalLangauge(secondIntersectionOperand, labelContainer);
                    label = labelContainer.getString() + ")"; 
                    labelContainer.setString(label);
                }
            });
        }
        
        if(C instanceof OWLObjectUnionOf){
            C.accept(new OWLClassExpressionVisitor() {
                @Override
                public void visit(OWLObjectUnionOf ou) {
                    String label = labelContainer.getString() + "(";
                    labelContainer.setString(label);

                    OWLClassExpression firstUnionOperand =  ou.getOperandsAsList().get(0);
                    expressionToNaturalLangauge(firstUnionOperand, labelContainer);
                    label = labelContainer.getString() + " ⊔ "; 
                    labelContainer.setString(label);

                    OWLClassExpression secondUnionOperand =  ou.getOperandsAsList().get(1);
                    expressionToNaturalLangauge(secondUnionOperand, labelContainer);
                    label = labelContainer.getString() + ")";
                    labelContainer.setString(label);
                }
            });
        }
        
        if(C instanceof OWLObjectSomeValuesFrom){
            C.accept(new OWLClassExpressionVisitor() {
                @Override
                public void visit(OWLObjectSomeValuesFrom svf) {
                    String propertyName = getPropertyName(svf.getProperty());

                    String label = labelContainer.getString() + " ∃" + propertyName + ".";
                    labelContainer.setString(label);

                    expressionToNaturalLangauge(svf.getFiller(), labelContainer);
                    label = labelContainer.getString() + " "; 
                    labelContainer.setString(label);

                }
            });
        }
        
        if(C instanceof OWLObjectAllValuesFrom){
            C.accept(new OWLClassExpressionVisitor() {
                @Override
                public void visit(OWLObjectAllValuesFrom avf) {
                    String propertyName = getPropertyName(avf.getProperty());

                    String label = labelContainer.getString() + " ∀" + propertyName + ".";
                    labelContainer.setString(label);

                    expressionToNaturalLangauge(avf.getFiller(), labelContainer);
                    label = labelContainer.getString() + " "; 
                    labelContainer.setString(label);
                }
            });
        }
    }
	
	private String getConceptName(OWLClass C) {
        String concept = C.toStringID();
        int hashMarkIndex = concept.indexOf("#");
        String conceptName = concept.substring(hashMarkIndex+1);

        if(conceptName.equals("Nothing")){
            conceptName = "⟂"; //BOTTOM
        } 
        else if(conceptName.equals("Thing")){
            conceptName = "⊤"; //T
        }
        return conceptName;
    }

    private String getPropertyName(OWLObjectPropertyExpression R) {
        String property = R.toString();
        int hashMarkIndex = property.indexOf("#");
        String propertyName = property.substring(hashMarkIndex+1, property.length()-1);
        return propertyName;
    }
	
    public void renderGraph(String filePath) {
    	/*
    	 * salva il tableaux risultante in formato SVG (si può anche cambiare formato, tipo in PNG
    	 * */
    	
    	//cancella il vecchio file svg
        try {
            Files.delete(Paths.get(filePath + ".svg"));
        } catch(IOException e) {}

        
        //crea nuovo file svg e lo apre
        try {
            Graphviz.fromGraph(graph).width(10000).render(Format.SVG).toFile(new File(filePath));
        } catch(IOException e) {
            e.printStackTrace();
            System.exit(1);
        } catch(GraphvizException e){
            System.err.println("[ERR:] Etichetta troppo grande da rappresentare\n");
            System.exit(1);
        }
    }
    
}
