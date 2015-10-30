package com.example.sarahshen.myapplication;

/**
 * Created by sarah.shen on 2015/10/27.
 */

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;


/**
     * A placeholder fragment containing a simple view.
     */
    public class ForecastFragment extends Fragment {

        public ForecastFragment() {
        }

        @Override
        public void onCreate(Bundle saveInstanceState){
            //Add this line in order for this fragment to handle menu event
            super.onCreate(saveInstanceState);
            setHasOptionsMenu(true);
        }

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
            inflater.inflate(R.menu.forecastfragment, menu);
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item){
            int id = item.getItemId();
            if(id == R.id.action_refresh){
                FetchWeatherTask weatherTask = new FetchWeatherTask();
                //add Postcode
                weatherTask.execute("94043");
                return true;
            }
            return super.onOptionsItemSelected(item);
        }


        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {

            //create some data for the ListView, here is a sample weekly data
            // represented as "day whether high/low"
            String[] forecastArray =
                    {
                            "Today -  sunny - 88/63",
                            "Tomorrow - Foggy - 70/40",
                            "Weds - Cloudy - 72/63",
                            "Thurs - Cloudy - 72/63",
                            "Fri - Heavy Rain - 72/63",
                            "Sat. - HELP TRAPPED IN WEATHERSTATION - 60/50",
                            "Sun - Sunny - 80/63",
                    };

            List<String> weekForecast = new ArrayList<String>(
                    Arrays.asList(forecastArray));
            //Now that we have some dummy forecast data, create an ArrayAdapter
            //The ArrayAdapter will take data from a source
            //use it to populate the ListView it is attached to.
            ArrayAdapter<String> mForecastAdapter = new ArrayAdapter<String>(
                    //The current context(this fragment's parent activity)
                    getActivity(),
                    //ID of list item layout
                    R.layout.list_item_forecast,
                    //ID of textview to populate
                    R.id.list_item_forecast_textview,
                    weekForecast);
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
         //   rootView = inflater.inflate(R.layout.fragment_main, container, false);
            //get a reference to the ListView
            ListView listView = (ListView) rootView.findViewById(
                    R.id.ListView_forecast);
            listView.setAdapter(mForecastAdapter);
            return rootView;
        }


    /**
     *
     */
        public class FetchWeatherTask extends AsyncTask <String,Void,String[]> {
            private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();
            /** The date/time conversion code is going to be moved outside the asynctask later,
             * so for convenience we're breaking it out into its own method now.
             */
            private String getReadableDateString(long time){
                // the API return a unix timestamp(measure in seconds)
                //it must be converted to milliseconds in order to be converted to valid date
                Date date = new Date(time*1000);
                SimpleDateFormat format = new SimpleDateFormat("E, MMM d");
                return format.format(date).toString();
            }

            /**
             * Prepare the weather high/lows for presentation.
             */
            private String formatHighLows(double high, double low){
                //for presention ,assume the user doesn't care about tenths of a degree.
                long roundedHigh = Math.round(high);
                long roundedLow = Math.round(low);

                String highLowStr = roundedHigh +"/"+roundedLow;
                return highLowStr;
            }

            private String[] getWeatherDataFromJson(String forecastJasonStr, int numDays)
                throws JSONException{
                // these are the names of theJASON objects that need to be extracted.
                final String OWM_LIST = "list";
                final String OWM_WEATHER = "weather";
                final String OWM_TEMPERATURE = "temp";
                final String OWM_MAX = "max";
                final String OWM_MIN = "min";
                final String OWM_DATETIME = "dt";
                final String OWM_DESCRIPTION = "main";

                JSONObject forecastJson = new JSONObject(forecastJasonStr);
                JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

                String[] resultStrs=new String[numDays];
                for(int i=0; i < weatherArray.length();i++){
                    //now use the format "Day ,description , hi/low"
                    String day;
                    String description;
                    String highAndLow;

                    //Get the JSON object representing the day
                    JSONObject dayForecast = weatherArray.getJSONObject(i);

                    // The date/time is returned as a long.  We need to convert that
                    // into something human-readable, since most people won't read "1400356800" as
                    // "this saturday".
                    long dateTine = dayForecast.getLong(OWM_DATETIME);
                    day = getReadableDateString(dateTine);

                    // description is in a child array called "weather", which is 1 element long.
                    JSONObject weatherObject =dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
                    description = weatherObject.getString(OWM_DESCRIPTION);


                    // Temperatures are in a child object called "temp".  Try not to name variables
                    // "temp" when working with temperature.  It confuses everybody.
                    JSONObject temperatureObject= dayForecast.getJSONObject(OWM_TEMPERATURE);
                    double high = temperatureObject.getDouble(OWM_MAX);
                    double low = temperatureObject.getDouble(OWM_MIN);

                    highAndLow=formatHighLows(high,low);
                    resultStrs[i]=day+"-"+description+"-"+highAndLow;

                }

                for (String s:resultStrs){
                    Log.v(LOG_TAG,"Forecast entry:" +s);
                }
                return resultStrs;
            }



            /**
             * @param params
             * @return
             */
            @Override
            protected String[] doInBackground(String...params) {

                //IF there is no zip code, there is nothing to look up
                if(params.length == 0){
                    return null;
                }
                //These two need to be declared outside the try/catch
                //so that they can be closed in the finally block
                HttpURLConnection urlConnection = null;
                BufferedReader reader = null;

                //will contain the raw JSON response as a string.
                String forecastJsonStr = null;

                String format = "jason";
                String units ="metric";
                int numDays = 7;

                try {
                    //Construct the URL for the OpenWeatherMap query
                    // Possible parameters are available at OWM 's forecast API page
                    //http://openweathermap.org/API#forecast
                    //创建URL对象
                   // URL url = new URL("http://api.openweathermap.org/data/2.5/forecast/daily?q=94043&mode=jason&units=metric& cnt=7");
                    final String FORECAST_BASE_URL =
                           "http://api.openweathermap.org/data/2.5/forecast/daily?";
                    final String QUERY_PARAM = "q";
                    final String FORMAT_PARAM = "mode";
                    final String UNITS_PARAM = "units";
                    final String DAYS_PARAM ="cnt";

                    Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                            .appendQueryParameter(QUERY_PARAM, params[0])
                            .appendQueryParameter(FORMAT_PARAM, format)
                            .appendQueryParameter(UNITS_PARAM, units)
                            .appendQueryParameter(DAYS_PARAM, Integer.toString(numDays))
                            .build();

                    URL url=new URL(builtUri.toString());

                    Log.v(LOG_TAG,"Built URI" + builtUri.toString());

                    //create the request to OpwnWeatherMap and open the connection
                    //返回一个URLConnection 对象，他表示URL所引用的远程对象的连接
                    urlConnection = (HttpURLConnection) url.openConnection();
                    //设定请求方式
                    urlConnection.setRequestMethod("GET");
                    //建立到远程对象的实际连接
                    urlConnection.connect();

                    //Read the input stream into a string
                    // 返回打开连接读取的输入流
                    InputStream inputStream = urlConnection.getInputStream();
                    StringBuffer buffer = new StringBuffer();
                    if (inputStream == null) {
                        //nothing to do
                        return null;
                    }
                    reader = new BufferedReader(new InputStreamReader(inputStream));

                    String line;
                    while ((line = reader.readLine()) != null){
                        //
                        buffer.append(line + "\n");
                    }
                    if (buffer.length()==0){
                        //Stream is empty.
                        return  null;
                    }
                    forecastJsonStr = buffer.toString();

                    Log.v(LOG_TAG,"forecast JSON String:" +forecastJsonStr);

                }catch (IOException e){
                    Log.e(LOG_TAG,"Error", e);
                    // if the code did not successfully get the weather data,
                    // to parse it
                    return null;
                }finally {
                    if(urlConnection !=null){
                        urlConnection.disconnect();
                    }
                    if(reader !=null){
                        try {
                            reader.close();
                        }catch (final IOException e){
                            Log.e(LOG_TAG,"Error closing stream", e);
                        }

                        }
                    }
                try {
                    return getWeatherDataFromJson(forecastJsonStr,numDays);
                }catch (JSONException e){
                    Log.e(LOG_TAG,e.getMessage(), e);
                    e.printStackTrace();
                }
                return null;
            }
        }
    }
