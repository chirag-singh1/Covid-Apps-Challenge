package com.example.nasaproject;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static android.graphics.Bitmap.Config.ALPHA_8;
import static android.graphics.Bitmap.Config.ARGB_8888;
import static android.graphics.Bitmap.createBitmap;

/**
 * Main Activity Class
 */
public class MainActivity extends AppCompatActivity {
    private int currentMonth;
    private int currentIndex;
    private String fileDirectory;
    Spinner latLonSelector;
    ImageView mainView;
    SeekBar timeSelector;
    TextView outputText;
    Map<Integer, double[]> coords = new HashMap<Integer, double[]>();
    HashMap<Integer, HistoricalData> historicalDataMap;
    Map<Integer,String>locations=new HashMap<Integer,String>();
    Bitmap currentBitmap;
    String updatedText;

    /**
     * OnCreate method to initialize main activity
     * @param savedInstanceState
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mainView = (ImageView) findViewById(R.id.imageView);
        latLonSelector = (Spinner) findViewById(R.id.spinner);
        timeSelector = (SeekBar) findViewById(R.id.seekBar);
        outputText = (TextView) findViewById(R.id.textView2);
        fileDirectory = getApplicationContext().getFilesDir().getPath().toString();
        String fileName = fileDirectory + "/HistoricalData.txt";
        historicalDataMap = new HashMap<Integer, HistoricalData>();
        final File file = new File(fileName);
        fillLatLonArray();
        fillSpinner(latLonSelector);
        fillLocations();
        currentMonth = 0;
        updatedText="";
        currentIndex = 0;

        //Reads file for historical data if it exists, if not creates new historical data
        try {
            if (file.createNewFile()) {
                createHistoricalData(0);
            } else {
                historicalDataMap = readFile();
            }
        } catch (Exception e) {}
    }

    /**
     * Sets OnSelectListener for the Lat/Lon selector, sets OnSeekBarChangeListener for month selector
     */
    public void setOnClickListeners() {
        latLonSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentIndex = position;
                updateImage(currentIndex, currentMonth);
                updateText(currentIndex);

            }

            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        timeSelector.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                currentMonth = progress;
                updateImage(currentIndex, currentMonth);
                updateText(currentIndex);
            }
        });
    }

    /**
     * Stores dedicated lat/lon data for selected locations
     */
    public void fillLatLonArray() {
        coords.put(0, new double[]{34.16038, -118.468907});
        coords.put(1, new double[]{35.68365, 139.76781});
        coords.put(2, new double[]{30.58311, 114.30297});
        coords.put(3, new double[]{31.20403, 121.49213});
        coords.put(4, new double[]{14.72578, -17.45801});
        coords.put(5, new double[]{-1.28759, 36.82764});
        coords.put(6, new double[]{34.06964, -118.44420});
        coords.put(7, new double[]{42.34200, -83.06627});
        coords.put(8, new double[]{19.46498, -99.14243});
        coords.put(9, new double[]{51.89000, 1.47620});
    }

    /**
     * Stores region codes for each lat/lon for calls to COVID API
     */
    public void fillLocations() {
        locations.put(0, "US");
        locations.put(1, "JP");
        locations.put(2, "CN");
        locations.put(3, "CN");
        locations.put(4, "SN");
        locations.put(5, "KE");
        locations.put(6, "US");
        locations.put(7, "US");
        locations.put(8, "MX");
        locations.put(9, "GB");
    }

    /**
     * Populates spinner with lat/lon options to select
     */
    public void fillSpinner(Spinner latLonSelector) {
        String[] spinnerArr = new String[coords.size()];
        for (int i : coords.keySet()) {
            spinnerArr[i] = "Lat: " + coords.get(i)[0] + ", Lon: " + coords.get(i)[1];
        }
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(MainActivity.this, R.layout.spinner_item, spinnerArr);
        spinnerAdapter.setDropDownViewResource(R.layout.spinner_item);
        latLonSelector.setAdapter(spinnerAdapter);
    }

    /**
     * Calls Async thread to create historical data for a given lat/lon location
     * @param i  the index of lat/lon location to make historical data for
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public void createHistoricalData(int i) throws ParseException {
        //call api
        AsyncTaskRunner caller = new AsyncTaskRunner();
        Double lat;
        Double lon;
        lat = coords.get(i)[0];
        lon = coords.get(i)[1];
        caller.execute("" + lat, "" + lon, "" + i);
    }

    /**
     * Updates informational text with COVID information currently
     * @param latLongIndex the current selecte lat/lon
     */
    public void updateText(int latLongIndex) {
        String country = locations.get(latLongIndex);
        AsyncTaskRunner3 caller = new AsyncTaskRunner3();
        caller.execute(country);
        outputText.setText(updatedText);
    }

    /**
     * Updates image with the change in light from historical average and data selected by user
     * @param latLongIndex
     * @param dateIndex
     */
    public void updateImage(int latLongIndex, int dateIndex) {
        HistoricalData temp = historicalDataMap.get(latLongIndex);
        Bitmap historicalDataBitmap = temp.getHistoricalAverageMap();
        float[] brightnessOfEachPixelHistorical = getBrightnessOfEachPixel(historicalDataBitmap);
        AsyncTaskRunner2 caller = new AsyncTaskRunner2();
        Double lat;
        Double lon;
        lat = coords.get(latLongIndex)[0];
        lon = coords.get(latLongIndex)[1];
        caller.execute("" + lat, "" + lon);
        float[] brightnessOfEachPixelCurrent = getBrightnessOfEachPixel(currentBitmap);
        float[] difference = new float[brightnessOfEachPixelCurrent.length];
        for (int i = 0; i < brightnessOfEachPixelCurrent.length; i++) {
            float diff = brightnessOfEachPixelCurrent[i] - brightnessOfEachPixelHistorical[i];
            difference[i] = diff;
        }
        Map hsvMap = new HashMap<Integer, Float[]>();
        for (int i = 0; i < difference.length; i++) {
            if (difference[i] < 0) {
                //current brightness is less than historical brightness
                float[] hsv = new float[3];
                hsv[0] = 120; //green
                hsv[1] = 1; //saturation
                hsv[2] = (float) Math.abs(difference[i]);
                hsvMap.put(i, hsv);
            }
            if (difference[i] <= 0) {
                //current brightness is greater than historical brightness
                float[] hsv = new float[3];
                hsv[0] = 0; //red
                hsv[1] = 1; //saturation
                hsv[2] = (float) Math.abs(difference[i]);
                hsvMap.put(i, hsv);
            }
        }
        int[] rgbColors = new int[brightnessOfEachPixelCurrent.length];
        for (int i = 0; i < rgbColors.length; i++) {
            float[] hsv = (float[]) hsvMap.get(i);
            int argb = Color.HSVToColor(hsv);
            rgbColors[i] = argb;
        }
        Bitmap result = createBitmap(rgbColors, historicalDataBitmap.getWidth(),
                historicalDataBitmap.getHeight(), ARGB_8888);
        mainView.setImageBitmap(result);

    }

    /**
     * Gives bitmap in gresycale form
     * @param b input bitmap
     * @return float[] of HSV value of each pixel (essentially greyscale value of pixel)
     */
    public float[] getBrightnessOfEachPixel(Bitmap b) {
        int[] pixel = new int[b.getWidth() * b.getHeight()];
        b.getPixels(pixel, 0, b.getWidth(), 0, 0, b.getWidth(), b.getHeight());
        List<float[]> hsvList = new ArrayList<float[]>();
        for (int i = 0; i < pixel.length; i++) {
            hsvList.add(new float[3]);
        }
        float[] brightnessOfEachPixel = new float[pixel.length];
        for (int i = 0; i < pixel.length; i++) {
            Color.colorToHSV(pixel[i], hsvList.get(i));
        }
        for (int i = 0; i < brightnessOfEachPixel.length; i++) {
            brightnessOfEachPixel[i] = hsvList.get(i)[2];   //gets value from each pixel's HSV representation
        }
        return brightnessOfEachPixel;
    }

    /**
     * Initializes HashMap of historical data objects from local files after it has been created
     * @return HashMap of historical data objects indexed corresponding to lat/lon coordinates
     */
    public HashMap<Integer, HistoricalData> readFile() {
        HashMap<Integer, HistoricalData> out = new HashMap<Integer, HistoricalData>();
        try {
            String filepath = fileDirectory + "/HistoricalData.txt";
            File inputData = new File(filepath);
            BufferedReader in = new BufferedReader(new FileReader(inputData));
            String currentLine = "";
            int index = 0;
            while (!currentLine.equals("$CLOSE$")) {
                currentLine = in.readLine();
                String bitmapPath = fileDirectory + "/DataBitmap" + index + ".jpg";
                File image = new File(bitmapPath);
                Double latitude = Double.parseDouble(currentLine.substring(currentLine.indexOf("Lat:") + 4, currentLine.indexOf("Long")));
                Double longitude = Double.parseDouble(currentLine.substring(currentLine.indexOf("Long:") + 5, currentLine.indexOf("Stdev")));
                Double stdev = Double.parseDouble(currentLine.substring(currentLine.indexOf("Stdev") + 5, currentLine.indexOf("index")));
                int ind = Integer.parseInt(currentLine.substring(currentLine.indexOf("index:") + 6));
                Bitmap bitmap = BitmapFactory.decodeFile(image.getAbsolutePath());
                out.put(index, new HistoricalData(latitude, longitude, bitmap, stdev, ind));
                index++;
            }
        } catch (Exception e) {
          //  Log.e("Error1", e.toString());
            e.printStackTrace();
        }
        return out;

    }

    /**
     * Private AsyncTaskRunner2 class dedicated to fetching most recent image for selected location for comparison with historical average
     */
    private class AsyncTaskRunner2 extends AsyncTask<String, String, Bitmap> {
        String key = "WhaKMPWGQF6kcsomfMQLhCGuqrGRZDPhq3FLqSnk";

        /**
         * Gets bitmap from selected time from Nasa API
         * @param params
         * @return Most Recent bitmap
         */
        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        protected Bitmap doInBackground(String... params) {
            Bitmap temp = createBitmap(1, 1, ALPHA_8);
            LocalDate today = LocalDate.now();
            Bitmap todayBitmap = createBitmap(1, 1, ALPHA_8);
            try {
                int day = today.getDayOfMonth();
                int month = today.getMonthValue();
                int year = today.getYear();
                String dateStr = year + "/" + month + "/" + day;
                URL url = new URL("https://api.nasa.gov/planetary/earth/imagery?lon=" + params[1] + "&lat=" + params[0] + "&date=" + dateStr + "&api_key=" + key);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setInstanceFollowRedirects(true);
                conn.setConnectTimeout(3000);
                temp = BitmapFactory.decodeStream(conn.getInputStream());
                todayBitmap = temp;
            } catch (Exception e) {
               // Log.e("Error creatHistorical", e.toString());

            }
            return todayBitmap;
        }

        /**
         * Updates most recent bitmap with fetched data
         * @param result
         */
        protected void onPostExecute(Bitmap result) {
            currentBitmap = result;

        }
    }

    /**
     * Private Async thread dedicated to creating new historical data
     */
    private class AsyncTaskRunner extends AsyncTask<String, String, HistoricalData> {

        String key = "gNYIqJ0i5fVQLTJQJwzNy4zcjiJDaSbLwGHCafay";

        /**
         * Calls to NASA API to get an average of historical night images, calls to other functions to create average bitmap
         * @param params (latitude, longtiude, index)
         * @return new historical data object corresponding to given lat/lon
         */
        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        protected HistoricalData doInBackground(String... params) {
            publishProgress();
            LocalDate startDate = LocalDate.of(2015, 01, 01); //startDate
            LocalDate endDate = LocalDate.of(2019, 05, 01); //endDate
            Bitmap temp = createBitmap(1, 1, ALPHA_8);
            HistoricalData output = new HistoricalData(0, 0, temp, 0, 0);
            Bitmap[] bmaps = new Bitmap[25];
            try {
                int i = 0;
                while (!endDate.equals(startDate)) {
                    //fetches date
                    int day = startDate.getDayOfMonth();
                    int month = startDate.getMonthValue();
                    int year = startDate.getYear();
                    //creates what will be put in api
                    String dateStr = year + "-" + month + "-" + day;
                    if (month <= 5 && day == 1 && year < 2020) {
                     //   Log.e("bruhment", "i: " + i + "date: " + dateStr+"index: "+params[2]);
                        try {
                            URL url = new URL("https://api.nasa.gov/planetary/earth/imagery?lon=" + params[1] + "&lat=" + params[0] + "&date=" + dateStr + "&api_key=" + key);
                            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                            conn.setInstanceFollowRedirects(true);
                            conn.setConnectTimeout(3000);
                            temp = BitmapFactory.decodeStream(conn.getInputStream());
                        bmaps[i] = temp;
                        }catch(Exception e){}
                        i++;
                    }
                    startDate = startDate.plusMonths(1);
                }
                output = new HistoricalData(Double.parseDouble(params[0]), Double.parseDouble(params[1]), takeAverage(bmaps), takeStdev(bmaps), Integer.parseInt(params[2]));

            } catch (Exception e) {
                output = new HistoricalData(0, 0, temp, 0, Integer.parseInt(params[2]));
              //  Log.e("Error creatHistorical", e.toString());
                e.printStackTrace();

            }
            return output;
        }

        /**
         * Updates user on progress
         * @param values
         */
        @Override
        protected void onProgressUpdate(String... values) {
            outputText.setText("Creating Historical Data...");
        }

        /**
         * Updates Image Bitmap temporarily, writes historical data object to file, and calls for the next index creation
         * @param result
         */
        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        protected void onPostExecute(HistoricalData result) {
            historicalDataMap.put(result.getIndex(), result);
            mainView.setImageBitmap(result.getHistoricalAverageMap());
            result.writeToFile(fileDirectory, result.getIndex());
            if (result.getIndex() == 9) {
                setOnClickListeners();
            }
            else{
                try {
                    createHistoricalData(result.getIndex() + 1);
                }catch(Exception e){}
            }
        }

        /**
         * Returns the average greyscale value of array of bitmaps
         * @param bmaps An array of bitmaps
         * @return 50 x 50 Bitmap of greyscale pixels
         */
        public Bitmap takeAverage(Bitmap[] bmaps) {
            Bitmap average = bmaps[0];
            average = Bitmap.createScaledBitmap(average, 50, 50, false);
            float[] real = getBrightnessOfEachPixel(average);
            for (Bitmap b : bmaps) {
                if (b instanceof Bitmap) {
                    b = Bitmap.createScaledBitmap(b, 50, 50, false);
                    float[] pixels = getBrightnessOfEachPixel(b);
                    real = runningAvg(pixels, 10, real);
                }
            }
            int[] output = new int[real.length];
            for (int i = 0; i < real.length; i++) {
                output[i] = Color.HSVToColor(new float[]{0, 0, real[i]});
            }
          // outputText.setText(Arrays.toString(output));
            ByteBuffer byteBuf = ByteBuffer.allocate(4 * real.length);
            IntBuffer intBuf = byteBuf.asIntBuffer();
            intBuf.put(output);
            byte[] byte_array = byteBuf.array();

            average = createBitmap(output,50,50,ARGB_8888);
           // Log.e("nice", "take average success");
            return average;
        }

        /**
         * Returns standard deviation of average pixels of bitmap array
         * @param bmaps
         * @return standard deviation double
         */
        public double takeStdev(Bitmap[] bmaps) {
            double[] avgArray = new double[bmaps.length];
            for (int j = 0; j < bmaps.length; j++) {
                Bitmap b = bmaps[j];
                if (b instanceof Bitmap) {
                    int[] pixels = new int[b.getWidth() * b.getHeight()];
                    b.getPixels(pixels, 0, b.getWidth(), 0, 0,
                            b.getWidth(), b.getHeight());
                    double tot = 0;
                    for (int i : pixels) {
                        tot += i;
                    }
                    avgArray[j] = tot / pixels.length;
                }
            }
          //  Log.e("nice", "first part stdev success");
            double out = calculateSD(avgArray);
           // Log.e("nice", "stdev success");

            return out;
        }

        /**
         * Returns standard deviation of array of doubles
         * @param numArray double array
         * @return standard deviation
         */
        public double calculateSD(double numArray[]) {
            double sum = 0.0, standardDeviation = 0.0;
            int length = numArray.length;
            for (double num : numArray) {
                sum += num;
            }
            double mean = sum / length;
            for (double num : numArray) {
                standardDeviation += Math.pow(num - mean, 2);
            }
            return Math.sqrt(standardDeviation / length);
        }

        /**
         *  Finds the running average of an array
         * @param a the array of floats
         * @param n the total number of floats
         * @param average the average array so far
         * @return the running average of the array
         */
        public float[] runningAvg(float[] a, int n, float[] average) {
            float[] realAvg = average;

            for (int i = 0; i < a.length; i++) {
                realAvg[i] -= realAvg[i] / n;
                realAvg[i] += a[i] / n;
            }
            return realAvg;
        }

        /**
         * See earlier method
         */
        public float[] getBrightnessOfEachPixel(Bitmap b) {
            int[] pixel = new int[b.getWidth() * b.getHeight()];
            b.getPixels(pixel, 0, b.getWidth(), 0, 0, b.getWidth(), b.getHeight());
            List<float[]> hsvList = new ArrayList<float[]>();
            for (int i = 0; i < pixel.length; i++) {
                hsvList.add(new float[3]);
            }
            float[] brightnessOfEachPixel = new float[pixel.length];
            for (int i = 0; i < pixel.length; i++) {
                Color.colorToHSV(pixel[i], hsvList.get(i));
            }
            for (int i = 0; i < brightnessOfEachPixel.length; i++) {
                brightnessOfEachPixel[i] = hsvList.get(i)[2];   //gets value from each pixel's HSV representation
            }
            return brightnessOfEachPixel;
        }
    }

    /**
     * Async Thread dedicated to fetching current COVID data to update text
     */
    private class AsyncTaskRunner3 extends AsyncTask<String, String, String>{

        /**
         * Calls to API to get current COVID data
         * @param params
         * @return
         */
        protected String doInBackground(String... params) {
            String temp = "";
            try{
                URL url = new URL("https://covid19-us-api.herokuapp.com/country" + params[1]);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setInstanceFollowRedirects(true);
                conn.setConnectTimeout(3000);
                StringWriter writer = new StringWriter();
                BufferedInputStream bis = new BufferedInputStream(conn.getInputStream());
                ByteArrayOutputStream buf = new ByteArrayOutputStream();
                int result = bis.read();
                while(result != -1) {
                    buf.write((byte) result);
                    result = bis.read();
                }
                temp = buf.toString("UTF-8");
            }   catch (Exception e) {
             //   Log.e("Error updateText", e.toString());
            }
            return temp;
        }

        /**
         * Updates text with current COVID results
         * @param result
         */
        protected void onPostExecute(String result) {
            updatedText = result;
        }

    }
}
