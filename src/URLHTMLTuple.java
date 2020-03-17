class URLHTMLTuple{
    private String URL;
    private String HTMLContent;

    public URLHTMLTuple(String URL, String HTMLContent){
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