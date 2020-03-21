package entity;

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
    private HashSet<String> foundURLs = new HashSet<>();

    public UrlTuple(String URL, Collection<String> foundURLs){
        this.URL = URL;
        this.foundURLs.addAll(foundURLs);
    }

    public String getURL(){
        return URL;
    }

    public HashSet<String> getFoundUrls(){
        return foundURLs;
    }
}