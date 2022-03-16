package net.golbarg.skillassessment.models;

public class QuestionCode {
    private String language;
    private String code;

    public QuestionCode(String language, String code) {
        this.language = language;
        this.code = code;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @Override
    public String toString() {
        return "[language: " + getLanguage() + ", code: " + getCode() + "]";
    }
}