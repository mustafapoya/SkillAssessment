package net.golbarg.skillassessment.models;

public enum AnswerResponseType {
    CORRECT(0), WRONG(1), NO_ANSWER(2);

    public final int code;

    AnswerResponseType(int code) {
        this.code = code;
    }

    public static AnswerResponseType fromCode(int code) {
        for (AnswerResponseType type : values()) {
            if (type.code == code) return type;
        }
        return NO_ANSWER;
    }
}
