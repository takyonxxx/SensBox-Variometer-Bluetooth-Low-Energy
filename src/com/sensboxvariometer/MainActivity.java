/* Copyright (C) Türkay Biliyor 
   turkaybiliyor@hotmail.com */
package com.sensboxvariometer;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Date;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.location.Location;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.PowerManager;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.preference.PreferenceManager;
import android.content.SharedPreferences;
public class MainActivity extends Activity {

	private gauge_bearing compass;	
	private WindCalculator windCalculator;
	java.util.Date df; 
	private double[] wind;	
	private double[] windError;
	private double[][] headingArray;
	private String pilotname ,glidermodel,glidercertf,pilotid,compid,logfilename=null; 
	private int logtime=3000,graphspeed=1,trckcount=0,soundtype=2;
	private boolean gettakeoff=false,logging=false,logfooter=false,logheader=false,sdcardready=false,init = false,hasWind=false;
	private AudioManager amanager=null;
	private TextView AltitudeTxt,TempTxt,GpsSpeedTxt,VertSpeedTxt,GpsfixTxt,LatitudeTxt,LongitudeTxt,WindTxt,Distancetotakeoff;
	private ArrayList<String> FlightValues=null;		
	private ProgressBar climbProgress,sinkProgress;		
	private Button exit,startlog,altinc,altdec,mute,volumedec,volumeinc,settings;
	private double pressure=0,baroaltitude=0,dblLatitude,dblLongitude,dbltakeoffLatitude,
			dbltakeoffLongitude,sinkalarm=1, dbvario=0,dtemp=0;
	private int interval=250,gpsfix=0,gpsalt=0,speed=0,heading=0;
	private long dv,senstime;
	private GraphView mGraphView;
	private double slp_inHg_=29.92;	
	private static double pressure_hPa_= 1013.0912;		
	private static final double SLT_K = 288.15;  // Sea level temperature.
	private static final double TLAPSE_K_PER_M = -0.0065;  // Linear temperature atmospheric lapse rate.
	private static final double G_M_PER_S_PER_S = 9.80665;  // Acceleration from gravity.
	private static final double R_J_PER_KG_PER_K = 287.052;  // Specific gas constant for air, US Standard Atmosphere edition.	
	private static final double PA_PER_INHG = 3386;  // Pascals per inch of mercury.		
	private Handler loghandler = new Handler();
	private Handler sensboxhandler = new Handler();	
	private BeepThread beeps=null;	
	private BluetoothAdapter bluetoothAdapter;
	private PowerManager.WakeLock wl;	 
	private SenseBoxAdapter senseBoxAdapter = null;
	private static final int REQUEST_ENABLE_BT = 0;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);		
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "My Tag");
		wl.acquire();
		setContentView(R.layout.activity_main);			
		//this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);		
		
	    LatitudeTxt = (TextView) findViewById(R.id.Latitudetxt);
	 	LongitudeTxt = (TextView) findViewById(R.id.Longitudetxt);		
	    AltitudeTxt = (TextView) findViewById(R.id.Altitude);				
		GpsSpeedTxt = (TextView) findViewById(R.id.Speed);	
		VertSpeedTxt = (TextView) findViewById(R.id.Vert_Speed);	
		TempTxt= (TextView) findViewById(R.id.TempTxt);
		GpsfixTxt=(TextView) findViewById(R.id.Gpsfix);	
		Distancetotakeoff=(TextView) findViewById(R.id.Distancetakeoff);
		WindTxt=(TextView) findViewById(R.id.Wind_Speed);
		startlog= (Button) findViewById(R.id.button_log);		
		altinc= (Button)findViewById(R.id.altinc);
		altdec= (Button) findViewById(R.id.altdec);		
		volumeinc= (Button)findViewById(R.id.volumeinc);
		volumedec= (Button) findViewById(R.id.volumedec);					
		exit= (Button) findViewById(R.id.exit);	
		mute= (Button) findViewById(R.id.mute);	
		settings= (Button) findViewById(R.id.btn_settings);					
		compass = (gauge_bearing) findViewById(R.id.gauge_bearing);
		mGraphView = (GraphView) findViewById(R.id.graph);	
		FlightValues = new ArrayList<String>();  
		
			if(comprobarSDCard(this))
			 sdcardready=true;
			 else
			 sdcardready=false;		 
       
			climbProgress = (ProgressBar) findViewById(R.id.climb_progressbar);
			sinkProgress = (ProgressBar) findViewById(R.id.sink_progressbar);
			climbProgress.setMax(100);
			sinkProgress.setMax(100);				  
	        amanager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE); 
	        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
		           Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
		           finish();
		           return;
		       }
			   final BluetoothManager bluetoothManager =(BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		       bluetoothAdapter = bluetoothManager.getAdapter();

		       // Checks if Bluetooth is supported on the device.
		       if (bluetoothAdapter == null) {
		           Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
		           finish();
		           return;
		       }  
		    ViewTreeObserver vto =  compass.getViewTreeObserver();
		      vto.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
		          @Override
		          public void onGlobalLayout() {
		        	  int gaugeheight=compass.getHeight();
		        	  int gaugewith=compass.getWidth();
		        	 if(gaugewith>gaugeheight)
		        	 {
		 		    	RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(gaugeheight, gaugeheight);	 
		 		    	lp.addRule(RelativeLayout.ABOVE,findViewById(R.id.graph).getId());
		 		    	lp.addRule(RelativeLayout.CENTER_HORIZONTAL);		
		 		        compass.setLayoutParams(lp);  
		        	 }else{
		        		RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(gaugewith, gaugewith);	 
		        		lp.addRule(RelativeLayout.ABOVE,findViewById(R.id.graph).getId());
		 		    	lp.addRule(RelativeLayout.CENTER_HORIZONTAL);		
		  		        compass.setLayoutParams(lp);
		        	 }
		        		 
		        	  compass.getViewTreeObserver().removeGlobalOnLayoutListener(this);
		          }
		      });
			exit.setOnClickListener(new View.OnClickListener() {					
				@Override
				public void onClick(View arg0) {					
					 exit();					 
				}
			});	
	 	
	 	mute.setOnClickListener(new View.OnClickListener() {			
			@Override
			public void onClick(View arg0) {	
				try{
					if(mute.getText().equals(" Mute "))
	            	{
						if(beeps!=null)
						{
						beeps.stop();						
						mute.setText(" Sound On ");	
						}
	            	}else{
	            		if(beeps!=null)
						{
	        			beeps.start(getBaseContext(),soundtype,sinkalarm);	        			
	            		mute.setText(" Mute ");	 
						}
	            	}		
				}catch(Exception e){}
			}
		});	
     
		volumeinc.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View arg0) {	
				amanager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
			
			}
		});		
		volumedec.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View arg0) {	
				amanager.adjustStreamVolume(AudioManager.STREAM_MUSIC,AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
				
			}
		});		
		altinc.setOnClickListener(new View.OnClickListener() {
					
					@Override
					public void onClick(View arg0) {									
						long slp_inHg_long = Math.round(100.0 * slp_inHg_);
						if (slp_inHg_long < 3100) ++slp_inHg_long;	
				    	slp_inHg_ = slp_inHg_long / 100.0;		
				    	pressure=altTohPa(baroaltitude);					    				
				    	AltitudeTxt.setText(String.format("%1.1f m",hPaToMeter(slp_inHg_,pressure))); 
					}
				});		
		altdec.setOnClickListener(new View.OnClickListener() {
					
					@Override
					public void onClick(View arg0) {
						long slp_inHg_long = Math.round(100.0 * slp_inHg_);
						if (slp_inHg_long > 2810) --slp_inHg_long;	 
				    	slp_inHg_ = slp_inHg_long / 100.0;	
				    	pressure=altTohPa(baroaltitude);					    				
				    	AltitudeTxt.setText(String.format("%1.1f m",hPaToMeter(slp_inHg_,pressure))); 
					}								
				});	
		startlog.setOnClickListener(new View.OnClickListener() {					
			@Override
			public void onClick(View arg0) {					
				if(startlog.getText().equals(" Start Log "))
				{									
					 try{	
						 FlightValues.clear();	
						 trckcount=0;
						 preparelogheader();					     
					     loghandler.postDelayed(logrunnable, logtime);	
					     startlog.setText(" Stop Log ");
					     logging=true; 						   
					     //startlog.getBackground().setColorFilter(new LightingColorFilter(Color.parseColor("#363c45"), Color.parseColor("#A52A2A")));
					     startlog.setTextColor(Color.YELLOW);
					     File root = new File(Environment.getExternalStorageDirectory(), "VarioLog");	
						 Toast.makeText(MainActivity.this, "IGC File path\n" + root.toString(), Toast.LENGTH_LONG).show();
					 }
					     catch (Exception e)  
				         { 							            								            		
				         }					
				}
				else{  
					 AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
			         alertDialogBuilder
			                 .setMessage("Are you sure you want stop logging?")
			                 .setCancelable(true)
			                 .setPositiveButton("YES",
			                         new DialogInterface.OnClickListener() {
			                             public void onClick(DialogInterface dialog,
			                                     int id) {		                                
			                            	 if(sdcardready & logging)
			        						 {				            		        		    
			        	            			 createigc();
			        	            			 trckcount=0;
			        							 startlog.setText(" Start Log ");
			        							 logfooter=logheader=false;
			        							// startlog.getBackground().setColorFilter(new LightingColorFilter(Color.parseColor("#363c45"), Color.parseColor("#363c45")));
			        							 startlog.setTextColor(Color.WHITE);
			        						 }	
			                             }
			                         })	
			                 .setNegativeButton("NO", new DialogInterface.OnClickListener() {
			                 public void onClick(DialogInterface dialog, int id) {
			                     //  Action for 'NO' Button
			                     dialog.cancel();
			                 }
		             });
			         AlertDialog alert = alertDialogBuilder.create();
			         alert.show();  
            	}
			}
		});	
       settings.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View arg0) {	
				if(!logging)
	        	{
		        	Intent i = new Intent(getApplicationContext(),Prefs.class); 
		        	i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		        	startActivity(i);	
	        	}else
	        	{	       		
	        	 Toast.makeText(MainActivity.this, "You can not enter settings while logging! Firstly stop logging.", Toast.LENGTH_LONG).show();	        	 
	        	}				
			}
		});			
	}
	 private void init()
	   {		   	  
		   new startSensBox(this).execute();			    	   
	   }
	@Override
	public void onDestroy() {
	    super.onDestroy();
	    wl.release();
	    sensboxhandler.removeCallbacks(sensrunnable);	
	}
	
	boolean getvalues=false;
	 @Override
		protected void onResume() {        	
			super.onResume();	
			 if (!bluetoothAdapter.isEnabled()) {
		           final Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		           startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		           return;
		       }else  if(!isInit())
		    	   		  init();
			
			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this); 
			pilotname = preferences.getString("pilotname", "n/a"); 
	        glidermodel = preferences.getString("glidermodel", "n/a"); 
	        glidercertf = preferences.getString("glidercertf", "n/a"); 
	        pilotid = preferences.getString("pilotid", "n/a"); 
	        compid = preferences.getString("compid", "n/a");  
	        
	        String soundfreqstr=preferences.getString("soundfreq", "2"); 	        
	        soundtype=Integer.parseInt(soundfreqstr); 
	        String logtimestr=preferences.getString("log_updates_interval", "3000");  
	        logtime=Integer.parseInt(logtimestr);    	        
	        String graphspeedstr=preferences.getString("graphspeed_interval", "1");  
	        graphspeed=Integer.parseInt(graphspeedstr);  
	        String sinkalarmstr=preferences.getString("sink_alarm", "1.5");  
	        sinkalarm=Double.parseDouble(sinkalarmstr); 	
	       
	        if(beeps==null)
			   {       
				beeps = new BeepThread(MainActivity.this);				   
				beeps.start(getApplicationContext(),soundtype,sinkalarm);
			   }
			else{
				beeps.stop();
				beeps.start(getApplicationContext(),soundtype,sinkalarm);
			}			
	        Calendar c = Calendar.getInstance();
			SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
		    String formattedTime = df.format(c.getTime());			    
		    GpsfixTxt.setText(formattedTime +"  SatFix: "+ getGpsFix(gpsfix)  + "  Trck: " + trckcount);			   		    
		    windCalculator = new WindCalculator(16, 0.3, 300);     //todo settings
	        wind = new double[3];	
	        windError = new double[3];
	        if(!getvalues)
			{
			 getvalues();	
			 getvalues=true;
			}
	      
	 }
	  @Override
		protected void onActivityResult(int requestCode, int resultCode, Intent data) {		
			super.onActivityResult(requestCode, resultCode, data);	
			 if (requestCode == REQUEST_ENABLE_BT) {
		           if (resultCode == Activity.RESULT_CANCELED) {
		               finish();
		           } else {
		               init();                
		           }
		       }
	  }	
	@Override
	 public boolean onKeyDown(int keyCode, KeyEvent event) {
	   //Handle the back button
	   if (keyCode == KeyEvent.KEYCODE_BACK) {
		   exit();
	   }	 
	   return super.onKeyDown(keyCode, event);
	 } 
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}
	public String getHemisphereLat(double coord) {
		if (coord < 0) {
			return "S";
		} else {
			return "N";
		}
	}
	public String getHemisphereLon(double coord) {
		if (coord < 0) {
			return "W";
		} else {
			return "E";
		}
	}
	public boolean onOptionsItemSelected(MenuItem item) 
	{	
	    switch (item.getItemId()) 
	    {
	        case R.id.settings:	
	        	if(!logging)
	        	{
	        	Intent i = new Intent(getApplicationContext(),Prefs.class); 
	        	i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
	        	startActivity(i);	
	        	}else
	        	{	       		
	        	 Toast.makeText(MainActivity.this, "You can not enter settings while logging! Firstly stop logging.", Toast.LENGTH_LONG).show();
	        	 return super.onOptionsItemSelected(item);
	        	}
	        return true;	 	        
	    }
	  return false;
	}
	public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);	   
	}
	
	public void exit()
	{		
		savevalues();
		 if(logging)
			{
			 AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
	         alertDialogBuilder
	                 .setMessage("Are you sure you want exit?")
	                 .setCancelable(true)
	                 .setPositiveButton("EXIT",
	                         new DialogInterface.OnClickListener() {
	                             public void onClick(DialogInterface dialog,
	                                     int id) {	
	                                 	 stopdevices();
	                  		        	 if(sdcardready & logging)
	             						 {	
	             	            			 createigc();
	             						 }
	                  		        	android.os.Process.killProcess(android.os.Process.myPid());		             					
	                             }
	                         })	
	                 .setNegativeButton("NO", new DialogInterface.OnClickListener() {
	                 public void onClick(DialogInterface dialog, int id) {
	                     //  Action for 'NO' Button
	                     dialog.cancel();
	                 }
         });
	         AlertDialog alert = alertDialogBuilder.create();
	         alert.show();        
			}else
			{
			   	 stopdevices();
			   	android.os.Process.killProcess(android.os.Process.myPid());
			}
	}	
	public static String ConvertDecimalToDegMinSec(double coord)
    {
    	String output, degrees, minutes, seconds;   
    	double mod = coord % 1;
    	int intPart = (int)coord;     
    	degrees = String.valueOf(intPart);
      	coord = mod * 60;
    	mod = coord % 1;
    	intPart = (int)coord;
            if (intPart < 0) {
               // Convert number to positive if it's negative.
               intPart *= -1;
            }     
    	minutes = String.format("%02d",intPart);     
    	coord = mod * 60;
    	intPart = (int)coord;
            if (intPart < 0) {
               // Convert number to positive if it's negative.
               intPart *= -1;
            }
    	seconds = String.format("%02d",intPart);     
    	//Standard output of D°M′S″
    	output = degrees + "° " + minutes + "' " + seconds + "\"";
    	return output;
    }   
    
	public void savevalues()
    {
    	String sFileName="variometer_settings.txt";
		try {
			File root = new File(Environment.getExternalStorageDirectory(),
					"VarioLog");
			if (!root.exists()) {
				root.mkdirs();
			}
			File settingsfile = new File(root, sFileName);
			FileWriter writer = new FileWriter(settingsfile);
			writer.write(String.valueOf(slp_inHg_));	
			writer.flush();
			writer.close();			
		}catch(Exception e){}
    }
	
    public void getvalues()
    {
    	String sFileName="variometer_settings.txt";
		try {
			File root = new File(Environment.getExternalStorageDirectory(),	"VarioLog");
			File settingsfile = new File(root, sFileName);
			if (settingsfile.exists()) 
			{				
	    		    BufferedReader br = new BufferedReader(new FileReader(settingsfile));
	    		    String line;
	    		    line = br.readLine();
	    		    this.slp_inHg_ = Double.parseDouble(line);
	    		    if (slp_inHg_ < 28.1 || slp_inHg_ > 31.0) slp_inHg_ = 29.92;
	    		    br.close();
	    	}else
	    	{	    		
	    		this.slp_inHg_ = 29.92;
	    	}
		}catch(Exception e){}
    }
    public void startvario()
	{		 
			try{			 
			   sensboxhandler.postDelayed(sensrunnable, 250);				   
			}catch(Exception e){ 				
				}		 
	} 
    public boolean isInit() {
    	return init;
    }
	
    private static double hPaToMeter(double slp_inHg, double pressure_hPa) {
	   	// Algebraically unoptimized computations---let the compiler sort it out.
	   	double factor_m = SLT_K / TLAPSE_K_PER_M;
	   	double exponent = -TLAPSE_K_PER_M * R_J_PER_KG_PER_K / G_M_PER_S_PER_S;    	
	   	double current_sea_level_pressure_Pa = slp_inHg * PA_PER_INHG;
	   	double altitude_m =
	   			factor_m *
	   			(Math.pow(100.0 * pressure_hPa / current_sea_level_pressure_Pa, exponent) - 1.0);
	   	return altitude_m;
	   }
	   public static double altTohPa(double altitude) {
		     return Math.pow(((44330.8 - altitude) / 4946.54), 5.25588) / pressure_hPa_* 1013.25 /100.0;	   
		  }
  
	
	private String getGpsFix(int gpsfix)
    {
         String gpsString;
         switch (gpsfix) {
             case 0:  gpsString = "No fix";
                      break;
             case 1:  gpsString = "2D fix";
                      break;
             case 2:  gpsString = "3D fix";
                      break;
             case 3:  gpsString = "2D + DPGS";
                      break;
             case 4:  gpsString = "3D + DPGS";
                      break;             
             default: gpsString = "No fix";
                      break;
         }
		return gpsString;    	
    }
	private void stopdevices()
	{
		 try{
			 if(beeps!=null)
				{
					beeps.stop();
					beeps.onDestroy();
				}				
				if(loghandler!=null)  
				loghandler.removeCallbacks(logrunnable);
				  if (senseBoxAdapter != null) {
					  sensboxhandler.removeCallbacks(sensrunnable);	
					  senseBoxAdapter.onDestroy();				  
				  }
				savevalues();					
			    sensboxhandler.removeCallbacks(sensrunnable);	
	        }
		    catch(Exception e)
		    {
		    }
	}
	private void createigc()
	{
		try{
			 logging=false;
		   	 preparelogfooter();
			 generateIGC_onSD(logfilename + ".igc",FlightValues);	
	 		}
 		catch (Exception e)  
         { 	 			
         }	
	}
	private void preparelogfooter()
	{	
	   if(!logfooter)
	   {
		     Calendar c = Calendar.getInstance();
			 SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd  HH:mm:ss");
			 String formattedDate = df.format(c.getTime());
			 String value = "LXGD Turkay Biliyor Android Igc Version 1.00" + "\r\n";
			 value=value+("LXGD Downloaded " + formattedDate);    
			 FlightValues.add(value);				 
			     formattedDate = df.format(c.getTime());		
			     logfilename="FlightLog_"+formattedDate.replace(" ", "_");
			 logfooter=true;
	   }
	}
	private void preparelogheader()
	{	
		if(!logheader){					
			 Calendar c = Calendar.getInstance();
			 SimpleDateFormat dfdetail = new SimpleDateFormat("ddMMyy");
		     String formattedDate = dfdetail.format(c.getTime());			    
		     String value ="AXSV ANDROID SENSBOX VARIO TURKAY BILIYOR" + "\r\n";
		     value=value+"HFDTE" + formattedDate + "\r\n";
		     value=value+"HOPLTPILOT:"+ pilotname + "\r\n";
		     value=value+"HOGTYGLIDERTYPE: " + glidermodel + " - " + glidercertf + "\r\n";
		     value=value+"HOGIDGLIDERID:" + pilotid + "\r\n";			    
		     value=value+"HOCIDCOMPETITIONID:" + compid + "\r\n";
		     value=value+"HODTM100GPSDATUM: WGS-84" + "\r\n";		    
		     value=value+"HOSITSite: None" + "\r\n"; 		   
		     FlightValues.add(value);	
		     logheader=true;
		}
	}
	private void generateIGC_onSD(String sFileName, ArrayList<String> FlightValues)
	{
	    try
	    {	    	
	        File root = new File(Environment.getExternalStorageDirectory(), "VarioLog");
	        if (!root.exists()) 
	        {
	            root.mkdirs();
	        }
	        File igcfile = new File(root, sFileName);
	        FileWriter writer = new FileWriter(igcfile);
	        for(String str: FlightValues) {
	        	  writer.write(str);	        	 
	        	}	       
	        writer.flush();	      
	        writer.close();	 
	        FlightValues.clear();
	        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
	        Uri uri = Uri.fromFile(igcfile);
	        intent.setData(uri);
	        sendBroadcast(intent);    
	    }
	    catch(IOException e)
	    {	       
	    }
	} 	
	 private void SetGraph(final double alt, final double maxvalue,final float graphspeed) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					mGraphView.addDataPoint(alt,maxvalue,graphspeed);
				}
			});
		}	
	
    private void distancetotakeoff(double targetlt,double targetlon)
    {    	
    	Location currentLocation = new Location("reverseGeocoded");
		currentLocation.setLatitude(dblLatitude);          
		currentLocation.setLongitude(dblLongitude);  
		
		Location targetLocation = new Location("reverseGeocoded");
	    targetLocation.setLatitude(targetlt);           
	    targetLocation.setLongitude(targetlon); 	    
    	String str=null;
     	double distance = (int)currentLocation.distanceTo(targetLocation); 
     	if(distance<=999)
     		 str = String.valueOf(String.format("%.0f",distance)) + " m";
     	else
     		 str = String.valueOf(String.format("%.1f",distance/1000)) + " km";  
     	Distancetotakeoff.setText(str);
    } 
	
	 private Runnable logrunnable = new Runnable() {
		   @Override
		   public void run() {
			    if (logging & dblLatitude!=0){
			    	setigcfile();	
			    }
			    loghandler.postDelayed(this,logtime);
		   }
		};	
		void playsound(){	
			if(beeps!=null)				
			beeps.setAvgVario(dbvario);			
		}
	
		public String decimalToDMSLat(double coord) {
			try{
	        String output, degrees, minutes, hemisphere;
	        if (coord < 0)
	        {
	        	coord=-1*coord;
	        	hemisphere="S";
	        }else
	        {
	        	hemisphere="N";  
			}        
	        double mod = coord % 1;
	        int intPart = (int)coord;
	        degrees = String.format("%02d",intPart);
	        coord = mod * 60;              
	        DecimalFormat df = new DecimalFormat("00.000");
	        minutes= df.format(coord).replace(".", "");
	        minutes=minutes.replace(",", "");
	        output = degrees + minutes + hemisphere;	 
	        return output;
			}
		    catch(Exception e)
		    {		    	
		    	 return null;
		    }
		}
		public String decimalToDMSLon(double coord) {
			try{
	        String output, degrees, minutes, hemisphere;
	        if (coord < 0)
	        {
	        	coord=-1*coord;
	        	hemisphere="W";
	        }else
	        {
	        	hemisphere="E";  
			}
	        double mod = coord % 1;
	        int intPart = (int)coord;
	        degrees = String.format("%03d",intPart);
	        coord = mod * 60;              
	        DecimalFormat df = new DecimalFormat("00.000");
	        minutes= df.format(coord).replace(".", "");
	        minutes=minutes.replace(",", "");
	        output = degrees + minutes + hemisphere;	 
	        return output;
			}
		    catch(Exception e)
		    {		    	
		    	 return null;
		    }
		}
	
	private void setigcfile()
	  {	
			//B
		    Date date = new Date(senstime);      		   
		    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
	    	String igcgpstime = sdf.format(date);    	
		  	String igclat=decimalToDMSLat(dblLatitude);    	
		  	String igclon=decimalToDMSLon(dblLongitude);
		  	//A
		  	String igcaltpressure=String.format("%05.0f",baroaltitude);
		  	String igcaltgps=String.format("%05d",gpsalt);
		  	String igcval="B"+igcgpstime.replace(":","")+igclat+igclon+"A"+igcaltpressure+igcaltgps;    
		  	if(gpsfix!=0)
		  	{    		 
			      FlightValues.add(igcval+"\r\n");	 
			      trckcount++;
		  	} 
	  }	
	
	private static Boolean comprobarSDCard(Context mContext) {
	    String auxSDCardStatus = Environment.getExternalStorageState();

	    if (auxSDCardStatus.equals(Environment.MEDIA_MOUNTED))
	    {	    	
	        return true;
	    }
	    else if (auxSDCardStatus.equals(Environment.MEDIA_MOUNTED_READ_ONLY)) {
	        Toast.makeText(
	                mContext,
	                "Warning, the SDCard it's only in read mode.\nthis does not result in malfunction"
	                        + " of the read aplication", Toast.LENGTH_LONG)
	                .show();
	        return false;
	    } else if (auxSDCardStatus.equals(Environment.MEDIA_NOFS)) {
	        Toast.makeText(
	                mContext,
	                "Error, the SDCard can be used, it has not a corret format or "
	                        + "is not formated.", Toast.LENGTH_LONG)
	                .show();
	        return false;
	    } else if (auxSDCardStatus.equals(Environment.MEDIA_REMOVED)) {
	        Toast.makeText(
	                mContext,
	                "Error, the SDCard is not found, to use the reader you need "
	                        + "insert a SDCard on the device.",
	                Toast.LENGTH_LONG).show();
	        return false;
	    } else if (auxSDCardStatus.equals(Environment.MEDIA_SHARED)) {
	        Toast.makeText(
	                mContext,
	                "Error, the SDCard is not mounted beacuse is using "
	                        + "connected by USB. Plug out and try again.",
	                Toast.LENGTH_LONG).show();
	        return false;
	    } else if (auxSDCardStatus.equals(Environment.MEDIA_UNMOUNTABLE)) {
	        Toast.makeText(
	                mContext,
	                "Error, the SDCard cant be mounted.\nThe may be happend when the SDCard is corrupted "
	                        + "or crashed.", Toast.LENGTH_LONG).show();
	        return false;
	    } else if (auxSDCardStatus.equals(Environment.MEDIA_UNMOUNTED)) {
	        Toast.makeText(
	                mContext,
	                "Error, the SDCArd is on the device but is not mounted."
	                        + "Mount it before use the app.",
	                Toast.LENGTH_LONG).show();
	        return false;
	    }

	    return true;
	}
	private class startSensBox extends AsyncTask<Object, Void, Boolean>{	
		 private ProgressDialog mProgressDialog;	
		 public startSensBox(Context context) 
			{				 
			     mProgressDialog = new ProgressDialog(context);
			     mProgressDialog.setMessage("Connecting to SensBox ...");		     		    	
			}
		 @Override
		    protected void onPreExecute() {
		        super.onPreExecute();	               	        
		        mProgressDialog.show();
		 }
		@Override
		protected Boolean doInBackground(Object... params)  {					
			    if(senseBoxAdapter==null)
				senseBoxAdapter = new SenseBoxAdapter();		
				init = senseBoxAdapter.connect();	
			return init;
		}			
		@Override
		protected void onPostExecute(Boolean result) {			
		        super.onPostExecute(result);
		        mProgressDialog.dismiss();		               
		        if(result)
		    	{
		    		senseBoxAdapter.readAllNewValues();		    		
		    		sensboxhandler.postDelayed(sensrunnable,interval);	
		    		 Toast.makeText(getApplicationContext(), "SensBox Connected", Toast.LENGTH_LONG).show();		    		
		    	} else
		    	{		 
		    		 Toast.makeText(getApplicationContext(), "SensBox Not Found!", Toast.LENGTH_LONG).show();	
		    		 AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
			         alertDialogBuilder
			                 .setMessage("SensBox Not Found!")
			                 .setCancelable(true)
			                 .setPositiveButton("Try Again",
			                         new DialogInterface.OnClickListener() {
			                             public void onClick(DialogInterface dialog,int id) {		
			                     				init();
			                     				return;
			                             }
			                         })	
			                 .setNegativeButton("EXIT", new DialogInterface.OnClickListener() {
			                 public void onClick(DialogInterface dialog, int id) {
			                     //  Action for 'NO' Button
			                     exit();
			                 }
		             });
			         AlertDialog alert = alertDialogBuilder.create();
			         alert.show();  
		    	}
		    }
	 }	
	public void setvario()  
	  {	
		 runOnUiThread(new Runnable() {
	            @Override
	            public void run() { 
					 			     		     
						    if(dbvario>=0)
						    {
						    	climbProgress.setProgress((int)(dbvario*100/8));			    	
						    	sinkProgress.setProgress(0);	
						    	VertSpeedTxt.setTextColor(Color.GREEN);
						    }
						    else if(dbvario<0)
						    {
						    	sinkProgress.setProgress((int) ((-1*dbvario)*100/8));			    	
						        climbProgress.setProgress(0);
						        VertSpeedTxt.setTextColor(Color.RED);
						    }
						    
						    LatitudeTxt.setText(ConvertDecimalToDegMinSec(dblLatitude) + " " + getHemisphereLat(dblLatitude));
							LongitudeTxt.setText(ConvertDecimalToDegMinSec(dblLongitude) + " " +  getHemisphereLon(dblLongitude));	
							dv = Long.valueOf(senstime);
		                    df = new java.util.Date(dv);
				   	        VertSpeedTxt.setText(String.format("%.1f m/s",dbvario));	
				   	        pressure=altTohPa(baroaltitude);
		    				   				
		    				AltitudeTxt.setText(String.format("%1.1f m",hPaToMeter(slp_inHg_,pressure))); 
		    				GpsSpeedTxt.setText(String.format("%d km",speed)); 	
		    				GpsfixTxt.setText(new SimpleDateFormat("HH:mm:ss").format(df)+"  SatFix: "+ getGpsFix(gpsfix)  + "  Trck: " + trckcount);
		    				if(dbltakeoffLatitude!=0)
							distancetotakeoff(dbltakeoffLatitude,dbltakeoffLongitude);	
						      //new wind calculation
						        windCalculator.addSpeedVector(heading, speed, senstime / 1000.0);
						        headingArray = windCalculator.getPoints();
						        if (headingArray.length > 2) {
						            wind = FitCircle.taubinNewton(headingArray);
						            windError = FitCircle.getErrors(headingArray, wind);						           
						            hasWind = true;
						        } else {
						            hasWind = false;
						        }     
					        if(hasWind)
				            {					            
					            double windspeed=getWindSpeed();
					            if (!Double.isNaN(windspeed) && !Double.isInfinite(windspeed))
					            {
					            	WindTxt.setText(String.format("%.0f km", windspeed));
					            	compass.setval((float) getWindDirection());
					            }
				            }    
						    compass.rotaterose((float) (-1*heading));
						    TempTxt.setText(String.format("%1.1f°C", dtemp).replace(",","."));				   	       
				   	        SetGraph(dbvario+6,12,graphspeed);		   	       
					 
	            }
		 });
	  }
	 
	    private Runnable sensrunnable = new Runnable() {
			   @Override
			   public void run() {
				   if(isInit())
					{
					   senseBoxAdapter.readAllNewValues();			   
			           dbvario=senseBoxAdapter.getVario();    
			           playsound();
			           senstime=senseBoxAdapter.getGpsTime();           
			           dblLatitude = senseBoxAdapter.getLat();    
			           dblLongitude = senseBoxAdapter.getLng();   	
			           gpsalt = senseBoxAdapter.getGpsAlt();   	
			           baroaltitude = senseBoxAdapter.getPressureAttitude();           
			           speed = senseBoxAdapter.getGroundSpeed(); 
			           heading = (int) senseBoxAdapter.getHeading();
			           dtemp= senseBoxAdapter.getTemp();  	
			           gpsfix=senseBoxAdapter.getGpsFix();  
			           if(!gettakeoff && gpsfix!=0 )
						{
							dbltakeoffLatitude=dblLatitude;
							dbltakeoffLongitude=dblLongitude;
							gettakeoff=true;
						}
			           try{
			        	   setvario(); 
			           }catch(Exception e){}
					}			    
				    sensboxhandler.postDelayed(this,interval);			  
			   }
			};	
			   public synchronized double getWindSpeed() {
			        return Math.sqrt(wind[0] * wind[0] + wind[1] * wind[1]);
			    }

			    public synchronized double getWindSpeedError() {
			        return Math.sqrt(windError[0] * windError[0] + windError[1] * windError[1]);
			    }

			    public synchronized double getAirSpeed() {
			        return wind[2];
			    }

			    public synchronized double getAirSpeedError() {
			        return windError[2];
			    }

			    public synchronized double[] getWindError() {
			        return ArrayUtil.copy(windError);
			    }

			    public synchronized double[] getWind() {
			        return ArrayUtil.copy(wind);
			    }

			    public synchronized double getWindDirection() {
			        return resolveDegrees(Math.toDegrees(Math.atan2(wind[1], wind[0]))); //the direction the wind is coming from! (not the wind vector direction - that would be + 90.0 instead
			    }

			    public double resolveDegrees(double degrees) {
			        if (degrees < 0.0) {
			            return resolveDegrees(degrees + 360.0);

			        }
			        if (degrees > 360.0) {
			            return resolveDegrees(degrees - 360.0);
			        }
			        return degrees;
			    }

			
}

