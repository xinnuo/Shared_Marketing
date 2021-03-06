package com.ruanmeng.shared_marketing.Partner;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.RadioButton;

import com.baidu.location.BDLocation;
import com.flyco.animation.BounceEnter.BounceTopEnter;
import com.flyco.dialog.listener.OnBtnClickL;
import com.flyco.dialog.widget.MaterialDialog;
import com.ruanmeng.base.BaseActivity;
import com.ruanmeng.fragment.CenterFragment;
import com.ruanmeng.fragment.CustomerFragment;
import com.ruanmeng.fragment.HomeFragment;
import com.ruanmeng.fragment.HouseFragment;
import com.ruanmeng.fragment.TeamFragment;
import com.ruanmeng.model.HouseMessageEvent;
import com.ruanmeng.model.MainMessageEvent;
import com.ruanmeng.nohttp.CustomHttpListener;
import com.ruanmeng.service.UpdateService;
import com.ruanmeng.share.Const;
import com.ruanmeng.share.HttpIP;
import com.ruanmeng.shared_marketing.NoticeActivity;
import com.ruanmeng.shared_marketing.R;
import com.ruanmeng.shared_marketing.WebActivity;
import com.ruanmeng.utils.BDLocationUtil;
import com.ruanmeng.utils.CommonUtil;
import com.ruanmeng.utils.DialogHelper;
import com.ruanmeng.utils.Tools;
import com.yolanda.nohttp.NoHttp;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedHashSet;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;
import cn.jpush.android.api.JPushInterface;
import cn.jpush.android.api.TagAliasCallback;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnNeverAskAgain;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.RuntimePermissions;

@RuntimePermissions
public class MainActivity extends BaseActivity {

    @BindView(R.id.fl_main_container)
    FrameLayout mContainer;
    @BindView(R.id.rb_main_check_1)
    RadioButton rb_check1;
    @BindView(R.id.rb_main_check_2)
    RadioButton rb_check2;
    @BindView(R.id.rb_main_check_3)
    RadioButton rb_check3;
    @BindView(R.id.rb_main_check_4)
    RadioButton rb_check4;
    @BindView(R.id.rb_main_check_5)
    RadioButton rb_check5;

    private HomeFragment home;
    private HouseFragment house;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        transparentStatusBar(false);
        init_title();

        EventBus.getDefault().register(this);

        JPushInterface.requestPermission(getApplicationContext());

        JPushInterface.resumePush(getApplicationContext());
        Set<String> tagSet = new LinkedHashSet<>();
        switch (getString("user_type")) {
            case "4": //员工合伙人
                tagSet.add("employee");
                break;
            case "5": //业主合伙人
                tagSet.add("proprietor");
                break;
            case "6": //社会合伙人
                tagSet.add("social");
                break;
        }
        JPushInterface.setAliasAndTags( //设置别名与标签
                getApplicationContext(),
                "hkt_" + getString("user_id"),
                tagSet,
                new TagAliasCallback() {
                    @Override
                    public void gotResult(int responseCode, String alias, Set<String> set) {
                        Log.i("JPush", responseCode + ": " + alias + " , " + set.toString());
                    }
                });

        rb_check5.performClick();

        if (TextUtils.isEmpty(getString("city_name"))) {
            MainActivityPermissionsDispatcher.needsPermissionWithCheck(this);
        } else {
            getSystem();
        }

        getVersion();
    }

    @Override
    public void init_title() {
        rb_check1.setOnCheckedChangeListener(this);
        rb_check2.setOnCheckedChangeListener(this);
        rb_check3.setOnCheckedChangeListener(this);
        rb_check4.setOnCheckedChangeListener(this);
        rb_check5.setOnCheckedChangeListener(this);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        //instantiateItem从FragmentManager中查找Fragment，找不到就getItem新建一个，
        //setPrimaryItem设置隐藏和显示，最后finishUpdate提交事务。
        if (isChecked) {
            Fragment fragment = (Fragment) mFragmentPagerAdapter
                    .instantiateItem(mContainer, buttonView.getId());
            mFragmentPagerAdapter.setPrimaryItem(mContainer, 0, fragment);
            mFragmentPagerAdapter.finishUpdate(mContainer);
        }
    }

    private FragmentPagerAdapter mFragmentPagerAdapter = new FragmentPagerAdapter(
            getSupportFragmentManager()) {

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case R.id.rb_main_check_5:
                    home =  new HomeFragment();
                    return home;
                case R.id.rb_main_check_1:
                    return new CustomerFragment();
                case R.id.rb_main_check_2:
                    return new TeamFragment();
                case R.id.rb_main_check_3:
                    house = new HouseFragment();
                    return house;
                case R.id.rb_main_check_4:
                    return new CenterFragment();
                default:
                    return new CustomerFragment();
            }

        }

        @Override
        public int getCount() {
            return 5;
        }

    };

    @Override
    public void doClick(View v) {
        Intent intent;
        switch (v.getId()) {
            // 首页
            case R.id.tv_fragment_home_location:
                startActivity(CityActivity.class);
                break;
            case R.id.iv_fragment_home_msg:
                startActivity(NoticeActivity.class);
                break;
            case R.id.iv_fragment_home_search:
                intent = new Intent(this, SearchActivity.class);
                intent.putExtra("type", "1");
                startActivity(intent);
                break;
            case R.id.ll_fragment_home_trule:
                intent = new Intent(this, WebActivity.class);
                intent.putExtra("title", "推荐用户规则");
                intent.putExtra("type", "8");
                startActivity(intent);
                break;
            case R.id.ll_fragment_home_yrule:
                intent = new Intent(this, WebActivity.class);
                intent.putExtra("title", "佣金结算说明");
                intent.putExtra("type", "5");
                startActivity(intent);
                break;

            // 客户
            case R.id.tv_fragment_customer_location:
                startActivity(CityActivity.class);
                break;
            case R.id.iv_fragment_customer_msg:
                startActivity(NoticeActivity.class);
                break;
            case R.id.iv_fragment_customer_search:
                intent = new Intent(this, SearchActivity.class);
                intent.putExtra("type", "1");
                startActivity(intent);
                break;
            case R.id.ll_fragment_customer_recommend:
                startActivity(RecommendActivity.class);
                break;

            //团队
            case R.id.ll_fragment_team_lower:
                startActivity(LowerActivity.class);
                break;

            // 我的
            case R.id.iv_fragment_center_setting:
                startActivity(SettingActivity.class);
                break;
            case R.id.iv_fragment_center_img:
            case R.id.ll_fragment_center_info:
                startActivity(InfoActivity.class);
                break;
            case R.id.ll_fragment_center_yue:
                startActivity(AccountActivity.class);
                break;
            case R.id.ll_fragment_center_yongjin:
            case R.id.btn_fragment_center_check:
                startActivity(CommissionActivity.class);
                break;
        }
    }

    private void checkVersion(String mContent, final String mUrl, final String version) {
        DialogHelper.showDialog(
                this,
                "版本更新",
                mContent,
                "取消",
                "更新",
                false, new DialogHelper.HintCallBack() {
                    @Override
                    public void doWork() {
                        Intent intent = new Intent(baseContext, UpdateService.class);
                        intent.putExtra("url", mUrl);
                        intent.putExtra("versionCode", version);
                        startService(intent);
                    }
                });
        /*DialogHelper.showDialog(
                this,
                "版本更新",
                mContent,
                "更新", new DialogHelper.HintCallBack() {
                    @Override
                    public void doWork() {
                        Intent intent = new Intent(baseContext, UpdateService.class);
                        intent.putExtra("url", mUrl);
                        intent.putExtra("versionCode", version);
                        startService(intent);
                    }
                }
        );*/
    }

    private void getVersion() {
        mRequest = NoHttp.createStringRequest(HttpIP.appVersion, Const.POST);
        mRequest.add("current_version", Tools.getVersion(this));

        getRequest(new CustomHttpListener<JSONObject>(baseContext, false, JSONObject.class) {
            @Override
            public void doWork(JSONObject data, String code) {
                /**
                 * {
                 *   "msg": "获取成功！",
                 *   "msgcode": 1,
                 *   "data": {
                 *     "description": "",
                 *     "version": "",
                 *     "url": ""
                 *   }
                 * }
                 */
                try {
                    String version = data.getJSONObject("data").getString("version");
                    String link = data.getJSONObject("data").getString("url");
                    String remark = data.getJSONObject("data").getString("description");

                    int version_new = Integer.parseInt(version.replace(".", ""));
                    int version_old = Integer.parseInt(Tools.getVersion(baseContext).replace(".", ""));

                    if (version_new > version_old) checkVersion(remark, link, version);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, false);
    }

    private void getSystem() {
        mRequest = NoHttp.createStringRequest(HttpIP.sysParam, Const.POST);
        mRequest.add("province_id", getString("province_id"));
        mRequest.add("city_id", getString("city_id"));

        getRequest(new CustomHttpListener<JSONObject>(baseContext, false, JSONObject.class) {
            @Override
            public void doWork(JSONObject data, String code) {
                try {
                    Const.SYSTEM_PARAM = data.getJSONObject("data").toString();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, false);
    }

    private void getLocation(String name) {
        mRequest = NoHttp.createStringRequest(HttpIP.getLocationId, Const.POST);
        mRequest.add("location_name", name);

        getRequest(new CustomHttpListener<JSONObject>(baseContext, false, JSONObject.class) {
            @Override
            public void doWork(JSONObject data, String code) {
                /**
                 * {
                 *   "msgcode": 1,
                 *   "msg": "获取成功！",
                 *   "data": {
                 *      "id": "410100",
                 *      "parent_id": "410000",
                 *      "name": "郑州市",
                 *      "is_open": "2",
                 *      "level_type": "2"
                 *   }
                 * }
                 */
                try {
                    JSONObject obj = data.getJSONObject("data");

                    putString("province_id", obj.getString("parent_id")); //省份id
                    putString("city_id", obj.getString("id"));            //城市id
                    putString("city_name", obj.getString("name"));        //城市名称
                    putString("is_open", obj.getString("is_open"));       //是否开通1：未开通 2：已开通

                    if (home != null) home.setLocation(obj.getString("name"));

                    getSystem();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, false);
    }

    public void jumpToHouse() {
        rb_check3.performClick();
    }

    private long exitTime = 0;

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
            if ((System.currentTimeMillis() - exitTime) > 2000) {
                CommonUtil.showToask(getApplicationContext(), "再按一次退出程序");
                exitTime = System.currentTimeMillis();
            } else {
                onBackPressed();
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        EventBus.getDefault().unregister(this);                        //反注册eventBus
        BDLocationUtil.getInstance(this).stopLocation();               //停止定位服务
        super.onBackPressed();
    }

    @Subscribe
    public void onHouseMessageEvent(HouseMessageEvent event) {
        if (home != null) home.updateHouseList();
        if (house != null) house.updateHouseList();
    }

    @Subscribe
    public void onMessageEvent(MainMessageEvent event) {
        if (TextUtils.equals("4", event.getId())
                || TextUtils.equals("5", event.getId())
                || TextUtils.equals("6", event.getId())) {
            getLocation(event.getName());
        }
    }

    @NeedsPermission({ Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.WRITE_EXTERNAL_STORAGE })
    void needsPermission() {
        BDLocationUtil.getInstance(this).startLocation(new BDLocationUtil.LocationCallback() {
            @Override
            public void doWork(BDLocation location, int code) {
                if (code == 1) {
                    getLocation(location.getCity());
                }
            }
        }, 1);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        MainActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    @OnShowRationale({ Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.WRITE_EXTERNAL_STORAGE })
    void showRationale(final PermissionRequest request) {
        final MaterialDialog materialDialog = new MaterialDialog(this);
        materialDialog.content("需要使用定位功能获取当前位置")
                .title("需要权限")
                .btnText("取消", "确定")
                .btnTextColor(
                        getResources().getColor(R.color.black),
                        getResources().getColor(R.color.blue))
                .showAnim(new BounceTopEnter())
                .show();
        materialDialog.setOnBtnClickL(
                new OnBtnClickL() { //left btn click listener
                    @Override
                    public void onBtnClick() {
                        materialDialog.dismiss();
                        request.cancel();
                    }
                },
                new OnBtnClickL() { //right btn click listener
                    @Override
                    public void onBtnClick() {
                        materialDialog.dismiss();
                        request.proceed();
                    }
                });
    }

    @OnPermissionDenied({ Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.WRITE_EXTERNAL_STORAGE })
    void permissionDenied() {
        showToask("请求权限被拒绝，请重新开启权限");
    }

    @OnNeverAskAgain({ Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.WRITE_EXTERNAL_STORAGE })
    void neverAskAgain() {
        showToask("不再询问权限");
    }
}
