package progetto.view;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;

import javafx.application.Platform;

public class OntologyPanel extends JPanel{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String ontologyPath = "";

	public OntologyPanel() {
		createPanel();
	}
	
    public void createPanel() {    	
    	JDialog dialog = new JDialog();
    	dialog.getContentPane().setLayout(new FlowLayout());
    	dialog.setSize(900, 500);
    	dialog.setModal(true);
    	dialog.setLayout(null);
    	dialog.setTitle("Select your ontology");
    	
    	JTextArea textArea = new JTextArea("Your selected ontology will be written here");
    	textArea.setVisible(true);
    	textArea.setBounds(150,0,600,300);
        textArea.setEditable(false);
    	    	
    	JScrollPane scroll = new JScrollPane(textArea);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        scroll.setBounds(150,0,600,300);
        
        dialog.getContentPane().add(scroll);

    	JButton openButton = new JButton("Open File");
    	openButton.setVisible(true);
    	openButton.setBounds(0,0,100,50);
    	dialog.getContentPane().add(openButton);
    	openButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser j = new JFileChooser("f:");
	            int r = j.showOpenDialog(null);
	 
	            if (r == JFileChooser.APPROVE_OPTION) {
	                File fi = new File(j.getSelectedFile().getAbsolutePath());
	                ontologyPath = fi.toString();
	                
	                try {
	                    String s1 = "", sl = "";

	                    FileReader fr = new FileReader(fi);
	                    BufferedReader br = new BufferedReader(fr);
	 
	                    sl = br.readLine();
	 
	                    while ((s1 = br.readLine()) != null) {
	                        sl = sl + "\n" + s1;
	                    }
	 
	                    textArea.setText(sl);
	                    br.close();
	                }
	                catch (Exception evt) {
	                    JOptionPane.showMessageDialog(dialog, evt.getMessage());
	                }
	            }
	            else
	                JOptionPane.showMessageDialog(dialog, "the user cancelled the operation");
				
			}
		});
    	
    	
    	JButton doneButton = new JButton("Done");
    	doneButton.setVisible(true);
    	doneButton.setBounds(650,310,100,50);
    	dialog.getContentPane().add(doneButton);
    	doneButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (ontologyPath.equals("")) {
		            JOptionPane.showMessageDialog(dialog, "Non hai inserito nessuna ontologia!!", "ERROR", JOptionPane.INFORMATION_MESSAGE);
				}
				else {
		            JOptionPane.showMessageDialog(dialog, "Ontology obtained: " + ontologyPath, "SUCCESS", JOptionPane.INFORMATION_MESSAGE);
					
		            dialog.setModal(false);
		            dialog.setVisible(false);
		            Platform.exit();
				}

			}
    		
    	});
    	
    	//Make dialog visible
    	dialog.setVisible(true);
    	
    }
	public String getOntologyPath() {
		return this.ontologyPath;
	}
}
