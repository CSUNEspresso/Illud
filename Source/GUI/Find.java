package Source.GUI;

// IO Imports
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import java.awt.Color;
import javax.swing.border.TitledBorder;

// Listener imports
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.KeyEvent;

// Regex imports
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Highlighter imports
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;

public class Find extends JDialog {
    // GUI components
    private JPanel contentPane;
    private JPanel findPanel;
    private JPanel replacePanel;
    private JButton prevButton;
    private JButton nextButton;
    private JButton replaceButton;
    private JButton replaceAllButton;
    private JTextField queryField;
    private JTextField replaceField;
    private JCheckBox wordOption;
    private JCheckBox capsOption;
    private final JTextArea area;

    // Highlighter components
    private final Highlighter.HighlightPainter currResultHighlight = new highlighter(Color.ORANGE);
    private final Highlighter.HighlightPainter allResultsHighlight = new highlighter(Color.YELLOW);
    private Highlighter.Highlight[] highlightArr;   // Array of highlights of occurrences
    private final Highlighter high;                 // Highlights each element

    // Class logic components
    private int index;                              // Current index in search
    private boolean isHidden;                       // Used for when find window is closed to reshow results
    private int oldTextHash;                        // Used to check if the document changed while hiding
    private int oldArrChecksum;                     // Same as above
    private boolean oldWordVal;
    private boolean oldCapsVal;
    private String oldQuery;                        // Used to store last user search for find

    // Constants
    private static final int DIALOG_WIDTH = 480;
    private static final int SMALL_DIALOG_HEIGHT = 160;
    private static final int BIG_DIALOG_HEIGHT = 240;
    private static final String SEARCH_DIALOG_TITLE = "Search Results";
    private static final String DEFAULT_TITLE = "Find";

    public Find(JTextArea area) {
        area.setAutoscrolls(true);
        this.area = area;
        setContentPane(contentPane);
        setModal(true);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.setTitle(DEFAULT_TITLE);

        // Adding listeners
        prevButton.addActionListener(e -> onPrev());
        nextButton.addActionListener(e -> onNext());
        replaceButton.addActionListener(e -> onReplace());
        replaceAllButton.addActionListener(e -> onReplaceAll());

        queryField.addActionListener(e -> onFind());
        replaceField.addActionListener(e -> onReplace());

        // Listener that checks for window closing and opening events
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                super.windowClosed(e);
                isHidden = true;                                        // Find has been opened before and is hidden
                setLastState();
                onClear();                                              // Clears the window
                if(isInstanceValid() && !hasQueryChanged()){
                    highlightElement(highlightArr[index]);              // Highlights the search result
                }
            }

            @Override
            public void windowActivated(WindowEvent e) {
                super.windowActivated(e);
                if(isHidden){                       // Checks if window was opened before
                    reDisplay();                    // Re displays highlights
                    isHidden = false;               // Sets sentinel value
                }
                queryField.requestFocusInWindow();  // Gets focus for query field
            }
        });

        // Keyboard Listeners
        contentPane.registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        setPanelVis(false);                 // Cannot access some GUI elements initially
        this.setLocationRelativeTo(null);   // Centers dialog
        high = area.getHighlighter();       // Class highlighter variable
        isHidden = false;                   // Is not in hidden state
        oldQuery = "";                      // Initializes query storage
    }

    // Runs on find
    private void onFind() {
        String newQuery = queryField.getText(); // Text in find bar
        if (!newQuery.isEmpty()) {
            if(hasQueryChanged() | hasStateChanged()){
                highlightText(newQuery);
                if(highlightArr.length > 0){
                    index = ((index < highlightArr.length) ? index : 0);
                    scrollToQuery(highlightArr[index]);
                }
                else{
                    JOptionPane.showMessageDialog(this,
                            "Error: No results found for: \"" + newQuery + "\"",
                            SEARCH_DIALOG_TITLE, JOptionPane.INFORMATION_MESSAGE);
                    high.removeAllHighlights();
                    newQuery += "<{";
                }
            } else{
                changeInstance(index, 0);
            }
        } else {
            JOptionPane.showMessageDialog(this,
                    "Error: No input detected",
                    SEARCH_DIALOG_TITLE,
                    JOptionPane.ERROR_MESSAGE);
            high.removeAllHighlights();
            setPanelVis(false);
        }
        setLastState();
        oldQuery = newQuery;
    }

    // Previous instance of found string
    private void onPrev() {
        if(isInstanceValid() && !hasQueryChanged()){
            // Determines new index based on old index and looping
            int newIdx = (index == 0) ? highlightArr.length - 1 : (index - 1);
            changeInstance(index, newIdx);
            this.getRootPane().setDefaultButton(prevButton); // Sets default button to last pressed one
        } else{
            onFind();
        }
    }

    // Next instance of found string
    private void onNext() {
        if(isInstanceValid() && !hasQueryChanged()){
            // Determines new index based on old index and looping
            int newIdx = (index == highlightArr.length - 1) ? 0 : (index + 1);
            changeInstance(index, newIdx);
            this.getRootPane().setDefaultButton(nextButton); // Sets default button to last pressed one
        } else{
            onFind();
        }
    }

    // Next instance of found string
    private void onClear() {
        high.removeAllHighlights();
        setPanelVis(false);
    }

    // Replace current instance
    private void onReplace(){
        StringBuilder sb = new StringBuilder(area.getText());               // New string builder
        int startPos = highlightArr[index].getStartOffset();
        int endPos = highlightArr[index].getEndOffset();
        area.setText(                                                       // Setting text of main text area
                sb.replace(startPos,                                        // Replace the substring from startPos
                        endPos,                                             // to endPos
                        replaceField.getText()).toString()                  // with what is in replace field
        );
        int oldIndex = index;                                               // Saves current index
        highlightText(queryField.getText());                                // Gets new text and highlights it
        if(--oldIndex > 0){                                                 // If oldIndex can be decremented safely
            changeInstance(index, oldIndex);                                // Move cursor to old spot
        }
        onFind();                                                           // Does new search
        this.getRootPane().setDefaultButton(replaceButton);                 // Sets default button to last pressed one
    }

    // Replace all instances
    private void onReplaceAll(){
        String replacement = replaceField.getText();                        // String to replace
        StringBuilder sb = new StringBuilder(area.getText());               // String builder with current text
        for(int i = highlightArr.length - 1; i > -1; i--){                  // Reversed loop through highlight array
            sb.replace(highlightArr[i].getStartOffset(),                    // Replace from start
                    highlightArr[i].getEndOffset(),                         // To finish
                    replacement);                                           // With replacement text
        }
        area.setText(sb.toString());                                        // Sets text to replacement text
        onFind();                                                           // Does new search
        this.getRootPane().setDefaultButton(replaceAllButton);              // Sets default button to last pressed one
    }

    // Sets the highlight for a single element
    private void setHighlight(int index, Highlighter.HighlightPainter p){
        if(highlightArr != null && index < highlightArr.length){
            high.removeHighlight(highlightArr[index]);
            try{
                highlightArr[index] = (Highlighter.Highlight) high.addHighlight(highlightArr[index].getStartOffset(),
                        highlightArr[index].getEndOffset(), p);
            } catch (Exception e) { e.printStackTrace(); }
            String resultString = "Result: " + (index + 1) + " of " + highlightArr.length;
            javax.swing.border.TitledBorder titledBorder = javax.swing.BorderFactory.createTitledBorder(resultString);
            titledBorder.setTitleJustification(TitledBorder.CENTER);
            findPanel.setBorder(titledBorder);
        }
    }

    // Overloaded function that sets highlights for all elements
    private void setHighlights(Highlighter.HighlightPainter p){
        high.removeAllHighlights();
        for(int i = 0; i < highlightArr.length; i++){
            try{
                highlightArr[i] = (Highlighter.Highlight) high.addHighlight(highlightArr[i].getStartOffset(),
                        highlightArr[i].getEndOffset(), p);
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    // Run by onNext and onPrev, generalizes their actions before their core code
    private boolean isInstanceValid(){
        return highlightArr != null && highlightArr.length > 0;
    }

    // Returns if the search query has changed, depending on the checkbox modifiers as well
    private boolean hasQueryChanged(){
        boolean wordChanged = (oldWordVal != wordOption.isSelected());
        boolean capsChanged = (oldCapsVal != capsOption.isSelected());
        boolean queryChanged = !oldQuery.equals(queryField.getText());
        return wordChanged | capsChanged | queryChanged;
    }

    // Run by onNext and onPrev, generalizes their actions after their core code
    private void changeInstance(int oldIndex, int newIndex){
        if(isInstanceValid() && !hasQueryChanged()){
            if(newIndex != oldIndex){ // Only runs if new and old indexes differ
                setHighlight(index, allResultsHighlight);
                index = newIndex;
                scrollToQuery(highlightArr[index]);
            }
        }
    }

    // Scrolls to query if it is off screen
    private void scrollToQuery(Highlighter.Highlight h){
        setHighlight(index, currResultHighlight);                       // Sets current highlight color
        int pos = h.getEndOffset();
        try{
            java.awt.geom.Rectangle2D view = area.modelToView2D(pos);   // View where pos is visible
            area.scrollRectToVisible(view.getBounds());                 // Scroll to the rectangle
            area.setCaretPosition(pos);                                 // Sets carat position to pos
        } catch (Exception e) {e.printStackTrace();}
    }

    private static class highlighter extends DefaultHighlighter.DefaultHighlightPainter {
        public highlighter(Color color) {
            super(color);
        }
    }

    // Initializes highlight array based on pattern
    private void highlightText(String pattern) {
        String text = area.getText();                       // Input text
        String patternCopy = pattern;                       // Copy of pattern used to keep pattern the same
        if(wordOption.isSelected()){
            patternCopy = String.format("\\b%s\\b", patternCopy);
        }
        patternCopy = String.format("(%s)", patternCopy);
        if(!capsOption.isSelected()){
            patternCopy = String.format("(?i)%s", patternCopy);
        }
        high.removeAllHighlights();
        Pattern regexPattern = Pattern.compile(patternCopy);
        Matcher regexMatcher = regexPattern.matcher(text);
        try{
            while(regexMatcher.find()){
                high.addHighlight(regexMatcher.start(), regexMatcher.end(), allResultsHighlight); // Add highlight
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        highlightArr = high.getHighlights();            // Populates array of highlights
        if(highlightArr.length > 0){ // 1+ matches
            setHighlight(index, currResultHighlight);   // Highlights index 0
            setPanelVis(true);                          // Shows GUI elements
        } else{ // 0 matches
            setPanelVis(false);                         // Hides GUI elements
        }
    }

    // Re displays find after it is closed
    private void reDisplay(){
        String queryText = queryField.getText();
        if(!queryText.isEmpty()){                                               // There is a query
            if(hasStateChanged() | hasQueryChanged()){                          // Area or query has changed
                // Changed
                int temp = index;                                               // Saves index before doing new search
                highlightText(queryText);                                       // Searches new text based on old query;
                index = ((temp < (highlightArr.length)) ? temp : 0);            // Adjusts index to value in array
            } else{
                if(isInstanceValid()){                                          // Has matches
                    scrollToQuery(highlightArr[index]);                         // Moves view box to cursor
                    setPanelVis(true);                                          // Shows GUI elements
                }
            }
            setHighlights(allResultsHighlight);                                 // Sets all to highlight color
            if(isInstanceValid()){
                setHighlight(index, currResultHighlight);                       // Sets index to current highlight color
            }
        }
    }

    private void highlightElement(Highlighter.Highlight h){
        int pos = h.getEndOffset();
        try{
            java.awt.geom.Rectangle2D view = area.modelToView2D(pos);           // View where pos is visible
            area.scrollRectToVisible(view.getBounds());                         // Scroll to the rectangle
            area.setCaretPosition(pos);                                         // Sets carat position to pos
            area.moveCaretPosition(h.getStartOffset());                         // Highlights text
        } catch (Exception e) {e.printStackTrace();}
    }

    // Sum of start and end offsets to ensure no change has occurred
    private int getHighlightArrCheckSum(){
        int sum = 0;
        if(highlightArr != null){
            for(Highlighter.Highlight h : highlightArr){
                sum += h.getStartOffset();
                sum += h.getEndOffset();
            }
        }
        return sum;
    }

    // Sets panel visibility for panels that only appear if there are search results
    private void setPanelVis(boolean isVis){
        // Sets size depending on the GUI elements that are visible
        if(isVis){
            this.setSize(DIALOG_WIDTH, BIG_DIALOG_HEIGHT);
        }
        else{
            // Resets to default state with default title and dimensions
            this.setSize(DIALOG_WIDTH, SMALL_DIALOG_HEIGHT);
            javax.swing.border.TitledBorder titledBorder = javax.swing.BorderFactory.createTitledBorder(DEFAULT_TITLE);
            titledBorder.setTitleJustification(TitledBorder.CENTER);
            findPanel.setBorder(titledBorder);
        }
        this.replacePanel.setVisible(isVis);
    }

    // Returns a boolean value of whether either the text field hash or highlight object array checksum changed
    private boolean hasStateChanged(){
        boolean textChanged = (oldTextHash != area.getText().hashCode());       // Check for change in text input
        boolean arrChanged = (oldArrChecksum != getHighlightArrCheckSum());     // Check for change in checksum
        setLastState();                                                         // Saves last state
        return (textChanged | arrChanged);                                      // Returns true if change was detected
    }

    // Saves the last state to be used in the above function
    private void setLastState(){
        oldTextHash = area.getText().hashCode();                                // Saves hash of current document
        oldArrChecksum = getHighlightArrCheckSum();                             // Saves checksum of highlight array
        oldWordVal = wordOption.isSelected();
        oldCapsVal = capsOption.isSelected();
    }
}
