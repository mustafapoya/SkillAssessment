package net.golbarg.skillassessment.db;

import android.content.ContentValues;
import android.database.Cursor;

import java.util.ArrayList;

public interface CRUDHandler<T> {
    public void create(T object);
    public T get(int id);
    public ArrayList<T> getAll();
    public int update(T object);
    public void delete(T object);
    public int getCount();
    public T mapColumn(Cursor cursor);
    public ContentValues putValues(T object);
    public void emptyTable();
}
