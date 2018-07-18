package com.android.framework_test;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.android.framework.jc.recyclerview.NormalRvAdapter;
import com.android.framework.jc.recyclerview.ViewHolder;
import com.android.framework.jc.util.ScreenUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author hjc
 */
public class MainActivity extends AppCompatActivity {

    private MainAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        RecyclerView recyclerView=findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(mAdapter=new MainAdapter());
        System.out.println("屏幕高度："+ScreenUtils.getScreenHeight());
        System.out.println("屏幕宽度："+ScreenUtils.getScreenWidth());
        System.out.println("状态栏高度："+ScreenUtils.getStatusHeight());
        System.out.println("全屏高度："+ScreenUtils.getFullScreenHeight());
        System.out.println("虚拟按键高度："+ScreenUtils.getNavigationBarHeight());

        List<String> list=new ArrayList<>();
        list.add("K线测试");

        mAdapter.setList(list).notifyDataSetChanged();
    }


    private static class MainAdapter extends NormalRvAdapter<String> {

        public MainAdapter() {
            super(R.layout.item_main_adapter);
        }

        @Override
        public void convert(ViewHolder holder, String bean, int position) {

            holder.setText(R.id.tv_name,bean);
        }
    }

    private static class MainBean{
        String name;
        String className;

        public MainBean(String name, String className) {
            this.name = name;
            this.className = className;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getClassName() {
            return className;
        }

        public void setClassName(String className) {
            this.className = className;
        }


    }
}
