package com.example.clothfinder;

import android.net.Uri;

import java.io.Serializable;

public class Product implements Serializable {
    private String linkUri;
    private String imageUri;
    private String name;
    private int accuracy;
    private int price;

    public Product() {}
    public Product(String name) {
        this.name = name;
    }
    public Product(String linkUri, String imageUri, String name, int accuracy, int price) {
        this.linkUri = linkUri;
        this.imageUri = imageUri;
        this.name = name;
        this.accuracy = accuracy;
        this.price = price;
    }


    public Uri getLinkUri() {
        return Uri.parse(linkUri);
    }

    public void setLinkUri(String linkUri) {
        this.linkUri = linkUri;
    }

    public Uri getImageUri() {
        return Uri.parse(imageUri);
    }

    public void setImageUri(String imageUri) {
        this.imageUri = imageUri;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(int accuracy) {
        this.accuracy = accuracy;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }
}
