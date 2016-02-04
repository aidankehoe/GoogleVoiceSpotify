package com.mycompany.myvoiceapp;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.os.AsyncTask;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

public class DisplayMessageActivity extends AppCompatActivity {

    private static final String TAG = "DisplayMessageActivity";

    String jsonString;
    TextView textViewTrack;
    String trackMatches;

    String previewURL;

    //ListView for displaying suggested words
    ArrayList<String> matchedTracksList;
    ArrayList<String> matchedTracksListPreview;
    private ListView matchedTracksListView;


    MediaPlayer player;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_message);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Emailed result files to akehoe@logitech.com", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // retrieve parameters for activity
        Intent intent = getIntent();
        String message = intent.getStringExtra(MyActivity.EXTRA_MESSAGE);

        // create a list of matched tracks
        matchedTracksList = new ArrayList<String> ();
        matchedTracksListPreview = new ArrayList<String> ();


        // trim spaces
        message = message.trim();
        // replace any space with +
        // https://developer.spotify.com/web-api/search-item/
        message = message.replace(" ", "+");

        // create the search query
        String searchRequest = "https://api.spotify.com/v1/search?q=" + message + "+&type=track";

        Context context = getApplicationContext();
        WebFetcher webFetcher = new WebFetcher();

        if (webFetcher.checkNetworkConnection(context)) {
            message += (" - connected");
            new FetchWebTask().execute(searchRequest);
        }
        else
            message += (" - not connected");



        /*
        // display search query for debug
        TextView textView = new TextView(this);
        textView.setTextSize(20);
        textView.setText(searchRequest);
        RelativeLayout layout = (RelativeLayout) findViewById(R.id.content);
        layout.addView(textView);
        */

        /*
        // dynamically create the track results
        textViewTrack = new TextView(this);
        textViewTrack.setTextSize(20);
        RelativeLayout layout = (RelativeLayout) findViewById(R.id.content);
        layout.addView(textViewTrack);
        textViewTrack.setText(searchRequest);
        */

        //gain reference to word list
        matchedTracksListView = (ListView) findViewById(R.id.track_list);

        //detect user clicks of suggested words
        matchedTracksListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            //click listener for items within list
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                //cast the view
                TextView wordView = (TextView)view;
                //retrieve the chosen word
                String wordChosen = (String) wordView.getText();
                String urlChosen = (String) matchedTracksListPreview.get((int)id);

                //output for debugging
                Log.v(TAG, "chosen: " + wordChosen + ", " + urlChosen + " at id=" + Long.toString(id));
                //output Toast message
                Toast.makeText(DisplayMessageActivity.this, "Selected: " + wordChosen  + ", id=" + Long.toString(id), Toast.LENGTH_SHORT).show();


                // play the new track
                try {
                    if (player != null)
                    {
                        player.stop();
                        player.release();
                        player = null;
                    }
                    player = new MediaPlayer();
                    player.setAudioStreamType(AudioManager.STREAM_MUSIC);
                    player.setDataSource(urlChosen);
                    player.prepare();
                    player.start();
                }
                catch (Exception e) {
                    // TODO: handle exception
                    Log.i(TAG, "Could not play: " + wordChosen + ", " + urlChosen);
                }
            }
        });
    }



    @Override
    protected void onStop() {
        super.onStop();  // Always call the superclass method first

        if (player != null)
        {
            player.release();
            player = null;
        }
    }

    // example
    // http://examples.javacodegeeks.com/android/core/os/asynctask/android-asynctask-example/
    private class FetchWebTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            jsonString = null;
            try {
                Log.i(TAG, "About to get : " + params[0]);
                //String result = new WebFetcher().getUrlString("https://www.google.com");
                jsonString = new WebFetcher().getUrlString(params[0]);
                //String result = new WebFetcher().getUrlString(params[0]);

                Log.i(TAG, "Fetched contents of URL: " + jsonString);
                //Log.i(TAG, "Fetched contents of URL: ");

            } catch (IOException ioe) {
                Log.e(TAG, "Failed to fetch URL: ", ioe);
            }
            return null;
        }


        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {
            //jsonString = "123";

            if (jsonString != null)
            {
                ParseSearchResults();
                //textViewTrack.setText(jsonString);
                //textViewTrack.setText(trackMatches);

                matchedTracksListView.setAdapter(new ArrayAdapter<String>(DisplayMessageActivity.this, R.layout.support_simple_spinner_dropdown_item, matchedTracksList));

                try {
                    if (matchedTracksListPreview.size() > 0) {
                        player = new MediaPlayer();
                        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
                        String firstPreviewURL = (String) matchedTracksListPreview.get(0);
                        Log.i(TAG, " Try to play: " + firstPreviewURL);
                        player.setDataSource(firstPreviewURL);
                        player.prepare();
                        player.start();
                    }
                } catch (Exception e) {
                    // TODO: handle exception
                }
            }
        }
    }

    protected void ParseSearchResults() {
        // parse JSON
        // http://www.tutorialspoint.com/android/android_json_parser.htm
        // http://json.parser.online.fr/

        String eol = System.getProperty("line.separator");
        trackMatches = null;
        previewURL = null;

        try {
                    JSONObject jsonRootObject = new JSONObject(jsonString);

                    Log.i(TAG, " Find: tracks");
                    JSONObject jsonTracks = jsonRootObject.getJSONObject("tracks");

                    //Get the instance of JSONArray that contains JSONObjects
                    Log.i(TAG, " Find: items");
                    JSONArray jsonArray = jsonTracks.optJSONArray("items");

                    //Iterate the jsonArray and print the info of JSONObjects
                    if (jsonArray != null) {
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject jsonObject = jsonArray.getJSONObject(i);
                            // get track name
                            String name = jsonObject.optString("name").toString();
                            Log.i(TAG, Integer.toString(i) + ": name: " + name);
                            trackMatches += name + eol;

                            // get preview url
                            previewURL = jsonObject.optString("preview_url").toString();
                            Log.i(TAG, Integer.toString(i) + ": preview_url: " + previewURL);

                            // save in the list
                            matchedTracksList.add(name);
                            matchedTracksListPreview.add(previewURL);

                        }
                    }

        } catch (JSONException e) {e.printStackTrace();}
    }
}


