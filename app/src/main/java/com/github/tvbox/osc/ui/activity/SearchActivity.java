package com.github.tvbox.osc.ui.activity;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Build;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;
import com.github.tvbox.osc.modern.data.LegacyContentSnapshots;
import com.github.tvbox.osc.modern.model.ContentItem;
import com.github.tvbox.osc.modern.search.LegacySearchSites;
import com.github.tvbox.osc.modern.search.SearchViewModel;
import com.github.tvbox.osc.modern.search.SearchUiBindings;
import com.github.tvbox.osc.modern.search.SearchUiState;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.base.BaseActivity;
import com.github.tvbox.osc.bean.AbsXml;
import com.github.tvbox.osc.bean.Movie;
import com.github.tvbox.osc.bean.SourceBean;
import com.github.tvbox.osc.event.ServerEvent;
import com.github.tvbox.osc.ui.adapter.PinyinAdapter;
import com.github.tvbox.osc.ui.adapter.SearchAdapter;
import com.github.tvbox.osc.ui.dialog.RemoteDialog;
import com.github.tvbox.osc.ui.dialog.SearchCheckboxDialog;
import com.github.tvbox.osc.ui.tv.widget.SearchKeyboard;
import com.github.tvbox.osc.util.FastClickCheckUtil;
import com.github.tvbox.osc.util.HawkConfig;
import com.github.tvbox.osc.util.HistoryHelper;
import com.github.tvbox.osc.util.SearchHelper;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.AbsCallback;
import com.lzy.okgo.model.Response;
import com.orhanobut.hawk.Hawk;
import com.owen.tvrecyclerview.widget.TvRecyclerView;
import com.owen.tvrecyclerview.widget.V7GridLayoutManager;
import com.owen.tvrecyclerview.widget.V7LinearLayoutManager;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @author pj567
 * @date :2020/12/23
 * @description:
 */
public class SearchActivity extends BaseActivity {
    private static final String HOT_SEARCH_URL = "https://movie.douban.com/j/search_subjects?type=tv&tag=%E7%83%AD%E9%97%A8&sort=recommend&page_limit=20&page_start=0";
    private static final String[] DEFAULT_HOT_WORDS = {
            "\u5bb6\u4e1a",
            "\u4e3b\u89d2",
            "\u4f4e\u667a\u5546\u72af\u7f6a",
            "\u82cf\u8d85",
            "\u4e66\u5377\u4e00\u68a6",
            "\u7f8e\u4eba\u4f59",
            "\u85cf\u6d77\u4f20",
            "\u957f\u5b89\u7684\u8354\u679d",
            "\u5e86\u4f59\u5e74",
            "\u51e1\u4eba\u4fee\u4ed9\u4f20"
    };
    private LinearLayout llLayout;
    private LinearLayout llHistoryWord;
    private TvRecyclerView mGridView;
    private TvRecyclerView mGridViewWord;
    private GridLayout historyWordGrid;
    private SearchViewModel searchModel;
    private List<ContentItem> renderedResults = new ArrayList<>();
    private int renderedFolderDepth;
    private boolean folderFocusPending;
    private boolean returnToResults;
    private RemoteDialog remoteDialog;
    private EditText etSearch;
    private TextView tvSearch;
    private TextView tvClear;
    private ImageView tvHistoryClear;
    private SearchKeyboard keyboard;
    private SearchAdapter searchAdapter;
    private PinyinAdapter wordAdapter;
    private PinyinAdapter hotWordAdapter;
    private TextView tvSearchCheckboxBtn;

    private static HashMap<String, String> mCheckSources = null;
    private SearchCheckboxDialog mSearchCheckboxDialog = null;

    private TextView wordsSwitch;
    private boolean aggregateSearchMode;
    private boolean aggregateSearchModeInited = false;

    @Override
    protected int getLayoutResID() {
        return R.layout.activity_search;
    }


    private boolean hasKeyBoard;
    private boolean isSearchBack;
    @Override
    protected void init() {
        initView();
        initViewModel();
        initData();
        hasKeyBoard = true;
        isSearchBack = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        searchModel.resume();
        requestSearchFocusWhenReady();
        applySearchWordMode();
        if (aggregateSearchMode) {
            refreshSearchHistoryWords();
            if (hots != null && !hots.isEmpty()) {
                hotWordAdapter.setNewData(hots);
            }
        }
    }

    private void requestSearchFocusWhenReady() {
        if (returnToResults && !searchModel.getState().getValue().getResults().isEmpty()) {
            requestResultFocus(searchModel.getState().getValue().getSelection());
            returnToResults = false;
            return;
        }
        final View focusView = hasKeyBoard || isSearchBack ? tvSearch : etSearch;
        if (focusView == null) return;
        focusView.post(new Runnable() {
            @Override
            public void run() {
                if (isFinishing()) return;
                focusView.requestFocus();
                focusView.requestFocusFromTouch();
            }
        });
    }

    private void initView() {
        EventBus.getDefault().register(this);
        llLayout = findViewById(R.id.llLayout);
        llHistoryWord = findViewById(R.id.llHistoryWord);
        etSearch = findViewById(R.id.etSearch);
        tvSearch = findViewById(R.id.tvSearch);
        tvSearchCheckboxBtn = findViewById(R.id.tvSearchCheckboxBtn);
        tvClear = findViewById(R.id.tvClear);
        mGridView = findViewById(R.id.mGridView);
        keyboard = findViewById(R.id.keyBoardRoot);
        mGridViewWord = findViewById(R.id.mGridViewWord);
        historyWordGrid = findViewById(R.id.historyWordGrid);
        tvHistoryClear = findViewById(R.id.tvHistoryClear);
        mGridViewWord.setHasFixedSize(true);
        wordAdapter = new PinyinAdapter();
        hotWordAdapter = new PinyinAdapter();
        wordsSwitch = findViewById(R.id.wordSwitch);
        applySearchWordMode();
        wordAdapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                startSearch(wordAdapter.getItem(position));
            }
        });
        hotWordAdapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                startSearch(hotWordAdapter.getItem(position));
            }
        });
        mGridView.setHasFixedSize(true);
        // lite
        if (Hawk.get(HawkConfig.SEARCH_VIEW, 0) == 0)
            mGridView.setLayoutManager(new V7LinearLayoutManager(this.mContext, 1, false));
            // with preview
        else
            mGridView.setLayoutManager(new V7GridLayoutManager(this.mContext, 3));
        searchAdapter = new SearchAdapter();
        mGridView.setAdapter(searchAdapter);
        searchAdapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                FastClickCheckUtil.check(view);
                Movie.Video video = searchAdapter.getData().get(position);
                if (video != null) {
                    openSearchVideo(video);
                }
            }
        });
        wordsSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (aggregateSearchMode) {
                    return;
                }
                FastClickCheckUtil.check(v);
                String wd = wordsSwitch.getText().toString().trim();
                if(wd.contains("热词")){
                    ArrayList<String> hisWord= Hawk.get(HawkConfig.SEARCH_HISTORY, new ArrayList<String>());
                    if (hisWord.isEmpty()){
                        Toast.makeText(mContext, "暂无历史搜索", Toast.LENGTH_SHORT).show();
                    }else {
                        wordsSwitch.setText("历史 搜索");
                        wordAdapter.setNewData(hisWord);
                    }
                }
                if(wd.equals("历史 搜索")){
                    wordsSwitch.setText("热词 搜索");
                    if(hots!=null && !hots.isEmpty()){
                        wordAdapter.setNewData(hots);
                    }
                }
            }
        });
        tvHistoryClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FastClickCheckUtil.check(v);
                HistoryHelper.clearSearchHistory();
                refreshSearchHistoryWords();
                Toast.makeText(mContext, "已清空搜索历史", Toast.LENGTH_SHORT).show();
            }
        });
        tvSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FastClickCheckUtil.check(v);
                hasKeyBoard = true;
                String wd = etSearch.getText().toString().trim();
                if (!TextUtils.isEmpty(wd)) {
                    if(Hawk.get(HawkConfig.FAST_SEARCH_MODE, true)){
                        Bundle bundle = new Bundle();
                        bundle.putString("title", wd);
                        jumpActivity(FastSearchActivity.class, bundle);
                    }else {
                        search(wd);
                    }
                } else {
                    Toast.makeText(mContext, "输入内容不能为空", Toast.LENGTH_SHORT).show();
                }
            }
        });
        tvClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FastClickCheckUtil.check(v);
                initData();
                etSearch.setText("");
            }
        });

        //软键盘

        etSearch.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_SEARCH || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    String wd = etSearch.getText().toString().trim();
                    if (!TextUtils.isEmpty(wd)) {
                        if (Hawk.get(HawkConfig.FAST_SEARCH_MODE, true)) {
                            Bundle bundle = new Bundle();
                            bundle.putString("title", wd);
                            jumpActivity(FastSearchActivity.class, bundle);
                        } else {
                            hiddenImm();
                            search(wd);
                        }
                    } else {
                        Toast.makeText(mContext, "输入内容不能为空", Toast.LENGTH_SHORT).show();
                    }
                    return true;
                }
                return false;
            }
        });

        // 监听遥控器
        etSearch.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN && (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER)) {
                    String wd = etSearch.getText().toString().trim();
                    if (!TextUtils.isEmpty(wd)) {
                        if (Hawk.get(HawkConfig.FAST_SEARCH_MODE, true)) {
                            Bundle bundle = new Bundle();
                            bundle.putString("title", wd);
                            jumpActivity(FastSearchActivity.class, bundle);
                        } else {
                            hiddenImm();
                            search(wd);
                        }
                    } else {
                        Toast.makeText(mContext, "输入内容不能为空", Toast.LENGTH_SHORT).show();
                    }
                    return true;
                }
                return false;
            }
        });
        keyboard.setOnSearchKeyListener(new SearchKeyboard.OnSearchKeyListener() {
            @Override
            public void onSearchKey(int pos, String key) {
                if (pos > 1) {
                    String text = etSearch.getText().toString().trim();
                    text += key;
                    etSearch.setText(text);
                    if (text.length() > 0) {
                        loadRec(text);
                    }
                } else if (pos == 1) {
                    String text = etSearch.getText().toString().trim();
                    if (text.length() > 0) {
                        text = text.substring(0, text.length() - 1);
                        etSearch.setText(text);
                    }
                    if (text.length() > 0) {
                        loadRec(text);
                    }
                } else if (pos == 0) {
                    remoteDialog = new RemoteDialog(mContext);
                    remoteDialog.show();
                }
            }
        });
        setLoadSir(llLayout);
        tvSearchCheckboxBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                List<SourceBean> searchAbleSource = ApiConfig.get().getSearchSourceBeanList();
                if (mSearchCheckboxDialog == null) {
                    mSearchCheckboxDialog = new SearchCheckboxDialog(SearchActivity.this, searchAbleSource, mCheckSources);
                }else {
                    if(searchAbleSource.size()!=mSearchCheckboxDialog.mSourceList.size()){
                        mSearchCheckboxDialog.setMSourceList(searchAbleSource);
                    }
                }
                mSearchCheckboxDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        dialog.dismiss();
                    }
                });
                mSearchCheckboxDialog.show();
            }
        });
    }

    private void startSearch(String wd) {
        if (TextUtils.isEmpty(wd)) {
            return;
        }
        if (Hawk.get(HawkConfig.FAST_SEARCH_MODE, true)) {
            Bundle bundle = new Bundle();
            bundle.putString("title", wd);
            jumpActivity(FastSearchActivity.class, bundle);
        } else {
            search(wd);
        }
    }

    private boolean isAggregateSearchMode() {
        return Hawk.get(HawkConfig.FAST_SEARCH_MODE, true);
    }

    private void setAggregateHotTitle() {
        wordsSwitch.setText("热  门");
        wordsSwitch.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.ts_22));
        wordsSwitch.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            wordsSwitch.setLetterSpacing(0.08f);
        }
    }

    private void setNormalWordTitle() {
        wordsSwitch.setText("热词 | 历史");
        wordsSwitch.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.ts_20));
        wordsSwitch.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            wordsSwitch.setLetterSpacing(0f);
        }
    }

    private void applySearchWordMode() {
        boolean aggregateMode = isAggregateSearchMode();
        if (aggregateSearchModeInited && aggregateSearchMode == aggregateMode) {
            return;
        }
        aggregateSearchModeInited = true;
        aggregateSearchMode = aggregateMode;
        if (aggregateSearchMode) {
            llHistoryWord.setVisibility(View.VISIBLE);
            llLayout.setVisibility(View.GONE);
            mGridView.setVisibility(View.GONE);
            setAggregateHotTitle();
            wordsSwitch.setFocusable(false);
            wordsSwitch.setBackground(null);
            mGridViewWord.setLayoutManager(new V7LinearLayoutManager(this.mContext, 1, false));
            mGridViewWord.setAdapter(hotWordAdapter);
            refreshSearchHistoryWords();
        } else {
            llHistoryWord.setVisibility(View.GONE);
            llLayout.setVisibility(View.VISIBLE);
            if (mGridView.getVisibility() == View.GONE) {
                mGridView.setVisibility(View.INVISIBLE);
            }
            setNormalWordTitle();
            wordsSwitch.setFocusable(true);
            wordsSwitch.setBackgroundResource(R.drawable.shape_user_focus);
            mGridViewWord.setLayoutManager(new V7LinearLayoutManager(this.mContext, 1, false));
            mGridViewWord.setAdapter(wordAdapter);
        }
    }

    private void setHotWordsData(ArrayList<String> data) {
        if (aggregateSearchMode) {
            hotWordAdapter.setNewData(data);
        } else {
            wordAdapter.setNewData(data);
        }
    }

    private void refreshSearchHistoryWords() {
        historyWordGrid.post(new Runnable() {
            @Override
            public void run() {
                if (!aggregateSearchMode) return;
                ArrayList<String> history = Hawk.get(HawkConfig.SEARCH_HISTORY, new ArrayList<String>());
                historyWordGrid.removeAllViews();
                int itemHeight = getResources().getDimensionPixelSize(R.dimen.vs_50);
                int itemMargin = getResources().getDimensionPixelSize(R.dimen.vs_5);
                int paddingH = getResources().getDimensionPixelSize(R.dimen.vs_10);
                int minWidth = getResources().getDimensionPixelSize(R.dimen.vs_80);
                int availableWidth = historyWordGrid.getWidth();
                if (availableWidth <= 0) availableWidth = llHistoryWord.getWidth();
                float textSize = getResources().getDimension(R.dimen.ts_22);
                int textColor = getResources().getColor(R.color.color_FFFFFF);
                LinearLayout row = null;
                int rowWidth = 0;
                for (int i = 0; i < history.size(); i++) {
                    final String word = history.get(i);
                    TextView item = new TextView(SearchActivity.this);
                    item.setText(word);
                    item.setSingleLine(true);
                    item.setGravity(Gravity.CENTER);
                    item.setIncludeFontPadding(false);
                    item.setFocusable(true);
                    item.setTextColor(textColor);
                    item.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
                    item.setMinWidth(minWidth);
                    item.setPadding(paddingH, 0, paddingH, 0);
                    item.setBackgroundResource(R.drawable.shape_user_focus);
                    item.measure(
                            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                            View.MeasureSpec.makeMeasureSpec(itemHeight, View.MeasureSpec.EXACTLY));
                    int itemWidth = Math.max(minWidth, item.getMeasuredWidth());
                    int rowItemWidth = itemWidth + itemMargin * 2;
                    if (row == null || (rowWidth > 0 && rowWidth + rowItemWidth > availableWidth)) {
                        row = new LinearLayout(SearchActivity.this);
                        row.setOrientation(LinearLayout.HORIZONTAL);
                        GridLayout.LayoutParams rowParams = new GridLayout.LayoutParams(
                                GridLayout.spec(GridLayout.UNDEFINED),
                                GridLayout.spec(GridLayout.UNDEFINED));
                        rowParams.width = GridLayout.LayoutParams.MATCH_PARENT;
                        rowParams.height = GridLayout.LayoutParams.WRAP_CONTENT;
                        historyWordGrid.addView(row, rowParams);
                        rowWidth = 0;
                    }
                    item.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            startSearch(word);
                        }
                    });
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(itemWidth, itemHeight);
                    params.setMargins(itemMargin, itemMargin, itemMargin, itemMargin);
                    row.addView(item, params);
                    rowWidth += rowItemWidth;
                }
            }
        });
    }

    private void initViewModel() {
        searchModel = new ViewModelProvider(this).get(SearchViewModel.class);
        if (!searchModel.getState().getValue().getQuery().isEmpty()) {
            etSearch.setText(searchModel.getState().getValue().getQuery());
            returnToResults = true;
        }
        SearchUiBindings.bind(this, searchModel, this::renderSearch,
                notice -> Toast.makeText(mContext, "没有指定搜索源", Toast.LENGTH_SHORT).show());
    }

    private void renderSearch(SearchUiState state) {
        List<ContentItem> results = state.getResults();
        boolean folderChanged = renderedFolderDepth != state.getFolderDepth();
        if (folderChanged) folderFocusPending = true;
        if (!renderedResults.equals(results)) {
            List<Movie.Video> videos = new ArrayList<>();
            for (ContentItem item : results) videos.add(LegacyContentSnapshots.toVideo(item));
            if (!folderChanged && results.size() > renderedResults.size()
                    && results.subList(0, renderedResults.size()).equals(renderedResults)) {
                searchAdapter.addData(videos.subList(renderedResults.size(), videos.size()));
            } else {
                searchAdapter.setNewData(videos);
            }
            renderedResults = new ArrayList<>(results);
        }
        renderedFolderDepth = state.getFolderDepth();
        switch (state.getPhase()) {
            case Loading:
                mGridView.setVisibility(View.INVISIBLE);
                showLoading();
                break;
            case Content:
                mGridView.setVisibility(View.VISIBLE);
                showSuccess();
                if (folderFocusPending) {
                    requestResultFocus(state.getSelection());
                    folderFocusPending = false;
                }
                break;
            case Empty:
            case Error:
                mGridView.setVisibility(View.INVISIBLE);
                showEmpty();
                break;
            default: break;
        }
    }

    private void requestResultFocus(int position) {
        mGridView.post(() -> {
            if (isFinishing() || searchAdapter.getData().isEmpty()) return;
            int target = Math.min(position, searchAdapter.getData().size() - 1);
            mGridView.setSelectedPosition(target);
            mGridView.setSelection(target);
            mGridView.post(() -> {
                androidx.recyclerview.widget.RecyclerView.ViewHolder holder = mGridView.findViewHolderForAdapterPosition(target);
                if (holder != null) holder.itemView.requestFocus();
                else mGridView.requestFocus();
            });
        });
    }

    private void openSearchVideo(Movie.Video video) {
        int position = searchAdapter.getData().indexOf(video);
        ContentItem item = LegacyContentSnapshots.item(video, video.sourceKey);
        searchModel.select(position);
        hasKeyBoard = false;
        if (TextUtils.equals("folder", video.tag)) {
            searchModel.openFolder(item, position);
            return;
        }
        searchModel.pause();
        isSearchBack = true;
        returnToResults = true;
        Bundle bundle = new Bundle();
        bundle.putString("id", video.id);
        bundle.putString("sourceKey", video.sourceKey);
        bundle.putString("title", video.name);
        bundle.putString("picture", video.pic);
        ArrayList<Movie.Video> candidates = new ArrayList<>();
        for (ContentItem candidate : searchModel.fallbackCandidates(item))
            candidates.add(LegacyContentSnapshots.toVideo(candidate));
        if (!candidates.isEmpty()) bundle.putSerializable(DetailActivity.EXTRA_DETAIL_FALLBACK_CANDIDATES, candidates);
        jumpActivity(DetailActivity.class, bundle);
    }

    @Override
    public void onBackPressed() {
        if (searchModel.backFolder()) return;
        super.onBackPressed();
    }

    /**
     * 拼音联想
     */
    private void loadRec(String key) {
        OkGo.get("https://tv.aiseet.atianqi.com/i-tvbin/qtv_video/search/get_search_smart_box")
                .params("format", "json")
                .params("page_num", 0)
                .params("page_size", 20)
                .params("key", key)
                .execute(new AbsCallback() {
                    @Override
                    public void onSuccess(Response response) {
                        try {
                            ArrayList hots = new ArrayList<>();
                            String result = (String) response.body();
                            Gson gson = new Gson();
                            JsonElement json = gson.fromJson(result, JsonElement.class);
                            JsonArray groupDataArr = json.getAsJsonObject()
                                    .get("data").getAsJsonObject()
                                    .get("search_data").getAsJsonObject()
                                    .get("vecGroupData").getAsJsonArray()
                                    .get(0).getAsJsonObject()
                                    .get("group_data").getAsJsonArray();
                            for (JsonElement groupDataElement : groupDataArr) {
                                JsonObject groupData = groupDataElement.getAsJsonObject();
                                String keywordTxt = groupData.getAsJsonObject("dtReportInfo")
                                        .getAsJsonObject("reportData")
                                        .get("keyword_txt").getAsString();
                                hots.add(keywordTxt.trim());
                            }
                            wordsSwitch.setText("猜你 想搜");
                            setHotWordsData(hots);
                            mGridViewWord.smoothScrollToPosition(0);
                        } catch (Throwable th) {
                            th.printStackTrace();
                        }
                    }

                    @Override
                    public String convertResponse(okhttp3.Response response) throws Throwable {
                        return response.body().string();
                    }
                });
    }

    private static ArrayList<String> hots;
    private static boolean hotWordsRequested;

    private void useDefaultHotWords() {
        ArrayList<String> data = new ArrayList<>();
        for (String word : DEFAULT_HOT_WORDS) {
            data.add(word);
        }
        cacheHotWords(data);
    }

    private void cacheHotWords(ArrayList<String> data) {
        hots = data;
        setHotWordsData(hots);
    }

    private String cleanHotWord(String title) {
        if (TextUtils.isEmpty(title)) return "";
        return title.trim().replaceAll("<|>|《|》|-", "").split(" ")[0];
    }

    private void addHotWord(ArrayList<String> data, String title) {
        String word = cleanHotWord(title);
        if (!TextUtils.isEmpty(word) && !data.contains(word)) {
            data.add(word);
        }
    }

    private void initData() {
        initCheckedSourcesForSearch();
        applySearchWordMode();
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("title") && searchModel.getState().getValue().getQuery().isEmpty()) {
            String title = intent.getStringExtra("title");
            showLoading();
            if(Hawk.get(HawkConfig.FAST_SEARCH_MODE, true)){
                Bundle bundle = new Bundle();
                bundle.putString("title", title);
                jumpActivity(FastSearchActivity.class, bundle);
            }else {
                search(title);
            }
        }
        if (aggregateSearchMode) {
            setAggregateHotTitle();
            refreshSearchHistoryWords();
        } else {
            setNormalWordTitle();
        }
        if(hots!=null && !hots.isEmpty()){
            setHotWordsData(hots);
            return;
        }
        if (hotWordsRequested) {
            return;
        }
        hotWordsRequested = true;
        // 加载热词
        OkGo.<String>get(HOT_SEARCH_URL)
//        OkGo.<String>get("https://api.web.360kan.com/v1/rank")
//                .params("cat", "1")
                .headers("User-Agent", "Mozilla/5.0")
                .execute(new AbsCallback<String>() {
                    @Override
                    public void onSuccess(Response<String> response) {
                        try {
                            ArrayList<String> data = new ArrayList<String>();
                            JsonArray itemList = JsonParser.parseString(response.body()).getAsJsonObject().get("subjects").getAsJsonArray();
//                            JsonArray itemList = JsonParser.parseString(response.body()).getAsJsonObject().get("data").getAsJsonArray();
                            for (JsonElement ele : itemList) {
                                JsonObject obj = (JsonObject) ele;
                                if (obj.has("title")) {
                                    addHotWord(data, obj.get("title").getAsString());
                                }
                            }
                            if (data.isEmpty()) {
                                useDefaultHotWords();
                                return;
                            }
                            cacheHotWords(data);
                        } catch (Throwable th) {
                            th.printStackTrace();
                            useDefaultHotWords();
                        }
                    }

                    @Override
                    public void onError(Response<String> response) {
                        super.onError(response);
                        useDefaultHotWords();
                    }

                    @Override
                    public String convertResponse(okhttp3.Response response) throws Throwable {
                        return response.body().string();
                    }
                });

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void server(ServerEvent event) {
        if (event.type == ServerEvent.SERVER_SEARCH) {
            String title = (String) event.obj;
            showLoading();
            if(Hawk.get(HawkConfig.FAST_SEARCH_MODE, true)){
                Bundle bundle = new Bundle();
                bundle.putString("title", title);
                jumpActivity(FastSearchActivity.class, bundle);
            }else{
                search(title);
            }
        }
    }

    private void initCheckedSourcesForSearch() {
        mCheckSources = SearchHelper.getSourcesForSearch();
    }

    public static void setCheckedSourcesForSearch(HashMap<String,String> checkedSources) {
        mCheckSources = checkedSources;
    }

    private void search(String title) {
        if (remoteDialog != null) {
            remoteDialog.dismiss();
            remoteDialog = null;
        }
        etSearch.setText(title);
        HistoryHelper.setSearchHistory(title);
        searchModel.search(title, LegacySearchSites.snapshot(), LegacySearchSites.homeKey(),
                mCheckSources == null ? null : new java.util.HashSet<>(mCheckSources.keySet()));
    }

    @Override
    protected void onPause() {
        searchModel.pause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    private void hiddenImm()
    {
        InputMethodManager imm = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
        }
    }
}
