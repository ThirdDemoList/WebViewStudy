package com.example.jingbin.webviewstudy;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.jingbin.webviewstudy.config.FullscreenHolder;
import com.example.jingbin.webviewstudy.config.IWebPageView;
import com.example.jingbin.webviewstudy.config.ImageClickInterface;
import com.example.jingbin.webviewstudy.config.MyWebChromeClient;
import com.example.jingbin.webviewstudy.config.MyWebViewClient;
import com.example.jingbin.webviewstudy.utils.BaseTools;
import com.example.jingbin.webviewstudy.utils.StatusBarUtil;

/**
 * 网页可以处理:
 * 点击相应控件：
 * - 拨打电话、发送短信、发送邮件
 * - 上传图片(版本兼容)
 * - 全屏播放网络视频
 * - 进度条显示
 * - 返回网页上一层、显示网页标题
 * JS交互部分：
 * - 前端代码嵌入js(缺乏灵活性)
 * - 网页自带js跳转
 * 被作为第三方浏览器打开
 */
public class WebViewActivity extends AppCompatActivity implements IWebPageView {

    // 进度条
    private ProgressBar mProgressBar;
    private WebView webView;
    // 全屏时视频加载view
    private FrameLayout videoFullView;
    // 加载视频相关
    private MyWebChromeClient mWebChromeClient;
    // 网页链接
    private String mUrl;
    private Toolbar mTitleToolBar;
    // 可滚动的title 使用简单 没有渐变效果，文字两旁有阴影
    private TextView tvGunTitle;
    private String mTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_view);
        getIntentData();
        initTitle();
        initWebView();
        webView.loadUrl(mUrl);
        getDataFromBrowser(getIntent());
    }

    private void getIntentData() {
        mUrl = getIntent().getStringExtra("mUrl");
        mTitle = getIntent().getStringExtra("mTitle");
    }


    private void initTitle() {
        StatusBarUtil.setColor(this, ContextCompat.getColor(this, R.color.colorPrimary), 0);
        mProgressBar = findViewById(R.id.pb_progress);
        webView = findViewById(R.id.webview_detail);
        videoFullView = findViewById(R.id.video_fullView);
        mTitleToolBar = findViewById(R.id.title_tool_bar);
        tvGunTitle = findViewById(R.id.tv_gun_title);
        initToolBar();
    }

    private void initToolBar() {
        setSupportActionBar(mTitleToolBar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            //去除默认Title显示
            actionBar.setDisplayShowTitleEnabled(false);
        }
        mTitleToolBar.setOverflowIcon(ContextCompat.getDrawable(this, R.drawable.actionbar_more));
        tvGunTitle.postDelayed(new Runnable() {
            @Override
            public void run() {
                tvGunTitle.setSelected(true);
            }
        }, 1900);
        setTitle(mTitle);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_webview, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:// 返回键
                handleFinish();
                break;
            case R.id.actionbar_share:// 分享到
                String shareText = webView.getTitle() + webView.getUrl();
                BaseTools.share(WebViewActivity.this, shareText);
                break;
            case R.id.actionbar_cope:// 复制链接
                if (!TextUtils.isEmpty(webView.getUrl())) {
                    BaseTools.copy(webView.getUrl());
                    Toast.makeText(this, "复制成功", Toast.LENGTH_LONG).show();
                }
                break;
            case R.id.actionbar_open:// 打开链接
                BaseTools.openLink(WebViewActivity.this, webView.getUrl());
                break;
            case R.id.actionbar_webview_refresh:// 刷新页面
                if (webView != null) {
                    webView.reload();
                }
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    private void initWebView() {
        mProgressBar.setVisibility(View.VISIBLE);
        WebSettings ws = webView.getSettings();
        // 网页内容的宽度是否可大于WebView控件的宽度
        ws.setLoadWithOverviewMode(false);
        // 保存表单数据
        ws.setSaveFormData(true);
        // 是否应该支持使用其屏幕缩放控件和手势缩放
        ws.setSupportZoom(true);
        ws.setBuiltInZoomControls(true);
        ws.setDisplayZoomControls(false);
        // 启动应用缓存
        ws.setAppCacheEnabled(true);
        // 设置缓存模式
        ws.setCacheMode(WebSettings.LOAD_DEFAULT);
        // setDefaultZoom  api19被弃用
        // 设置此属性，可任意比例缩放。
        ws.setUseWideViewPort(true);
        // 不缩放
        webView.setInitialScale(100);
        // 告诉WebView启用JavaScript执行。默认的是false。
        ws.setJavaScriptEnabled(true);
        //  页面加载好以后，再放开图片
        ws.setBlockNetworkImage(false);
        // 使用localStorage则必须打开
        ws.setDomStorageEnabled(true);
        // 排版适应屏幕
        ws.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NARROW_COLUMNS);
        // WebView是否新窗口打开(加了后可能打不开网页)
        ws.setSupportMultipleWindows(true);

        // webview从5.0开始默认不允许混合模式,https中不能加载http资源,需要设置开启。
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ws.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
        /** 设置字体默认缩放大小(改变网页字体大小,setTextSize  api14被弃用)*/
        ws.setTextZoom(100);

        mWebChromeClient = new MyWebChromeClient(this);
        webView.setWebChromeClient(mWebChromeClient);
        // 与js交互
        webView.addJavascriptInterface(new ImageClickInterface(this), "injectedObject");
        webView.setWebViewClient(new MyWebViewClient(this));


        webView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                final WebView.HitTestResult hitTestResult = webView.getHitTestResult();
                // 如果是图片类型或者是带有图片链接的类型
                if (hitTestResult.getType() == WebView.HitTestResult.IMAGE_TYPE ||
                        hitTestResult.getType() == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
                    // 弹出保存图片的对话框
                    new AlertDialog.Builder(WebViewActivity.this)
                            .setItems(new String[]{"查看大图", "保存图片到相册"}, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    String picUrl = hitTestResult.getExtra();
                                    //获取图片
                                    Log.e("picUrl", picUrl);
                                    switch (which) {
                                        case 0:
                                            break;
                                        case 1:
                                            break;
                                        default:
                                            break;
                                    }
                                }
                            })
                            .show();
                    return true;
                }
                return false;
            }
        });

    }

    @Override
    public void hindProgressBar() {
        mProgressBar.setVisibility(View.GONE);
    }

    @Override
    public void showWebView() {
        webView.setVisibility(View.VISIBLE);
    }

    @Override
    public void hindWebView() {
        webView.setVisibility(View.INVISIBLE);
    }

    @Override
    public void fullViewAddView(View view) {
        FrameLayout decor = (FrameLayout) getWindow().getDecorView();
        videoFullView = new FullscreenHolder(WebViewActivity.this);
        videoFullView.addView(view);
        decor.addView(videoFullView);
    }

    @Override
    public void showVideoFullView() {
        videoFullView.setVisibility(View.VISIBLE);
    }

    @Override
    public void hindVideoFullView() {
        videoFullView.setVisibility(View.GONE);
    }

    @Override
    public void startProgress(int newProgress) {
        mProgressBar.setVisibility(View.VISIBLE);
        mProgressBar.setProgress(newProgress);
        if (newProgress == 100) {
            mProgressBar.setVisibility(View.GONE);
        }
    }

    public void setTitle(String mTitle) {
        tvGunTitle.setText(mTitle);
    }

    /**
     * android与js交互：
     * 前端嵌入js代码：不能加重复的节点，不然会覆盖
     */
    @Override
    public void addImageClickListener() {
        // 这段js函数的功能就是，遍历所有的img节点，并添加onclick函数，函数的功能是在图片点击的时候调用本地java接口并传递url过去
        // 如要点击一张图片在弹出的页面查看所有的图片集合,则获取的值应该是个图片数组
        webView.loadUrl("javascript:(function(){" +
                "var objs = document.getElementsByTagName(\"img\");" +
                "for(var i=0;i<objs.length;i++)" +
                "{" +
                //  "objs[i].onclick=function(){alert(this.getAttribute(\"has_link\"));}" +
                "objs[i].onclick=function(){window.injectedObject.imageClick(this.getAttribute(\"src\"),this.getAttribute(\"has_link\"));}" +
                "}" +
                "})()");

        // 遍历所有的<li>节点,将节点里的属性传递过去(属性自定义,用于页面跳转)
        webView.loadUrl("javascript:(function(){" +
                "var objs =document.getElementsByTagName(\"li\");" +
                "for(var i=0;i<objs.length;i++)" +
                "{" +
                "objs[i].onclick=function(){" +
                "window.injectedObject.textClick(this.getAttribute(\"type\"),this.getAttribute(\"item_pk\"));}" +
                "}" +
                "})()");

        /**传应用内的数据给html，方便html处理*/
        // 无参数调用
        webView.loadUrl("javascript:javacalljs()");
        // 传递参数调用
        webView.loadUrl("javascript:javacalljswithargs('" + "android传入到网页里的数据，有参" + "')");

    }

    public FrameLayout getVideoFullView() {
        return videoFullView;
    }

    /**
     * 全屏时按返加键执行退出全屏方法
     */
    public void hideCustomView() {
        mWebChromeClient.onHideCustomView();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    /**
     * 上传图片之后的回调
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == MyWebChromeClient.FILECHOOSER_RESULTCODE) {
            mWebChromeClient.mUploadMessage(intent, resultCode);
        } else if (requestCode == MyWebChromeClient.FILECHOOSER_RESULTCODE_FOR_ANDROID_5) {
            mWebChromeClient.mUploadMessageForAndroid5(intent, resultCode);
        }
    }


    /**
     * 使用singleTask启动模式的Activity在系统中只会存在一个实例。
     * 如果这个实例已经存在，intent就会通过onNewIntent传递到这个Activity。
     * 否则新的Activity实例被创建。
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        getDataFromBrowser(intent);
    }

    /**
     * 作为三方浏览器打开传过来的值
     * Scheme: https
     * host: www.jianshu.com
     * path: /p/1cbaf784c29c
     * url = scheme + "://" + host + path;
     */
    private void getDataFromBrowser(Intent intent) {
        Uri data = intent.getData();
        if (data != null) {
            try {
                String scheme = data.getScheme();
                String host = data.getHost();
                String path = data.getPath();
                String text = "Scheme: " + scheme + "\n" + "host: " + host + "\n" + "path: " + path;
                Log.e("data", text);
                String url = scheme + "://" + host + path;
                webView.loadUrl(url);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 直接通过三方浏览器打开时，回退到首页
     */
    public void handleFinish() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            finishAfterTransition();
        } else {
            finish();
        }
        if (!MainActivity.isLaunch) {
            MainActivity.start(this);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            //全屏播放退出全屏
            if (mWebChromeClient.inCustomView()) {
                hideCustomView();
                return true;

                //返回网页上一页
            } else if (webView.canGoBack()) {
                webView.goBack();
                return true;

                //退出网页
            } else {
                handleFinish();
            }
        }
        return false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        webView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume();
        // 支付宝网页版在打开文章详情之后,无法点击按钮下一步
        webView.resumeTimers();
        // 设置为横屏
        if (getRequestedOrientation() != ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }

    @Override
    protected void onDestroy() {
        videoFullView.removeAllViews();
        if (webView != null) {
            ViewGroup parent = (ViewGroup) webView.getParent();
            if (parent != null) {
                parent.removeView(webView);
            }
            webView.removeAllViews();
            webView.loadDataWithBaseURL(null, "", "text/html", "utf-8", null);
            webView.stopLoading();
            webView.setWebChromeClient(null);
            webView.setWebViewClient(null);
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }

    /**
     * 打开网页:
     *
     * @param mContext 上下文
     * @param mUrl     要加载的网页url
     * @param mTitle   标题
     */
    public static void loadUrl(Context mContext, String mUrl, String mTitle) {
        Intent intent = new Intent(mContext, WebViewActivity.class);
        intent.putExtra("mUrl", mUrl);
        intent.putExtra("mTitle", mTitle == null ? "加载中..." : mTitle);
        mContext.startActivity(intent);
    }

    public static void loadUrl(Context mContext, String mUrl) {
        Intent intent = new Intent(mContext, WebViewActivity.class);
        intent.putExtra("mUrl", mUrl);
        intent.putExtra("mTitle", "详情");
        mContext.startActivity(intent);
    }
}
