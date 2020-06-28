package Source.GUI;

import javax.swing.*;
import java.awt.event.*;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

import org.json.simple.parser.JSONParser;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.ParseException;

public class Dictionary extends JDialog {
    // JavaSwing Variables
    private JPanel contentPane;
    private JButton buttonDefine;
    private JButton buttonCancel;
    private JTextField wordInputTextField;
    private JTextArea definitionJTextArea;

    private JSONParser jsonParser;

    public Dictionary() {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonDefine);

        buttonDefine.addActionListener(e -> onDefine());
        buttonCancel.addActionListener(e -> onCancel());

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(e -> onCancel(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        this.setSize(600, 320);                     // Setting Dialog Size
        this.setLocationRelativeTo(null);                       // Centers Dialog
        this.setTitle("Dictionary");                            // Sets Dialog Title
    }

    // onDefine Function
    // Will initiate reading online JSON File and output to the JTextArea
    private void onDefine() {
        definitionJTextArea.setText(null); // Clear text area for new output
        readJSON(); //retrieve definition
    }

    // Will Close Window when initiated
    private void onCancel() {
        dispose();
    }

    // Read JSON File from "https://api.dictionaryapi.dev/api/v2/entries/en/insertWordHere"
    private void readJSON(){
        // Get input from text field
        String input = wordInputTextField.getText();

        // Regular expression that I made to find the first word of almost any string. - Abraham
        // Works for words with single contractions.
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("^.*?([a-zA-Z']+'?[a-zA-Z']*)");
        java.util.regex.Matcher matcher = pattern.matcher(input);
        if(matcher.find()){
            input = matcher.group(1); // First matched input
            this.wordInputTextField.setText(input);

            // Getting information from online JSON File
            try {
                jsonParser = new JSONParser(); //creating new JSON parser

                URL url = new URL("https://api.dictionaryapi.dev/api/v2/entries/en/" + input); //Creates URL Object
                BufferedReader reader = new BufferedReader(new InputStreamReader((url.openStream())));  //Reads URL

                Object obj = jsonParser.parse(reader);  //creating Object from JSON File read online

                JSONArray array = (JSONArray) obj; //making obj into a JSON Array (extra step because website closes everything in []
                JSONObject dictJSON = (JSONObject) array.get(0); //Making array into object. defJSON should contain all objects from the web page

                // getting information from JSON Object
                String word = (String) dictJSON.get("word"); //getting word
                definitionJTextArea.append("Word: " + word + "\n\n");

                // getting info from meanings array
                JSONArray meaningArray = (JSONArray) dictJSON.get("meanings");
                for (int i=0; i<meaningArray.size(); i++){

                    JSONObject temp = (JSONObject) meaningArray.get(i);  //creating a temporary JSON Object for each array element
                    String type = (String) temp.get("partOfSpeech");  //type of speech

                    // making definition array into JSON Object
                    JSONArray defArray = (JSONArray) temp.get("definitions");
                    String definition = null;
                    for (int j=0; j<defArray.size(); j++){
                        JSONObject defJSON = (JSONObject) defArray.get(j);
                        definition = (String) defJSON.get("definition");
                    }
                    displayDef((i+1), type, definition);
                }
            }
            //Exceptions
            catch (MalformedURLException e){
                JOptionPane.showMessageDialog(this, "Malformed URL: "  + e.getMessage() + "\n");
            }
            catch(FileNotFoundException e){
                JOptionPane.showMessageDialog(this,
                        "No definitions found for: " + wordInputTextField.getText() + "\n");
            }
            catch (ParseException e){
                JOptionPane.showMessageDialog(this, "Parse Error :" + e.getMessage() + "\n");
            }
            catch (IOException e){
                JOptionPane.showMessageDialog(this, "I/O Error: " + e.getMessage() + "\n");
            }
        }
    }

    // Outputs definition to text area
    private void displayDef(int num, String type, String def){
        definitionJTextArea.append("Definition " + num + "\nType: " + type + "\nDefinition: " + def +"\n\n");
    }

    // Sets word input and searches
    public void setAndSearch(String query){
        this.wordInputTextField.setText(query);
        this.onDefine();
        this.setVisible(true);
    }
}
