package Source.GUI;

import javax.swing.*;
import java.awt.event.*;
import java.io.FileReader;
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
    private JPanel contentPane;
    private JButton buttonDefine;
    private JButton buttonCancel;
    private JTextField wordInputTextField;
    private JTextArea definitionTextArea;

    public Dictionary() {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonDefine);

        buttonDefine.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                onDefine();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                onCancel();
            }
        });

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {

                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    private void onDefine() {
        // add your code here
        readJSON();
    }

    private void onCancel() {
        // add your code here if necessary
        dispose();
    }

    // Read JSON File from "https://api.dictionaryapi.dev/api/v2/entries/en/insertWordHere"
    public void readJSON(){

        try {
            JSONParser jsonParser = new JSONParser(); //creating new JSON parser

            URL url = new URL("https://api.dictionaryapi.dev/api/v2/entries/en/hello"); //Creates URL Object
            //FileReader reader = new FileReader(".\\Resources\\hello.json");    //this is a test to read local JSON File
            BufferedReader reader = new BufferedReader(new InputStreamReader((url.openStream())));  //Reads URL

            Object obj = jsonParser.parse(reader);  //creating Object from JSON File read online

            JSONArray array = (JSONArray) obj; //making obj into a JSON Array (extra step because website closes everything in []
            JSONObject defJSON = (JSONObject) array.get(0); //Making array into object. defJSON should contain all objects from the webpage

            //getting information from JSON Object
            String word = (String) defJSON.get("word");
            System.out.println(word);
        }
        //Exceptions
        catch (MalformedURLException e){
            System.out.println("Malformed URL: " + e.getMessage());
        }
        catch(FileNotFoundException e){
            System.out.println("File Error: " + e.getMessage());
        }
        catch (ParseException e){
            System.out.println("Parse Error :" + e.getMessage());
        }
        catch (IOException e){
            System.out.println("I/O Error: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        Dictionary dialog = new Dictionary();
        dialog.pack();
        dialog.setVisible(true);
        System.exit(0);
    }
}
