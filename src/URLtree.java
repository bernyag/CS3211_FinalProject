import java.util.*;
import java.io.*;

public class URLtree {


    // Class for the nodes within the indexed URL tree
    private class URLTreeNode {
        // HashMap mapping characters to the children nodes
        HashMap<Character, URLTreeNode> children = new HashMap<>();

        // This string will be set when the string from root up to the specific
        // URLTreeNode is a valid, observed URL
        String HTMLFilePath = null;
    };

    // Root of the Indexed URL Tree
    public final URLTreeNode root = new URLTreeNode();

    // If not present, inserts key into trie
    // If the key is prefix of trie node, just marks leaf node
    public void insert(URLHTMLTuple website) {
        String urlString = website.getURL();
        String htmlContent = website.getHTMLContent();
        int length = urlString.length();
        char currentChar;

        URLTreeNode currentNode = root;

        for (int i = 0; i < length; i++) {
            // Get the current character
            currentChar = urlString.charAt(i);

            if (!currentNode.children.containsKey(currentChar)) {
                // Create a new node
                URLTreeNode newNode = new URLTreeNode();

                // Put the node into the tree
                currentNode.children.put(currentChar, newNode);
            }

            // Move down the tree
            currentNode = currentNode.children.get(currentChar);
        }

        // Maybe we have already created such html file
        if (currentNode.HTMLFilePath != null) {
            return;
        }

        // Save the URL string
        currentNode.HTMLFilePath = urlString + ".html";

        // Create an HTML file
        // Check if a folder exists
        File f = new File(".\\html_files\\");
        if(!f.exists()){
            f.mkdir();
        }

        f = new File(".\\html_files\\" + currentNode.HTMLFilePath);

        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(f));
            bw.write(htmlContent);
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    // Returns true if key presents in trie, else false
    public boolean search(URLHTMLTuple website) {
        String urlString = website.getURL();
        int length = urlString.length();
        char currentChar;

        URLTreeNode currentNode = root;

        for (int i = 0; i < length; i++) {
            // Get the current character
            currentChar = urlString.charAt(i);

            if (!currentNode.children.containsKey(currentChar))
                return false;

            currentNode = currentNode.children.get(currentChar);
        }

        return (currentNode != null && currentNode.HTMLFilePath != null);
    }

}
