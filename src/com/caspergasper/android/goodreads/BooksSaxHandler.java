package com.caspergasper.android.goodreads;

import static com.caspergasper.android.goodreads.GoodReadsApp.TAG;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.util.Log;


class BooksSaxHandler extends DefaultHandler {
    private StringBuilder builder;
    private final static String NAME = "name";
    private final static String TITLE = "title";
    private final static String DESCRIPTION = "description";
    private final static String REVIEWS = "reviews";
    private final static String START = "start";
    private final static String END = "end";
    private final static String TOTAL = "total";
    private final static String AVERAGE_RATING = "average_rating";
    private final static String LINK = "link";
    private final static String SMALL_IMAGE_URL = "small_image_url";
    private final static String AUTHORS = "authors";
    
    private UserData userdata;
    private boolean inAuthors = false;
    private static final int url_length = GoodreadsActivity.GOODREADS_IMG_URL.length(); 
    
    BooksSaxHandler(UserData ud) {
    	userdata = ud;
    	builder = new StringBuilder();
    }
    
    @Override
    public void characters(char[] ch, int start, int length)
            throws SAXException {
        super.characters(ch, start, length);
        builder.append(ch, start, length);
    }

    @Override
    public void endElement(String uri, String localName, String name)
            throws SAXException {
    	super.endElement(uri, localName, name);	
        if(localName.equalsIgnoreCase(TITLE)) {
        	userdata.tempBooks.add(new Book(builder.toString().trim()));
        } else if(localName.equalsIgnoreCase(DESCRIPTION)) {
    		userdata.tempBooks.get(userdata.tempBooks.size() - 1).description 
    		= builder.toString().trim().replaceAll("&lt;/?div&gt;", "");
        } else if(localName.equalsIgnoreCase(LINK)) { 
        	if(userdata.tempBooks.get(userdata.tempBooks.size() - 1).bookLink == null) {
        		userdata.tempBooks.get(userdata.tempBooks.size() - 1).setBookLink(builder.toString().trim());
        	}
        } else if(localName.equalsIgnoreCase(NAME)) {
        	userdata.tempBooks.get(userdata.tempBooks.size() -1).author += 
        		builder.toString().trim() + " ";
        } else if(localName.equalsIgnoreCase(AVERAGE_RATING)) {
        	userdata.tempBooks.get(userdata.tempBooks.size() - 1).average_rating = 
        		builder.toString().trim();
        } else if(localName.equalsIgnoreCase(SMALL_IMAGE_URL)) {
        	if(!inAuthors) {
        		String url = builder.toString().trim();
        		if(url.substring(0, url_length).compareTo(GoodreadsActivity.GOODREADS_IMG_URL) == 0) {
        			userdata.tempBooks.get(userdata.tempBooks.size() - 1).small_image_url = 
        				url.substring(url_length);
//        			Log.d(TAG, "small_image_url:" +
//        					userdata.tempBooks.get(userdata.tempBooks.size() - 1).small_image_url);
        		}
        	}
        } else if(localName.equalsIgnoreCase(AUTHORS)) {
        	inAuthors = false;
        } else {
//            	Log.d(TAG, "tag: " + localName);
//            	Log.d(TAG, "value: " + builder.toString().trim());
        }
        builder.setLength(0);
    }

    @Override
    public void startDocument() throws SAXException {
        super.startDocument();
    }

    @Override
    public void startElement(String uri, String localName, String name,
            Attributes attributes) throws SAXException {
    	super.startElement(uri, localName, name, attributes);
//       Log.d(TAG, "Start tag: " + localName);
        if(localName.equalsIgnoreCase(REVIEWS)){
        	userdata.startBook = Integer.parseInt(attributes.getValue(START));
        	userdata.endBook = Integer.parseInt(attributes.getValue(END));
        	userdata.totalBooks = Integer.parseInt(attributes.getValue(TOTAL));
        } if(localName.equalsIgnoreCase(AUTHORS)){
        	inAuthors = true;
        }
    }

}
