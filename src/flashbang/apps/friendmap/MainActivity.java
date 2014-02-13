package flashbang.apps.friendmap;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.facebook.HttpMethod;
import com.facebook.Request;
import com.facebook.Request.GraphUserListCallback;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.Session.Builder;
import com.facebook.Session.OpenRequest;
import com.facebook.Session.StatusCallback;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.android.FacebookError;
import com.facebook.model.GraphObject;
import com.facebook.model.GraphUser;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;



public class MainActivity extends Activity {

	
	// Google Map
    private GoogleMap googleMap;
    private UiLifecycleHelper lifecycleHelper;
	 boolean pickFriendsWhenSessionOpened;
	 Context ctx;
	 
	Set<LatLong> seen = new HashSet<LatLong>();
	 
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		ctx=this;
		  //LifeCycle manager Facebook
        lifecycleHelper = new UiLifecycleHelper(this, new Session.StatusCallback() {
            @Override
            public void call(Session session, SessionState state, Exception exception) {
                onSessionStateChanged(session, state, exception);
            }
        });
        lifecycleHelper.onCreate(savedInstanceState);
       ensureOpenSession();
		
       
       
		
		 try {
	            // Loading map
	            initilizeMap();
	 
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
		
		 
		 googleMap.setMyLocationEnabled(true);
		
		
	}
	
	
	/**
     * function to load map. If map is not created it will create it for you
     * */
    private void initilizeMap() {
        if (googleMap == null) {
            googleMap = ((MapFragment) getFragmentManager().findFragmentById(
                    R.id.map)).getMap();
 
            // check if map is created successfully or not
            if (googleMap == null) {
                Toast.makeText(getApplicationContext(),
                        "Sorry! unable to create maps", Toast.LENGTH_SHORT)
                        .show();
            }
        }
    }

    
    @Override
    protected void onResume() {
        super.onResume();
        initilizeMap();
    }
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	
	 @Override
	  public void onActivityResult(int requestCode, int resultCode, Intent data) {
	      super.onActivityResult(requestCode, resultCode, data);
	      switch (requestCode) {
      
         default:
             Session.getActiveSession().onActivityResult(this, requestCode, resultCode, data);
             break;
     }
	   
	  }
	 
	 @Override
		public boolean onOptionsItemSelected(MenuItem item) {
		    // Handle presses on the action bar items
		    switch (item.getItemId()) {
		        case R.id.showFriends:
		        	if(isNetworkAvailable() == true)
		        		onClickPickFriends();
		        	else
		        		  Toast.makeText(getApplicationContext(), "Please switch on your Internet Connection.", Toast.LENGTH_LONG).show();
		        	
		        	
		            return true;
		         
		        default:
		            return super.onOptionsItemSelected(item);
		    }
		}
	 
	 
	 private boolean ensureOpenSession() {
	        if (Session.getActiveSession() == null ||
	                !Session.getActiveSession().isOpened()) {
	        	
	        	List<String> perms = new ArrayList<String>();
	        	perms.add("friends_hometown");
	        	perms.add("friends_location");
	        	perms.add("user_hometown");
	        	perms.add("user_location");
	        	openActiveSession(this, true, new Session.StatusCallback() {
	                @Override
	                public void call(Session session, SessionState state, Exception exception) {
	                  
	                }
	            },perms);
	            return false;
	        }
	        return true;
	    }

	    private void onSessionStateChanged(Session session, SessionState state, Exception exception) {
	        if (pickFriendsWhenSessionOpened && state.isOpened()) {
	            pickFriendsWhenSessionOpened = false;
	           
	              
	        }
	    }

	  
	  
	  private static Session openActiveSession(Activity activity, boolean allowLoginUI, StatusCallback callback, List<String> permissions) {
	        OpenRequest openRequest = new OpenRequest(activity).setPermissions(permissions).setCallback(callback);
	        Session session = new Builder(activity).build();
	        if (SessionState.CREATED_TOKEN_LOADED.equals(session.getState()) || allowLoginUI) {
	            Session.setActiveSession(session);
	            session.openForRead(openRequest);
	            return session;
	        }
	        return null;
	    }
	    
	    
	  private void onClickPickFriends() {
	    
		  
		  Toast.makeText(ctx, "Fetching friends and plotting them....", Toast.LENGTH_LONG).show();
		  final Session session = Session.openActiveSessionFromCache(getApplicationContext());
		  
		  
		
		  Request.executeMyFriendsRequestAsync(session, new GraphUserListCallback() {
				@Override
			public void onCompleted(List<GraphUser> users, Response response) {
			
			if(response.getError()==null)
			{
				String id="";
				   for (int i = 0; i < users.size(); i++) {
				   id=id+users.get(i).getId();
				   if(i!=users.size()-1)
				     id=id+",";
				   }
				   getlocations(id,session);
				   
			}
			else
			{
				Toast.makeText(ctx, response.getError().getErrorMessage(), Toast.LENGTH_SHORT).show();
			}
			 }
		});
		  
		  
	    }

	    
	 
	    
	    private boolean isNetworkAvailable() {
	        ConnectivityManager connectivityManager 
	              = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
	        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
	        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
	    }
	

	    void getlocations(String id,Session session)
	    {
	    	 
			  final ArrayList<User> users_list = new ArrayList<User>();
			  String fqlQuery = "SELECT name,uid,current_location FROM user WHERE uid in ("+id+")";
		    		
		    		Bundle params = new Bundle();
		    		params.putString("q", fqlQuery);

		    		Request request = new Request(session, 
		    		    "/fql", 
		    		    params, 
		    		    HttpMethod.GET, 
		    		    new Request.Callback(){ 
		    		        public void onCompleted(final Response response_from_fb) {
		    		  
		    		        	
		    		        	
		    					new AsyncTask<String[], Long, Long>(){
		    			 			
		    			   		 @Override
		    			   		protected Long doInBackground(String[]... params) {
		    			   		
		    			   			
		    			   		GraphObject obj = response_from_fb.getGraphObject();
		    			   		if(obj == null) 
		    			   		{
		    			   			Log.d("LOL","Its null!!");
		    			   			cancel(true);
		    			   		}
		    			   //	Log.d("LOL",obj.getInnerJSONObject()+"");
		    		        	
		    		        	JSONObject json2;
		    		        	JSONArray data = null;	
		    		        	JSONObject rec2,rec3;
		    		        	String response;
		    		        	try {
		    		        		json2=  obj.getInnerJSONObject();
		    		        		data= json2.getJSONArray("data");
		    		        		
		    		        		for(int j=0;j<data.length();j++)
		    		        		{
		    		        				rec2 = data.getJSONObject(j);
		    		        				//rec3=
		    		        				String uid=rec2.getString("uid");
		    		        				String nam = rec2.getString("name");
		    		        				if(!rec2.isNull("current_location"))
		    		        				{
		    		        				rec3=rec2.getJSONObject("current_location");
		    		        				//Log.d("LOL",uid+" "+rec3.getDouble("latitude")+" "+rec3.getDouble("longitude"));
		    		        				User user = new User();
		    		        				user.setUid(uid);
		    		        				user.setLatitude(rec3.getDouble("latitude"));
		    		        				user.setLongitude(rec3.getDouble("longitude"));
		    		        				user.setName(nam);
		    		        				user.setImg("http://graph.facebook.com/"+uid+"/picture?type=square");
		    		        				users_list.add(user);
		    		        				}
	 	    		        			
		    		        		}
		    		     
		    		     
					} catch (FacebookError e) {
						// TODO Auto-generated catch block
						//Log.d("LOL","SOME ERROR1");
						e.printStackTrace();
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						//Log.d("LOL","SOME ERROR2");
						e.printStackTrace();
					}
		    		        	catch(Exception e)
		    		        	{
		    		        		//Log.d("LOL",e+"");
		    		        		
		    		        	}
		    		
		    			   			return null;
		    			   				}
		    			   			
		    			   		 protected void onPreExecute() {
		    			   					
		    			   			
		    			   				}
		    			                   
		    			   				@Override
		    			   		        public void onProgressUpdate(Long... value) {
		    			   		            
		    			   		        }
		    			   				@Override
		    			   				protected void onPostExecute(Long result){
		    			   		
		    			   						displayLocOnMap(users_list);
		    			   				}
		    			   		
		    			        	}.execute();
		    		      
		    		        }
		    		        
		    		        
		    		        
		    		        
		    		   
		    		});
		    		Request.executeBatchAsync(request);
			  
	    }
	    
	    
	    
	    
	    void showMarker(User user)
	    {
	    	
	    	final String name = user.getName();
	    	final double lat = user.getLatitude();
	    	final double lon = user.getLongitude(); 
	    	
	    	LatLong latlong = new LatLong(lat, lon);
	    	double offset = 0.00002f;
	    	
	    	while(seen.contains(latlong))
	    	{
	    		latlong = new LatLong(lat+offset, lon+offset);
	    		offset+=0.00002f;
	    		Log.d("LOL",offset+"");
	    	}
	    	
	    	seen.add(latlong);	    	
	    	
	    	
	    	AQuery androidAQuery=new AQuery(this);
	    	
	    	//AQuery ajax = androidAQuery.ajax("http://1.gravatar.com/avatar/17b0abd9e24dd27c85bf13b0cc8de494?s=96&d=http%3A%2F%2F1.gravatar.com%2Favatar%2Fad516503a11cd5ca435acc9bb6523536%3Fs%3D96&r=PG",Bitmap.class,0,new AjaxCallback<Bitmap>(){
	    	AQuery ajax = androidAQuery.ajax(user.getImg(),Bitmap.class,0,new AjaxCallback<Bitmap>(){
                @Override
                public void callback(String url, Bitmap object, AjaxStatus status) {
                    super.callback(url, object, status);

                    //You will get Bitmap from object.
                    
                	Bitmap.Config conf = Bitmap.Config.ARGB_8888;
        	    	Bitmap bmp = Bitmap.createBitmap(80, 80, conf);
        	    	Canvas canvas1 = new Canvas(bmp);

        	    	// paint defines the text color,
        	    	// stroke width, size
        	    	Paint color = new Paint();
        	    	color.setTextSize(20);
        	    	color.setColor(Color.BLACK);

        	    	//modify canvas
        	    	canvas1.drawBitmap(object, 0,0, color);
        	   

        	    	//add marker to Map
        	    	googleMap.addMarker(new MarkerOptions().position(new LatLng(lat,lon))
        	    			.title(name)
        	    	    .icon(BitmapDescriptorFactory.fromBitmap(bmp))
        	    	    // Specifies the anchor to be at a particular point in the marker image.
        	    	    .anchor(0.5f, 1));
                    
                    
                    
                    
                    
                    
                }

            });
	    	
	    	
	    	
	    	
	    	
	    	
	    	
	    	 
	    
	    	
	    	
	    	
	    	
	    	
	    	/*
	    	// create marker
			 MarkerOptions marker;
			 marker =  new MarkerOptions().position(new LatLng(lat,lon))
						.title(name);
			 // adding marker
			 googleMap.addMarker(marker);*/
	    }
	    
	    void displayLocOnMap(ArrayList<User> users_list)
	    {
	    	
	    	for(int i=0;i<users_list.size();i++)
	    	{
	    		//Log.d("LOL",users_list.get(i).getLatitude()+" "+users_list.get(i).getLongitude());
	    		showMarker(users_list.get(i));
	    			
	    		
	    	}
	    }
	    
	    
	    class LatLong
	    {
	    	public double latitude;
	    	public double longitude;
	    	public LatLong(double lat,double lon)
	    	{
	    		this.latitude=lat;
	    		this.longitude=lon;
	    	}
	    }
	    
	    
}



