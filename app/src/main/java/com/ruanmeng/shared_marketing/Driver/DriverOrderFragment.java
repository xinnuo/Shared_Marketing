package com.ruanmeng.shared_marketing.Driver;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
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

import com.bumptech.glide.Glide;
import com.dialog.nohttp.CallServer;
import com.makeramen.roundedimageview.RoundedImageView;
import com.ruanmeng.model.CommonData;
import com.ruanmeng.model.GrabData;
import com.ruanmeng.nohttp.CustomHttpListener;
import com.ruanmeng.share.Const;
import com.ruanmeng.share.HttpIP;
import com.ruanmeng.shared_marketing.R;
import com.ruanmeng.utils.MD5Util;
import com.ruanmeng.utils.PreferencesUtils;
import com.yolanda.nohttp.NoHttp;
import com.yolanda.nohttp.rest.Request;
import com.zhy.adapter.recyclerview.CommonAdapter;
import com.zhy.adapter.recyclerview.base.ViewHolder;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * A simple {@link Fragment} subclass.
 */
public class DriverOrderFragment extends Fragment {

    @BindView(R.id.tv_fragment_grab_title)
    TextView tv_title;
    @BindView(R.id.iv_fragment_grab_search)
    ImageView iv_search;
    @BindView(R.id.lv_fragment_grab_list)
    RecyclerView mRecyclerView;
    @BindView(R.id.iv_empty_hint)
    ImageView iv_hint;
    @BindView(R.id.tv_empty_hint)
    TextView tv_hint;
    @BindView(R.id.tv_fragment_grab_total)
    TextView tv_total;
    @BindView(R.id.ll_empty_hint)
    LinearLayout ll_hint;
    @BindView(R.id.rl_fragment_grab_refresh)
    SwipeRefreshLayout mRefresh;

    private Request<String> mRequest;
    private List<CommonData.CommonInfo> list = new ArrayList<>();
    private LinearLayoutManager linearLayoutManager;
    private CommonAdapter<CommonData.CommonInfo> adapter;
    private boolean isLoadingMore;

    private int pageNum = 1;

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
        View view = inflater.inflate(R.layout.fragment_grab, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        init();

        mRefresh.setRefreshing(true);
        getData(pageNum);
    }

    private void init() {
        tv_title.setText("我的订单");
        iv_search.setVisibility(View.INVISIBLE);
        tv_total.setVisibility(View.GONE);
        tv_hint.setText("暂无订单信息");

        mRefresh.setColorSchemeResources(R.color.colorAccent);
        linearLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(linearLayoutManager);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());

        adapter = new CommonAdapter<CommonData.CommonInfo>(getActivity(), R.layout.item_grab_driver_list, list) {
            @Override
            protected void convert(ViewHolder holder, final CommonData.CommonInfo info, int position) {
                holder.setText(R.id.tv_item_grab_driver_name, "申请人：" + info.getName());
                holder.setText(R.id.tv_item_grab_driver_time, info.getPick_up_time());
                holder.setText(R.id.tv_item_grab_driver_addr1, info.getDeparture_place());
                holder.setText(R.id.tv_item_grab_driver_addr2, info.getDestination_place());
                holder.setText(R.id.tv_item_grab_driver_num, info.getNumber_of_person() + "人");

                TextView tv_status = holder.getView(R.id.tv_item_grab_driver_status);
                tv_status.setTextColor(Color.parseColor(TextUtils.equals("3", info.getO_status()) ? "#0099CC" : "#F24500"));
                tv_status.setText(TextUtils.equals("3", info.getO_status()) ? "已完成" : "等待出发");

                RoundedImageView iv_img = holder.getView(R.id.iv_item_grab_driver_img);
                Glide.with(mContext)
                        .load(info.getLogo())
                        .placeholder(R.mipmap.personal_a20) // 等待时的图片
                        .error(R.mipmap.personal_a20) // 加载失败的图片
                        .crossFade()
                        .dontAnimate()
                        .into(iv_img);

                holder.setVisible(
                        R.id.tv_item_grab_driver_divider,
                        holder.getLayoutPosition() + 1 == mDatas.size());

                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(getActivity(), OrderActivity.class);
                        intent.putExtra("info", info);
                        startActivity(intent);
                    }
                });
            }
        };

        mRecyclerView.setAdapter(adapter);

        mRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                getData(1);
            }
        });

        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                int total = linearLayoutManager.getItemCount();
                int lastVisibleItem = linearLayoutManager.findLastVisibleItemPosition();
                //lastVisibleItem >= totalItemCount - 4 表示剩下4个item自动加载，各位自由选择
                // dy > 0 表示向下滑动
                if (lastVisibleItem >= total - 1 && dy > 0) {
                    if (!isLoadingMore) {
                        isLoadingMore = true;
                        getData(pageNum);
                    }
                }
            }
        });

        mRecyclerView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return mRefresh.isRefreshing();
            }
        });
    }

    public void setRefreshList() {
        mRefresh.setRefreshing(true);

        list.clear();
        adapter.notifyDataSetChanged();

        pageNum = 1;
        getData(pageNum);
    }

    public void getData(final int pindex) {
        mRequest = NoHttp.createStringRequest(HttpIP.orderList, Const.POST);
        mRequest.add("user_id", PreferencesUtils.getString(getActivity(), "user_id"));
        mRequest.add("pindex", pindex);
        mRequest.add("token", MD5Util.md5(Const.timeStamp));
        mRequest.add("time", String.valueOf(Const.timeStamp));

        // 添加到请求队列
        CallServer.getRequestInstance().add(getActivity(), mRequest,
                new CustomHttpListener<CommonData>(getActivity(), true, CommonData.class) {
                    @Override
                    public void doWork(CommonData data, String code) {
                        if (pindex == 1) list.clear();

                        list.addAll(data.getData());
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onFinally(JSONObject obj, String code, boolean isSucceed) {
                        mRefresh.setRefreshing(false);
                        isLoadingMore = false;

                        if (TextUtils.equals("1", code)) {
                            if (pindex == 1) pageNum = pindex;
                            pageNum++;
                        }

                        ll_hint.setVisibility(list.size() == 0 ? View.VISIBLE : View.GONE);
                    }
                }, false);
    }

}
