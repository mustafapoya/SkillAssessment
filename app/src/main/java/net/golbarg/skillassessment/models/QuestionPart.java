package net.golbarg.skillassessment.models;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class QuestionPart {
    private String title;
    private String image;
    private ArrayList<QuestionCode> codeList;

    public QuestionPart() {
        this.codeList = new ArrayList<>();
    }

    public QuestionPart(String title) {
        this.title = title;
        this.codeList = new ArrayList<>();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public boolean hasImage() {
        return getImage() != null && getImage().trim() != "";
    }

    public boolean isLocalImage() {
        return hasImage() && !(getImage().toLowerCase().contains("http") || getImage().toLowerCase().contains("https"));
    }

    public ArrayList<QuestionCode> getCodeList() {
        return codeList;
    }

    public void setCodeList(ArrayList<QuestionCode> codeList) {
        this.codeList = codeList;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder(title);

        if(hasImage())
            result.append(getImage()).append("\n");

        for(int i =0 ; i < codeList.size(); i++) {
            result.append(codeList.get(i)).append("\n");
        }

        return result.toString();
    }

}
