package com.example.nasaproject;

import android.graphics.Bitmap;
import android.util.Log;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintWriter;
import java.text.DecimalFormat;

/**
 * Helper class to store average data about specific latitude and longitude
 */
public class HistoricalData {
    private double latitude;
    private double longitude;
    private Bitmap historicalAverageMap;
    private double stdev;
    private int index;

    //Main constructor
    public HistoricalData(double lat, double lon, Bitmap averageMap, double std, int ind) {
        latitude = lat;
        longitude = lon;
        historicalAverageMap = averageMap;
        stdev = std;
        index = ind;
    }

    //Writes Lat,Lon, Stdev, index to .txt file in specific format
    //Saves Average Data bitmap to .jpeg of local files
    public void writeToFile(String fileDirectory, int index) {
        String filename = fileDirectory + "/HistoricalData.txt";
        String imagepath = fileDirectory + "/DataBitmap" + index + ".jpg";
        try {
            File image = new File(imagepath);
            File dataFile = new File(filename);
            image.createNewFile();
            BufferedReader in = new BufferedReader(new FileReader(dataFile));
            StringBuilder sb = new StringBuilder();
            String currentLine = "";
            do {
                currentLine = in.readLine();
                if (currentLine instanceof String && !currentLine.equals("$CLOSE$")) {
                    sb.append(currentLine);
                    sb.append("\n");
                }
            } while (currentLine instanceof String && !currentLine.equals("$CLOSE$") && !currentLine.equals(""));
            DecimalFormat df = new DecimalFormat("###.#####");
            sb.append("Lat:" + df.format(latitude) + "Long:" + df.format(longitude) + "Stdev" + df.format(stdev) +"index:"+index+"\n");
            PrintWriter out = new PrintWriter(dataFile);
            out.print(sb.toString());
            out.close();
            FileOutputStream imageOut = new FileOutputStream(image);
            historicalAverageMap.compress(Bitmap.CompressFormat.JPEG, 100, imageOut);
            imageOut.flush();
            imageOut.close();
        } catch (Exception e) {
            Log.e("aaaa",e.toString());
            e.printStackTrace();

        }
    }

    //Accessor methods
    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public Bitmap getHistoricalAverageMap() {
        return historicalAverageMap;
    }

    public double getStdev() {
        return stdev;
    }

    public int getIndex() {
        return index;
    }
}
