package progetto;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;

import progetto.utility.Node;

public class RDFWriter {
	
	private Model model;
    private String namespace;
    
    
    //Inizializza RDF, ovvero instanzia il modello, il namespace e il suo prefisso.
    public void initRDF() {
        model = ModelFactory.createDefaultModel();
        namespace = "http://example.org/";
        model.setNsPrefix("ex", namespace);
    }

    //Aggiunge una tripla RDF in formato TURTLE (resource, property, value)
    public void addRDFTriple(Node node, String propertyName, Object value) {
    	/*
    	 * value può essere di tipo Node oppure di tipo String (Non si fanno 2 metodi appositi per poter utilizzare lo stesso metodo, il disclaimer del tipo si fa con un if value instanceof)
    	 * La tripla aggiunta sarà:
    	 * node#node.getId() - propertyName - node#nodeValue.getId()
    	 * oppure
    	 * node#node.getId() - propertyName - stringValue
    	 * */
    	
        Resource resource, nodeValue;
        Property property;
        Node child;
        String labels;
        String nodePrefix = namespace + "node#";

        resource = model.createResource(nodePrefix + node.getId());
        property = model.createProperty(namespace, propertyName);

        if(value instanceof Node) {
            child = (Node) value;
            nodeValue = model.createResource(nodePrefix + child.getId());

            resource.addProperty(property, nodeValue);

        } 
        else if(value instanceof String) {
        	labels = (String) value;
            if(resource.hasProperty(property)) {
            	resource.removeAll(property);	
            }
            resource.addProperty(property, labels);
            
        }
    }

    //Salva il file RDF
    public void renderRDF(String filePath) {
    	
    	try {
            Files.delete(Paths.get(filePath));
        } catch(IOException e) {}
    	
    	
        try {
            FileOutputStream fileout = new FileOutputStream(filePath);
            RDFDataMgr.write(fileout, model, Lang.TURTLE);
        } catch(FileNotFoundException e) {
            e.printStackTrace();
        }   
    }
	
	
}
