package com.example.sarahshen.myapplication;

/**
 * Created by sarah.shen on 2015/10/27.
 */

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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
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
                weatherTask.execute();
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

        public class FetchWeatherTask extends AsyncTask <Void,Void,Void> {
            private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();
            @Override
            protected Void doInBackground(Void...params) {
                //These two need to be declared outside the try/catch
                //so that they can be closed in the finally block
                HttpURLConnection urlConnection = null;
                BufferedReader reader = null;

                //will contain the raw JSON response as a string.
                String forecastJsonStr = null;
                try {
                    //Construct the URL for the OpenWeatherMap query
                    // Possible parameters are available at OWM 's forecast API page
                    //http://openweathermap.org/API#forecast
                    //创建URL对象
                    URL url = new URL("http://api.openweathermap.org/data/2.5/forecast/daily?q=94043&mode=jason&units=metric& cnt=7");

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
                return null;
            }
        }
    }
