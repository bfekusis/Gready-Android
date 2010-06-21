package com.caspergasper.android.goodreads;

import static com.caspergasper.android.goodreads.OAuth_interface.CALLBACK_URL;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;


public class SettingsActivity extends Activity {	
	
	private GoodReadsApp myApp;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.userprefs);
        myApp = GoodReadsApp.getInstance();
	}
	
	public void onResume() {
		super.onResume();
		
		Uri uri = this.getIntent().getData();  
		if (uri != null && uri.toString().startsWith(CALLBACK_URL)) {  
			// Callback from getAuthorization() 
//		    Log.d(TAG, "looks like it works! " + uri.toString());
			String verifier = uri.getQueryParameter(OAuth_interface.OAUTH_VERIFIER);
			if(myApp.oauth.getAccessToken(verifier)) {
				TextView fl = (TextView) findViewById(R.id.enterid_feedback_label);
				fl.setText(R.string.auth_successful);
				myApp.oauth.goodreads_url = OAuth_interface.GET_USER_ID;
				myApp.oauth.getXMLFile();
				startActivity(new Intent(SettingsActivity.this, GoodreadsActivity.class));
			} else {
				showErrorDialog();
				return;
			}
		} 
		if(myApp.accessToken == null || myApp.accessTokenSecret == null) {
			getAuthorization();
		}
	}
	
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.startmenu, menu);
//        MenuItem item = menu.findItem(R.id.identifyuser);
//        item.setIntent(new Intent(SettingsActivity.this, GoodreadsActivity.class));
        MenuItem item = menu.findItem(R.id.updates);
        item.setIntent(new Intent(SettingsActivity.this, GoodreadsActivity.class));
        
        return true;
    }
		
	private void getAuthorization() {
    	String url = myApp.oauth.getRequestToken();
    	if(url != null){
    		Uri uri = Uri.parse(url);
    		Intent intent = new Intent(Intent.ACTION_VIEW, uri);
    		startActivity(intent);
    	} else {
    		// getRequestToken failed!
    		showErrorDialog();
    	}
	}

	private void showErrorDialog() {
		AlertDialog.Builder ad = new AlertDialog.Builder(SettingsActivity.this);
		ad.setTitle("ERROR!");
		ad.setMessage(myApp.errMessage);
		ad.setPositiveButton("OK", new OnClickListener() {
			public void onClick(DialogInterface dialog, int arg1) {
				// do nothing
			}
		});
		ad.show();
	}
}