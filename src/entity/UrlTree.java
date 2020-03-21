package entity;

import java.util.*;

/**
 * This class defines the URLTree. The tree is alphabetically indexed (wrt the
 * URLs), meaning that the children share part of its parents URL.
 * 
 * @since 2020-03-18
 *
 */
public class UrlTree {
	public final URLTreeNode root; // Root of the Indexed URL Tree

	public UrlTree() {
		this.root = new URLTreeNode();
	}

	/**
	 * This nested class defines the nodes of the tree. Each node contains a HashMap
	 * from Characters to nodes. This is how the mapping from parent to children is
	 * represented. Each node also contains an HTMLFilePath. However, this will only
	 * have a value in case the node is a leaf (if it's not a leaf the node is not
	 * associated with a unique URL).
	 * 
	 * @since 2020-03-18
	 */

	private class URLTreeNode {
		// HashMap mapping characters to the children nodes
		HashMap<Character, URLTreeNode> children = new HashMap<>();

		// This set will be set when the string from root up to the specific
		// URLTreeNode is a valid, observed URL
		HashSet<String> urls = null;
	};

	/**
	 * This method is used to insert a new element into the URLTree. It traverses
	 * the tree, starting at the root node, until it reaches a leaf node or until
	 * the node represents the full URL. In case of the latter, the corresponding
	 * node will be marked with an HTML file path
	 * 
	 * @param website: tuple to be inserted
	 */
	public void insert(UrlTuple urlTuple) {
		String oringinalUrl = urlTuple.getURL();
		int length = oringinalUrl.length();

		HashSet<String> foundlUrls = urlTuple.getFoundUrls();

		char currentChar;

		URLTreeNode currentNode = root;

		// traverse through all characters in the URL string
		for (int i = 0; i < length; i++) {
			currentChar = oringinalUrl.charAt(i);

			if (!currentNode.children.containsKey(currentChar)) {
				// Create a new node
				URLTreeNode newNode = new URLTreeNode();

				// Put the node into the tree
				currentNode.children.put(currentChar, newNode);

			}

			// Move down the tree
			currentNode = currentNode.children.get(currentChar);
		}

		// Maybe we have already created such HTML file
		if (currentNode.urls == null) {
			currentNode.urls = foundlUrls;
		}
	}

	/**
	 * Searches through the URLTree for the given element. Returns true if the
	 * element is already in the tree. Returns false otherwise
	 * 
	 * @param website
	 * @return
	 */
	public boolean search(UrlTuple website) {

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

		return (currentNode != null && currentNode.urls != null);
	}

	/**
	 * Gets all the urls (and their children) fromn the trie
	 */
	public HashMap<String,List<String>> getResult() {

		HashMap<String,List<String>> result = new HashMap<>();

		for(Character c : root.children.keySet()){
			getResult(result, "" + c, root.children.get(c));
		}

		return result;
	}

	private void getResult(HashMap<String,List<String>> map, String word, URLTreeNode node){
		if(node.urls != null){
			List<String> foundUrls = new LinkedList<>();
			foundUrls.addAll(node.urls);
			map.put(word, foundUrls);
		}

		for (Character c : node.children.keySet()){
			getResult(map, word + c, node.children.get(c));
		}
	}
}
