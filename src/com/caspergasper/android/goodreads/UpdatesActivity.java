package com.caspergasper.android.goodreads;


import static com.caspergasper.android.goodreads.GoodReadsApp.TAG;

import java.net.URL;
import java.util.List;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;


public class UpdatesActivity extends Activity implements OnItemClickListener, OnItemLongClickListener {
	
	private GoodReadsApp myApp;
	private ListView updatesListView;
	private static final String M_USER = "m/user/";
	private static final String REVIEWS = "/reviews";
	// Need handler for callbacks to the UI thread
    final Handler mHandler = new Handler();
    UpdateAdapter updateAdapter;
	
	@Override
	public void onResume() {
		myApp.goodreads_activity = this;
		try {
			super.onResume();
			if (myApp.accessToken == null  || myApp.accessTokenSecret == null) {
	        	Log.d(TAG, "Missing accessTokens, retrieving...");
	        	startActivity(new Intent(UpdatesActivity.this, OAuthCallbackActivity.class));
	        	return;
	        } 
			if(myApp.userID == 0  && !myApp.threadLock) {
				myApp.oauth.getXMLFile(myApp.xmlPage, OAuthInterface.GET_USER_ID);
				return;     
			} else {
				if(myApp.userData.updates.size() == 0 && !myApp.threadLock) {
					// Got valid tokens and a userid, let's go get some data...
					Log.d(TAG, "Getting updates now...");
					myApp.xmlPage = 1;
					myApp.oauth.getXMLFile(myApp.xmlPage, OAuthInterface.GET_FRIEND_UPDATES);
				} 
			}
		} catch(Exception e) {
			myApp.errMessage = "UpdatesActivity onResume " + e.toString();
			myApp.showErrorDialog(this);
		}
	}
	
	@Override
	public void onPause() {
		super.onPause();
	}
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        try{
	    	super.onCreate(savedInstanceState);
	        setContentView(R.layout.updates);
	        myApp = GoodReadsApp.getInstance();
	        if(myApp.userData.shelfToGet.compareToIgnoreCase("Updates") != 0) {
	        	startActivity(new Intent(UpdatesActivity.this, BooksActivity.class));
				finish();
	        }
	        myApp.goodreads_activity = this;
	        updatesListView = (ListView) findViewById(R.id.updates_listview);
	        newQuery();  // For when activity has been cleared from memory but app hasn't
    	} catch(Exception e) {
			myApp.errMessage = "UpdatesActivity onCreate " + e.toString();
			myApp.showErrorDialog(this);
		}
    }
       
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
    	super.onConfigurationChanged(newConfig);
    	// I'm supposed to re-initialize views here when screen is rotated, 
    	// but so far I haven't found I need to.
    }
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.updates_menu, menu);
        myApp.sub = menu.addSubMenu(0, 0, Menu.NONE, R.string.bookshelves_label);
        myApp.sub.setHeaderIcon(android.R.drawable.ic_menu_view);
        myApp.sub.setIcon(android.R.drawable.ic_menu_view);
    	List<Shelf> tempShelves = myApp.userData.shelves;
        if(tempShelves.size() == 0 || myApp.userData.endShelf < myApp.userData.totalShelves) {
        	toastMe(R.string.build_menu);
			return false;
        } else {
        	myApp.createShelvesMenu(tempShelves);
        }
        return true;
    }
		
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);
		if(myApp.threadLock) {
			return false;
		}
		int itemId = item.getItemId();
		if(item.getGroupId() == GoodReadsApp.SUBMENU_GROUPID) {
			if(myApp.handleBookshelfSelection(this, itemId)) {
				startActivity(new Intent(UpdatesActivity.this, BooksActivity.class));
				finish();
			}
			return true;
		} else if(itemId == R.id.updates) {
				showUpdateMessage(R.string.getUpdates);
				myApp.oauth.getXMLFile(myApp.xmlPage, OAuthInterface.GET_FRIEND_UPDATES);
				return true;
		} else if(itemId == R.id.update_status) {
			myApp.showUpdateDialog(this);
			myApp.userData.shelfToGet = GoodReadsApp.CURRENTLY_READING;
			myApp.xmlPage = 1;
			myApp.oauth.getXMLFile(myApp.xmlPage, OAuthInterface.GET_SHELF_FOR_UPDATE);
			return true;
		} else if(itemId == R.id.search || itemId == R.id.scanbook) {
			myApp.menuItem = item;
			myApp.getImageThreadRunning = false;	// Shouldn't be needed
			startActivity(new Intent(UpdatesActivity.this, BooksActivity.class));
			finish();
			return true;
		} else if(itemId == R.id.preferences) {
			startActivity(new Intent(UpdatesActivity.this, Preferences.class));
			return true;
		}
			return false;	
	}
	
	
	@Override
	public void onItemClick(AdapterView<?> _av, View _v, int _index, long arg3) {
		final Update u = myApp.userData.updates.get(_index);
		showUpdateDetail(u);
	}
	
	@Override
	public boolean onItemLongClick(AdapterView<?> _av, View _v, int _index, long arg3) {
		final Update u = myApp.userData.updates.get(_index);
		if(u.updateLink != null) {
			String path = OAuthInterface.URL_ADDRESS +
				M_USER + u.id + REVIEWS + u.updateLink;
			Log.d(TAG, path);
			myApp.gotoWebURL(path, myApp.goodreads_activity);
		} else {
			toastMe(R.string.no_update_page);
		}
		return true;
	}
	
	void showUpdateMessage(int resource) {
		TextView textView = (TextView) findViewById(R.id.status_label);
		textView.setText(resource);
		textView.setVisibility(View.VISIBLE);
	}
	
	
	private final void showUpdateDetail(Update update) {
		Dialog d = myApp.createDialogBox(UpdatesActivity.this, R.layout.update_dialog, true);
		if(update.bitmap != null) {
			((ImageView) d.findViewById(R.id.updateDialogImage)).setImageBitmap(update.bitmap);
		}
		((TextView) d.findViewById(R.id.title)).setText(update.getUpdateText());
		((TextView) d.findViewById(R.id.update)).setText(update.getBody());
		d.show();	
	}
	
	private void newQuery() {
		myApp.xmlPage = 1;
	}
	
    void updateMainScreenForUser(int result) {
    	Log.d(TAG, "updatesMainScreenForUser");
    	UserData ud = myApp.userData;
    	
    	if(result == 1) {
			myApp.showErrorDialog(this);
			return;
		} else if(result == 2) {
			myApp.showGetAuthorizationDialog(this);
			return;
		}
    	try {
    	switch (myApp.oauth.goodreads_url) {
		case OAuthInterface.GET_FRIEND_UPDATES:
			if(updatesListView == null) {
				Log.d(TAG, "updatesListView is null");
			}
			if(updatesListView.getAdapter() == null && ud.updates.size() != 0) {
				Log.d(TAG, "updatesListViewgetadapter is null");	
			}
			if(updatesListView.getAdapter() == null) {
				updatesListView.setOnItemClickListener(this);
				updatesListView.setOnItemLongClickListener(this);
			    updateAdapter = new UpdateAdapter(this, R.layout.updateitem,
						ud.updates);
			    updatesListView.setAdapter(updateAdapter);
			} else {
				updateAdapter = (UpdateAdapter) updatesListView.getAdapter();
				updateAdapter.clear();
			}
			addUpdatesToListView();
			updatesListView.setVisibility(View.VISIBLE);
			findViewById(R.id.status_label).setVisibility(View.INVISIBLE);
			((TextView) findViewById(R.id.updates_label)).setText(R.string.updates_label);
			if(myApp.userData.shelves.size() == 0) {
				myApp.xmlPage = 1;
				myApp.oauth.getXMLFile(myApp.xmlPage, OAuthInterface.GET_SHELVES);
			} else if(!myApp.gettingShelves) {
				getImages();
			}
			ud.books.clear();
		break;
		case OAuthInterface.GET_USER_ID:
			Log.d(TAG, "Getting friend updates for first time");
			ud.updates.clear();
			myApp.oauth.getXMLFile(myApp.xmlPage, OAuthInterface.GET_FRIEND_UPDATES);
		break;
    	case OAuthInterface.GET_SHELVES:
    		// We need to cater for users with > 100 bookshelves like Cait :-)
    		if(ud.endShelf < ud.totalShelves) {
    			Log.d(TAG, "Getting extra shelf -- total shelves: " + ud.totalShelves);
    			myApp.oauth.getXMLFile(++myApp.xmlPage, OAuthInterface.GET_SHELVES);
    		} else if(myApp.gettingShelves) {
    			myApp.createShelvesMenu(myApp.userData.shelves);
    		} else {
    			getImages();
    		}
    	break;
    	case OAuthInterface.GET_SHELF_FOR_UPDATE:
    		updateDialogRadioGroup();
    	break;
    	}
    	} catch(Exception e) {
			myApp.errMessage = "UpdatesActivity updateMainScreenForUser " + e.toString();
			myApp.showErrorDialog(this);
		}
    }

	void toastMe(int msgid) {
		Toast toast = Toast.makeText(getApplicationContext(), msgid, Toast.LENGTH_SHORT);
		toast.show();
	}
	
	private void addUpdatesToListView() {
		for(Update u : myApp.userData.tempUpdates) {
			updateAdapter.add(u);
		}
		myApp.userData.tempUpdates.clear();	
	}
	
	
    private void getImages() {
        // Fire off a thread to do some work that we shouldn't do directly in the UI thread
        Thread t = new Thread(null, doBackgroundThreadProcessing, "Background");
        myApp.getImageThreadRunning = true;
        t.start();
    }
    
	// Create runnable for posting
    private final Runnable doUpdateGUI = new Runnable() {
        public void run() {
            updateResultsInUi();
        }
    };

    
    private Runnable doBackgroundThreadProcessing = new Runnable() {
    	public void run() {
    		backgroundThreadProcessing();
    	}
    };
    
    private void backgroundThreadProcessing() {
    	Update u;
    	int size = myApp.userData.updates.size();
    	for(int i = 0; i < size; i++) {
    		if(!myApp.getImageThreadRunning) {
    			Log.d(TAG, "stopping getImage thread.");
    			break;
    		}
			u = updateAdapter.getItem(i);
			try {
				if(u.imgUrl == null) {
					continue;
				}
				if(u.bitmap == null) {
					URL newurl = new URL(GoodReadsApp.GOODREADS_IMG_URL + u.imgUrl); 
					Log.d(TAG, "Getting " + GoodReadsApp.GOODREADS_IMG_URL + u.imgUrl);
					u.bitmap = BitmapFactory.decodeStream(newurl.openConnection().getInputStream());
					// Add this image to any further updates by same user
					Update temp;
					for(int j = i+1; j < size; j++) {
						temp = updateAdapter.getItem(j);
						if(temp.id == u.id) {
							temp.bitmap = u.bitmap;
						}
					}
					
				}
				u.imgUrl = null;
				mHandler.post(doUpdateGUI);
			} catch (Exception e) {
				Log.e(TAG, e.toString());
				myApp.errMessage = e.toString() + " " + e.getStackTrace().toString();
				myApp.showErrorDialog(this);
			}
		} // for
    		
    }
    
    private void updateResultsInUi() {
        // Back in the UI thread -- update UI elements
    	updateAdapter.notifyDataSetChanged();
    }
    
	private void updateDialogRadioGroup() {
		// Callback from getting the currently-reading shelf.
		// book ids are stored as the radiobutton ids.
		// For some reason moving this away from the activity failed.
		Log.d(TAG, "in updateDialogRadioGroup");
		RadioGroup group = (RadioGroup) myApp.updateDialog.findViewById(R.id.RadioGroup);
		
		for(Book b : myApp.userData.tempBooks){
			RadioButton button = new RadioButton(this);
			button.setId(b.id);
			button.setText(b.title);
			group.addView(button);	
		}
		myApp.userData.tempBooks.clear();
	}
	
}