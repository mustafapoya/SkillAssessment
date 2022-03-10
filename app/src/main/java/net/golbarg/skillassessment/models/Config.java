package net.golbarg.skillassessment.models;

import android.os.Build;

import androidx.annotation.RequiresApi;

import net.golbarg.skillassessment.util.CryptUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class Config {
    private int id;
    private String key;
    private String value;
    private long updatedAt;

    public Config(int id, String key, String value, long updatedAt) {
        this.id = id;
        this.key = key;
        this.value = value;
        this.updatedAt = updatedAt;
    }

    public Config(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getDecryptedValue() throws NoSuchPaddingException, InvalidKeyException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException {
        return CryptUtil.decrypt(getValue());
    }

    @Override
    public String toString() {
        return "Config{" +
                "id=" + id +
                ", key='" + key + '\'' +
                ", value='" + value + '\'' +
                ", updatedAt=" + updatedAt +
                '}';
    }

    public static Config createFromJson(JSONObject json) throws JSONException {
        return new Config(json.getInt("id"), json.getString("key"), json.getString("value"), json.getLong("updated_at"));
    }
}
