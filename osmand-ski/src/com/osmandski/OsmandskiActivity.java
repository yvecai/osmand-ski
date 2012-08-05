package com.osmandski;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.lang.Math;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import java.util.zip.GZIPInputStream;


public class OsmandskiActivity extends Activity {
	String extStorageDirectory;

    private ProgressBar mProgress;
    private TextView mStatus;
    
    static final int DIALOG_NO_OSMAND = 0;
    static final int DIALOG_BAD_OSMAND_VERSION = 1;
    static final int DIALOG_SERVER_ERROR = 2;
    // Various render.xml for Osmand version:
	protected static final String StyleFileName = "winter-old_016.render.xml";
	protected static final String PlusStyleFileName = "winter-plus_016.render.xml";
	protected static final String DevStyleFileName = "winter-latest_016.render.xml";
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        mProgress = (ProgressBar) findViewById(R.id.progressBar);
        
        // Download and show the mapped pistes status
        mStatus = (TextView) findViewById(R.id.status);
        TextView mPistes = (TextView) findViewById(R.id.pistes_status);
        String str= DownloadText("http://www.pistes-nordiques.org/data/pistes_length.en.txt");
        mPistes.setText(str);
        
        // checking installed versions
        PackageManager manager = this.getPackageManager();
        PackageInfo info;
        
        int osmandVersionCode=0;
        int osmandPlusVersionCode=0;
		try {
			info = manager.getPackageInfo("net.osmand", 0);
			osmandVersionCode= info.versionCode;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		try {
			info = manager.getPackageInfo("net.osmand.plus", 0);
			osmandPlusVersionCode= info.versionCode;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		
		int higherVersion= Math.max(osmandVersionCode,osmandPlusVersionCode);
		if (higherVersion == 0) {
			showDialog(DIALOG_NO_OSMAND);
		}
		else if (higherVersion < 40){
			showDialog(DIALOG_BAD_OSMAND_VERSION);
		}
	    
		// do the job: Copy the render.xml to sdcard/osmand/rendering and download world-ski.obf
        Button buttonInstall = (Button)findViewById(R.id.install);
        buttonInstall.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
      		  //copyStyle(PlusStyleFileName);
      		  getPistes();

            }
        });
        
    }
    // Copy the winter style
    /*private void copyStyle(String styleFileName)
    {
		  AssetManager am = getResources().getAssets();
		  File f = new File(extStorageDirectory 
				  = Environment.getExternalStorageDirectory().toString()+"/osmand");
		  if(f.isDirectory()) {
			  try {
				  //open a stream
				  InputStream inStream = null;
				  inStream = am.open(styleFileName);
				  OutputStream outStream = null;
				  try {
					  outStream = new FileOutputStream(f + "/rendering/winter.render.xml");
					  try {
						  copyFile(inStream, outStream);
						  inStream.close();
						  inStream = null;
						  outStream.flush();
						  outStream.close();
						  outStream = null; 
					  } catch (IOException e){
						  Toast.makeText(OsmandskiActivity.this, "Error, can't copy style", Toast.LENGTH_LONG).show();
					  }
				  } catch (IOException e){
					  Toast.makeText(OsmandskiActivity.this, "Error, can't open output", Toast.LENGTH_LONG).show();
				  }
			  } catch (IOException e) {
				  //fail if the file cannot be read
				  Toast.makeText(OsmandskiActivity.this, "Error, giving up", Toast.LENGTH_LONG).show();
			  } 
			  //Toast.makeText(OsmandskiActivity.this, "Style installed", Toast.LENGTH_LONG).show();
		  } else {
			  Toast.makeText(OsmandskiActivity.this, "Fail. Osmand is not installed", Toast.LENGTH_LONG).show();
		  }
    }
    */
    
    // Download world-ski.obf
	public void getPistes()
    {
    	//Download the world-ski.obf
		  mStatus.setText(R.string.downloading_msg);
		  String file_URL=
		  "http://www.pistes-nordiques.org/download/World-ski_2.obf.gz";
		  //String file_URL=
		  //"http://www.pistes-nordiques.org/download/test.txt.gz";
		  final AsyncTask<String, String, String> DL = new DownloadFileAsync().execute(file_URL);

		  Button buttonInstall = (Button)findViewById(R.id.install);
	      buttonInstall.setVisibility(View.GONE);
		  
		  Button buttonCancel = (Button)findViewById(R.id.cancel);
		  buttonCancel.setVisibility(View.VISIBLE);
		  
		  buttonCancel.setOnClickListener(new Button.OnClickListener(){
			@Override
			public void onClick(View arg0) {
				// Send the cancel to the async task
				DL.cancel(true);
			}
		  });
	  }
    
	// Final dialog
	protected void end(){
		mStatus.setText("Done");
		File sd=Environment.getExternalStorageDirectory();
		File f= new File(sd,"/osmand/World-ski_2.obf.part");
		f.renameTo(new File(sd , "/osmand/World-ski_2.obf"));
		Button buttonCancel = (Button)findViewById(R.id.cancel);
		buttonCancel.setVisibility(View.GONE);
		//Toast.makeText(OsmandskiActivity.this, R.string.help, Toast.LENGTH_LONG).show();
		Dialog dialog = new Dialog(OsmandskiActivity.this);
		dialog.setContentView(R.layout.help_dialog);
		dialog.setTitle("Read this!");

		//set up button
		Button button = (Button) dialog.findViewById(R.id.ButtonDone);
		button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
		dialog.show();
	}
	
    // Error dialogs declaration
    protected Dialog onCreateDialog(int id) {
    	
        switch(id) {
        case DIALOG_NO_OSMAND:
	        return new AlertDialog.Builder(OsmandskiActivity.this)
	        	.setMessage(R.string.no_osmand)
	               .setCancelable(false)
	               .setPositiveButton(R.string.quit, new DialogInterface.OnClickListener() {
	                   public void onClick(DialogInterface dialog, int id) {
	                        finish();
	                   }
	               })
	        .create();

        case DIALOG_BAD_OSMAND_VERSION:
	        return new AlertDialog.Builder(OsmandskiActivity.this)
	        		.setMessage(R.string.unsupported)
	               .setCancelable(false)
	               	               .setNegativeButton(R.string.anyway, new DialogInterface.OnClickListener() {
	                   public void onClick(DialogInterface dialog, int id) {
	                	   dialog.cancel();
	                   }
	               })
	               .setPositiveButton(R.string.quit, new DialogInterface.OnClickListener() {
	                   public void onClick(DialogInterface dialog, int id) {
	                        finish();
	                   }
	               })
	        .create();
        case DIALOG_SERVER_ERROR:
	        return new AlertDialog.Builder(OsmandskiActivity.this)
	        	.setMessage(R.string.server_error)
	        	.setCancelable(false)
	        	.setPositiveButton("Quit", new DialogInterface.OnClickListener() {
	        		public void onClick(DialogInterface dialog, int id) {
	        			finish();
	        			}
	               })
	               .create();
        }
        return null;
    }
    
    // Downloading a simple text file
    private String DownloadText(String URL)
    {
        int BUFFER_SIZE = 2000;
        InputStream in = null;
        try {
            in = OpenHttpConnection(URL);
            InputStreamReader isr = new InputStreamReader(in);
            int charRead;
              String str = "";
              char[] inputBuffer = new char[BUFFER_SIZE];
              while ((charRead = isr.read(inputBuffer))>0)
              {                    
                  //---convert the chars to a String---
                  String readString = 
                      String.copyValueOf(inputBuffer, 0, charRead);                    
                  str += readString;
                  inputBuffer = new char[BUFFER_SIZE];
              }
              in.close();
              return str;  
        } catch (MalformedURLException e) {
        	e.printStackTrace();
        	showDialog(DIALOG_SERVER_ERROR);
        	return "";
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            showDialog(DIALOG_SERVER_ERROR);
            return "";
        }catch (RuntimeException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            showDialog(DIALOG_SERVER_ERROR);
            return "";
        }
       
    }
    
    // GET
    private InputStream OpenHttpConnection(String urlString) 
    throws IOException
    {
        InputStream in = null;
        int response = -1;
               
        URL url = new URL(urlString); 
        URLConnection conn = url.openConnection();
                 
        if (!(conn instanceof HttpURLConnection))                     
            throw new IOException("Not an HTTP connection");
        
        try{
            HttpURLConnection httpConn = (HttpURLConnection) conn;
            httpConn.setAllowUserInteraction(false);
            httpConn.setInstanceFollowRedirects(true);
            httpConn.setRequestMethod("GET");
            httpConn.connect(); 

            response = httpConn.getResponseCode();                 
            if (response == HttpURLConnection.HTTP_OK) {
                in = httpConn.getInputStream();                                 
            }                     
        }
        catch (Exception ex)
        {
            throw new IOException("Error connecting");
        }
        return in;     
    }
    
    // Copy a file to SDcard
	public void copyFile(InputStream in, OutputStream out) throws IOException {
	    byte[] buffer = new byte[1024];
	    int read;
	    while((read = in.read(buffer)) != -1){
	      out.write(buffer, 0, read);
	    }
	}
	
	// Async downloader
	class DownloadFileAsync extends AsyncTask<String, String, String> {
		
		private int er = 0;
        
		@Override
		protected void onPreExecute() {
			super.onPreExecute();  
		}

		@Override
		protected String doInBackground(String... aurl) {
			int count;

		try {

		URL url = new URL(aurl[0]);
		URLConnection connexion = url.openConnection();
		connexion.setRequestProperty("Referer", "OsmAnd-Ski");
		connexion.connect();

		int lenghtOfFile = connexion.getContentLength();
		Log.d("ANDRO_ASYNC", "Length of file: " + lenghtOfFile);

		File f = new File(extStorageDirectory = Environment.getExternalStorageDirectory().toString()+"/osmand/World-ski_2.obf.part");

		InputStream zinput = new BufferedInputStream(url.openStream());
		GZIPInputStream input = new GZIPInputStream(new BufferedInputStream(zinput));
		OutputStream output = new FileOutputStream(f);
		byte data[] = new byte[1024];

		long total = 0;

			while ((count = input.read(data)) != -1) {
				if (isCancelled()) 
					// stop download if canceled
				    break;
				else {
					total += count;
					publishProgress(""+(int)((total*100)/lenghtOfFile));
					output.write(data, 0, count);
					}
				}				
			output.flush();
			output.close();
			input.close();
			if (isCancelled()) {
				// delete file if download canceled
				f.delete();
				// and refresh
				 finish(); startActivity(getIntent());
			}
			
		} catch (IOException e) {
			// flag for error dialog in postexecute
			Log.e("MYAPP", "exception", e);
			er = 1;
			}
		return null;

		}
		protected void onProgressUpdate(String... progress) {
			 Log.d("ANDRO_ASYNC",progress[0]);
			 mProgress.setProgress(Integer.parseInt(progress[0]));
   		     
		}

		@Override
		protected void onPostExecute(String unused) {
			//Toast.makeText(OsmandskiActivity.this, "Done", Toast.LENGTH_LONG).show();
			if (er == 1) {
				showDialog(DIALOG_SERVER_ERROR);
			}
			else {
					end();
			}
		}
	}

}