package com.samedtemiz.artbook;

public class Art {
    private int id;
    private String artName;


    public Art(int id, String artName) {
        this.id = id;
        this.artName = artName;
    }

    //Getter & Setter
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getArtName() {
        return artName;
    }

    public void setArtName(String artName) {
        this.artName = artName;
    }
}
