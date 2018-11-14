package com.android.framework.jc.base;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.android.framework.jc.base.mvp.IFkContract;
import com.android.framework.jc.wrapper.IViewWrapper;

import java.util.ArrayList;

/**
 * @author Mr.Hu(Jc) JCFramework
 * @create 2018/7/16 18:28
 * @describe
 * @update
 */
public abstract class FkFragment<P extends IFkContract.IPresenter> extends Fragment implements IFkContract.IView<P> {
    protected Context mContext;
    private final ArrayList<IViewWrapper> headWrappers = new ArrayList<>();
    private final ArrayList<IViewWrapper> footWrappers = new ArrayList<>();
    protected P mPresenter;
    private String mFragmentTag;

    @Override
    public void onInflate(Context context, AttributeSet attrs, Bundle savedInstanceState) {
        super.onInflate(context, attrs, savedInstanceState);
    }

    @CallSuper
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        for (IViewWrapper wrapper : headWrappers) {
            wrapper.onWrapperAttach(context);
        }
        for (IViewWrapper wrapper : footWrappers) {
            wrapper.onWrapperAttach(context);
        }

        mContext = context;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = onCreateRootView(inflater, container, savedInstanceState);
        if (headWrappers.size() > 0 || footWrappers.size() > 0) {
            LinearLayout result;
            if (rootView instanceof LinearLayout && ((LinearLayout) rootView).getOrientation() == LinearLayout.VERTICAL) {
                result = (LinearLayout) rootView;
                addView(result, inflater, container, savedInstanceState);
            } else {
                result = new LinearLayout(mContext);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                result.setLayoutParams(params);
                result.setOrientation(LinearLayout.VERTICAL);
                result.addView(rootView);
                addView(result, inflater, container, savedInstanceState);
            }
            return result;
        }

        return rootView;
    }

    /**
     * 添加头部装饰者
     *
     * @param wrapper
     *
     * @return
     */
    public FkFragment addHeadWrapper(IViewWrapper wrapper) {

        headWrappers.add(wrapper);
        return this;
    }

    /**
     * 添加尾部装饰者
     *
     * @param wrapper
     *
     * @return
     */
    public FkFragment addFootWrapper(IViewWrapper wrapper) {
        footWrappers.add(wrapper);
        return this;
    }

    /**
     * 根据上层的view动态添加头部跟尾部装饰者View进去
     *
     * @param rootView
     * @param inflater
     * @param container
     * @param savedInstanceState
     */
    private void addView(LinearLayout rootView, LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        for (int i = 0; i < headWrappers.size(); i++) {
            View headView = headWrappers.get(i).onCreateWrapperView(inflater, null, savedInstanceState);
            if (headView == null) {
                continue;
            }
            if (headView.getParent() != null) {
                throw new IllegalArgumentException("传入头部装饰者的rootView必须没有父控件！");
            }
            rootView.addView(headView, i);

        }
        for (IViewWrapper wrapper : footWrappers) {
            View footView = wrapper.onCreateWrapperView(inflater, null, savedInstanceState);
            if (footView == null) {
                continue;
            }
            if (footView.getParent() != null) {
                throw new IllegalArgumentException("传入尾部装饰者的rootView必须没有父控件！");
            }
            rootView.addView(footView, rootView.getChildCount() - 1);
        }

    }

    protected void onFinish() {
        if (mContext instanceof Activity) {
            ((Activity) mContext).finish();
        }
    }

    protected void onResultOkFinish() {
        if (mContext instanceof Activity) {
            ((Activity) mContext).setResult(Activity.RESULT_OK);
            ((Activity) mContext).finish();
        }
    }

    /**
     * 上层添加布局View
     *
     * @param inflater
     * @param container
     * @param savedInstanceState
     *
     * @return
     */
    protected abstract View onCreateRootView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState);


    @Override
    public void setPresenter(P presenter) {
        mPresenter = presenter;
        mPresenter.onAttachView(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mPresenter != null) {
            mPresenter.onDetachView();
        }
        for (IViewWrapper wrapper : headWrappers) {
            wrapper.onWrapperDestroy();
        }
        for (IViewWrapper wrapper : footWrappers) {
            wrapper.onWrapperDestroy();
        }
    }

    protected void setFragmentTag(String tag) {
        this.mFragmentTag = tag;
    }

    protected String getFragmentTag() {
        return mFragmentTag;
    }
}
