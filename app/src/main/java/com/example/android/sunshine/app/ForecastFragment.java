package com.example.android.sunshine.app;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.format.Time;
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
import java.util.List;

/**
 * Created by JuanCarlos on 27/02/2016.
 */
public class ForecastFragment extends Fragment {

    private ArrayAdapter<String> mForecastAdapter;

    public ForecastFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.forecastfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_refresh) {
            FetchWeatherTask weatherTask  = new FetchWeatherTask();
            weatherTask.execute("94043");
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Create some dummy data for the ListView.  Here's a sample weekly forecast
        String[] data = {
                "Mon 6/23 - Sunny - 31/17",
                "Tue 6/24 - Foggy - 21/8",
                "Wed 6/25 - Cloudy - 22/17",
                "Thurs 6/26 - Rainy - 18/11",
                "Fri 6/27 - Foggy - 21/10",
                "Sat 6/28 - TRAPPED IN WEATHERSTATION - 23/18",
                "Sun 6/29 - Sunny - 20/7"
        };
        List<String> weekForecast = new ArrayList<String>(Arrays.asList(data));

        // Now that we have some dummy forecast data, create an ArrayAdapter.
        // The ArrayAdapter will take data from a source (like our dummy forecast) and
        // use it to populate the ListView it's attached to.
        mForecastAdapter =
                new ArrayAdapter<String>(
                        getActivity(), // The current context (this activity)
                        R.layout.list_item_forecast, // The name of the layout ID.
                        R.id.list_item_forecast_textview, // The ID of the textview to populate.
                        weekForecast);

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        // Get a reference to the ListView, and attach this adapter to it.
        ListView listView = (ListView) rootView.findViewById(R.id.listview_forecast);
        listView.setAdapter(mForecastAdapter);

        return rootView;
    }

    public class FetchWeatherTask extends AsyncTask<String, Void, String[]> {

        private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();

        /* The date/time conversion code is going to be moved outside the asynctask later,
         * so for convenience we're breaking it out into its own method now.
         */
        private String getReadableDateString(long time){
            // Because the API returns a unix timestamp (measured in seconds),
            // it must be converted to milliseconds in order to be converted to valid date.
            SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE MMM dd");
            return shortenedDateFormat.format(time);
        }

        /**
         * Prepare the weather high/lows for presentation.
         */
        private String formatHighLows(double high, double low) {
            // For presentation, assume the user doesn't care about tenths of a degree.
            long roundedHigh = Math.round(high);                                        //Math.round Redondea un numero al numero
            long roundedLow = Math.round(low);                                          //entero mas cercano

            String highLowStr = roundedHigh + "/" + roundedLow;
            return highLowStr;
        }

        /**
         * Take the String representing the complete forecast in JSON Format and
         * pull out the data we need to construct the Strings needed for the wireframes.
         *
         * Fortunately parsing is easy:  constructor takes the JSON string and converts it
         * into an Object hierarchy for us.
         */
        private String[] getWeatherDataFromJson(String forecastJsonStr, int numDays)
                throws JSONException {

            // These are the names of the JSON objects that need to be extracted.
            final String OWM_LIST = "list";
            final String OWM_WEATHER = "weather";
            final String OWM_TEMPERATURE = "temp";
            final String OWM_MAX = "max";
            final String OWM_MIN = "min";
            final String OWM_DESCRIPTION = "main";

            JSONObject forecastJson = new JSONObject(forecastJsonStr);
            JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

            // OWM returns daily forecasts based upon the local time of the city that is being
            // asked for, which means that we need to know the GMT offset to translate this data
            // properly.

            // Since this data is also sent in-order and the first day is always the
            // current day, we're going to take advantage of that to get a nice
            // normalized UTC date for all of our weather.

            Time dayTime = new Time();
            dayTime.setToNow();

            // we start at the day returned by local time. Otherwise this is a mess.
            int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);

            // now we work exclusively in UTC
            dayTime = new Time();

            String[] resultStrs = new String[numDays];
            Log.v(LOG_TAG, "WeatherArray: " + weatherArray.length());
            for(int i = 0; i < weatherArray.length(); i++) {                    //metodo length nos arroja cuantos array existen
                // For now, using the format "Day, description, hi/low"         /en este caso son 7 dias con ese metodo la pc
                String day;                                                     //conoce cuantos son
                String description;
                String highAndLow;

                // Get the JSON object representing the day
                JSONObject dayForecast = weatherArray.getJSONObject(i);
                Log.v(LOG_TAG, "dayForecast" + i + " :" + dayForecast);
                // The date/time is returned as a long.  We need to convert that
                // into something human-readable, since most people won't read "1400356800" as
                // "this saturday".

                long dateTime;
                // Cheating to convert this to UTC time, which is what we want anyhow
                dateTime = dayTime.setJulianDay(julianStartDay+i);
                Log.v(LOG_TAG, "dateTime: " + dateTime);
                day = getReadableDateString(dateTime);
                Log.v(LOG_TAG, "day: " + day);

                // weatherObject es una variable tipo JSONObject y tiene valor de:
                //un objetoJson con el valor de un JSONArray(OWM_WEATHER)
                //OWM_WEATHER tiene un valor de weather por lo tanto es  lo que busca en el objeto JSON
                JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
                Log.v(LOG_TAG, "weatherObject: " + weatherObject);

                //OWM_DESCRIPTION = main
                description = weatherObject.getString(OWM_DESCRIPTION);
                Log.v(LOG_TAG, "description: " + description);

                // Temperatures are in a child object called "temp".  Try not to name variables
                // "temp" when working with temperature.  It confuses everybody.
                JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
                Log.v(LOG_TAG, "Temperature: " + temperatureObject);

                //Obtenemos maximo y minimo d ela temperatura
                double high = temperatureObject.getDouble(OWM_MAX);
                double low = temperatureObject.getDouble(OWM_MIN);

                highAndLow = formatHighLows(high, low);
                resultStrs[i] = day + " - " + description + " - " + highAndLow;
            }

            for (String s : resultStrs) {
                Log.v(LOG_TAG, "Forecast entry: " + s);
            }
            return resultStrs;

        }
        @Override
        protected String[] doInBackground(String... params) {

            // If there's no zip code, there's nothing to look up.  Verify size of params.
            if (params.length == 0) {
                return null;
            }

            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String forecastJsonStr = null;
            int numDays = 7;
            try {
                // Construct the URL for the OpenWeatherMap query
                // Possible parameters are avaiable at OWM's forecast API page, at
                // http://openweathermap.org/API#forecast

                Uri.Builder builder = new Uri.Builder();        //Para crear URL de abajo
                builder.scheme("http")                         //"http://api.openweathermap.org/data/2.5/forecast/
                        .authority("api.openweathermap.org")    //daily?q=94043&mode=json&units=metric&cnt=7&
                        .appendPath("data")                     //APPID =21bc600df014fa2c0beb6d7ca2df89e7";
                        .appendPath("2.5")
                        .appendPath("forecast")
                        .appendPath("daily")
                        .appendQueryParameter("q", params[0])
                        .appendQueryParameter("mode", "json")
                        .appendQueryParameter("units", "metric")
                        .appendQueryParameter("cnt", Integer.toString(numDays))
                        .appendQueryParameter("APPID", "21bc600df014fa2c0beb6d7ca2df89e7");
                String myUrl = builder.build().toString();
//                String apiKey = "&APPID=" + BuildConfig.OPEN_WEATHER_MAP_API_KEY;
//                URL url = new URL(baseUrl.concat(apiKey));
                URL url = new URL(myUrl);
                Log.v(LOG_TAG, "URL: " + myUrl);
                // Crear la peticion OpenWeatherMap, y abrir la conexion
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();
                Log.v(LOG_TAG, "URL CONECTION: " + urlConnection);

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();            //Lee Bytes de la url de aqui a inputStreamReader
                StringBuilder buffer = new StringBuilder();                            //se declara el String Buffer "Buffer"
                Log.v(LOG_TAG, "InputStream: " + inputStream);
                Log.v(LOG_TAG, "buffer: " + buffer);
                if (inputStream == null) {
                    // Nothing to do.
                    return null;
                }
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);  //Lee Bytes y los pasa a caracteres Unicode
                Log.v(LOG_TAG, "InputStreamReader: " + inputStream);
                reader = new BufferedReader(inputStreamReader);                           //Utiliza el buffer para mejorar la efficiencia
//                reader = new BufferedReader(new InputStreamReader(inputStream));        //de lectura de caracteres
                Log.v(LOG_TAG, "reader: " + reader);
                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    Log.v(LOG_TAG, "line: " + line);
                    Log.v(LOG_TAG, "buffer antes de append: " + buffer);
                    buffer.append(line + "\n");                                             //para cambiar o concatenar el contenido
                    Log.v(LOG_TAG, "buffer despues de append: " + buffer);                  //de un stringbuffer se utiliza el metodo append()
                }

                //Se pasa la Info de buffer
                if (buffer.length() == 0) {                                                 //si buffer no tiene info regresa nada
                    // Stream was empty.  No point in parsing.
                    return null;
                }
                forecastJsonStr = buffer.toString();                                        //sino la información de buffer entra
                                                                                            //a forecastJsonStr con un String
                Log.v(LOG_TAG, "Forecast string: " + forecastJsonStr);
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                // If the code didn't successfully get the weather data, there's no point in attemping
                // to parse it.
                return null;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }

            try {
                return getWeatherDataFromJson(forecastJsonStr, numDays);            //ESTA ES NUESTRA SALIDA PARA POSEXECUTE
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }

            // This will only happen if there was an error getting or parsing the forecast.
            return null;
        }

        @Override
        protected void onPostExecute(String[] result) {
            if (result != null) {
                mForecastAdapter.clear();
                for(String dayForecastStr : result) {                   //Muestra cada uno de los nombres dentro de result
                    mForecastAdapter.add(dayForecastStr);               //Este caso nuestro array getWeatherDataFromJson
                }
                // New data is back from the server.  Hooray!
            }
        }
    }
}
