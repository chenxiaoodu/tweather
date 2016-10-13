package com.tweather.app.util;

import android.text.TextUtils;

import com.tweather.app.model.City;
import com.tweather.app.model.County;
import com.tweather.app.model.Province;
import com.tweather.app.model.TWeatherDB;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by DADOU on 2016/10/13.
 */
public class Utility {
    public synchronized static boolean handleProvincesResponse(TWeatherDB tWeatherDB,
                                                               String response) {
        if (!TextUtils.isEmpty(response)) {
            // 使用正则表达式筛选出""里面的内容
            Pattern pattern = Pattern.compile("\"(.*?)\"");
            Matcher matcher = pattern.matcher(response);
            int times = 0;
            Province province = null;
            while (matcher.find()) {
                String provinceInfo = matcher.group().replace("\"", "");
                if (times++ % 2 == 0) {
                    province = new Province();
                    province.setProvinceCode(provinceInfo);
                } else {
                    province.setProvinceName(provinceInfo);
                    tWeatherDB.saveProvince(province);
                }
            }
            return true;
        }
        return false;
    }

    /**
     * 解析和处理服务器返回的市级数据
     */
    public static boolean handleCitiesResponse(TWeatherDB tWeatherDB, String response,
                                               Province province) {
        if (!TextUtils.isEmpty(response)) {
            Pattern pattern = Pattern.compile("\"(.*?)\"");
            Matcher matcher = pattern.matcher(response);
            int times = 0;
            City city = null;
            while (matcher.find()) {
                String cityInfo = matcher.group().replace("\"", "");
                if (times++ % 2 == 0) {
                    city = new City();
                    city.setProvinceId(province.getId());
                    city.setCityCode(province.getProvinceCode() + cityInfo);
                } else {
                    city.setCityName(cityInfo);
                    tWeatherDB.saveCity(city);
                }
            }
            return true;
        }
        return false;
    }

    /**
     * 解析和处理服务器返回的县级数据
     */
    public static boolean handleCountiesResponse(TWeatherDB tWeatherDB, String response,
                                                 City city) {
        if (!TextUtils.isEmpty(response)) {
            Pattern pattern = Pattern.compile("\"(.*?)\"");
            Matcher matcher = pattern.matcher(response);
            int times = 0;
            County county = null;
            while (matcher.find()) {
                String countyInfo = matcher.group().replace("\"", "");
                if (times++ % 2 == 0) {
                    county = new County();
                    county.setCityId(city.getId());
                    county.setCountyCode(city.getCityCode() + countyInfo);
                } else {
                    county.setCountyName(countyInfo);
                    tWeatherDB.saveCounty(county);
                }
            }
            return true;
        }
        return false;
    }
}
