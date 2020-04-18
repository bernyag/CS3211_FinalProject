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
	private String parent;
    private String URL;
    private String HTML;
    private ArrayList<String> children;
    private boolean dead = false;

    public UrlTuple(String parent, String URL){
    	this.parent = parent;
        this.URL = URL;
    }
    
    public String getParent(){
        return parent;
    }

    public String getURL(){
        return URL;
    }

    public String getHTML(){
        return HTML;
    }
    
    public void setHTML(String html){
        HTML = html;
    }
    
    public ArrayList<String> getChildren(){
    	return children;
    }
    
    public void setChildren(ArrayList<String> c){
    	children = c;
    }
    
    public void setDead(){
    	dead = true;
    }
    
    public boolean dead(){
    	return dead;
    }
}