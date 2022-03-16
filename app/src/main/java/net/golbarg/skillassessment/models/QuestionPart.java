package net.golbarg.skillassessment.models;

import java.util.ArrayList;

public class QuestionPart {
    private String title;
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

    public ArrayList<QuestionCode> getCodeList() {
        return codeList;
    }

    public void setCodeList(ArrayList<QuestionCode> codeList) {
        this.codeList = codeList;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder(title);

        for(int i =0 ; i < codeList.size(); i++) {
            result.append(codeList.get(i)).append("\n");
        }

        return result.toString();
    }

}
