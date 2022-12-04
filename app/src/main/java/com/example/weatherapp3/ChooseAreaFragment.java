package com.example.weatherapp3;

import static java.security.AccessController.getContext;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;

import com.example.weatherapp3.db.City;
import com.example.weatherapp3.db.County;
import com.example.weatherapp3.db.Province;
import com.example.weatherapp3.util.Constant;
import com.example.weatherapp3.util.HttpUtil;
import com.example.weatherapp3.util.Utility;

import org.litepal.LitePal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class ChooseAreaFragment extends Fragment {
    private static final String TAG = "ChooseAreaFragment";

    public static final int LEVEL_PROVINCE = 0;
    public static final int LEVEL_CITY = 1;
    public static final int LEVEL_COUNTY = 2;

    private ProgressDialog progressDialog;
    private TextView titleText;
    private Button backButton;
    private ListView listView;
    private ArrayAdapter<String> adapter;
    private List<String> dataList = new ArrayList<>();

    private List<Province> provinceList; //省列表
    private List<City> cityList; //市列表
    private List<County> countyList;//县列表

    private Province selectedProvince;//选中的省份
    private City selectedCity;//选中的城市
    private int currentLevel; //当前选中级别

    public ChooseAreaFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.choose_area, container, false);
        titleText = (TextView) view.findViewById(R.id.title_text);
        backButton = (Button) view.findViewById(R.id.back_button);
        listView = (ListView) view.findViewById(R.id.list_view);
        adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, dataList);
        listView.setAdapter(adapter);
        return view;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        requireActivity().getLifecycle().addObserver(new LifecycleEventObserver() {
            @Override
            public void onStateChanged(@NonNull LifecycleOwner source, @NonNull Lifecycle.Event event) {
                if (event.getTargetState() == Lifecycle.State.CREATED) {
                    listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                            if (currentLevel == LEVEL_PROVINCE){
                                selectedProvince = provinceList.get(i);
                                queryCities();
                            }
                            else if (currentLevel == LEVEL_CITY){
                                selectedCity =cityList.get(i);
                                queryCounties();
                            }else if (currentLevel == LEVEL_COUNTY){
                                //天气查看界面
                                County county =countyList.get(i);
                                Intent intent =new Intent();
                                intent.setClass(getActivity(),WeatherActivity.class);//Main少一行代码
                                intent.putExtra("weather_id",county.getWeatherId());
                                startActivity(intent);
                            }
                        }
                    });

                    //添加业务逻辑
                    queryProvinces();
                    getLifecycle().removeObserver(this);
                }
            }
        });
    }

    private void queryProvinces(){
        titleText.setText(getString(R.string.china));
        backButton.setVisibility(View.GONE);
        provinceList = LitePal.findAll(Province.class);
        if (provinceList.size() > 0){
            dataList.clear();
            for (Province item : provinceList){
                dataList.add(item.getProvinceName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_PROVINCE;
        }else {
            queryFromServer(Constant.BASE_URL,LEVEL_PROVINCE);
        }
    }

    private void queryCities() {
        titleText.setText(selectedProvince.getProvinceName());
        backButton.setVisibility(View.VISIBLE);
        cityList = LitePal.where("provinceid = ?",
                String.valueOf(selectedProvince.getId())).find(City.class);
        if (cityList.size() > 0) {
            dataList.clear();
            for (City city : cityList) {
                dataList.add(city.getCityName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_CITY;
        } else {
            int provinceCode = selectedProvince.getProvinceCode();
            String address = Constant.BASE_URL + "/" + provinceCode;
            queryFromServer(address, LEVEL_CITY);
        }
    }

    /**
     * 查询选中市内所有的县，优先从数据库查询，如果没有查询到再去服务器上查询。
     */
    private void queryCounties() {
        titleText.setText(selectedCity.getCityName());
        backButton.setVisibility(View.VISIBLE);
        countyList = LitePal.where("cityid = ?", String.valueOf(selectedCity.getId())).find(County.class);
        if (countyList.size() > 0) {
            dataList.clear();
            for (County county : countyList) {
                dataList.add(county.getCountyName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_COUNTY;
        } else {
            int provinceCode = selectedProvince.getProvinceCode();
            int cityCode = selectedCity.getCityCode();
            String address = Constant.BASE_URL + "/" + provinceCode + "/" + cityCode;
            queryFromServer(address, LEVEL_COUNTY);
        }
    }


    private void queryFromServer(String url, final int type) {
        showProgressDialog();

        HttpUtil.sendOkHttpRequest(url, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.d(TAG, "request " + url + " exception " + e.toString());
                closeProgressDialog();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String body = response.body().string();
                Log.d(TAG, "response=" + body);
                boolean result = false;
                if (type == LEVEL_PROVINCE) {
                    result = Utility.handleProvinceResponse(body);
                } else if (type == LEVEL_CITY) {
                    result = Utility.handleCityResponse(body,selectedProvince.getId());
                } else if (type == LEVEL_COUNTY) {
                    result = Utility.handleCountyResponse(body,selectedCity.getId());
                }
                closeProgressDialog();

                if (result) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeProgressDialog();
                            if (type == LEVEL_PROVINCE) {

                            } else if (type == LEVEL_CITY) {

                            } else if (type == LEVEL_COUNTY) {

                            }
                        }
                    });
                }
            }
        });
    }

    /**
     * 显示进度对话框
     */
    private void showProgressDialog() {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setMessage("正在加载...");
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

}
