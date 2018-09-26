package it.wsh.cn.wshlibrary.http.download;

import android.content.Context;
import android.text.TextUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.Schedulers;
import it.wsh.cn.wshlibrary.database.bean.DownloadInfo;
import it.wsh.cn.wshlibrary.database.daohelper.DownloadInfoDaoHelper;
import it.wsh.cn.wshlibrary.http.HttpConfig;
import it.wsh.cn.wshlibrary.http.HttpConstants;
import it.wsh.cn.wshlibrary.http.IOUtil;
import it.wsh.cn.wshlibrary.http.https.SslContextFactory;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;

/**
 * author: wenshenghui
 * created on: 2018/8/25 12:51
 * description:
 */
public class DownloadTask {

    private Context mContext;
    private OkHttpClient mClient;
    private File mSaveFile; //存储路径
    private DownloadObserver mDownloadObserver; //回调
    private boolean mExit = false; //控制退出

    public DownloadTask(Context context) {
        if (context == null) {
            return;
        }
        mContext = context.getApplicationContext();
    }

    /**
     * @param url
     * @return
     */
    public DownloadTask init(String url) {

        if (TextUtils.isEmpty(url)) {
            return null;
        }

        HttpConfig config = HttpConfig.create(true);

        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(config.getConnectTimeout(), TimeUnit.SECONDS)
                .readTimeout(config.getReadTimeout(), TimeUnit.SECONDS)
                .writeTimeout(config.getWriteTimeout(), TimeUnit.SECONDS);
        //测试用  跳过所有认证
        if (url.startsWith(HttpConstants.HTTPS)) {
            //SSLSocketFactory sslSocketFactory = new SslContextFactory().getSslSocket(mContext).getSocketFactory();
            //builder.sslSocketFactory(sslSocketFactory);
            builder.sslSocketFactory(new SslContextFactory().createSSLSocketFactory())
                    .hostnameVerifier(new HostnameVerifier() {
                        @Override
                        public boolean verify(String hostname, SSLSession session) {
                            return true;
                        }
                    });
        }
        mClient = builder.build();
        return this;
    }

    //下载
    public void start(DownloadInfo info,
                      DownloadObserver downLoadObserver) {

        mDownloadObserver = downLoadObserver;
        Observable.just(info).flatMap(new Function<DownloadInfo, Observable<DownloadInfo>>() {
            @Override
            public Observable<DownloadInfo> apply(DownloadInfo info) throws Exception {
                return Observable.just(createDownInfo(info));
            }
        }).flatMap(new Function<DownloadInfo, Observable<DownloadInfo>>() {
            @Override
            public Observable<DownloadInfo> apply(DownloadInfo downloadInfo) throws Exception {
                return Observable.create(new DownloadSubscribe(downloadInfo));
            }
        }).observeOn(AndroidSchedulers.mainThread())//在主线程回调
                .subscribeOn(Schedulers.io())//在子线程执行
                .subscribe(downLoadObserver);
    }

    /**
     * 创建DownInfo
     *
     * @param downloadInfo 请求网址
     * @return DownInfo
     */
    private DownloadInfo createDownInfo(DownloadInfo downloadInfo) {

        int key = downloadInfo.getKey();
        String url = downloadInfo.getUrl();
        DownloadInfo info = DownloadInfoDaoHelper.queryTask(key);
        if (info != null) {
            downloadInfo = info;
            mSaveFile = new File(info.getSavePath());
            long totalSize = downloadInfo.getTotalSize();
            if (mSaveFile.exists()) {
                downloadInfo.setDownloadPosition(mSaveFile.length());
            }

            if (downloadInfo.getDownloadPosition() != 0) {
                long serverContentLength = getServerContentLength(url);
                if (serverContentLength != totalSize) {
                    downloadInfo.setDownloadPosition(0);
                    downloadInfo.setTotalSize(serverContentLength);
                    if (mSaveFile.exists()) {
                        mSaveFile.delete();
                    }
                }
            }
        }else {
            initFirstDownload(downloadInfo);

        }
        return downloadInfo;
    }

    /**
     * 第一次下载的初始化
     * @param downloadInfo
     */
    private void initFirstDownload(DownloadInfo downloadInfo) {
        String downloadPath = downloadInfo.getSavePath();
        mSaveFile = new File(downloadPath);
        if (mSaveFile.exists()) {
            mSaveFile.delete();
        }
    }

    /**
     * 请求完整数据长度
     * @param url
     */
    private long getServerContentLength(String url) {
        Request request = new Request.Builder()
                .url(url)
                .build();
        try {
            okhttp3.Response response = mClient.newCall(request).execute();
            if (response != null && response.isSuccessful()) {
                long contentLength = response.body().contentLength();
                response.close();
                return contentLength == 0 ? -1 : contentLength;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public void exit() {
        mExit = true;
    }

    /**
     * 添加下载监听
     * @param processListener
     */
    public void addProcessListener(IDownloadListener processListener) {
        if (mDownloadObserver != null) {
            mDownloadObserver.addProcessListener(processListener);
        }
    }

    /**
     * 删除下载监听
     * @param processListener
     */
    public boolean removeProcessListener(IDownloadListener processListener) {
        if (mDownloadObserver != null) {
            return mDownloadObserver.removeProcessListener(processListener);
        }
        return false;
    }

    private class DownloadSubscribe implements ObservableOnSubscribe<DownloadInfo> {
        private DownloadInfo downloadInfo;

        public DownloadSubscribe(DownloadInfo downloadInfo) {
            this.downloadInfo = downloadInfo;
        }

        @Override
        public void subscribe(ObservableEmitter<DownloadInfo> e) throws Exception {
            String url = downloadInfo.getUrl();
            long downloadLength = downloadInfo.getDownloadPosition();//已经下载好的长度
            long responseLength = downloadInfo.getTotalSize();//文件的总长度, 注意此处可能为0
            String saveFilePath = downloadInfo.getSavePath();
            File saveFile = new File(saveFilePath);
            if (!saveFile.exists()) {
                saveFile.getParentFile().mkdirs();
            }
            if (!saveFile.exists()) {
                saveFile.createNewFile();
            }
            if (responseLength == -1) {
                e.onError(new SocketTimeoutException());
                return;
            }

            Request.Builder builder = new Request.Builder().url(url);
            if (responseLength != 0) {
                builder.addHeader("RANGE", "bytes=" + downloadLength + "-" + responseLength);
            }
            Request request = builder.build();
            Call call = mClient.newCall(request);
            okhttp3.Response response = call.execute();

            if (responseLength == 0) {
                responseLength = response.body().contentLength();
            }
            //初始进度信息
            e.onNext(downloadInfo);
            if (downloadLength == 0) {
                downloadInfo.setTotalSize(responseLength);
                DownloadInfoDaoHelper.insertInfo(downloadInfo);
            }
            if (downloadLength >= responseLength) {
                //初始进度信息
                e.onNext(downloadInfo);
                e.onComplete();//完成
                return;
            }
            RandomAccessFile randomAccessFile = null;
            InputStream inputStream = null;

            try {
                randomAccessFile = new RandomAccessFile(saveFile, "rwd");
                randomAccessFile.seek(downloadLength);
                inputStream = response.body().byteStream();
                byte[] buffer = new byte[1024 * 16];//缓冲数组16kB
                int len;
                while (!mExit && (len = inputStream.read(buffer)) != -1) {
                    randomAccessFile.write(buffer, 0, len);
                    downloadLength += len;
                    downloadInfo.setDownloadPosition(downloadLength);
                    e.onNext(downloadInfo);
                }
            } catch (Exception t) {
                e.onError(t);
                return;
            } finally {
                //关闭IO流
                IOUtil.closeAll(inputStream, randomAccessFile);
            }
            if (mExit) {
                downloadInfo.setExit(true);
                e.onNext(downloadInfo);
            }else {
                e.onComplete();//完成
            }
        }
    }
}
