package com.seansouthern.anchoragebustimes;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

public class MySQLiteHelper extends SQLiteOpenHelper{

	private static String DATABASE_PATH = "/data/data/com.seansouthern.anchoragebustimes/databases/";
	private static final String DATABASE_NAME = "abtv4.db";
	private static final Integer DATABASE_VERSION = 4;
	
	static SQLiteDatabase ABTDatabase;
	private final Context myContext;

	
	public MySQLiteHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		this.myContext = context;
	}
	
	public void createDatabase() throws IOException{
		boolean dbExist = checkDataBase();
		
		if(dbExist){
			//Do Nothing
		}
		else{
			this.getReadableDatabase();
			try {
				//this.close();
				copyDataBase();
			}
			catch(IOException e){
				throw new Error("Error copying database!");
			}
		}
	}
	
	//Return true if db exists, false otherwise
	public boolean checkDataBase(){
		SQLiteDatabase checkDB = null;
		try{
    		String myPath = DATABASE_PATH + DATABASE_NAME;
    		checkDB = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READONLY);
    	}catch(SQLiteException e){
    		//database does't exist yet.
    	}
    	if(checkDB != null){
    		checkDB.close();
    	}
    	return checkDB != null;
    }

	private void copyDataBase() throws IOException{
		 
    	//Open your local db as the input stream
    	InputStream myInput = myContext.getAssets().open(DATABASE_NAME);
 
    	// Path to the just created empty db
    	String outFileName = DATABASE_PATH + DATABASE_NAME;
 
    	//Open the empty db as the output stream
    	OutputStream myOutput = new FileOutputStream(outFileName);
 
    	//transfer bytes from the inputfile to the outputfile
    	byte[] buffer = new byte[1024];
    	int length;
    	while ((length = myInput.read(buffer))>0){
    		myOutput.write(buffer, 0, length);
    	}
 
    	//Close the streams
    	myOutput.flush();
    	myOutput.close();
    	myInput.close();
 
    }
 
    public void openDataBase() throws SQLException{
    	//Open the database
        String myPath = DATABASE_PATH + DATABASE_NAME;
    	ABTDatabase = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READONLY);
    }
 
    @Override
	public synchronized void close() {
    	    if(ABTDatabase != null)
    		    ABTDatabase.close();
    	    super.close();
	}
 
	@Override
	public void onCreate(SQLiteDatabase db) {
 
	}
 
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
 
	}

}

