package com.osmandski;


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
import java.util.HashSet;
import java.util.Locale;
import java.util.zip.GZIPInputStream;

//import android.app.Activity;
import android.support.v4.app.FragmentActivity;
import android.app.AlertDialog;
import android.app.Dialog;
//import android.app.DialogFragment;
import android.support.v4.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;


public class OsmandskiActivity extends FragmentActivity {
	String extStorageDirectory;

    private ProgressBar mProgress;
    private TextView mStatus;
    private TextView mTitle;
    
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
        
        mTitle = (TextView) findViewById(R.id.textView2);
        mTitle.setText(
        			getResources().getString(R.string.title) 
        			+" " 
        			+ getResources().getString(R.string.Version));
        
        
        mProgress = (ProgressBar) findViewById(R.id.progressBar);
        
        // Download and show the mapped pistes status
        mStatus = (TextView) findViewById(R.id.status);
        TextView mPistes = (TextView) findViewById(R.id.pistes_status);
        String str= DownloadText("http://www.opensnowmap.org/data/pistes_length.en.txt");
        mPistes.setText(str);
        
        // checking installed versions
        PackageManager manager = this.getPackageManager();
        PackageInfo info;
        
        int osmandVersionCode=0;
        int osmandPlusVersionCode=0;
		try {
			info = manager.getPackageInfo("net.osmand", 0);
			osmandVersionCode= info.versionCode;
			//String appPath = info.applicationInfo.dataDir;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		try {
			info = manager.getPackageInfo("net.osmand.plus", 0);
			osmandPlusVersionCode= info.versionCode;
			//String appPath = info.applicationInfo.dataDir;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		
		int higherVersion= Math.max(osmandVersionCode,osmandPlusVersionCode);
		if (higherVersion == 0) {
			new MyDialogFragment(OsmandskiActivity.this,
					getResources().getString(R.string.no_osmand)).show( getSupportFragmentManager(), "Error");
		}
		else if (higherVersion < 40){
			new MyDialogFragment(OsmandskiActivity.this,
					getResources().getString(R.string.unsupported)).show( getSupportFragmentManager(), "Error");
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
    
    // Download world-ski.obf
	public void getPistes()
    {
		  String OsmandPath=FindDir("osmand");
    	  //Download the world-ski.obf
		  mStatus.setText(OsmandPath+"World-ski_2.obf");
		  String file_URL=
		  "http://www.opensnowmap.org/download/World-ski_2.obf.gz";
		  //String file_URL=
		  //"http://www.opensnowmap.org/download/test.txt.gz";
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
		
		String OsmandPath=FindDir("osmand");
		
		File f = new File(OsmandPath+"World-ski_2.obf.part");
		
		f.renameTo(new File(OsmandPath ,"World-ski_2.obf"));
		
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
	// Find osmand dir
    private String FindDir(String dir){
    	String OsmandPath ="";
    	
		HashSet<String> set =getExternalMounts();
		String[] list =set.toArray(new String[set.size()]);
		for (String p : list) {
			File d = new File(p+"/"+dir+"/");
			if (d.isDirectory()){
				OsmandPath=p+"/"+dir+"/";
				break;
			}
		}
		if (OsmandPath == "") {
			File sd=Environment.getExternalStorageDirectory();
			File d= new File(sd+"/"+dir+"/"); 
			if (d.isDirectory()){
				OsmandPath=sd+"/"+dir+"/";
			}
		}
		return OsmandPath;
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
			new MyDialogFragment(OsmandskiActivity.this,
					e.getMessage()).show( getSupportFragmentManager(), "Error");
        	return "";
        } catch (IOException e) {
            e.printStackTrace();
			new MyDialogFragment(OsmandskiActivity.this,
					e.getMessage()).show( getSupportFragmentManager(), "Error");
            return "";
        }catch (RuntimeException e) {
            e.printStackTrace();
			new MyDialogFragment(OsmandskiActivity.this,
					e.getMessage()).show( getSupportFragmentManager(), "Error");
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

	public static HashSet<String> getExternalMounts() {
	    final HashSet<String> out = new HashSet<String>();
	    String reg = "(?i).*vold.*(vfat|ntfs|exfat|fat32|ext3|ext4).*rw.*";
	    String s = "";
	    try {
	        final Process process = new ProcessBuilder().command("mount")
	                .redirectErrorStream(true).start();
	        process.waitFor();
	        final InputStream is = process.getInputStream();
	        final byte[] buffer = new byte[1024];
	        while (is.read(buffer) != -1) {
	            s = s + new String(buffer);
	        }
	        is.close();
	    } catch (final Exception e) {
	        e.printStackTrace();
	    }

	    // parse output
	    final String[] lines = s.split("\n");
	    for (String line : lines) {
	        if (!line.toLowerCase(Locale.US).contains("asec")) {
	            if (line.matches(reg)) {
	                String[] parts = line.split(" ");
	                for (String part : parts) {
	                    if (part.startsWith("/"))
	                        if (!part.toLowerCase(Locale.US).contains("vold"))
	                            out.add(part);
	                }
	            }
	        }
	    }
	    return out;
	}
	
	// Async downloader
	class DownloadFileAsync extends AsyncTask<String, String, String> {
		
		private String er = "";
        
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
		
		String OsmandPath=FindDir("osmand");
		
		File f = new File(OsmandPath+"World-ski_2.obf.part");

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
			Log.e("OsmandSki", "exception", e);
			er = e.getMessage();
			}
		return null;

		}
		protected void onProgressUpdate(String... progress) {
			 Log.d("ANDRO_ASYNC",progress[0]);
			 mProgress.setProgress(Integer.parseInt(progress[0]));
   		     
		}

		@Override
		protected void onPostExecute(String unused) {

			if (er != "") {
				new MyDialogFragment(OsmandskiActivity.this,er).show( getSupportFragmentManager(), "Error");
			}
			else {
					end();
			}
		}
	}

	class MyDialogFragment extends DialogFragment{
	    Context mContext;
	    String mType;
	    public MyDialogFragment(Context context, String error) {
	        mContext = context;
	        mType  =error;
	    }
	    @Override
	    public Dialog onCreateDialog(Bundle savedInstanceState) {
	    	return new AlertDialog.Builder(OsmandskiActivity.this)
        	.setMessage(mType)
               .setCancelable(false)
               .setPositiveButton(R.string.quit, new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                        finish();
                   }
               })
               .create();
	    }
	}
}

