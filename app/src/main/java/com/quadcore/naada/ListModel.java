package com.quadcore.naada;

public class ListModel {
	private  String title="";
    private  String duration="";
    private String image="";
     
    /*********** Set Methods ******************/
     
    public void setTitle(String T)
    {
        this.title = T;
    }
     
    public void setDuration(String D)
    {
        this.duration = D;
    }
     
    public void setImage(String I)
    {
        this.image = I;
    }
     
    /*********** Get Methods ****************/
     
    public String getTitle()
    {
        return this.title;
    }
     
    public String getDuration()
    {
        return this.duration;
    }
    
    public String getImage()
    {
        return this.image;
    }
 
}
