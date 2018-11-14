package com.android.framework.jc;

import android.Manifest;
import android.app.Activity;
import android.text.TextUtils;

import com.android.framework.jc.base.FkPermission;
import com.android.framework.jc.data.bean.DownloadBean;
import com.android.framework.jc.exception.NetworkErrorException;
import com.android.framework.jc.functions.RetryWhenFunction;
import com.android.framework.jc.network.RetrofitManager;
import com.android.framework.jc.network.interceptor.ProgressInterceptor;
import com.android.framework.jc.util.FileUtils;
import com.android.framework.jc.util.FormatUtils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.LongConsumer;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Streaming;
import retrofit2.http.Url;

/**
 * @author Mr.Hu(Jc) JCFramework
 * @create 2018/11/1 14:30
 * @describe 文件下载类
 * @update
 */
public class FkDownload {
    private final static String BASE_URL = "https://github.com/MrHuGit/";
    private final static String CONNECT_TIMEOUT_KEY = "okHttpConnectTimeout";
    private final static String READ_TIMEOUT_KEY = "okHttpReadTimeout";
    private final static String WRITE_TIMEOUT_KEY = "okHttpWriteTimeout";
    private final Build mBuild;

    private FkDownload(Build build) {
        this.mBuild = build;
    }

    /**
     * 开始下载
     */
    public void onStartDownload() {
        Activity activity = JcFramework.getInstance().getTopActivity();
        FkPermission.with(activity)
                .permission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .permission(Manifest.permission.READ_EXTERNAL_STORAGE)
                .listener(new FkPermission.IPermissionListener() {
                    @Override
                    public void onGrant() {
                        DownloadBean downloadBean = createDownloadBean();
                        if (mBuild.cache && downloadBean.getDownLength() == downloadBean.getTotalLength()) {
                            onSuccess(downloadBean.getSavePath());
                        } else {
                            NetworkManager.getInstance().addDispose(mBuild.downloadUrl, download(downloadBean));
                        }
                    }

                    @Override
                    public void onRationale(String[] permissions) {
                        onError(new NetworkErrorException(activity.getString(R.string.Authorized_refused_to)));
                    }

                    @Override
                    public void onDenied(String[] permissions) {
                        onError(new NetworkErrorException(activity.getString(R.string.Authorized_refused_to)));
                    }
                }).request();
    }

    /**
     * 取消下载
     *
     * @param downloadUrl
     *         下载地址
     */
    public void onStopDownload(String downloadUrl) {
        NetworkManager.getInstance().dispose(downloadUrl);
        if (mBuild.cache) {
            Cache.getInstance().update(Cache.getInstance().getCache(downloadUrl));
        }
    }

    /**
     * 获取下载bean
     *
     * @return 下载的bean
     */
    private DownloadBean createDownloadBean() {
        String downloadUrl = mBuild.downloadUrl;
        String savePath = mBuild.savePath;
        if (TextUtils.isEmpty(savePath)) {
            savePath = FileUtils.getSavePath(downloadUrl);
        }
        DownloadBean downloadBean = null;
        if (mBuild.cache) {
            downloadBean = Cache.getInstance().getCache(downloadUrl, savePath);
        }
        if (downloadBean == null) {
            downloadBean = new DownloadBean();
            downloadBean.setSavePath(savePath);
            downloadBean.setFileUrl(downloadUrl);
            try {
                URL url = new URL(downloadUrl);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                int fileLength = httpURLConnection.getContentLength();
                httpURLConnection.disconnect();
                downloadBean.setTotalLength(fileLength);
            } catch (MalformedURLException e) {
                e.printStackTrace();
                downloadBean.setTotalLength(-1);
            } catch (IOException e) {
                e.printStackTrace();
                downloadBean.setTotalLength(-1);
            }


        }
        if (mBuild.cache) {
            Cache.getInstance().update(downloadBean);
        }
        return downloadBean;
    }


    /**
     * 下载
     *
     * @param downloadBean
     *         下载bean
     *
     * @return disposable
     */
    private Disposable download(DownloadBean downloadBean) {
        OkHttpClient.Builder okHttpClientBuild = createOkHttpClientBuild();
        if (mBuild.onProgressListener != null) {
            okHttpClientBuild.addInterceptor(new ProgressInterceptor((progress, total, bytesRead, done) ->
                    FkScheduler.runOnUiThread(() -> mBuild.onProgressListener.onProgress(progress, total, done))));
        }
        return RetrofitManager.getInstance()
                .getRetrofitBuilder()
                .client(okHttpClientBuild.build())
                .baseUrl(BASE_URL)
                .build()
                .create(IDownloadService.class)
                .download("bytes=" + downloadBean.getDownLength() + "-", downloadBean.getDownloadUrl())
                .doOnRequest(new LongConsumer() {
                    @Override
                    public void accept(long t) throws Exception {

                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .retryWhen(new RetryWhenFunction<>(mBuild.retryCount,mBuild.delay))
                .doOnNext(responseBody -> FileUtils.write(responseBody, downloadBean))
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally(() -> {

                })
                .subscribe(responseBody -> onSuccess(downloadBean.getSavePath()), this::onError);
    }


    private OkHttpClient.Builder createOkHttpClientBuild() {
        final long connectTimeout = FormatUtils.parseLong(ConfigManager.getInstance().getValue(CONNECT_TIMEOUT_KEY));
        final long readTimeout = FormatUtils.parseLong(ConfigManager.getInstance().getValue(READ_TIMEOUT_KEY));
        final long writeTimeout = FormatUtils.parseLong(ConfigManager.getInstance().getValue(WRITE_TIMEOUT_KEY));
        return new OkHttpClient.Builder()
                .connectTimeout(connectTimeout <= 0 ? 10000 : connectTimeout, TimeUnit.MILLISECONDS)
                .readTimeout(readTimeout <= 0 ? 10000 : readTimeout, TimeUnit.MILLISECONDS)
                .writeTimeout(writeTimeout <= 0 ? 10000 : writeTimeout, TimeUnit.MILLISECONDS);
    }

    /**
     * 下载失败回调
     *
     * @param throwable
     *         异常
     */
    private void onError(Throwable throwable) {
        if (mBuild.onErrorListener != null) {
            mBuild.onErrorListener.onError(throwable);
        }
    }

    /**
     * 下载成功回调
     *
     * @param savePath
     *         保存路径
     */
    private void onSuccess(String savePath) {
        if (mBuild.onFinishListener != null) {
            mBuild.onFinishListener.onFinish(savePath);
        }
    }

    public interface OnFinishListener {
        /**
         * 下载成功回调
         *
         * @param savePath
         *         本地保存路径
         */
        void onFinish(String savePath);
    }

    public interface OnProgressListener {
        /**
         * 下载进度回调
         *
         * @param currentLength
         *         当前读取的进度
         * @param totalLength
         *         总长度
         * @param done
         *         是否完成
         */
        void onProgress(long currentLength, long totalLength, boolean done);
    }

    public interface OnErrorListener {
        /**
         * 下载出现异常回调
         *
         * @param throwable
         *         异常
         */
        void onError(Throwable throwable);
    }

    public interface IDownloadService {


        /**
         * 断点下载{Streaming 大文件需要加入这个判断，防止下载过程中写入到内存中}
         *
         * @param start
         *         起始位置
         * @param url
         *         下载地址
         *
         * @return Flowable
         */
        @Streaming
        @GET
        Flowable<ResponseBody> download(@Header("RANGE") String start, @Url String url);

    }


    public static class Build {
        private OnProgressListener onProgressListener;
        private OnErrorListener onErrorListener;
        private OnFinishListener onFinishListener;
        private String downloadUrl;
        private String savePath;
        private boolean cache = false;
        private int retryCount = 0;
        private long delay = 1000L;

        public Build() {
        }

        public Build setOnProgressListener(OnProgressListener progressListener) {
            this.onProgressListener = progressListener;
            return this;
        }

        public Build setOnErrorListener(OnErrorListener errorListener) {
            this.onErrorListener = errorListener;
            return this;
        }

        public Build setOnFinishListener(OnFinishListener finishListener) {
            this.onFinishListener = finishListener;
            return this;
        }

        public Build downloadUrl(String downloadUrl) {
            this.downloadUrl = downloadUrl;
            return this;
        }

        public Build savePath(String savePath) {
            this.savePath = savePath;
            return this;
        }

        public Build cache(boolean needCache) {
            this.cache = needCache;
            return this;
        }

        public Build retryCount(int retryCount) {
            this.retryCount = retryCount;
            return this;
        }

        public Build delay(long delay) {
            this.delay = delay;
            return this;
        }

        public FkDownload build() {
            return new FkDownload(this);
        }
    }


    protected static class Cache {
        private final List<DownloadBean> mCacheList;

        private Cache() {
            mCacheList = new ArrayList<>();
            List<DownloadBean> list = FkRealmManager.getInstance().query(DownloadBean.class).findAll();
            if (list != null) {
                mCacheList.addAll(FkRealmManager.getInstance().copyFromRealm(list));
            }
        }

        private final static class Holder {
            private final static FkDownload.Cache INSTANCE = new FkDownload.Cache();
        }


        protected static Cache getInstance() {
            return Holder.INSTANCE;
        }

        private DownloadBean getCache(String downloadUrl) {
            if (!TextUtils.isEmpty(downloadUrl)) {
                for (DownloadBean bean : mCacheList) {
                    if (downloadUrl.equals(bean.getDownloadUrl())) {
                        return bean;
                    }
                }
            }
            return null;
        }

        private DownloadBean getCache(String downloadUrl, String savePath) {
            if (!TextUtils.isEmpty(downloadUrl) && TextUtils.isEmpty(savePath)) {
                for (DownloadBean bean : mCacheList) {
                    if (downloadUrl.equals(bean.getDownloadUrl()) && savePath.equals(bean.getSavePath())) {
                        return bean;
                    }
                }
            }
            return null;
        }

        private void update(DownloadBean downloadBean) {
            if (downloadBean != null) {
                mCacheList.remove(downloadBean);
                mCacheList.add(downloadBean);
                FkRealmManager.getInstance().copyToRealmOrUpdate(mCacheList);
            }

        }

        void updateAll() {
            if (mCacheList.size() > 0) {
                FkRealmManager.getInstance().copyToRealmOrUpdate(mCacheList);
            }
        }
    }
}
