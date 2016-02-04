package com.mycompany.myvoiceapp;

// Some references
// http://code.tutsplus.com/tutorials/android-sdk-build-a-speak-and-repeat-app--mobile-11197
// https://github.com/SueSmith/android-speak-repeat
// http://code4reference.com/2012/07/tutorial-android-voice-recognition/
//
// List View
// http://www.vogella.com/tutorials/AndroidListView/article.html

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.speech.RecognizerIntent;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MyActivity extends AppCompatActivity {

    public final static String EXTRA_MESSAGE = "com.mycompany.myfirstapp.MESSAGE";

    //---------------------------------------------------
    // voice recognition variables
    private static final int VR_REQUEST = 999;
    // ListView for displaying suggested words
    private ListView wordList;
    // List of confidence scores associated with string
    ArrayList <Float> confidenceASR;
    ArrayList <String> wordListASR;
    //Log tag for output information
    private final String LOG_TAG = "SpeechRepeatActivity";//***enter your own tag here***

    //Edit
    EditText selectedVoiceInput;

    //TTS variables
    //variable for checking TTS engine data on user device
    private int MY_DATA_CHECK_CODE = 0;
    //Text To Speech instance
    private TextToSpeech repeatTTS;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);
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

        // gain reference to speak button
        ImageButton speechBtn = (ImageButton) findViewById(R.id.button_voice_input);
        // gain reference to word list
        wordList = (ListView) findViewById(R.id.word_list);
        // gain reference to selected item from
        selectedVoiceInput = (EditText) findViewById(R.id.edit_message);
        // confidence values associated with each item in the wordlist
        confidenceASR = new ArrayList<Float>();
        wordListASR = new ArrayList<String>();

        //----------------------------------------------------------------
        // SPEECH RECOGNITION
        //find out whether speech recognition is supported
        PackageManager packManager = getPackageManager();
        final List<ResolveInfo> intActivities = packManager.queryIntentActivities(new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0);
        if (intActivities.size() != 0) {
            //speech recognition is supported - detect user button clicks
            speechBtn.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    //start the speech recognition intent passing required data
                    Intent listenIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                    //indicate package
                    listenIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getClass().getPackage().getName());
                    //message to display while listening (optional extra prompt)
                    //listenIntent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak Now!");
                    //set speech model
                    listenIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                    //specify number of results to retrieve
                    listenIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 10);
                    //get confidence scores too??
                    //listenIntent.putExtra(RecognizerIntent.EXTRA_CONFIDENCE_SCORES, 10);
                    //start listening
                    startActivityForResult(listenIntent, VR_REQUEST);
                }
            });
        }
        else
        {
            //speech recognition not supported, disable button and output message
            speechBtn.setEnabled(false);
            Toast.makeText(this, "Oops - Speech recognition not supported!", Toast.LENGTH_LONG).show();
        }

        //----------------------------------------------------------------
        // SPEECH RECOGNITION - detect when user selects an alternate suggested word
        wordList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            //click listener for items within list
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                //cast the view
                TextView wordView = (TextView)view;
                //retrieve the chosen word .. but don't take it from the GUI (because it has the confidence rating appended)
                //String wordChosen = (String) wordView.getText();
                String wordChosen =  wordListASR.get((int)id);

                //output for debugging
                Log.v(LOG_TAG, "chosen: " + wordChosen);
                //output Toast message
                Toast.makeText(MyActivity.this, "You selected: "+wordChosen, Toast.LENGTH_SHORT).show();

                // copy the selected word
                selectedVoiceInput.setText(wordChosen);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_my, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    //----------------------------------------------------------------
    // MUSIC SEARCH - Called when the user clicks the Search
    public void sendMessage(View view) {
        // Do something in response to button
        Intent intent = new Intent(this, DisplayMessageActivity.class);
        EditText editText = (EditText) findViewById(R.id.edit_message);
        String message = editText.getText().toString();
        intent.putExtra(EXTRA_MESSAGE, message);
        startActivity(intent);
    }

    //----------------------------------------------------------------
    /**
     * onActivityResults handles:
     *  - retrieving results of speech recognition listening
     *  - retrieving result of TTS data check
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //check speech recognition result
        if (requestCode == VR_REQUEST && resultCode == RESULT_OK)
        {
            //store the returned word list as an ArrayList
            ArrayList<String> suggestedWords = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            // extract confidence scores
            float[] confidence = data.getFloatArrayExtra(RecognizerIntent.EXTRA_CONFIDENCE_SCORES);
            confidenceASR.clear();
            wordListASR.clear();

            // process confidence scores
            if (suggestedWords.size() > 0) {
                // copy the selected word (the 1st item in the list)
                String wordChosen = (String) suggestedWords.get(0);
                selectedVoiceInput.setText(wordChosen);

                // save confidence scores
                if (confidence == null)
                {
                    for (int i = 0; i < suggestedWords.size(); i++)
                    {
                        Log.d(LOG_TAG, i + ": " + suggestedWords.get(i));
                        confidenceASR.add(0.0f);
                        wordListASR.add(suggestedWords.get(i));
                    }
                }
                else
                {
                    for (int i = 0; i < suggestedWords.size(); i++)

                    {
                        Log.d(LOG_TAG, i + ": " + suggestedWords.get(i) + " confidence : "  + confidence[i]);
                        confidenceASR.add(confidence[i]);
                        wordListASR.add(suggestedWords.get(i));
                        // add the confidence scores to the suggestions
                        suggestedWords.set(i, suggestedWords.get(i) + " (" + Float.toString(confidence[i]) + ")");
                    }
                }

            }

            //set the retrieved list to display in the ListView using an ArrayAdapter
            wordList.setAdapter(new ArrayAdapter<String>(this, R.layout.support_simple_spinner_dropdown_item, suggestedWords));

        }

        //tss code here

        //call superclass method
        super.onActivityResult(requestCode, resultCode, data);
    }

}
