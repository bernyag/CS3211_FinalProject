package entity;


/**
 * Represents the elements we acquire. Each element is a tuple consisting of a URL tag
 * as well as its HTML content. 
 * 
 * @author niklas
 * @since 2020-03-18
 *
 */
public class UrlHtmlTuple{
    private String URL;
    private String HTMLContent;

    public UrlHtmlTuple(String URL, String HTMLContent){
        this.URL = URL;
        this.HTMLContent = HTMLContent;
    }

    public String getURL(){
        return URL;
    }

    public String getHTMLContent(){
        return HTMLContent;
    }
}