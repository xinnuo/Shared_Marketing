package com.ruanmeng.shared_marketing.Distribution;


import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.dialog.nohttp.CallServer;
import com.ruanmeng.model.CommonData;
import com.ruanmeng.model.NoticeData;
import com.ruanmeng.nohttp.CustomHttpListener;
import com.ruanmeng.share.Const;
import com.ruanmeng.share.HttpIP;
import com.ruanmeng.shared_marketing.NoticeDetailActivity;
import com.ruanmeng.shared_marketing.R;
import com.ruanmeng.utils.MD5Util;
import com.ruanmeng.utils.PreferencesUtils;
import com.ruanmeng.view.AlwaysMarqueeTextView;
import com.yolanda.nohttp.NoHttp;
import com.yolanda.nohttp.rest.Request;
import com.zhy.adapter.recyclerview.CommonAdapter;
import com.zhy.adapter.recyclerview.base.ViewHolder;

import net.cachapa.expandablelayout.ExpandableLayout;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * A simple {@link Fragment} subclass.
 */
public class DistributionFragment extends Fragment {

    @BindView(R.id.tv_fragment_company_location)
    TextView tv_location;
    @BindView(R.id.iv_fragment_company_msg)
    ImageView iv_msg;
    @BindView(R.id.lv_fragment_company_list)
    RecyclerView mRecyclerView;
    @BindView(R.id.tv_empty_hint)
    TextView tv_hint;
    @BindView(R.id.ll_empty_hint)
    LinearLayout ll_hint;
    @BindView(R.id.rl_fragment_company_refresh)
    SwipeRefreshLayout mRefresh;
    @BindView(R.id.tv_fragment_company_gonggao)
    AlwaysMarqueeTextView tv_notice;
    @BindView(R.id.el_fragment_company_expand)
    ExpandableLayout expand;

    private Request<String> mRequest;
    private List<CommonData.CommonInfo> list = new ArrayList<>();
    private CommonAdapter<CommonData.CommonInfo> adapter;

    private String id_notice;

    //调用这个方法切换时不会释放掉Fragment
    @Override
    public void setMenuVisibility(boolean menuVisible) {
        super.setMenuVisibility(menuVisible);
        if (this.getView() != null)
            this.getView().setVisibility(menuVisible ? View.VISIBLE : View.GONE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_company, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        init();

        getNotice();

        mRefresh.setRefreshing(true);
        getData();
    }

    @Override
    public void onStart() {
        super.onStart();

        setLocation(PreferencesUtils.getString(getActivity(), "city_name"));
    }

    private void init() {
        tv_hint.setText("暂无企业合伙人信息");
        setNoticeIcon(false);

        mRefresh.setColorSchemeResources(R.color.colorAccent);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(linearLayoutManager);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());

        adapter = new CommonAdapter<CommonData.CommonInfo>(getActivity(), R.layout.item_help_list, list) {
            @Override
            protected void convert(ViewHolder holder, final CommonData.CommonInfo info, int position) {
                holder.setText(R.id.tv_item_help_title, info.getCompany_name());

                holder.setVisible(R.id.v_item_help_divider_1, holder.getLayoutPosition() + 1 != mDatas.size());
                holder.setVisible(R.id.v_item_help_divider_2, holder.getLayoutPosition() + 1 == mDatas.size());

                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(getActivity(), SaleActivity.class);
                        intent.putExtra("id", info.getId());
                        intent.putExtra("name", info.getCompany_name());
                        startActivity(intent);
                    }
                });
            }
        };

        mRecyclerView.setAdapter(adapter);

        mRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                getData();
            }
        });

        mRecyclerView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return mRefresh.isRefreshing();
            }
        });
    }

    public void setLocation(String city_name) {
        tv_location.setText(city_name);
    }

    public void setNoticeIcon(boolean isShown) {
        iv_msg.setImageResource(isShown ? R.mipmap.ico_ann_b : R.mipmap.ico_ann);
    }

    public void getData() {
        mRequest = NoHttp.createStringRequest(HttpIP.companyList2, Const.POST);
        mRequest.add("user_id", PreferencesUtils.getString(getActivity(), "user_id"));
        mRequest.add("token", MD5Util.md5(Const.timeStamp));
        mRequest.add("time", String.valueOf(Const.timeStamp));

        // 添加到请求队列
        CallServer.getRequestInstance().add(getActivity(), mRequest,
                new CustomHttpListener<CommonData>(getActivity(), true, CommonData.class) {
                    @Override
                    public void doWork(CommonData data, String code) {
                        list.clear();
                        list.addAll(data.getData());
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onFinally(JSONObject obj, String code, boolean isSucceed) {
                        mRefresh.setRefreshing(false);

                        ll_hint.setVisibility(list.size() == 0 ? View.VISIBLE : View.GONE);
                    }
                }, false);
    }

    private void getNotice() {
        mRequest = NoHttp.createStringRequest(HttpIP.noticeList, Const.POST);
        mRequest.add("role_type", PreferencesUtils.getString(getActivity(), "user_type"));
        mRequest.add("pindex", 1);
        mRequest.add("token", MD5Util.md5(Const.timeStamp));
        mRequest.add("time", String.valueOf(Const.timeStamp));

        // 添加到请求队列
        CallServer.getRequestInstance().add(getActivity(), mRequest,
                new CustomHttpListener<NoticeData>(getActivity(), true, NoticeData.class) {
                    @Override
                    public void doWork(NoticeData data, String code) {

                        if (data.getData().size() > 0) {
                            id_notice = data.getData().get(0).getId();
                            tv_notice.setText("公告：" + data.getData().get(0).getTitle());
                        } else
                            expand.collapse();

                    }

                    @Override
                    public void onFinally(JSONObject obj, String code, boolean isSucceed) {
                        if (!TextUtils.equals("1", code)) expand.collapse();
                    }
                }, false);
    }

    @OnClick({R.id.iv_fragment_company_close,
            R.id.tv_fragment_company_gonggao})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.iv_fragment_company_close:
                expand.collapse();
                break;
            case R.id.tv_fragment_company_gonggao:
                if (id_notice != null) {
                    Intent intent = new Intent(getActivity(), NoticeDetailActivity.class);
                    intent.putExtra("id", id_notice);
                    startActivity(intent);
                }
                break;
        }
    }

}
