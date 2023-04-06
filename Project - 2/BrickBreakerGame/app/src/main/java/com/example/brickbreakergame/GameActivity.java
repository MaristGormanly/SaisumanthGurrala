package com.example.brickbreakergame;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

public class GameActivity extends AppCompatActivity
        implements SurfaceHolder.Callback, Runnable, View.OnTouchListener {
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private Paint paint;
    private boolean play;
    private int score;
    public String name;
    private int totalBricks;
    private Handler handler;
    private int delay;
    private int playerX;
    private int ballPosX;
    private int ballPosY;
    private int ballDirX;
    private int ballDirY;
    private MapGenerator map;

    // Persist the Android Sensor Manager
    private SensorManager sensorManager;

    // Persist the Accelerometer sensor
    private Sensor accelerometer;

    /**
     * Define Common Accelerometer sensitivity values
     */
    final static class AccelerometerSensitivity {
        public final static int VERY_LOW = 5;
        public final static int LOW = 10;
        public final static int MEDIUM_SLOW = 20;
        public final static int MEDIUM = 25;
        public final static int MEDIUM_FAST = 30;
        public final static int HIGH = 40;
        public final static int EXTREME = 50;
    }

    /**
     * Define a utility class to play all audio from <code>raw</code> resource directory
     */
    private static class AudioPlayer {
        private final Context context;
        private final int resId;
        private final LooperThread looperThread;

        private final int PLAY = 0;
        private final int PAUSE = 1;

        /**
         * Create an audio player, to play an audio from the
         * app's <code>raw</code> resource directory
         * @param context the basic android context
         * @param resId the resource id of the raw audio file to be played
         */
        public AudioPlayer(Context context, int resId) {
            this.context = context;
            this.resId = resId;
            // Start a new handler thread to play the audio
            looperThread = new LooperThread();
            // Start a thread and make the thread have a Looper
            looperThread.start();
            synchronized (looperThread){
                try {
                    // Block the current thread until the looper is initialized
                    looperThread.wait(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        /**
         * Create a thread with an android Looper, able to handle {@link Handler} messages
         */
        private class LooperThread extends Thread {
            private Handler mHandler;

            public void run() {
                Looper.prepare();

                mHandler = new Handler(Looper.myLooper()) {
                    MediaPlayer mp;
                    public void handleMessage(Message msg) {
                        // process incoming messages here
                        if (msg.what == PLAY){
                            if (mp == null){
                                mp = MediaPlayer.create(context, resId);
                                if (msg.arg1 == 1){
                                    mp.setLooping(true);
                                    assert mp.isLooping();
                                }
                            }
                            if(!mp.isLooping()) mp.seekTo(0);
                            mp.start();
                        } else if (msg.what == PAUSE){
                            mp.pause();
                        }
                    }
                };
                synchronized (this){
                    // The Looper's HandlerThread is initialized,
                    // we initially kept a thread waiting, release the lock
                    this.notify();
                }
                Looper.loop();
            }

            /**
             * Retrieves the message handler used by this Handler thread
             * @return the message handler used by this Handler thread,
             * or null if this thread has not been started
             */
            public Handler getHandler(){
                return mHandler;
            }
        }

        /**
         * Play the audio
         * @param loop whether to loop the audio
         */
        public void play(boolean loop){
            int arg1;
            if (loop) {
                arg1 = 1;
            }
            else arg1 = 0;
            send(PLAY, arg1);
        }

        /**
         * Play the audio, without looping
         * @see #play(boolean)
         */
        public void play(){
            play(false);
        }

        /**
         * Pause the audio if it is playing
         */
        public void pause(){
            send(PAUSE, 0);
        }

        /**
         * Instead of playing/pausing the audio in the calling thread, play/pause the audio
         * in a specified handler thread
         * @param what whether to play or pause
         * @param arg1 whether to loop the audio, 1 to loop, other to not loop
         */
        private void send(int what, int arg1) {
            Message message = new Message();
            message.what = what;
            message.arg1 = arg1;
            looperThread.getHandler().sendMessage(message);
        }
    }

    // Assign an audio player for the background music
    private AudioPlayer backgroundMusicPlayer;
    // Assign an audio player for the brick crash sound
    private AudioPlayer brickCrashMusicPlayer;
    private AudioPlayer gameOverAudioPlayer;

    /**
     * Set the Accelerometer sensitivity
     */
    private int accSensitivity;

    /**
     * Define an Event Listener for changes in the Accelerometer sensor
     */
    private class AccSensorEventListener implements SensorEventListener {

        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

                // int sy = (int) sensorEvent.values[1];
                int sx = (int) sensorEvent.values[0];
                playerX -= (sx / 10.0) * accSensitivity;

                // Ensure the bar is drawn within the screen
                if (playerX < 0) playerX = 0;
                int max_x = surfaceView.getWidth() - 100;
                if (playerX > max_x) playerX = max_x;
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }
    }

    // Instantiate an Event Listener for changes in the Accelerometer sensor
    private AccSensorEventListener accSensorEventListener;

    public GameActivity() {
        this.play = false;
        this.score = 0;
        this.totalBricks = 48;
        this.playerX = 310;
        this.ballPosX = 120;
        this.ballPosY = 350;
        this.ballDirX = -1;
        this.ballDirY = -2;
    }

    private Drawable ballDrawable;
    private Drawable paddleDrawable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        name = intent.getStringExtra("name");
        { // Initialize the Accelerometer Sensitivity
            int sensitivity = intent.getIntExtra("sensitivity", -1);
            if (sensitivity == -1){
                sensitivity = AccelerometerSensitivity.MEDIUM;
            }
            accSensitivity = sensitivity;
        }
        setContentView(R.layout.activity_gameplay);
        surfaceView = findViewById(R.id.surfaceView);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);
        surfaceView.setOnTouchListener(this);
        paint = new Paint();
        paint.setAntiAlias(true);
        handler = new Handler();
        delay = 8;
        map = new MapGenerator(4, 12);

        {
            Drawable d = ResourcesCompat.getDrawable(getResources(), R.drawable.brick1, null);
            map.setBitmap(d);

            ballDrawable = ResourcesCompat.getDrawable(getResources(), R.drawable.ball, null);
            paddleDrawable = ResourcesCompat.getDrawable(getResources(), R.drawable.paddle, null);
        }

        // Initialize the Android Accelerometer sensor
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        // There may be many Accelerometer sensors, use the default for the device
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        accSensorEventListener = new AccSensorEventListener();
        sensorManager.registerListener(accSensorEventListener, accelerometer,
                SensorManager.SENSOR_DELAY_GAME);

        // Initialize Game Music Players
        backgroundMusicPlayer = new AudioPlayer(this, R.raw.background);
        brickCrashMusicPlayer = new AudioPlayer(this, R.raw.brick_crash);
        gameOverAudioPlayer = new AudioPlayer(this, R.raw.game_over);

    }

    @Override
    protected void onPause() {
        super.onPause();
        // The application is paused, we don't need sensor events; unregister to save resources
        sensorManager.unregisterListener(accSensorEventListener);
        // Pause the background music
        backgroundMusicPlayer.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // The application was initially paused,
        // we may have de-registered our sensor events handler; do register the sensor again
        sensorManager.registerListener(
                accSensorEventListener, accelerometer,
                SensorManager.SENSOR_DELAY_GAME
        );

        // Resume the background music
        backgroundMusicPlayer.play(true);
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        handler.postDelayed(this, delay);
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        handler.removeCallbacks(this);
    }

    @Override
    public void run() {
        update();
        draw();
        handler.postDelayed(this, delay);
    }
    // Radius of the ball
    final int ballRadius = 25;
    final int ballDiameter = 2 * ballRadius;
    private void update() {
        /* x -> Offset of the left wall's location from the View's left
         * y -> Offset of the top wall's location from the View's top
         */
        final Point wallStart = new Point(0, 0);
        if (play) {
            ballPosX += ballDirX * 2;
            ballPosY += ballDirY * 3;

            if (ballPosX <= (wallStart.x)) {
                // The ball hit the left wall, bounce it off to the right
                ballDirX = -ballDirX;
            }
            if (ballPosY < (wallStart.y)) {
                // The ball hit the top wall, bounce it off towards the bottom
                ballDirY = -ballDirY;
            }
            if (ballPosX > surfaceView.getWidth() - ballRadius) {
                ballDirX = -ballDirX;
            }

            int ballCenterX = ballPosX + 10;
            int paddleCenterX = playerX + 50;
            int distanceX = Math.abs(ballCenterX - paddleCenterX);
            if (distanceX <= 60 && ballDirY > 0) {
                int ballCenterY = ballPosY + 10;
                int paddleTopY = surfaceView.getHeight() - 50;
                int distanceY = Math.abs(ballCenterY - paddleTopY);
                if (distanceY <= 10) {
                    ballDirY = -ballDirY;
                }
            }

            if (ballPosY + ballRadius > surfaceView.getHeight() || totalBricks == 0) {
                // Ball has missed the paddle, or all bricks destroyed; game over
                play = false;
                handler.removeCallbacks(this);

                if(totalBricks != 0){
                    Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                    // Vibrate the device for 300 milliseconds
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        v.vibrate(VibrationEffect.createOneShot(
                                300,
                                VibrationEffect.DEFAULT_AMPLITUDE));
                    } else {
                        // Pre-Oreo
                        v.vibrate(300);
                    }
                }
                // Play some sound to signal the game has ended
                gameOverAudioPlayer.play();

                Intent intent = new Intent(GameActivity.this, ResultActivity.class);
                intent.putExtra("name", name);
                intent.putExtra("score", score);
                startActivity(intent);
                saveScore(GameActivity.this, name, score);
                finish();
            }

            loop:
            for (int i = 0; i < map.map.length; i++) {
                for (int j = 0; j < map.map[0].length; j++) {
                    if (map.map[i][j] > 0) {
                        int brickX = j * map.brickWidth + 80;

                        int brickY = i * map.brickHeight + 150;
                        int brickWidth = map.brickWidth;
                        int brickHeight = map.brickHeight;
                        Rect brickRect = new Rect(brickX, brickY, brickX + brickWidth, brickY + brickHeight);
                        Rect ballRect = new Rect(ballPosX, ballPosY, ballPosX + 20, ballPosY + 20);

                        //if (brickRect.intersect(ballRect)) {
                        if (Rect.intersects(brickRect, ballRect)) {
                            map.setBrickValue(0, i, j);
                            totalBricks--;
                            score += 5;

                            // The ball crushed a brick
                            brickCrashMusicPlayer.play();

                            if (ballPosX + 19 <= brickRect.left || ballPosX + 1 >= brickRect.right) {
                                ballDirX = -ballDirX;
                            } else {
                                ballDirY = -ballDirY;
                            }
                            break loop;
                        }
                    }
                }
            }
        }
    }

    private void draw() {
        if (surfaceHolder.getSurface().isValid()) {
            Canvas canvas = surfaceHolder.lockCanvas();
            canvas.drawColor(Color.LTGRAY);
            paint.setColor(Color.BLACK);
            paint.setTextSize(30);

            canvas.drawText("Score: " + score, 10, 30, paint);
            canvas.drawText("Player: " + name, surfaceView.getWidth() - 250, 30, paint);

            // canvas.drawRect(playerX, surfaceView.getHeight() - 50, playerX + 100,
              //      surfaceView.getHeight() - 42, paint);
            paddleDrawable.setBounds(playerX, surfaceView.getHeight() - 50, playerX + 100,
                    surfaceView.getHeight());
            paddleDrawable.draw(canvas);

            paint.setColor(Color.RED);
            // canvas.drawCircle(ballPosX, ballPosY, 20, paint);
            ballDrawable.setBounds(ballPosX - ballRadius,
                    ballPosY - ballRadius,
                    ballPosX + 2 * ballRadius,
                    ballPosY + 2 * ballRadius);
            ballDrawable.draw(canvas);

            paint.setColor(Color.YELLOW);
            map.draw(canvas, paint);
            surfaceHolder.unlockCanvasAndPost(canvas);
        }
    }

    private void startPlay(){
        // Start Playing the background music
        backgroundMusicPlayer.play(true);

        play = true;
    }

    private void pausePlay(){
        // Pause the background music
        backgroundMusicPlayer.pause();

        play = false;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                startPlay();
                float touchX = event.getX();
                if (touchX < surfaceView.getWidth() / 2.0) {
                    playerX -= 20;
                    if (playerX < 0) {
                        playerX = 0;
                    }
                } else {
                    playerX += 20;
                    if (playerX > surfaceView.getWidth() - 100) {
                        playerX = surfaceView.getWidth() - 100;
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                // handle when the user stops touching the screen
                break;
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        backgroundMusicPlayer.pause();
    }

    public static void saveScore(Context context, String name, int score) {
        try {
            File file = new File(context.getFilesDir(), "hsfile.bb");
            if (!file.exists()) {
                file.createNewFile();
            }
            Path filePath = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                filePath = file.toPath();
            }
            boolean isNamePresent = false;
            // Check if name already exists in file
            BufferedReader reader = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                reader = new BufferedReader(new FileReader(filePath.toFile()));
            }
            StringBuilder stringBuilder = new StringBuilder();
            String line = reader.readLine();
            while (line != null) {
                String[] parts = line.split(":");
                if (parts[0].equals(name)) {
                    int existingScore = Integer.parseInt(parts[1]);
                    // Update the score if the new score is higher
                    line = name + ":" + (existingScore + score);

                    isNamePresent = true;
                }
                stringBuilder.append(line + "\n");
                line = reader.readLine();
            }
            reader.close();
            // Add the new name and score if it is not already present in the file
            if (!isNamePresent) {
                stringBuilder.append(name + ":" + score + "\n");
            }

            // Write the updated high scores to the file
            FileWriter writer = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                writer = new FileWriter(filePath.toFile());
            }
            writer.write(stringBuilder.toString());
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
