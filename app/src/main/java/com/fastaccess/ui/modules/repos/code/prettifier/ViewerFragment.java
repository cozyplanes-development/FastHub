package com.fastaccess.ui.modules.repos.code.prettifier;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.design.widget.AppBarLayout;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;

import com.evernote.android.state.State;
import com.fastaccess.R;
import com.fastaccess.helper.ActivityHelper;
import com.fastaccess.helper.BundleConstant;
import com.fastaccess.helper.Bundler;
import com.fastaccess.helper.InputHelper;
import com.fastaccess.helper.Logger;
import com.fastaccess.helper.PrefGetter;
import com.fastaccess.ui.base.BaseFragment;
import com.fastaccess.ui.widgets.StateLayout;
import com.prettifier.pretty.PrettifyWebView;

import butterknife.BindView;
import it.sephiroth.android.library.bottomnavigation.BottomNavigation;

/**
 * Created by Kosh on 28 Nov 2016, 9:27 PM
 */

public class ViewerFragment extends BaseFragment<ViewerMvp.View, ViewerPresenter> implements ViewerMvp.View {

    public static final String TAG = ViewerFragment.class.getSimpleName();

    @BindView(R.id.readmeLoader) ProgressBar loader;
    @BindView(R.id.webView) PrettifyWebView webView;
    @BindView(R.id.stateLayout) StateLayout stateLayout;
    private AppBarLayout appBarLayout;
    private BottomNavigation bottomNavigation;
    private boolean scrolledTop = true;
    @State boolean isWrap = PrefGetter.isWrapCode();

    public static ViewerFragment newInstance(@NonNull String url, @Nullable String htmlUrl) {
        return newInstance(url, htmlUrl, false);
    }

    public static ViewerFragment newInstance(@NonNull String url, boolean isRepo) {
        return newInstance(url, null, isRepo);
    }

    public static ViewerFragment newInstance(@NonNull String url, @Nullable String htmlUrl, boolean isRepo) {
        return newInstance(Bundler.start()
                .put(BundleConstant.ITEM, url)
                .put(BundleConstant.EXTRA_TWO, htmlUrl)
                .put(BundleConstant.EXTRA, isRepo)
                .end());
    }

    private static ViewerFragment newInstance(@NonNull Bundle bundle) {
        ViewerFragment fragmentView = new ViewerFragment();
        fragmentView.setArguments(bundle);
        return fragmentView;
    }

    @Override public void onSetImageUrl(@NonNull String url) {
        webView.loadImage(url);
        webView.setOnContentChangedListener(this);
        webView.setVisibility(View.VISIBLE);
        getActivity().supportInvalidateOptionsMenu();
    }

    @Override public void onSetMdText(@NonNull String text, String baseUrl) {
        webView.setVisibility(View.VISIBLE);
        webView.setGithubContent(text, baseUrl);
        webView.setOnContentChangedListener(this);
        getActivity().supportInvalidateOptionsMenu();
    }

    @Override public void onSetCode(@NonNull String text) {
        webView.setVisibility(View.VISIBLE);
        webView.setSource(text, isWrap);
        webView.setOnContentChangedListener(this);
        getActivity().supportInvalidateOptionsMenu();
    }

    @Override public void onShowError(@NonNull String msg) {
        hideProgress();
        showErrorMessage(msg);
    }

    @Override public void onShowError(@StringRes int msg) {
        hideProgress();
        onShowError(getString(msg));
    }

    @Override public void onShowMdProgress() {
        loader.setVisibility(View.VISIBLE);
        stateLayout.showProgress();
    }

    @Override public void openUrl(@NonNull String url) {
        ActivityHelper.startCustomTab(getActivity(), url);
    }

    @Override public void showProgress(@StringRes int resId) {
        onShowMdProgress();
    }

    @Override public void hideProgress() {
        loader.setVisibility(View.GONE);
        stateLayout.hideProgress();
        if (!getPresenter().isImage()) stateLayout.showReload(getPresenter().downloadedStream() == null ? 0 : 1);
    }

    @Override public void showErrorMessage(@NonNull String msgRes) {
        hideProgress();
        super.showErrorMessage(msgRes);
    }

    @Override public void showMessage(int titleRes, int msgRes) {
        hideProgress();
        super.showMessage(titleRes, msgRes);
    }

    @Override public void showMessage(@NonNull String titleRes, @NonNull String msgRes) {
        hideProgress();
        super.showMessage(titleRes, msgRes);
    }

    @Override protected int fragmentLayout() {
        return R.layout.general_viewer_layout;
    }

    @NonNull @Override public ViewerPresenter providePresenter() {
        return new ViewerPresenter();
    }

    @Override public void onContentChanged(int progress) {
        if (loader != null) {
            loader.setProgress(progress);
            if (progress == 100) {
                hideProgress();
                if (!getPresenter().isMarkDown() && !getPresenter().isImage()) {
                    webView.scrollToLine(getPresenter().url());
                }
            }
        }
    }

    @Override public void onScrollChanged(boolean reachedTop, int scroll) {
        if (getPresenter().isRepo()) {
            if (appBarLayout != null && bottomNavigation != null) {
                Logger.e(scroll, appBarLayout.getTotalScrollRange());
                if (scroll == 0) {
                    scrolledTop = true;
                    bottomNavigation.setExpanded(true, true);
                    appBarLayout.setExpanded(true, true);
                } else if (scroll >= appBarLayout.getTotalScrollRange() && scrolledTop) {
                    bottomNavigation.setExpanded(false, true);
                    appBarLayout.setExpanded(false, true);
                    scrolledTop = false;
                }
                webView.setNestedScrollingEnabled(scroll < 800);
            }
        }
    }

    @Override public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override protected void onFragmentCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (InputHelper.isEmpty(getPresenter().downloadedStream())) {
            getPresenter().onHandleIntent(getArguments());
        } else {
            if (getPresenter().isMarkDown()) {
                onSetMdText(getPresenter().downloadedStream(), getPresenter().url());
            } else {
                onSetCode(getPresenter().downloadedStream());
            }
        }
        getActivity().invalidateOptionsMenu();
        stateLayout.setEmptyText(R.string.no_data);
        if (savedInstanceState == null) {
            stateLayout.showReload(0);
        }
        stateLayout.setOnReloadListener(view1 -> getPresenter().onHandleIntent(getArguments()));
        if (getPresenter().isRepo()) {
            appBarLayout = (AppBarLayout) getActivity().findViewById(R.id.appbar);
            bottomNavigation = (BottomNavigation) getActivity().findViewById(R.id.bottomNavigation);
        }
    }

    @Override public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.wrap_menu_option, menu);
        menu.findItem(R.id.wrap).setVisible(false);
    }

    @Override public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        MenuItem menuItem = menu.findItem(R.id.wrap);
        if (menuItem != null) {
            if (getPresenter().isMarkDown() || getPresenter().isRepo() || getPresenter().isImage()) {
                menuItem.setVisible(false);
            } else {
                menuItem.setVisible(true).setCheckable(true).setChecked(isWrap);
            }
        }
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.wrap) {
            item.setChecked(!item.isChecked());
            isWrap = item.isChecked();
            showProgress(0);
            onSetCode(getPresenter().downloadedStream());
        }
        return super.onOptionsItemSelected(item);
    }

    @Override public void onScrollTop(int index) {
        super.onScrollTop(index);
        if (webView != null) webView.scrollTo(0, 0);
    }

    @Override public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (!isVisibleToUser && appBarLayout != null) {
            appBarLayout.setVisibility(View.VISIBLE);
        }
    }
}
