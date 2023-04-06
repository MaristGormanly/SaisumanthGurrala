package com.example.brickbreakergame;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

public class MapGenerator {
    public int[][] map;
    public int brickWidth;
    public int brickHeight;

    private Drawable bitmap;

    public MapGenerator(int row, int col) {
        map = new int[row][col];

        for (int i = 0; i < map.length; i++) {
            for (int j = 0; j < map[0].length; j++) {
                map[i][j] = 1;
            }
        }

        brickWidth = 540 / col;
        brickHeight = 150 / row;
    }

    void setBitmap(Drawable bitmap){
        this.bitmap = bitmap;
    }

    public void draw(Canvas canvas, Paint paint) {
        for (int i = 0; i < map.length; i++) {
            for (int j = 0; j < map[0].length; j++) {
                if (map[i][j] > 0) {
                    Rect bounds = new Rect(j * brickWidth + 80,
                            i * brickHeight + 150,
                            j * brickWidth + brickWidth + 80,
                            i * brickHeight + brickHeight + 150);
                    if(bitmap != null){
                        bitmap.setBounds(bounds);
                        bitmap.draw(canvas);
                    } else {
                        canvas.drawRect(bounds, paint);
                    }

                }
            }
        }
    }

    public void setBrickValue(int value, int row, int col) {
        map[row][col] = value;
    }
}
