package Source.GUI;
import Libraries.MaryTTS.Tutorial.TextToSpeech;
import Source.Logic.CounterUtil;
import Source.Logic.FileOpener;

// GUI Imports
import javax.swing.event.DocumentEvent;                         // Used for getting jTextArea text
import javax.swing.event.DocumentListener;                      // Used for creating jTextArea listeners
import javax.swing.JFrame;
import javax.swing.ImageIcon;
import javax.swing.UIManager;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JTextArea;
import javax.swing.JList;
import javax.swing.KeyStroke;

public class Window extends JFrame {
    // Window Variables
    private static final String ICON_PATH = "/Resources/icon.png";  // Path to the icon
    private static final String WIN_NAME = "Illud - Text Analysis"; // Name of the window

    private Find find;                                              // Find dialog
    private Dictionary dictionary;                                  // Dictionary dialog
    private About about;                                            // About dialog
    private FileOpener fileOpener;                                  // Opens files

    // JMenuItems to add listeners to in the menu
    private JMenuItem open_menu_item;
    private JMenuItem dict_menu_item;
    private JMenuItem find_menu_item;
    private JMenuItem tts_menu_item;
    private JMenuItem about_menu_item;

    // UserInput variables
    private UserInput userInput;                                    // Form for user input

    // Text to Speech Variables
    private final TextToSpeech tts;                                 // Text to speech object
    private final float volume;                                     // Volume of Text To Speech

    // Enum for getting strings corresponding to different voices
    private enum Voice{
        poppy("dfki-poppy-hsmm");
//        rms("cmu-rms-hsmm"),
//        slt("cmu-slt-hsmm");
        public final String voiceString;                            // Unmodifiable value
        Voice(String vS){                                           // Enum constructor
            this.voiceString = vS;
        }
    }

    // Constructor
    public Window(){
        // Setting look and feel
        try {
            // For each installed look and feel (UI theme)
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) { // Searches for Nimbus theme
                    UIManager.setLookAndFeel(info.getClassName()); // Sets to Nimbus theme if found
                    break; // Breaks loop
                }
            }
        }
        catch (Exception e) { // Theme is not found
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); // Sets to default theme
            } catch (Exception ex) { e.printStackTrace(); }
        }

        // Initializing text to speech
        tts = new TextToSpeech();                               // Creates new Text to Speech Object
        tts.setVoice(Voice.poppy.voiceString);                  // Sets a voice to the text to speech object
        this.volume = 1.0f;                                     // Sets volume to a default number
        initUI();                                               // Initializes the User Interface
    }

    // Misc Functions
    private void speak(String text) {tts.speak(text, volume, false, false);}    // Uses MaryTTS on the text
    private void endSpeak() { tts.stopSpeaking(); }                                         // Ends MaryTTS playback

    // Initializing all of the UI elements in Window
    private void initUI() {
        // Initializing JFrame
        this.setTitle(WIN_NAME);                                // Creates new JFrame to put JPanels on

        ImageIcon illudIcon = new ImageIcon(                    // New icon image composed of:
                getClass()                                      // From the instance of current class:
                        .getResource(ICON_PATH));               // Get the resource at ICON_PATH

        // Setting Icon image in the JFrame
        this.setIconImage(illudIcon.getImage());

        userInput = new UserInput();                            // Creates new instance of UserInput
        this.setContentPane(userInput.getMainPanel());          // Sets content pane to new instance of UserInput
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);    // Exits when X is clicked
        this.pack();                                            // Packs the elements on top of the JFrame
        this.setSize(500, 400);                     // Sets size to a default amount
        this.setLocationRelativeTo(null);                       // Centers Window
        this.setVisible(true);                                  // Makes everything visible

        // Creating jMenuBar, jMenus and jMenuItems
        // jMenuBar > jMenu > jMenuItem
        // jMenuBar holds all of the jMenus
        // A jMenu for example would be "File" or something you would see inside the bar
        // A jMenuItem or jMenu would show up once you click the jMenu in the jMenuBar
        JMenuBar jMenuBar = new JMenuBar();
        JMenu file = new JMenu("File");                               // "File"
        open_menu_item = new JMenuItem("Open");                     // "File > Open"
        JMenu actions = new JMenu("Actions");                         // "Actions"
        dict_menu_item = new JMenuItem("Dictionary");               // "Actions" > "Dictionary"
        find_menu_item = new JMenuItem("Find");                     // "Actions" > "Find"
        tts_menu_item = new JMenuItem("Read Highlighted Text");     // "Actions" > "Read Highlighted Text"
        JMenu help = new JMenu("Help");                               // "Help"
        about_menu_item = new JMenuItem("About");                   // "Help" > About"

        // Creating the menu bar from the above elements
        jMenuBar.add(file);
        jMenuBar.add(actions);
        jMenuBar.add(help);
        file.add(open_menu_item);
        actions.add(dict_menu_item);
        actions.add(find_menu_item);
        actions.add(tts_menu_item);
        help.add(about_menu_item);
        this.setJMenuBar(jMenuBar);                                         // Sets the menu bar
        makeListeners();                                                    // Creates action listeners

        find = new Find(userInput.getMainTextArea());                       // Creating Find Dialog
        find.setIconImage(illudIcon.getImage());                            // Sets Icon to Illud Icon

        dictionary = new Dictionary();                                      // Creating Dictionary Dialog
        dictionary.setIconImage(illudIcon.getImage());                      // Sets Icon to Illud Icon

        about = new About();                                                // Creating About Dialog
        about.setIconImage(illudIcon.getImage());                           // Sets Icon to Illud Icon

        fileOpener = new FileOpener();                                      // Creating file handler object
    }

    // Makes listeners for UserInput
    private void makeListeners(){
        // Gets UI elements from userInput
        JTextArea jTextArea = userInput.getMainTextArea();
        JList<String> list = userInput.getJList();

        // Listener for Document
        jTextArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { updateCounters(jTextArea, list); }
            @Override
            public void removeUpdate(DocumentEvent e) { updateCounters(jTextArea, list); }
            @Override
            public void changedUpdate(DocumentEvent e) { updateCounters(jTextArea, list); }
        });

        // Listener for File > Open
        open_menu_item.addActionListener(e -> fileOpener.activate(this, userInput));

        // Listener for Action > Find
        find_menu_item.addActionListener(e -> find.setVisible(true));

        // Listener for Action > Dictionary
        dict_menu_item.addActionListener(e -> {
            // Gets Text from jTextArea
            String text = jTextArea.getSelectedText();

            if(text != null && !text.equals("")){ // Highlighted Text
                dictionary.setAndSearch(text);
            } else{
                dictionary.setVisible(true);
            }
        });

        // Listener for Help > About
        about_menu_item.addActionListener(e -> about.setVisible(true));

        tts_menu_item.addActionListener(e -> {
            // Gets Text from jTextArea
            String text = jTextArea.getSelectedText();

            // No text highlighted
            if(text == null){                   // If text is null end TTS playback
                endSpeak();
            } else if (!text.equals("")){       // Checks if text is not empty
                speak(text);                    // Uses TTS on the text
            }
        });

        // Assigning shortcut keys
        assignCmdListener(java.awt.event.KeyEvent.VK_D, dict_menu_item);   // Opens dict on Command + D
        assignCmdListener(java.awt.event.KeyEvent.VK_F, find_menu_item);   // Opens find on Command + F
        assignCmdListener(java.awt.event.KeyEvent.VK_I, about_menu_item);  // Opens find on Command + I
        assignCmdListener(java.awt.event.KeyEvent.VK_O, open_menu_item);   // Opens open on Command + O
        assignCmdListener(java.awt.event.KeyEvent.VK_T, tts_menu_item);    // Opens tts on Command + T
    }

    private void updateCounters(JTextArea jTextArea, JList<String> jList){
        String currentText = jTextArea.getText();
        jList.setListData(CounterUtil.getCounterData(currentText));
    }

    // Adds listener that runs on command + key platform independently
    public static void assignCmdListener(int key, JMenuItem jMenuItem){
        jMenuItem.setAccelerator(KeyStroke.getKeyStroke(
                key, java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
    }
}
