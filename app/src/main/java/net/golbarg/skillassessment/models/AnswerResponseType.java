package net.golbarg.skillassessment.models;

/** How one question was answered. The codes are stored in the database, so they must never change. */
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
