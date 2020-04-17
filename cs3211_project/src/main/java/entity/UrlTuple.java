package entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

/**
 * Represents the elements we acquire. Each element is a tuple consisting of a URL tag
 * as well as its HTML content. 
 * 
 * @author niklas
 * @since 2020-03-18
 *
 */
public class UrlTuple{
    private String URL;
    private String HTML;
    private ArrayList<String> foundUrls;

    private HashSet<String> foundURLs = new HashSet<>();

    public UrlTuple(String URL, String HTML, ArrayList<String> foundUrls){
        this.URL = URL;
        this.HTML = HTML;
        this.foundUrls = foundUrls; 
    }

    public String getURL(){
        return URL;
    }

    public String GetHTML(){
        return HTML;
    }
    
    public ArrayList<String> getFoundUrls(){
    	return foundUrls;
    }
}