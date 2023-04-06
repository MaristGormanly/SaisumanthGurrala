package com.example.brickbreakergame;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class MainActivity extends AppCompatActivity {

    private LinearLayout panel;
    private TextView heading;
    private Button highScoresButton, playButton, backButton;
    private EditText nameField;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI elements
        panel = findViewById(R.id.panel);
        heading = findViewById(R.id.heading);
        highScoresButton = findViewById(R.id.highScoresButton);
        playButton = findViewById(R.id.playButton);
        nameField = findViewById(R.id.nameField);
        TextView nameFieldLabel = findViewById(R.id.nameFieldLabel);
        backButton = findViewById(R.id.backButton);

        final int[] sensitivity = {0};
        Spinner spinner = (Spinner) findViewById(R.id.spinner);
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.planets_array, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // An item was selected. You can retrieve the selected item using
                // parent.getItemAtPosition(pos)
                switch (position){
                    case 0: {
                        sensitivity[0] = GameActivity.AccelerometerSensitivity.VERY_LOW;
                        break;
                    }case 1: {
                        sensitivity[0] = GameActivity.AccelerometerSensitivity.LOW;
                        break;
                    }case 2: {
                        sensitivity[0] = GameActivity.AccelerometerSensitivity.MEDIUM_SLOW;
                        break;
                    }case 3: {
                        sensitivity[0] = GameActivity.AccelerometerSensitivity.MEDIUM;
                        break;
                    }case 4: {
                        sensitivity[0] = GameActivity.AccelerometerSensitivity.MEDIUM_FAST;
                        break;
                    }case 5: {
                        sensitivity[0] = GameActivity.AccelerometerSensitivity.HIGH;
                        break;
                    }case 6: {
                        sensitivity[0] = GameActivity.AccelerometerSensitivity.EXTREME;
                        break;
                    } default: throw new RuntimeException("Error Key");
                }
                // String itemAtPosition = (String) parent.getItemAtPosition(position);
                // Toast.makeText(MainActivity.this, itemAtPosition + " " + sensitivity[0],
                // Toast.LENGTH_LONG).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        spinner.setSelection(3);


        SharedPreferences sharedPref = this.getSharedPreferences(
                getString(R.string.preference_file_key), Context.MODE_PRIVATE);

        final CheckBox checkBox = findViewById(R.id.checkBox);
        checkBox.setChecked(true);

        // Set the heading text style
        heading.setTextSize(40);
        heading.setTextColor(Color.BLACK);
        heading.setGravity(Gravity.CENTER);

        // Set the button texts
        highScoresButton.setText("HighScores");
        playButton.setText("Play");
        backButton.setText("Back");

        // Set button click listeners
        highScoresButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nameField.setVisibility(View.GONE);
                checkBox.setVisibility(View.GONE);
                nameFieldLabel.setVisibility(View.GONE);
                showHighScores();
            }
        });

        final boolean[] initial = {true};
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Set the nameField visibility to VISIBLE
                nameField.setVisibility(View.VISIBLE);
                nameFieldLabel.setVisibility(View.VISIBLE);
                checkBox.setVisibility(View.VISIBLE);
                backButton.setVisibility(View.VISIBLE);
                highScoresButton.setVisibility(View.GONE);
                String name = nameField.getText().toString().trim();

                final String key_name = "name";
                if (name.isEmpty()) {
                    if( checkBox.isChecked()){
                        name = sharedPref.getString(key_name, "");
                    }
                } else {
                    if( checkBox.isChecked()){
                        SharedPreferences.Editor editor = sharedPref.edit();
                        editor.putString(key_name, name);
                        editor.apply();
                    }
                }

                if (initial[0]) {
                    initial[0] = false;
                } else {
                    if ((name.isEmpty() || name.matches("^\\d+$"))) {
                        Toast.makeText(MainActivity.this,
                                "Please enter a valid name (numbers not allowed).",
                                Toast.LENGTH_LONG).show();
                    } else {
                        // Start the gameplay with the entered name
                        Intent intent = new Intent(MainActivity.this, GameActivity.class);
                        intent.putExtra("name", name);
                        intent.putExtra("sensitivity", sensitivity[0]);
                        startActivity(intent);
                    }
                }
            }
        });

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                backButton.setVisibility(View.GONE);
                nameField.setVisibility(View.GONE);
                checkBox.setVisibility(View.GONE);
                nameFieldLabel.setVisibility(View.GONE);
                nameField.setText("");
                highScoresButton.setVisibility(View.VISIBLE);
                playButton.setVisibility(View.VISIBLE);
                initial[0] = true;
            }
        });

        // Hide the nameField initially
        nameField.setVisibility(View.GONE);
        checkBox.setVisibility(View.GONE);
        nameFieldLabel.setVisibility(View.GONE);
        backButton.setVisibility(View.GONE);
    }

    private void showHighScores() {
        Intent intent = new Intent(this, HighScoresActivity.class);

        // Pass the high scores data to the intent as a string array
        ArrayList<String[]> highScores = new ArrayList<>();
        // Check if file exists
        File file = new File(getFilesDir(), "hsfile.bb");
        if (!file.exists()) {
            // If file does not exist, add a "Game was never played" message to high scores list
            highScores.add(new String[]{"Game was never played", ""});
        } else {
            // Load high scores from file
            try {
                BufferedReader reader = new BufferedReader(new FileReader(file));
                String line = reader.readLine();
                while (line != null) {
                    String[] score = line.split(":");
                    highScores.add(score);
                    line = reader.readLine();
                }
                reader.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        // Sort the high scores by score descending
        highScores.sort(new Comparator<String[]>() {
            public int compare(String[] o1, String[] o2) {
                return Integer.parseInt(o2[1]) - Integer.parseInt(o1[1]);
            }
        });

        // Get the top ten high scores
        int numHighScoresToShow = Math.min(10, highScores.size());
        ArrayList<String[]> topHighScores = new ArrayList<>(highScores.subList(0, numHighScoresToShow));

        String[][] highScoresArray = new String[topHighScores.size()][2];
        for (int i = 0; i < topHighScores.size(); i++) {
            highScoresArray[i][0] = topHighScores.get(i)[0];
            highScoresArray[i][1] = topHighScores.get(i)[1];
        }
        intent.putExtra("highScores", highScoresArray);

        // Start the new activity
        startActivity(intent);
    }


}
