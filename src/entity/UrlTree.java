package entity;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.io.*;

/**
 * This class defines the URLTree. The tree is alphabetically indexed (wrt the URLs), 
 * meaning that the children share part of its parents URL. 
 * 
 * @since 2020-03-18
 *
 */
public class UrlTree {
	private int docID;
	public final URLTreeNode root; 	// Root of the Indexed URL Tree
	ReadWriteLock readWriteLock = new ReentrantReadWriteLock();


	public UrlTree() {
		this.docID = 0;
		this.root = new URLTreeNode();
	}
	
	/**
	 * This nested class defines the nodes of the tree. Each node contains a HashMap
	 * from Characters to nodes. This is how the mapping from parent to children
	 * is represented. Each node also contains an HTMLFilePath. However, this will
	 * only have a value in case the node is a leaf (if it's not a leaf the node is not
	 * associated with a unique URL).
	 * 
	 * @since 2020-03-18
	 */
	
	private class URLTreeNode {
		// HashMap mapping characters to the children nodes
		HashMap<Character, URLTreeNode> children = new HashMap<>();

		// This string will be set when the string from root up to the specific
		// URLTreeNode is a valid, observed URL
		String HTMLFilePath = null;
	};
	


	// If not present, inserts key into trie
	// If the key is prefix of trie node, just marks leaf node
	/**
	 * This method is used to insert a new element into the URLTree. It traverses
	 * the tree, starting at the root node, until it reaches a leaf node or until 
	 * the node represents the full URL. 
	 * 
	 * @param website: tuple to be inserted
	 */
	public void insert(UrlHtmlTuple website) {
		String urlString = website.getURL();
		String htmlContent = website.getHTMLContent();
		int length = urlString.length();
		char currentChar;

		URLTreeNode currentNode = root;

		// traverse through all characters in the URL string 
		for (int i = 0; i < length; i++) {
			currentChar = urlString.charAt(i);

			if (!currentNode.children.containsKey(currentChar)) {
				// Create a new node
				URLTreeNode newNode = new URLTreeNode();

				// Put the node into the tree
				currentNode.children.put(currentChar, newNode);
				
				//TODO break?
			}

			// Move down the tree
			currentNode = currentNode.children.get(currentChar);
		}

		// Maybe we have already created such HTML file
		if (currentNode.HTMLFilePath == null) {
			saveUrl(website);
		}
	}
	
	//TODO Finish implementation
	/**
	 * Helper function to insert(). It creates appropriate folders if needed and saves
	 * the tuple to the file system. 
	 * @param website: tuple to be saved
	 */
	private void saveUrl(UrlHtmlTuple website) {
		
	
		// Save the URL string
		String htmlFilePath = ((int)(Math.random() * 100000)) + ".html";

		// Create an HTML file
		// Check if a folder exists
		
		File f = new File("."+ File.separator + "html_files"+ File.separator); 
		if (!f.exists()) {
			f.mkdir();
		}

		f = new File("." + File.separator + "html_files" + File.separator  + htmlFilePath); 

		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(f));
			bw.write(website.getHTMLContent());
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}


	/**
	 * Searches through the URLTree for the given element. Returns true 
	 * if the element is already in the tree. Returns false otherwise
	 * 
	 * @param website
	 * @return
	 */
	public boolean search(UrlHtmlTuple website) {
		
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
