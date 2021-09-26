package com.ooftf.fake.location

import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.location.provider.ProviderProperties
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.baidu.mapapi.map.*
import com.baidu.mapapi.model.LatLng
import com.baidu.mapapi.search.core.SearchResult
import com.baidu.mapapi.search.geocode.*
import com.ooftf.arch.frame.mvvm.activity.BaseViewBindingActivity
import com.ooftf.fake.location.databinding.ActivityMainBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.baidu.mapapi.utils.CoordinateConverter
import com.ooftf.log.JLog


class MainActivity : BaseViewBindingActivity<ActivityMainBinding>() {
    val locationManager by lazy {
        getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }
    val centerMarker by lazy {
        val bitmap = BitmapDescriptorFactory
            .fromResource(R.mipmap.location)
        //构建MarkerOption，用于在地图上添加Marker
        val option: OverlayOptions = MarkerOptions()
            .zIndex(100)
            .position(LatLng(0.0,0.0))
            .icon(bitmap)
        //在地图上添加Marker，并显示
        binding.mapView.map.addOverlay(option) as Marker
    }

    val lockMarker by lazy {
        val bitmap = BitmapDescriptorFactory
            .fromResource(R.mipmap.lock_location)
        //构建MarkerOption，用于在地图上添加Marker
        val option: OverlayOptions = MarkerOptions()
            .zIndex(100)
            .visible(false)
            .anchor(0.5f,0.8f)
            .position(LatLng(0.0,0.0))
            .icon(bitmap)
        //在地图上添加Marker，并显示
        binding.mapView.map.addOverlay(option) as Marker
    }
    var isFaking = false
    val mGeoCoder = GeoCoder.newInstance().apply {
        setOnGetGeoCodeResultListener(object : OnGetGeoCoderResultListener {
            override fun onGetGeoCodeResult(geoCodeResult: GeoCodeResult) {}
            override fun onGetReverseGeoCodeResult(reverseGeoCodeResult: ReverseGeoCodeResult) {
                if (reverseGeoCodeResult.error == SearchResult.ERRORNO.PERMISSION_UNFINISHED) {
                    toast("获取位置详细信息报错：${reverseGeoCodeResult.error}")
                } else if (reverseGeoCodeResult.error == SearchResult.ERRORNO.NO_ERROR) { //获取城市
                    toast(reverseGeoCodeResult.address)
                }
            }
        })

    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.mapView.map.uiSettings.isRotateGesturesEnabled = false
        binding.mapView.map.setOnMapStatusChangeListener(object :BaiduMap.OnMapStatusChangeListener{
            override fun onMapStatusChangeStart(p0: MapStatus?) {

            }

            override fun onMapStatusChangeStart(p0: MapStatus?, p1: Int) {

            }

            override fun onMapStatusChange(mapStatus: MapStatus) {
                centerMarker.position = mapStatus.target
            }

            override fun onMapStatusChangeFinish(mapStatus: MapStatus) {
                if(mapStatus.targetScreen.x !=-1&&mapStatus.targetScreen.y!=-1){
                    geoCoder(mapStatus.target)
                }
            }

        })
        binding.fake.setOnClickListener {
            isFaking = !isFaking
            if(isFaking){
                binding.fake.setImageResource(R.mipmap.stop_fake)
                binding.mapView.map.mapStatus.target.run {
                    startFakeLocation(longitude,latitude)
                }
                showLockMarker(binding.mapView.map.mapStatus.target)
            }else{
                binding.fake.setImageResource(R.mipmap.start_fake)
                stopFakeLocation()
                hideLockMarker()
            }
        }
    }

    private fun showLockMarker(target: LatLng) {
        toast(target.toString())
        lockMarker.position = target
        lockMarker.isVisible = true
    }

    private fun hideLockMarker(){
        lockMarker.isVisible = false
    }


    fun geoCoder(center: LatLng) {
        center.let {
            mGeoCoder.reverseGeoCode(
                ReverseGeoCodeOption().apply {
                    newVersion(1)
                    pageNum(0)
                    pageSize(20)
                    location(it)
                }
            )
        }

    }
    var initTestProvider = false
    private fun initTestProvider() {
        try {
            locationManager.addTestProvider(
                LocationManager.GPS_PROVIDER, false, true, false, false, true, true,
                true, ProviderProperties.POWER_USAGE_HIGH, ProviderProperties.ACCURACY_FINE
            )
            locationManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true)
            initTestProvider = true
        }catch (e:Throwable){
            toast("请在开发者选项中，允许应用模拟位置！")
        }

    }

    fun startFakeLocation(longitude:Double, latitude:Double){

       val result =  CoordinateUtils.bd09ToWGS84(longitude,latitude)

//        val converter = CoordinateConverter()
//            .from(CoordinateConverter.CoordType.GPS)
//            .coord(LatLng(latitude,longitude))
//        val desLatLng = converter.convert()
        initTestProvider()
        if(initTestProvider){
            lifecycleScope.launch {
                while (isFaking){
                    val location = Location(LocationManager.GPS_PROVIDER)
                    location.time = System.currentTimeMillis()
                    location.latitude = result[1]
                    location.longitude = result[0]
                    location.altitude = 2.0
                    location.accuracy = 2f
                    location.time = System.currentTimeMillis()
                    location.elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos();
                    locationManager.setTestProviderLocation(LocationManager.GPS_PROVIDER,location)
                    JLog.e("setTestProviderLocation")
                    delay(10)
                }
            }
        }
    }

    fun stopFakeLocation(){
        //locationManager.addTestProvider()

        locationManager.removeTestProvider(LocationManager.GPS_PROVIDER)
    }
}