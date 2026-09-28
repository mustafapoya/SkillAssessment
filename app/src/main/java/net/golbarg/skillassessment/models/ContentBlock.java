package net.golbarg.skillassessment.models;

/** One piece of a question or answer body: prose, a fenced code block or an image. */
public class ContentBlock {
    public enum Type { TEXT, CODE, IMAGE }

    private final Type type;
    private final String content;
    private final String language;

    public ContentBlock(Type type, String content, String language) {
        this.type = type;
        this.content = content;
        this.language = language;
    }

    public Type getType() { return type; }

    /** Text, code, or the image path/URL depending on {@link #getType()}. */
    public String getContent() { return content; }

    public String getLanguage() { return language; }
}
