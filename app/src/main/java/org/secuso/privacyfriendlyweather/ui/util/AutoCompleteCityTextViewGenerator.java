package org.secuso.privacyfriendlyweather.ui.util;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import org.secuso.privacyfriendlyweather.database.City;
import org.secuso.privacyfriendlyweather.database.PFASQLiteHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * This class provides an AutoCompleteTextView which shows a drop down list with cities that match
 * the input string.
 */
public class AutoCompleteCityTextViewGenerator {

    /**
     * Member variables
     */
    private Context context;
    //private DatabaseHelper dbHelper;
    private PFASQLiteHelper dbHelper;
    private CitySelectAdapter cityAdapter;
    private Runnable selectAction;
    private AutoCompleteTextView editField;
    private MyConsumer<City> cityConsumer;
    private int listLimit;
    private City selectedCity;

    /**
     * Constructor.
     *
     * @param context  The context in which the AutoCompleteTextView is to be used.
     * @param dbHelper An instance of a DatabaseHelper. This object is used to make the database
     *                 queries.
     */
    public AutoCompleteCityTextViewGenerator(Context context, PFASQLiteHelper dbHelper) {
        this.context = context;
        this.dbHelper = dbHelper;
    }

    /**
     * @param editField    The component to "transform" into one that shows a city drop down list
     *                     based on the current input. Make sure to pass an initialized object,
     *                     else a java.lang.NullPointerException will be thrown.
     * @param listLimit    Determines how many items shall be shown in the drop down list at most.
     */
    public void generate(AutoCompleteTextView editField, int listLimit, final int enterActionId, final MyConsumer<City> cityConsumer, final Runnable selectAction) {
        cityAdapter = new CitySelectAdapter(context, android.R.layout.simple_list_item_1, new ArrayList<City>());
        this.editField = editField;
        this.cityConsumer = cityConsumer;
        this.listLimit = listLimit;
        editField.setAdapter(cityAdapter);
        editField.addTextChangedListener(new TextChangeListener());

        editField.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d("cityfind","parent: "+parent.getAdapter().toString());

                selectedCity = (City) parent.getItemAtPosition(position);
                cityConsumer.accept(selectedCity);
            }
        });

        editField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == enterActionId) {
                    Boolean checkCity = checkCity();
                    if (checkCity) {
                        selectAction.run();
                    }
                    return true;
                }
                return false;
            }
        });
    }

    private boolean checkCity() {
        Log.d("cityfind","checkcity called with");

        if (selectedCity == null) {
            String current = editField.getText().toString();
            Log.d("cityfind",current);
            String name;
            String country;
            if (current.contains("(")){
                int split = current.indexOf("(");
                name = current.substring(0,split);
                country = current.substring(split+1, Math.min(split+3,current.length()));
            } else {
                name = current;
                country="";
            }

            if (name.length() > 2) {
                List<City> cities = dbHelper.getCitiesWhereNameLike(name, listLimit, country);
                if (cities.size() == 1) {
                    selectedCity = cities.get(0);
                    cityConsumer.accept(selectedCity);
                    return true;
                }
            }

            Toast.makeText(context, "NO City selected", Toast.LENGTH_SHORT).show();
            return false;
        }
        Log.d("cityfind","unselected");
        return true;
    }

    /**
     * The following listener implementation provides the functionality / logic for the lookahead
     * dropdown.
     */
    private class TextChangeListener implements TextWatcher {
        private TextChangeListener() {
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            getCityFromText(false);
        }

        @Override
        public void afterTextChanged(Editable s) {
        }

    }

    public void getCityFromText(Boolean selectWhenUnique) {
        selectedCity = null;
        cityConsumer.accept(null);
        if (dbHelper != null) {
            //List<City> allCities = dbHelper.getAllCities();

            String content = editField.getText().toString();
            String name;
            String country;
            //if country code is given split the city name from country
            if (content.contains("(")){
                int split = content.indexOf("(");
                name = content.substring(0,split).replaceAll("\\s+$", "");
                country = content.substring(split+1, Math.min(split+3,content.length()));
            } else {
                //take whole input as city name (except whitespace at the end)
                name = content.replaceAll("\\s+$", "");
                country="";
            }

            if (name.length() > 2) {
                // Get the matched cities
                //List<City> cities = dbHelper.getCitiesWhereNameLike(content, allCities, dropdownListLimit);
                List<City> cities = dbHelper.getCitiesWhereNameLike(name, listLimit, country);
                // Set the drop down entries

                if (selectWhenUnique && cities.size() == 1) {
                    Log.d("cityfind","cities size 1, length: "+cities.get(0));

                    selectedCity = cities.get(0);
                    cityConsumer.accept(selectedCity);
                } else {
                    Log.d("cityfind","not unique, length: "+cities.size());

                    cityAdapter.clear();
                    cityAdapter.addAll(cities);
                    editField.showDropDown();
                }
            } else {
                editField.dismissDropDown();
            }
        }
    }

}
