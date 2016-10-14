package com.tweather.app.acticity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.tweather.app.R;
import com.tweather.app.model.City;
import com.tweather.app.model.County;
import com.tweather.app.model.Province;
import com.tweather.app.model.TWeatherDB;
import com.tweather.app.util.HttpCallbackListener;
import com.tweather.app.util.HttpUtil;
import com.tweather.app.util.Utility;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by DADOU on 2016/10/13.
 */
public class ChooseAreaActivity extends Activity {
    public static final int LEVEL_PROVINCE=0;
    public static final int LEVEL_CITY=1;
    public static final int LEVEL_COUNTY=2;

    private ProgressDialog progressDialog;
    private TextView tvTitle;
    private ListView lv;
    private ArrayAdapter<String> adapter;
    private TWeatherDB tWeatherDB;
    private List<String> dataList =  new ArrayList<String>();

    /**
     * 省列表
     */
    private List<Province> provinceList;

    /**
     * 市列表
     */
    private List<City> cityList;

    /**
     * 县列表
     */
    private List<County> countyList;

    /**
     * 选中的省份
     */
    private Province selectedProvince;


    /**
     * 选中的城市
     */
    private City selectedCity;

    /**
     * 当前选中的级别
     */
    private int currentLevel;

    /**
     * 是否从WeatherActivity中跳转过来
     */
     private boolean isFromWeatherActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);


        isFromWeatherActivity = getIntent().getBooleanExtra("from_weather_activity", false);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //已经选择了城市并不是从WeatherActivity跳转过来，才会直接跳转到WeatherActivity
        if (prefs.getBoolean("city_selected", false) && !isFromWeatherActivity) {
            Intent intent = new Intent(this, WeatherActivity.class);
            startActivity(intent);
            finish();
            return;
        }



        setContentView(R.layout.choose_area);
        lv =(ListView)findViewById(R.id.list_view);
        tvTitle =(TextView) findViewById(R.id.title_text);
        adapter =new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,dataList);
        lv.setAdapter(adapter);
        tWeatherDB =TWeatherDB.getInstance(this);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (currentLevel== LEVEL_PROVINCE) {
                    selectedProvince = provinceList.get(position);
                    queryCities();
                }else if (currentLevel == LEVEL_CITY){
                    selectedCity=cityList.get(position);
                    queryCounties();
                }else if (currentLevel==LEVEL_COUNTY){
                    String countyCode = countyList.get(position).getCountyCode();
                    Intent intent = new Intent(ChooseAreaActivity.this,WeatherActivity.class);
                    intent.putExtra("county_code",countyCode);
                    startActivity(intent);
                    finish();
                }
            }
        });
        queryProvinces();

    }

    /**
     * 查询全国所有省，优先从数据库查询，如果没有再到服务器查询
     */
     private void queryProvinces(){
         provinceList =tWeatherDB.loadProvinces();
         if (provinceList.size()>0){
             dataList.clear();
             for (Province province : provinceList){
                 dataList.add(province.getProvinceName());
             }
             adapter.notifyDataSetChanged();
             lv.setSelection(0);
             tvTitle.setText("中国");
             currentLevel = LEVEL_PROVINCE;
         }else {
             queryFromServer(null,LEVEL_PROVINCE);
         }

     }

    /**
     * 查询选中省所有的市，优先从数据库查询，如果没有查询到再去服务器上查询
     */
    private void queryCities() {
        cityList = tWeatherDB.loadCities(selectedProvince.getId());
        if (cityList.size() > 0) {
            dataList.clear();
            for (City city : cityList) {
                dataList.add(city.getCityName());
            }
            adapter.notifyDataSetChanged();
            lv.setSelection(0);
            tvTitle.setText(selectedProvince.getProvinceName());
            currentLevel = LEVEL_CITY;
        } else {
            queryFromServer(selectedProvince.getProvinceCode(), LEVEL_CITY);
        }
    }

    /**
     * 查询选中市所有的县，优先从数据库查询，如果没有查询到再去服务器上查询
     */
    private void queryCounties() {
        countyList = tWeatherDB.loadCounties(selectedCity.getId());
        if (countyList.size() > 0) {
            dataList.clear();
            for (County county : countyList) {
                dataList.add(county.getCountyName());
            }
            adapter.notifyDataSetChanged();
            lv.setSelection(0);
            tvTitle.setText(selectedCity.getCityName());
            currentLevel = LEVEL_COUNTY;
        } else {
            queryFromServer(selectedCity.getCityCode(), LEVEL_COUNTY);
        }

    }
    /**
     * 根据传入的代号和类型从服务器上查询省县市数据
     */
    private void queryFromServer(final String code, final int type) {
        String address = "";

        switch (type) {
            case LEVEL_PROVINCE:
                address = "http://www.weather.com.cn/data/city3jdata/china.html";
                break;
            case LEVEL_CITY:
                address = "http://www.weather.com.cn/data/city3jdata/provshi/" + code + ".html";
                break;
            case LEVEL_COUNTY:
                address = "http://www.weather.com.cn/data/city3jdata/station/" + code + ".html";
                break;
        }

        showProgressDialog();
        HttpUtil.sendHttpRequest(address, new HttpCallbackListener() {

            @Override
            public void onFinish(String response) {
                boolean result = false;
                switch (type) {
                    case LEVEL_PROVINCE:
                        result = Utility.handleProvincesResponse(tWeatherDB, response);
                        break;
                    case LEVEL_CITY:
                        result = Utility
                                .handleCitiesResponse(tWeatherDB, response, selectedProvince);
                        break;
                    case LEVEL_COUNTY:
                        result = Utility.handleCountiesResponse(tWeatherDB, response, selectedCity);
                        break;
                }

                if (result) {
                    ChooseAreaActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeProgressDialog();
                            switch (type) {
                                case LEVEL_PROVINCE:
                                    queryProvinces();
                                    break;
                                case LEVEL_CITY:
                                    queryCities();
                                    break;
                                case LEVEL_COUNTY:
                                    queryCounties();
                                    break;
                            }
                        }
                    });
                }
            }

            @Override
            public void onError(Exception e) {
                // 通过runOnUiThread()方法回到主线程处理逻辑
                runOnUiThread(new Runnable() {
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(ChooseAreaActivity.this, "加载失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

    }

    /**
     * 显示进度对话框
     */
    private void showProgressDialog() {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setMessage("正在加载");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();
    }

    /**
     * 关闭进度对话框
     */
    private void closeProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }

    /**
     * 捕获back按键，根据当前的级别来判断，此时应该返回市列表、省列表还是直接退出。
     */
    @Override
    public void onBackPressed() {
        if (currentLevel == LEVEL_COUNTY) {
            queryCities();
        } else if (currentLevel == LEVEL_CITY) {
            queryProvinces();
        } else {
           if (isFromWeatherActivity) {
                Intent intent = new Intent(this, WeatherActivity.class);
                startActivity(intent);
            }
            finish();
        }
    }






}
