package progetto.view;

import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import javafx.util.Pair;

public class ResultPanel extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private Pair<Boolean,Long> isSatisfiable_reasoningTime;
	
	public ResultPanel(Pair<Boolean,Long> b) {
		this.isSatisfiable_reasoningTime = b;
		createPanel();
	}
	
	
	private void createPanel() {
		JDialog dialog = new JDialog();
    	dialog.getContentPane().setLayout(new FlowLayout());
    	dialog.setSize(900, 500);
    	dialog.setModal(true);
    	dialog.setLayout(null);
    	dialog.setTitle("Results");
    	
    	JTextArea textArea = new JTextArea("\n[TABLEAUX ALGORITHM FOR ALC] RESULTS:\n\n"
    			+ "Concept Satisfiability: " + isSatisfiable_reasoningTime.getKey()
    			+ "\n\nReasoning Time: " + isSatisfiable_reasoningTime.getValue() + "ms");
    	textArea.setVisible(true);
    	textArea.setBounds(150,0,600,300);
        textArea.setLineWrap(true);
        textArea.setEditable(false);
        dialog.getContentPane().add(textArea);
        
        
        JButton openGraphButton = new JButton("View Graph");
    	openGraphButton.setVisible(true);
    	openGraphButton.setBounds(0,0,100,50);
    	dialog.getContentPane().add(openGraphButton);
    	openGraphButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				openGraphView();
			}
		});
        
    	JButton openRDFButton = new JButton("View RDF");
    	openRDFButton.setVisible(true);
    	openRDFButton.setBounds(0,150,100,50);
    	dialog.getContentPane().add(openRDFButton);
    	openRDFButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				openRDFView();
			}
		});
        
    	
    	JButton doneButton = new JButton("Close");
    	doneButton.setVisible(true);
    	doneButton.setBounds(650,310,100,50);
    	dialog.getContentPane().add(doneButton);
    	doneButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
    		
    	});
        
        
    	//Make dialog visible
    	dialog.setVisible(true);
    	
	}
	
	private void openGraphView() {
        try {
            Desktop.getDesktop().open(new File("result/tableau_graph.svg"));
        } catch(IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
	
	private void openRDFView() {
        try {
            Desktop.getDesktop().open(new File("result/tableau_rdf"));
        } catch(IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
