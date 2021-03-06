package com.example.xyzreader.ui;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.drawable.GradientDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.SubtitleCollapsingToolbarLayout;
import android.support.v4.app.ShareCompat;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.github.florent37.picassopalette.PicassoPalette;
import com.squareup.picasso.Picasso;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

import butterknife.BindView;
import butterknife.ButterKnife;
import ru.noties.markwon.Markwon;

/**
 * A fragment representing a single Article detail screen. This fragment is
 * either contained in a {@link ArticleListActivity} in two-pane mode (on
 * tablets) or a {@link ArticleDetailActivity} on handsets.
 */
public class ArticleDetailFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor> {
    public static final String ARG_ITEM_ID = "item_id";
    private static final String TAG = "ArticleDetailFragment";
    private static final String LIST_SCROLL_POSITION = "list_scroll_pos";
    @BindView(R.id.draw_insets_frame_layout)
    CoordinatorLayout mDrawInsetsFrameLayout;
    @BindView(R.id.photo)
    ImageView mPhotoView;
    @BindView(R.id.scrollview)
    NestedScrollView mScrollView;
    @BindView(R.id.share_fab)
    FloatingActionButton mFab;
    @BindView(R.id.fragment_collapsing_toolbar)
    SubtitleCollapsingToolbarLayout collapsingToolbar;
    @BindView(R.id.fragment_toolbar)
    Toolbar fragmentToolbar;
    @BindView(R.id.fragment_bar_layout)
    AppBarLayout fragmentBarLayout;
    @BindView(R.id.gradient_background)
    ImageView gradientBackground;
    @BindView(R.id.pb_wrap)
    FrameLayout progressBarWrap;
    private Cursor mCursor;
    private long mItemId;
    private View mRootView;
    private int mMutedColor = 0xFF333333;
    private boolean finishedLoading = false;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
    // Use default locale format
    private SimpleDateFormat outputFormat = new SimpleDateFormat();
    // Most time functions can only handle 1902 - 2037
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2, 1, 1);
    private String title;
    private String subtitle;
    private int savedScrollPos = 0;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ArticleDetailFragment() {
    }

    public static ArticleDetailFragment newInstance(long itemId) {
        Bundle arguments = new Bundle();
        arguments.putLong(ARG_ITEM_ID, itemId);
        ArticleDetailFragment fragment = new ArticleDetailFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            mItemId = getArguments().getLong(ARG_ITEM_ID);
        }

        setHasOptionsMenu(true);
    }

    private ArticleDetailActivity getActivityCast() {
        return (ArticleDetailActivity) getActivity();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final ArticleDetailFragment fragment = this;

        // In support library r8, calling initLoader for a fragment in a FragmentPagerAdapter in
        // the fragment's onCreate may cause the same LoaderManager to be dealt to multiple
        // fragments because their mIndex is -1 (haven't been added to the activity yet). Thus,
        // we do this in onActivityCreated.

        if (getLoaderManager().getLoader(0) == null) {
            getLoaderManager().initLoader(0, null, fragment);
        } else {
            getLoaderManager().restartLoader(0, savedInstanceState, fragment);
        }


    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(LIST_SCROLL_POSITION, mScrollView.getScrollY());
        super.onSaveInstanceState(outState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_article_detail, container, false);
        ButterKnife.bind(this, mRootView);

        mFab.hide();

        fragmentToolbar.setTitleMarginStart((int) (getResources().getDisplayMetrics().density * 72));

        bindViews();

        progressBarWrap.setVisibility(View.VISIBLE);

        return mRootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (savedInstanceState != null) {
            savedScrollPos = savedInstanceState.getInt(LIST_SCROLL_POSITION, 0);
        }

        moveToScrollIfPossible();
    }

    private void enableFab(final String title) {
        mFab.show();

        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(Intent.createChooser(ShareCompat.IntentBuilder.from(getActivity())
                        .setType("text/plain")
                        .setText(title)
                        .getIntent(), getString(R.string.action_share)));
            }
        });

        mScrollView.setOnScrollChangeListener(new NestedScrollView.OnScrollChangeListener() {
            @Override
            public void onScrollChange(NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                if (scrollY > 0) {
                    mFab.hide();
                } else {
                    mFab.show();
                }
            }
        });
    }

    private Date parsePublishedDate() {
        try {
            String date = mCursor.getString(ArticleLoader.Query.PUBLISHED_DATE);
            return dateFormat.parse(date);
        } catch (ParseException ex) {
            Log.e(TAG, ex.getMessage());
            Log.i(TAG, "passing today's date");
            return new Date();
        }
    }

    private void moveToScrollIfPossible() {
        if (finishedLoading) {
            mScrollView.scrollTo(0, savedScrollPos);
            mScrollView.scrollTo(0, savedScrollPos);
        }
    }

    private void bindViews() {
        if (mRootView == null) {
            return;
        }

        final TextView bodyView = mRootView.findViewById(R.id.article_body);

        if (mCursor != null) {
            mRootView.setVisibility(View.VISIBLE);
            title = mCursor.getString(ArticleLoader.Query.TITLE);
            subtitle = mCursor.getString(ArticleLoader.Query.AUTHOR);
            Date publishedDate = parsePublishedDate();

            String relativeDate = outputFormat.format(publishedDate);
            if (!publishedDate.before(START_OF_EPOCH.getTime())) {
                relativeDate = DateUtils.getRelativeTimeSpanString(
                        publishedDate.getTime(),
                        System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                        DateUtils.FORMAT_ABBREV_ALL).toString();
            }
            subtitle = Html.fromHtml(String.format(getString(R.string.date_by_author), relativeDate, mCursor.getString(ArticleLoader.Query.AUTHOR))).toString();

            collapsingToolbar.setTitle(title);
            collapsingToolbar.setSubtitle(subtitle);

            final Fragment fragment = this;

            new AsyncTask<Object, Void, CharSequence>() {

                @Override
                protected void onPostExecute(CharSequence s) {
                    bodyView.setText(s);

                    enableFab(title);

                    if (fragment.isVisible()) {
                        Animation fadeInAnimation = AnimationUtils.loadAnimation(getActivityCast(), R.anim.fade_out);
                        progressBarWrap.startAnimation(fadeInAnimation);
                        fadeInAnimation.setAnimationListener(new Animation.AnimationListener() {
                            @Override
                            public void onAnimationStart(Animation animation) {
                            }

                            @Override
                            public void onAnimationEnd(Animation animation) {
                                progressBarWrap.setVisibility(View.GONE);
                            }

                            @Override
                            public void onAnimationRepeat(Animation animation) {
                            }
                        });
                    } else {
                        progressBarWrap.setVisibility(View.GONE);
                    }

                    finishedLoading = true;

                    AsyncTask<Object, Void, Void> task = new AsyncTask<Object, Void, Void>() {
                        @Override
                        protected Void doInBackground(Object... objects) {
                            return null;
                        }

                        @Override
                        protected void onPostExecute(Void aVoid) {
                            super.onPostExecute(aVoid);

                            moveToScrollIfPossible();
                        }
                    };

                    task.execute();
                }

                @Override
                protected CharSequence doInBackground(Object... objects) {
                    return Markwon.markdown(getActivityCast(), mCursor.getString(ArticleLoader.Query.BODY));
                }
            }.execute();


            String imageUrl = mCursor.getString(ArticleLoader.Query.PHOTO_URL);

            Picasso.with(getActivityCast())
                    .load(imageUrl)
                    .into(mPhotoView,
                            PicassoPalette.with(imageUrl, mPhotoView)
                                    .use(PicassoPalette.Profile.MUTED_DARK)
                                    .intoCallBack(new PicassoPalette.CallBack() {
                                        @Override
                                        public void onPaletteLoaded(Palette palette) {
                                            Palette.Swatch swatch = palette.getDarkMutedSwatch();
                                            mMutedColor = swatch.getRgb();
                                            collapsingToolbar.setContentScrimColor(mMutedColor);
                                            int[] colors = new int[3];
                                            colors[0] = mMutedColor;
                                            colors[1] = mMutedColor;
                                            colors[2] = getResources().getColor(R.color.transparent);

                                            GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP, colors);

                                            gradient.setShape(GradientDrawable.RECTANGLE);
                                            gradient.setGradientType(GradientDrawable.LINEAR_GRADIENT);

                                            gradientBackground.setImageDrawable(gradient);

                                            collapsingToolbar.setExpandedTitleTextColor(swatch.getTitleTextColor());
                                            collapsingToolbar.setCollapsedTitleTextColor(swatch.getTitleTextColor());
                                        }
                                    }));
        } else {
            mRootView.setVisibility(View.GONE);
            bodyView.setText("Loading…");
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newInstanceForItemId(getActivity(), mItemId);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (!isAdded()) {
            if (cursor != null) {
                cursor.close();
            }
            return;
        }

        mCursor = cursor;
        if (mCursor != null && !mCursor.moveToFirst()) {
            Log.e(TAG, "Error reading item detail cursor");
            mCursor.close();
            mCursor = null;
        }

        bindViews();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mCursor = null;
        bindViews();
    }
}
