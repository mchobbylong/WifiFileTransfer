package computing.project.wififiletransfer.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.Nullable;

import java.text.MessageFormat;

public class FileTransferRecorder {
    private static final String TAG = "FileTransferRecorder";
    private static final int DB_VERSION = 2;
    private static final String TABLE_NAME = "transfer";


    private static class DBOpenHelper extends SQLiteOpenHelper {
        public DBOpenHelper(Context context) {
            super(context, context.getFilesDir().toString() + "file_transfer.db3", null, DB_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(MessageFormat.format("create table {0}("
                    + "id integer primary key autoincrement, "
                    + "md5 varchar(32) unique not null, "
                    + "filename text not null, "
                    + "size integer not null, "
                    + "absolute_path text, "
                    + "progress integer not null)", TABLE_NAME));
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("drop table " + TABLE_NAME);
            this.onCreate(db);
        }
    }

    private final SQLiteDatabase db;

    public FileTransferRecorder(Context context) throws SQLiteException {
        db = (new DBOpenHelper(context)).getReadableDatabase();
    }

    @Nullable
    public FileTransfer query(String md5) {
        String[] columns = new String[]{ "filename", "absolute_path", "progress", "size" };
        Cursor cursor = db.query(TABLE_NAME, columns, "md5 = ?", new String[]{ md5 }, null, null, null);
        if (!cursor.moveToFirst())
            return null;
        FileTransfer ret = new FileTransfer();
        ret.setMd5(md5);
        ret.setFilePath(cursor.getString(cursor.getColumnIndex("absolute_path")));
        ret.setFileName(cursor.getString(cursor.getColumnIndex("filename")));
        ret.setProgress(cursor.getLong(cursor.getColumnIndex("progress")));
        ret.setFileSize(cursor.getLong(cursor.getColumnIndex("size")));
        cursor.close();
        return ret;
    }

    public long update(FileTransfer fileTransfer) {
        ContentValues values = new ContentValues();
        values.put("md5", fileTransfer.getMd5());
        values.put("absolute_path", fileTransfer.getFilePath());
        values.put("progress", fileTransfer.getProgress());
        values.put("filename", fileTransfer.getFileName());
        values.put("size", fileTransfer.getFileSize());
        return db.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public void delete(String md5) {
        db.delete(TABLE_NAME, "md5 = ?", new String[]{ md5 });
    }

    public void close() {
        db.close();
    }
}
