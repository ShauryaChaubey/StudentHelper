package com.mtah.summerizer;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.Bundle;
import com.mtah.summerizer.db.SummaryDBHelper;
import com.mtah.tools.Grapher;
import com.mtah.tools.PreProcessor;

import java.util.ArrayList;

public class SummaryActivity extends AppCompatActivity implements SaveDialog.SaveDialogListener {
    private static final String TAG = "SummaryActivity";
    private final PreProcessor preProcessor = HomeActivity.preProcessor;
    private final Grapher grapher = HomeActivity.grapher;
    private Button saveSummaryButton;
    private String summaryText;
    private SummaryDBHelper dbHelper;
    public SQLiteDatabase summaryDatabase;
    private Intent textIntent;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_summary);

        dbHelper = new SummaryDBHelper(this);

        saveSummaryButton = findViewById(R.id.saveButton);
        saveSummaryButton.setEnabled(false);
        saveSummaryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openSaveDialog();

            }
        });

        TextView summaryTextView = findViewById(R.id.summaryTextView);
        textIntent = getIntent();
        Log.i(TAG, textIntent.toString());
        if (textIntent.hasExtra("docText")) {
            String documentText = textIntent.getStringExtra("docText");
            Log.i(TAG, "onCreate: DOC TEXT:" + documentText);

            String EMPTY_MESSAGE = "Summary not available";
            if (documentText == null || documentText.isEmpty()) {
                Toast.makeText(this, "No document text available", Toast.LENGTH_SHORT).show();
                summaryTextView.setText(EMPTY_MESSAGE);
                saveSummaryButton.setEnabled(false);
            } else {
                summaryText = summaryTool(documentText).replaceAll("    ", " ");

                Log.i("summary Text", summaryText);
            }
            if (summaryText == null || summaryText.isEmpty()) {
                summaryTextView.setText(EMPTY_MESSAGE);
                saveSummaryButton.setEnabled(false);
            } else {
                saveSummaryButton.setEnabled(true);
            }
        } else if (textIntent.hasExtra("open")){
            summaryText = textIntent.getStringExtra("open");
        }
        summaryTextView.setText(summaryText);
        summaryTextView.setMovementMethod(new ScrollingMovementMethod());

    }


    //summarize the text to 35 % of the original text size
    private String summaryTool(String documentText) {
        StringBuilder text = new StringBuilder();
        text.setLength(0);
        Log.i("documentText: ", documentText);
        String[] sentences = preProcessor.extractSentences(documentText.trim());
        Log.i("Sentence", sentences[0]);
        Log.i(TAG, "summarizedDocument: No of sentences: " + sentences.length);
        ArrayList<Grapher.SentenceVertex> sentenceVertices = grapher.sortSentences(sentences, preProcessor.tokenizeSentences(sentences));
        int summarySize = ((sentences.length * 35) / 100);
        int counter = 0;
        for (int i = 0; i < summarySize; i++) {
                text.append(sentenceVertices.get(i).getSentence().trim());
                text.append(" ");
                counter++;
        }
        Log.i(TAG, "summarizedDocument: Summary length = " + counter + " sentences");
        Log.i(TAG, "\nSUMMARY:\n" + text.toString());
        return text.toString();
    }

    //Dialog for summary name input for saving the summary
    private void openSaveDialog(){
        if (textIntent.hasExtra("open")){
            Toast.makeText(this, "This has already been saved", Toast.LENGTH_SHORT).show();
        }
        SaveDialog saveDialog = new SaveDialog();
        saveDialog.show(getSupportFragmentManager(), "save dialog");
    }

    @Override
    public void applyText(String name) {
        //summary save name
        Log.i(TAG, "applyText: Save name: " + name);

        Log.i(TAG, "onClick: GOT HERE, SAVING");
        if (name != null && !name.isEmpty()) {
            try {
                saveSummary(name, summaryText);
                saveSummaryButton.setEnabled(false);
                Log.i(TAG, "onClick: Save successful");
                Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Log.e(TAG, "applyText: ", e);
                Toast.makeText(this, "Could not save summary", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(SummaryActivity.this, "Try again, Enter save name", Toast.LENGTH_SHORT).show();
        }
    }

    // save a summary to database
    private void saveSummary (String summaryName, String summaryText) throws Exception{
        dbHelper.saveSummary(summaryName, summaryText);
        Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
    }

    // share summary on other apps
    public void shareSummary(View view) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, summaryText);
        Intent chooser = Intent.createChooser(shareIntent, "Share");
        startActivity(chooser);
    }

    // copy the summary to clipboard
    public void copySummary(View view) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("summary of text", summaryText);
        Toast.makeText(SummaryActivity.this, "Summary copied!", Toast.LENGTH_SHORT).show();
        clipboard.setPrimaryClip(clip);
    }

    public  void goBack(View view)
    {
        finish();
    }
}
