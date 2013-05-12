/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zenlibs.historyedittext;

import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.Selection;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

/**
 * <p>
 * An editable text view that shows completion suggestions automatically while
 * the user is typing. The list of suggestions is displayed in a drop down menu
 * from which the user can choose an item to replace the content of the edit box
 * with.
 * </p>
 * 
 * <p>
 * The drop down can be dismissed at any time by pressing the back key or, if no
 * item is selected in the drop down, by pressing the enter/dpad center key.
 * </p>
 * 
 * <p>
 * The list of suggestions is obtained from a data adapter and appears only
 * after a given number of characters defined by {@link #getThreshold() the
 * threshold}.
 * </p>
 * 
 * <p>
 * The following code snippet shows how to create a text view which suggests
 * various countries names while the user is typing:
 * </p>
 * 
 * <pre class="prettyprint">
 * public class CountriesActivity extends Activity {
 *     protected void onCreate(Bundle icicle) {
 *         super.onCreate(icicle);
 *         setContentView(R.layout.countries);
 * 
 *         ArrayAdapter&lt;String&gt; adapter = new ArrayAdapter&lt;String&gt;(this, android.R.layout.simple_dropdown_item_1line,
 *                 COUNTRIES);
 *         AutoCompleteTextView textView = (AutoCompleteTextView) findViewById(R.id.countries_list);
 *         textView.setAdapter(adapter);
 *     }
 * 
 *     private static final String[] COUNTRIES = new String[] { &quot;Belgium&quot;, &quot;France&quot;, &quot;Italy&quot;, &quot;Germany&quot;, &quot;Spain&quot; };
 * }
 * </pre>
 * 
 * @attr ref android.android.R.styleable#AutoCompleteTextView_completionHint
 * @attr ref
 *       android.android.R.styleable#AutoCompleteTextView_completionThreshold
 * @attr ref android.android.R.styleable#AutoCompleteTextView_completionHintView
 * @attr ref android.android.R.styleable#AutoCompleteTextView_dropDownSelector
 * @attr ref android.android.R.styleable#AutoCompleteTextView_dropDownAnchor
 * @attr ref android.android.R.styleable#AutoCompleteTextView_dropDownWidth
 * @attr ref android.android.R.styleable#AutoCompleteTextView_dropDownHeight
 * @attr ref
 *       android.android.R.styleable#AutoCompleteTextView_dropDownVerticalOffset
 * @attr ref
 *       android.android.R.styleable#AutoCompleteTextView_dropDownHorizontalOffset
 */
abstract class AbsHistoryEditText extends EditText {
    static final boolean DEBUG = false;
    static final String TAG = "AutoCompleteTextView";

    private static final int HINT_VIEW_ID = 0x17;

    /**
     * This value controls the length of time that the user must leave a pointer
     * down without scrolling to expand the autocomplete dropdown list to cover
     * the IME.
     */
    private static final int EXPAND_LIST_TIMEOUT = 250;

    private CharSequence mHintText;
    private int mHintResource;

    // Zenlibs: this was mAdapter, renamed to mUserAdapter
    private ListAdapter mUserAdapter;
    private Filter mFilter;

    private FroyoPopupWindow mPopup;
    private DropDownListView mDropDownList;
    private int mDropDownVerticalOffset;
    private int mDropDownHorizontalOffset;
    private int mDropDownAnchorId;
    private View mDropDownAnchorView; // view is retrieved lazily from id once needed
    private int mDropDownWidth;
    private int mDropDownHeight;
    private final Rect mTempRect = new Rect();

    private Drawable mDropDownListHighlight;

    private AdapterView.OnItemClickListener mItemClickListener;
    private AdapterView.OnItemSelectedListener mItemSelectedListener;

    private final DropDownItemClickListener mDropDownItemClickListener = new DropDownItemClickListener();

    private boolean mDropDownDismissedOnCompletion = true;

    private boolean mForceIgnoreOutsideTouch = false;

    private int mLastKeyCode = KeyEvent.KEYCODE_UNKNOWN;
    private boolean mOpenBefore;

    private Validator mValidator = null;

    private boolean mBlockCompletion;

    private ListSelectorHider mHideSelector;
    private Runnable mShowDropDownRunnable;
    private Runnable mResizePopupRunnable = new ResizePopupRunnable();

    private PassThroughClickListener mPassThroughClickListener;
    private PopupDataSetObserver mObserver;
    private InputMethodManager mImm;
    // Zenlibs
    private ListAdapter mCombinedAdapter;

    public AbsHistoryEditText(Context context) {
        this(context, null);
    }

    public AbsHistoryEditText(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.autoCompleteTextViewStyle);
    }

    public AbsHistoryEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        mImm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);

        mPopup = new FroyoPopupWindow(context, attrs, android.R.attr.autoCompleteTextViewStyle);
        mPopup.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.HistoryEditText, defStyle,
                R.style.Widget_HistoryEditText);

        mHintText = a.getText(R.styleable.HistoryEditText_android_completionHint);

        mDropDownListHighlight = a.getDrawable(R.styleable.HistoryEditText_android_dropDownSelector);
        mDropDownVerticalOffset = (int) a
                .getDimension(R.styleable.HistoryEditText_android_dropDownVerticalOffset, 0.0f);
        mDropDownHorizontalOffset = (int) a.getDimension(R.styleable.HistoryEditText_android_dropDownHorizontalOffset,
                0.0f);

        // Get the anchor's id now, but the view won't be ready, so wait to actually get the
        // view and store it in mDropDownAnchorView lazily in getDropDownAnchorView later.
        // Defaults to NO_ID, in which case the getDropDownAnchorView method will simply return
        // this TextView, as a default anchoring point.
        mDropDownAnchorId = a.getResourceId(R.styleable.HistoryEditText_android_dropDownAnchor, View.NO_ID);

        // For dropdown width, the developer can specify a specific width, or MATCH_PARENT
        // (for full screen width) or WRAP_CONTENT (to match the width of the anchored view).
        mDropDownWidth = a.getLayoutDimension(R.styleable.HistoryEditText_android_dropDownWidth,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        mDropDownHeight = a.getLayoutDimension(R.styleable.HistoryEditText_android_dropDownHeight,
                ViewGroup.LayoutParams.WRAP_CONTENT);

        mHintResource = a.getResourceId(R.styleable.HistoryEditText_android_completionHintView,
                R.layout.het__simple_dropdown_hint);

        // Always turn on the auto complete input type flag, since it
        // makes no sense to use this widget without it.
        int inputType = getInputType();
        if ((inputType & EditorInfo.TYPE_MASK_CLASS) == EditorInfo.TYPE_CLASS_TEXT) {
            inputType |= EditorInfo.TYPE_TEXT_FLAG_AUTO_COMPLETE;
            setRawInputType(inputType);
        }

        a.recycle();

        setFocusable(true);

        addTextChangedListener(new MyWatcher());

        mPassThroughClickListener = new PassThroughClickListener();
        super.setOnClickListener(mPassThroughClickListener);
    }

    @Override
    public void setOnClickListener(OnClickListener listener) {
        mPassThroughClickListener.mWrapped = listener;
    }

    /**
     * Private hook into the on click event, dispatched from
     * {@link PassThroughClickListener}
     */
    private void onClickImpl() {
        // If the dropdown is showing, bring the keyboard to the front
        // when the user touches the text field.
        if (mPopup.isShowing()) {
            ensureImeVisible(true);
        }
    }

    /**
     * <p>
     * Sets the optional hint text that is displayed at the bottom of the the
     * matching list. This can be used as a cue to the user on how to best use
     * the list, or to provide extra information.
     * </p>
     * 
     * @param hint
     *            the text to be displayed to the user
     * 
     * @attr ref android.android.R.styleable#AutoCompleteTextView_completionHint
     */
    public void setCompletionHint(CharSequence hint) {
        mHintText = hint;
    }

    /**
     * <p>
     * Returns the current width for the auto-complete drop down list. This can
     * be a fixed width, or {@link ViewGroup.LayoutParams#MATCH_PARENT} to fill
     * the screen, or {@link ViewGroup.LayoutParams#WRAP_CONTENT} to fit the
     * width of its anchor view.
     * </p>
     * 
     * @return the width for the drop down list
     * 
     * @attr ref android.android.R.styleable#AutoCompleteTextView_dropDownWidth
     */
    public int getDropDownWidth() {
        return mDropDownWidth;
    }

    /**
     * <p>
     * Sets the current width for the auto-complete drop down list. This can be
     * a fixed width, or {@link ViewGroup.LayoutParams#MATCH_PARENT} to fill the
     * screen, or {@link ViewGroup.LayoutParams#WRAP_CONTENT} to fit the width
     * of its anchor view.
     * </p>
     * 
     * @param width
     *            the width to use
     * 
     * @attr ref android.android.R.styleable#AutoCompleteTextView_dropDownWidth
     */
    public void setDropDownWidth(int width) {
        mDropDownWidth = width;
    }

    /**
     * <p>
     * Returns the current height for the auto-complete drop down list. This can
     * be a fixed height, or {@link ViewGroup.LayoutParams#MATCH_PARENT} to fill
     * the screen, or {@link ViewGroup.LayoutParams#WRAP_CONTENT} to fit the
     * height of the drop down's content.
     * </p>
     * 
     * @return the height for the drop down list
     * 
     * @attr ref android.android.R.styleable#AutoCompleteTextView_dropDownHeight
     */
    public int getDropDownHeight() {
        return mDropDownHeight;
    }

    /**
     * <p>
     * Sets the current height for the auto-complete drop down list. This can be
     * a fixed height, or {@link ViewGroup.LayoutParams#MATCH_PARENT} to fill
     * the screen, or {@link ViewGroup.LayoutParams#WRAP_CONTENT} to fit the
     * height of the drop down's content.
     * </p>
     * 
     * @param height
     *            the height to use
     * 
     * @attr ref android.android.R.styleable#AutoCompleteTextView_dropDownHeight
     */
    public void setDropDownHeight(int height) {
        mDropDownHeight = height;
    }

    /**
     * <p>
     * Returns the id for the view that the auto-complete drop down list is
     * anchored to.
     * </p>
     * 
     * @return the view's id, or {@link View#NO_ID} if none specified
     * 
     * @attr ref android.android.R.styleable#AutoCompleteTextView_dropDownAnchor
     */
    public int getDropDownAnchor() {
        return mDropDownAnchorId;
    }

    /**
     * <p>
     * Sets the view to which the auto-complete drop down list should anchor.
     * The view corresponding to this id will not be loaded until the next time
     * it is needed to avoid loading a view which is not yet instantiated.
     * </p>
     * 
     * @param id
     *            the id to anchor the drop down list view to
     * 
     * @attr ref android.android.R.styleable#AutoCompleteTextView_dropDownAnchor
     */
    public void setDropDownAnchor(int id) {
        mDropDownAnchorId = id;
        mDropDownAnchorView = null;
    }

    /**
     * <p>
     * Gets the background of the auto-complete drop-down list.
     * </p>
     * 
     * @return the background drawable
     * 
     * @attr ref android.android.R.styleable#PopupWindow_popupBackground
     */
    public Drawable getDropDownBackground() {
        return mPopup.getBackground();
    }

    /**
     * <p>
     * Sets the background of the auto-complete drop-down list.
     * </p>
     * 
     * @param d
     *            the drawable to set as the background
     * 
     * @attr ref android.android.R.styleable#PopupWindow_popupBackground
     */
    public void setDropDownBackgroundDrawable(Drawable d) {
        mPopup.setBackgroundDrawable(d);
    }

    /**
     * <p>
     * Sets the background of the auto-complete drop-down list.
     * </p>
     * 
     * @param id
     *            the id of the drawable to set as the background
     * 
     * @attr ref android.android.R.styleable#PopupWindow_popupBackground
     */
    public void setDropDownBackgroundResource(int id) {
        mPopup.setBackgroundDrawable(getResources().getDrawable(id));
    }

    /**
     * <p>
     * Sets the vertical offset used for the auto-complete drop-down list.
     * </p>
     * 
     * @param offset
     *            the vertical offset
     */
    public void setDropDownVerticalOffset(int offset) {
        mDropDownVerticalOffset = offset;
    }

    /**
     * <p>
     * Gets the vertical offset used for the auto-complete drop-down list.
     * </p>
     * 
     * @return the vertical offset
     */
    public int getDropDownVerticalOffset() {
        return mDropDownVerticalOffset;
    }

    /**
     * <p>
     * Sets the horizontal offset used for the auto-complete drop-down list.
     * </p>
     * 
     * @param offset
     *            the horizontal offset
     */
    public void setDropDownHorizontalOffset(int offset) {
        mDropDownHorizontalOffset = offset;
    }

    /**
     * <p>
     * Gets the horizontal offset used for the auto-complete drop-down list.
     * </p>
     * 
     * @return the horizontal offset
     */
    public int getDropDownHorizontalOffset() {
        return mDropDownHorizontalOffset;
    }

    /**
     * <p>
     * Sets the animation style of the auto-complete drop-down list.
     * </p>
     * 
     * <p>
     * If the drop-down is showing, calling this method will take effect only
     * the next time the drop-down is shown.
     * </p>
     * 
     * @param animationStyle
     *            animation style to use when the drop-down appears and
     *            disappears. Set to -1 for the default animation, 0 for no
     *            animation, or a resource identifier for an explicit animation.
     * 
     * @hide Pending API council approval
     */
    public void setDropDownAnimationStyle(int animationStyle) {
        mPopup.setAnimationStyle(animationStyle);
    }

    /**
     * <p>
     * Returns the animation style that is used when the drop-down list appears
     * and disappears
     * </p>
     * 
     * @return the animation style that is used when the drop-down list appears
     *         and disappears
     * 
     * @hide Pending API council approval
     */
    public int getDropDownAnimationStyle() {
        return mPopup.getAnimationStyle();
    }

    /**
     * Checks whether the drop-down is dismissed when a suggestion is clicked.
     * 
     * @hide Pending API council approval
     */
    public boolean isDropDownDismissedOnCompletion() {
        return mDropDownDismissedOnCompletion;
    }

    /**
     * Sets whether the drop-down is dismissed when a suggestion is clicked.
     * This is true by default.
     * 
     * @param dropDownDismissedOnCompletion
     *            Whether to dismiss the drop-down.
     * 
     * @hide Pending API council approval
     */
    public void setDropDownDismissedOnCompletion(boolean dropDownDismissedOnCompletion) {
        mDropDownDismissedOnCompletion = dropDownDismissedOnCompletion;
    }

    /**
     * <p>
     * Sets the listener that will be notified when the user clicks an item in
     * the drop down list.
     * </p>
     * 
     * @param l
     *            the item click listener
     */
    public void setOnItemClickListener(AdapterView.OnItemClickListener l) {
        mItemClickListener = l;
    }

    /**
     * <p>
     * Sets the listener that will be notified when the user selects an item in
     * the drop down list.
     * </p>
     * 
     * @param l
     *            the item selected listener
     */
    public void setOnItemSelectedListener(AdapterView.OnItemSelectedListener l) {
        mItemSelectedListener = l;
    }

    /**
     * <p>
     * Returns the listener that is notified whenever the user clicks an item in
     * the drop down list.
     * </p>
     * 
     * @return the item click listener
     * 
     * @deprecated Use {@link #getOnItemClickListener()} intead
     */
    @Deprecated
    public AdapterView.OnItemClickListener getItemClickListener() {
        return mItemClickListener;
    }

    /**
     * <p>
     * Returns the listener that is notified whenever the user selects an item
     * in the drop down list.
     * </p>
     * 
     * @return the item selected listener
     * 
     * @deprecated Use {@link #getOnItemSelectedListener()} intead
     */
    @Deprecated
    public AdapterView.OnItemSelectedListener getItemSelectedListener() {
        return mItemSelectedListener;
    }

    /**
     * <p>
     * Returns the listener that is notified whenever the user clicks an item in
     * the drop down list.
     * </p>
     * 
     * @return the item click listener
     */
    public AdapterView.OnItemClickListener getOnItemClickListener() {
        return mItemClickListener;
    }

    /**
     * <p>
     * Returns the listener that is notified whenever the user selects an item
     * in the drop down list.
     * </p>
     * 
     * @return the item selected listener
     */
    public AdapterView.OnItemSelectedListener getOnItemSelectedListener() {
        return mItemSelectedListener;
    }

    /**
     * <p>
     * Returns a filterable list adapter used for auto completion.
     * </p>
     * 
     * @return a data adapter used for auto completion
     */
    public ListAdapter getAdapter() {
        return mUserAdapter;
    }

    /**
     * <p>
     * Changes the list of data used for auto completion. The provided list must
     * be a filterable list adapter.
     * </p>
     * 
     * <p>
     * The caller is still responsible for managing any resources used by the
     * adapter. Notably, when the AutoCompleteTextView is closed or released,
     * the adapter is not notified. A common case is the use of
     * {@link android.widget.CursorAdapter}, which contains a
     * {@link android.database.Cursor} that must be closed. This can be done
     * automatically (see
     * {@link android.app.Activity#startManagingCursor(android.database.Cursor)
     * startManagingCursor()}), or by manually closing the cursor when the
     * AutoCompleteTextView is dismissed.
     * </p>
     * 
     * @param adapter
     *            the adapter holding the auto completion data
     * 
     * @see #getAdapter()
     * @see android.widget.Filterable
     * @see android.widget.ListAdapter
     */
    public <T extends ListAdapter & Filterable> void setAdapter(T adapter) {
        mUserAdapter = adapter;
        if (mUserAdapter != null) {  
            mFilter = ((Filterable) mUserAdapter).getFilter();
        } else {
            mFilter = null;
        }
        rebuildCombinedAdapter();
    }
    
    public void setCombinedAdapter(ListAdapter adapter) {
        if (mObserver == null) {
            mObserver = new PopupDataSetObserver();
        } else if (mCombinedAdapter != null) {
            mCombinedAdapter.unregisterDataSetObserver(mObserver);
        }
        mCombinedAdapter = adapter;
        if (mCombinedAdapter != null) {
            adapter.registerDataSetObserver(mObserver);
        } 

        if (mDropDownList != null) {
            mDropDownList.setAdapter(mCombinedAdapter);
        }
    }

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && isPopupShowing() && true) {
            // special case for the back key, we do not even try to send it
            // to the drop down list but instead, consume it immediately
            if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
                getKeyDispatcherState().startTracking(event, this);
                return true;
            } else if (event.getAction() == KeyEvent.ACTION_UP) {
                getKeyDispatcherState().handleUpEvent(event);
                if (event.isTracking() && !event.isCanceled()) {
                    dismissDropDown();
                    return true;
                }
            }
        }
        return super.onKeyPreIme(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (isPopupShowing() && mDropDownList.getSelectedItemPosition() >= 0) {
            boolean consumed = mDropDownList.onKeyUp(keyCode, event);
            if (consumed) {
                switch (keyCode) {
                // if the list accepts the key events and the key event
                // was a click, the text view gets the selected item
                // from the drop down as its content
                case KeyEvent.KEYCODE_ENTER:
                case KeyEvent.KEYCODE_DPAD_CENTER:
                    performCompletion();
                    return true;
                }
            }
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // when the drop down is shown, we drive it directly
        if (isPopupShowing()) {
            // the key events are forwarded to the list in the drop down view
            // note that ListView handles space but we don't want that to happen
            // also if selection is not currently in the drop down, then don't
            // let center or enter presses go there since that would cause it
            // to select one of its items
            if (keyCode != KeyEvent.KEYCODE_SPACE
                    && (mDropDownList.getSelectedItemPosition() >= 0 || (keyCode != KeyEvent.KEYCODE_ENTER && keyCode != KeyEvent.KEYCODE_DPAD_CENTER))) {
                int curIndex = mDropDownList.getSelectedItemPosition();
                boolean consumed;

                final boolean below = !mPopup.isAboveAnchor();

                final ListAdapter adapter = mCombinedAdapter;

                boolean allEnabled;
                int firstItem = Integer.MAX_VALUE;
                int lastItem = Integer.MIN_VALUE;

                if (adapter != null) {
                    allEnabled = adapter.areAllItemsEnabled();
                    firstItem = allEnabled ? 0 : mDropDownList.lookForSelectablePositionCompat(0, true);
                    lastItem = allEnabled ? adapter.getCount() - 1 : mDropDownList.lookForSelectablePositionCompat(
                            adapter.getCount() - 1, false);
                }

                if ((below && keyCode == KeyEvent.KEYCODE_DPAD_UP && curIndex <= firstItem)
                        || (!below && keyCode == KeyEvent.KEYCODE_DPAD_DOWN && curIndex >= lastItem)) {
                    // When the selection is at the top, we block the key
                    // event to prevent focus from moving.
                    clearListSelection();
                    mPopup.setInputMethodMode(PopupWindow.INPUT_METHOD_NEEDED);
                    showDropDown();
                    return true;
                } else {
                    // WARNING: Please read the comment where mListSelectionHidden
                    //          is declared
                    mDropDownList.mListSelectionHidden = false;
                }

                consumed = mDropDownList.onKeyDown(keyCode, event);
                if (DEBUG)
                    Log.v(TAG, "Key down: code=" + keyCode + " list consumed=" + consumed);

                if (consumed) {
                    // If it handled the key event, then the user is
                    // navigating in the list, so we should put it in front.
                    mPopup.setInputMethodMode(PopupWindow.INPUT_METHOD_NOT_NEEDED);
                    // Here's a little trick we need to do to make sure that
                    // the list view is actually showing its focus indicator,
                    // by ensuring it has focus and getting its window out
                    // of touch mode.
                    mDropDownList.requestFocusFromTouch();
                    showDropDown();

                    switch (keyCode) {
                    // avoid passing the focus from the text view to the
                    // next component
                    case KeyEvent.KEYCODE_ENTER:
                    case KeyEvent.KEYCODE_DPAD_CENTER:
                    case KeyEvent.KEYCODE_DPAD_DOWN:
                    case KeyEvent.KEYCODE_DPAD_UP:
                        return true;
                    }
                } else {
                    if (below && keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                        // when the selection is at the bottom, we block the
                        // event to avoid going to the next focusable widget
                        if (curIndex == lastItem) {
                            return true;
                        }
                    } else if (!below && keyCode == KeyEvent.KEYCODE_DPAD_UP && curIndex == firstItem) {
                        return true;
                    }
                }
            }
        } else {
            switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_DOWN:
                performValidation();
            }
        }

        mLastKeyCode = keyCode;
        boolean handled = super.onKeyDown(keyCode, event);
        mLastKeyCode = KeyEvent.KEYCODE_UNKNOWN;

        if (handled && isPopupShowing() && mDropDownList != null) {
            clearListSelection();
        }

        return handled;
    }

    /**
     * This is used to watch for edits to the text view. Note that we call to
     * methods on the auto complete text view class so that we can access
     * private vars without going through thunks.
     */
    private class MyWatcher implements TextWatcher {
        public void afterTextChanged(Editable s) {
            doAfterTextChanged();
        }

        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            doBeforeTextChanged();
        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
    }

    void doBeforeTextChanged() {
        if (mBlockCompletion)
            return;

        // when text is changed, inserted or deleted, we attempt to show
        // the drop down
        mOpenBefore = isPopupShowing();
        if (DEBUG)
            Log.v(TAG, "before text changed: open=" + mOpenBefore);
    }

    void doAfterTextChanged() {
        if (mBlockCompletion)
            return;

        // if the list was open before the keystroke, but closed afterwards,
        // then something in the keystroke processing (an input filter perhaps)
        // called performCompletion() and we shouldn't do any more processing.
        if (DEBUG)
            Log.v(TAG, "after text changed: openBefore=" + mOpenBefore + " open=" + isPopupShowing());
        if (mOpenBefore && !isPopupShowing()) {
            return;
        }

        if (mFilter != null) {
            performFiltering(getText(), mLastKeyCode);
        }
    }

    /**
     * <p>
     * Indicates whether the popup menu is showing.
     * </p>
     * 
     * @return true if the popup menu is showing, false otherwise
     */
    public boolean isPopupShowing() {
        return mPopup.isShowing();
    }

    /**
     * <p>
     * Converts the selected item from the drop down list into a sequence of
     * character that can be used in the edit box.
     * </p>
     * 
     * @param selectedItem
     *            the item selected by the user for completion
     * 
     * @return a sequence of characters representing the selected suggestion
     */
    protected CharSequence convertSelectionToString(Object selectedItem) {
        return mFilter.convertResultToString(selectedItem);
    }

    /**
     * <p>
     * Clear the list selection. This may only be temporary, as user input will
     * often bring it back.
     */
    public void clearListSelection() {
        final DropDownListView list = mDropDownList;
        if (list != null) {
            // WARNING: Please read the comment where mListSelectionHidden is declared
            list.mListSelectionHidden = true;
            list.hideSelectorCompat();
            list.requestLayout();
        }
    }

    /**
     * Set the position of the dropdown view selection.
     * 
     * @param position
     *            The position to move the selector to.
     */
    public void setListSelection(int position) {
        if (mPopup.isShowing() && (mDropDownList != null)) {
            mDropDownList.mListSelectionHidden = false;
            mDropDownList.setSelection(position);
            // ListView.setSelection() will call requestLayout()
        }
    }

    /**
     * Get the position of the dropdown view selection, if there is one. Returns
     * {@link ListView#INVALID_POSITION ListView.INVALID_POSITION} if there is
     * no dropdown or if there is no selection.
     * 
     * @return the position of the current selection, if there is one, or
     *         {@link ListView#INVALID_POSITION ListView.INVALID_POSITION} if
     *         not.
     * 
     * @see ListView#getSelectedItemPosition()
     */
    public int getListSelection() {
        if (mPopup.isShowing() && (mDropDownList != null)) {
            return mDropDownList.getSelectedItemPosition();
        }
        return ListView.INVALID_POSITION;
    }

    /**
     * <p>
     * Starts filtering the content of the drop down list. The filtering pattern
     * is the content of the edit box. Subclasses should override this method to
     * filter with a different pattern, for instance a substring of
     * <code>text</code>.
     * </p>
     * 
     * @param text
     *            the filtering pattern
     * @param keyCode
     *            the last character inserted in the edit box; beware that this
     *            will be null when text is being added through a soft input
     *            method.
     */
    protected void performFiltering(CharSequence text, int keyCode) {
        if (text == null) {
            updateDropDownForFilter(0, true);
        } else { 
            mFilter.filter(text, new Filter.FilterListener() {
                @Override
                public void onFilterComplete(int count) {
                    updateDropDownForFilter(count, false);
                }
            });
        }
    }

    /**
     * <p>
     * Performs the text completion by converting the selected item from the
     * drop down list into a string, replacing the text box's content with this
     * string and finally dismissing the drop down menu.
     * </p>
     */
    public void performCompletion() {
        performCompletion(null, -1, -1);
    }

    @Override
    public void onCommitCompletion(CompletionInfo completion) {
        if (isPopupShowing()) {
            mBlockCompletion = true;
            replaceText(completion.getText());
            mBlockCompletion = false;

            if (mItemClickListener != null) {
                final DropDownListView list = mDropDownList;
                // Note that we don't have a View here, so we will need to
                // supply null.  Hopefully no existing apps crash...
                mItemClickListener.onItemClick(list, null, completion.getPosition(), completion.getId());
            }
        }
    }

    private void performCompletion(View selectedView, int position, long id) {
        if (isPopupShowing()) {
            Object selectedItem;
            if (position < 0) {
                selectedItem = mDropDownList.getSelectedItem();
            } else {
                selectedItem = mCombinedAdapter.getItem(position);
            }
            if (selectedItem == null) {
                Log.w(TAG, "performCompletion: no selected item");
                return;
            }

            mBlockCompletion = true;
            replaceText(convertSelectionToString(selectedItem));
            mBlockCompletion = false;

            if (mItemClickListener != null) {
                final DropDownListView list = mDropDownList;

                if (selectedView == null || position < 0) {
                    selectedView = list.getSelectedView();
                    position = list.getSelectedItemPosition();
                    id = list.getSelectedItemId();
                }
                mItemClickListener.onItemClick(list, selectedView, position, id);
            }
        }

        if (mDropDownDismissedOnCompletion && true) {
            dismissDropDown();
        }
    }

    /**
     * Identifies whether the view is currently performing a text completion, so
     * subclasses can decide whether to respond to text changed events.
     */
    public boolean isPerformingCompletion() {
        return mBlockCompletion;
    }

    /**
     * Like {@link #setText(CharSequence)}, except that it can disable
     * filtering.
     * 
     * @param filter
     *            If <code>false</code>, no filtering will be performed as a
     *            result of this call.
     * 
     * @hide Pending API council approval.
     */
    public void setText(CharSequence text, boolean filter) {
        if (filter) {
            setText(text);
        } else {
            mBlockCompletion = true;
            setText(text);
            mBlockCompletion = false;
        }
    }

    /**
     * <p>
     * Performs the text completion by replacing the current text by the
     * selected item. Subclasses should override this method to avoid replacing
     * the whole content of the edit box.
     * </p>
     * 
     * @param text
     *            the selected suggestion in the drop down list
     */
    protected void replaceText(CharSequence text) {
        clearComposingText();

        setText(text);
        // make sure we keep the caret at the end of the text view
        Editable spannable = getText();
        Selection.setSelection(spannable, spannable.length());
    }

    private void updateDropDownForFilter(int count, boolean force) {
        // Not attached to window, don't update drop-down
        if (getWindowVisibility() == View.GONE)
            return;

        /*
         * This checks enoughToFilter() again because filtering requests
         * are asynchronous, so the result may come back after enough text
         * has since been deleted to make it no longer appropriate
         * to filter.
         */

        if (count > 0 || force) {
            if (hasFocus() && hasWindowFocus()) {
                showDropDown();
            }
        } else {
            dismissDropDown();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        if (!hasWindowFocus && true) {
            dismissDropDown();
        }
    }

    @Override
    protected void onDisplayHint(int hint) {
        super.onDisplayHint(hint);
        switch (hint) {
        case INVISIBLE:
            if (true) {
                dismissDropDown();
            }
            break;
        }
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect);
        // Perform validation if the view is losing focus.
        if (!focused) {
            performValidation();
        }
        if (!focused && true) {
            dismissDropDown();
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {
        dismissDropDown();
        super.onDetachedFromWindow();
    }

    /**
     * <p>
     * Closes the drop down if present on screen.
     * </p>
     */
    public void dismissDropDown() {
        if (mImm != null) {
            mImm.displayCompletions(this, null);
        }
        mPopup.dismiss();
        mPopup.setContentView(null);
        mDropDownList = null;
    }

    @Override
    protected boolean setFrame(final int l, int t, final int r, int b) {
        boolean result = super.setFrame(l, t, r, b);

        if (mPopup.isShowing()) {
            showDropDown();
        }

        return result;
    }

    /**
     * <p>
     * Used for lazy instantiation of the anchor view from the id we have. If
     * the value of the id is NO_ID or we can't find a view for the given id, we
     * return this TextView as the default anchoring point.
     * </p>
     */
    private View getDropDownAnchorView() {
        if (mDropDownAnchorView == null && mDropDownAnchorId != View.NO_ID) {
            mDropDownAnchorView = getRootView().findViewById(mDropDownAnchorId);
        }
        return mDropDownAnchorView == null ? this : mDropDownAnchorView;
    }

    /**
     * Issues a runnable to show the dropdown as soon as possible.
     * 
     * @hide internal used only by SearchDialog
     */
    public void showDropDownAfterLayout() {
        post(mShowDropDownRunnable);
    }

    /**
     * Ensures that the drop down is not obscuring the IME.
     * 
     * @param visible
     *            whether the ime should be in front. If false, the ime is
     *            pushed to the background.
     * @hide internal used only here and SearchDialog
     */
    public void ensureImeVisible(boolean visible) {
        mPopup.setInputMethodMode(visible ? PopupWindow.INPUT_METHOD_NEEDED : PopupWindow.INPUT_METHOD_NOT_NEEDED);
        showDropDown();
    }

    /**
     * @hide internal used only here and SearchDialog
     */
    public boolean isInputMethodNotNeeded() {
        return mPopup.getInputMethodMode() == PopupWindow.INPUT_METHOD_NOT_NEEDED;
    }

    /**
     * <p>
     * Displays the drop down on screen.
     * </p>
     */
    public void showDropDown() {
        int height = buildDropDown();

        int widthSpec = 0;
        int heightSpec = 0;

        boolean noInputMethod = isInputMethodNotNeeded();

        if (mPopup.isShowing()) {
            if (mDropDownWidth == ViewGroup.LayoutParams.MATCH_PARENT) {
                // The call to PopupWindow's update method below can accept -1 for any
                // value you do not want to update.
                widthSpec = -1;
            } else if (mDropDownWidth == ViewGroup.LayoutParams.WRAP_CONTENT) {
                widthSpec = getDropDownAnchorView().getWidth();
            } else {
                widthSpec = mDropDownWidth;
            }

            if (mDropDownHeight == ViewGroup.LayoutParams.MATCH_PARENT) {
                // The call to PopupWindow's update method below can accept -1 for any
                // value you do not want to update.
                heightSpec = noInputMethod ? height : ViewGroup.LayoutParams.MATCH_PARENT;
                if (noInputMethod) {
                    mPopup.setWindowLayoutMode(
                            mDropDownWidth == ViewGroup.LayoutParams.MATCH_PARENT ? ViewGroup.LayoutParams.MATCH_PARENT
                                    : 0, 0);
                } else {
                    mPopup.setWindowLayoutMode(
                            mDropDownWidth == ViewGroup.LayoutParams.MATCH_PARENT ? ViewGroup.LayoutParams.MATCH_PARENT
                                    : 0, ViewGroup.LayoutParams.MATCH_PARENT);
                }
            } else if (mDropDownHeight == ViewGroup.LayoutParams.WRAP_CONTENT) {
                heightSpec = height;
            } else {
                heightSpec = mDropDownHeight;
            }

            mPopup.setOutsideTouchable(!mForceIgnoreOutsideTouch && true);

            mPopup.update(getDropDownAnchorView(), mDropDownHorizontalOffset, mDropDownVerticalOffset, widthSpec,
                    heightSpec);
        } else {
            if (mDropDownWidth == ViewGroup.LayoutParams.MATCH_PARENT) {
                widthSpec = ViewGroup.LayoutParams.MATCH_PARENT;
            } else {
                if (mDropDownWidth == ViewGroup.LayoutParams.WRAP_CONTENT) {
                    mPopup.setWidth(getDropDownAnchorView().getWidth());
                } else {
                    mPopup.setWidth(mDropDownWidth);
                }
            }

            if (mDropDownHeight == ViewGroup.LayoutParams.MATCH_PARENT) {
                heightSpec = ViewGroup.LayoutParams.MATCH_PARENT;
            } else {
                if (mDropDownHeight == ViewGroup.LayoutParams.WRAP_CONTENT) {
                    mPopup.setHeight(height);
                } else {
                    mPopup.setHeight(mDropDownHeight);
                }
            }

            mPopup.setWindowLayoutMode(widthSpec, heightSpec);
            mPopup.setInputMethodMode(PopupWindow.INPUT_METHOD_NEEDED);

            // use outside touchable to dismiss drop down when touching outside of it, so
            // only set this if the dropdown is not always visible
            mPopup.setOutsideTouchable(!mForceIgnoreOutsideTouch && true);
            mPopup.setTouchInterceptor(new PopupTouchInterceptor());
            mPopup.showAsDropDown(getDropDownAnchorView(), mDropDownHorizontalOffset, mDropDownVerticalOffset);
            mDropDownList.setSelection(ListView.INVALID_POSITION);
            clearListSelection();
            post(mHideSelector);
        }
    }

    /**
     * Forces outside touches to be ignored. Normally if
     * {@link #isDropDownAlwaysVisible()} is false, we allow outside touch to
     * dismiss the dropdown. If this is set to true, then we ignore outside
     * touch even when the drop down is not set to always visible.
     * 
     * @hide used only by SearchDialog
     */
    public void setForceIgnoreOutsideTouch(boolean forceIgnoreOutsideTouch) {
        mForceIgnoreOutsideTouch = forceIgnoreOutsideTouch;
    }

    /**
     * <p>
     * Builds the popup window's content and returns the height the popup should
     * have. Returns -1 when the content already exists.
     * </p>
     * 
     * @return the content's height or -1 if content already exists
     */
    private int buildDropDown() {
        ViewGroup dropDownView;
        int otherHeights = 0;

        rebuildCombinedAdapter();
        
        if (mCombinedAdapter != null) {
            if (mImm != null) {
                final int count = Math.min(mCombinedAdapter.getCount(), 20);
                CompletionInfo[] completions = new CompletionInfo[count];
                int realCount = 0;

                for (int i = 0; i < count; i++) {
                    if (mCombinedAdapter.isEnabled(i)) {
                        realCount++;
                        Object item = mCombinedAdapter.getItem(i);
                        long id = mCombinedAdapter.getItemId(i);
                        completions[i] = new CompletionInfo(id, i, convertSelectionToString(item));
                    }
                }

                if (realCount != count) {
                    CompletionInfo[] tmp = new CompletionInfo[realCount];
                    System.arraycopy(completions, 0, tmp, 0, realCount);
                    completions = tmp;
                }

                mImm.displayCompletions(this, completions);
            }
        }

        if (mDropDownList == null) {
            Context context = getContext();

            mHideSelector = new ListSelectorHider();

            /**
             * This Runnable exists for the sole purpose of checking if the view
             * layout has got completed and if so call showDropDown to display
             * the drop down. This is used to show the drop down as soon as
             * possible after user opens up the search dialog, without waiting
             * for the normal UI pipeline to do it's job which is slower than
             * this method.
             */
            mShowDropDownRunnable = new Runnable() {
                public void run() {
                    // View layout should be all done before displaying the drop down.
                    View view = getDropDownAnchorView();
                    if (view != null && view.getWindowToken() != null) {
                        showDropDown();
                    }
                }
            };

            mDropDownList = new DropDownListView(context);
            mDropDownList.setSelector(mDropDownListHighlight);
            mDropDownList.setAdapter(mCombinedAdapter);
            mDropDownList.setVerticalFadingEdgeEnabled(true);
            mDropDownList.setOnItemClickListener(mDropDownItemClickListener);
            mDropDownList.setFocusable(true);
            mDropDownList.setFocusableInTouchMode(true);
            mDropDownList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                    if (position != -1) {
                        DropDownListView dropDownList = mDropDownList;

                        if (dropDownList != null) {
                            dropDownList.mListSelectionHidden = false;
                        }
                    }
                }

                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
            mDropDownList.setOnScrollListener(new PopupScrollListener());

            if (mItemSelectedListener != null) {
                mDropDownList.setOnItemSelectedListener(mItemSelectedListener);
            }

            dropDownView = mDropDownList;

            View hintView = getHintView(context);
            if (hintView != null) {
                // if an hint has been specified, we accomodate more space for it and
                // add a text view in the drop down menu, at the bottom of the list
                LinearLayout hintContainer = new LinearLayout(context);
                hintContainer.setOrientation(LinearLayout.VERTICAL);

                LinearLayout.LayoutParams hintParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f);
                hintContainer.addView(dropDownView, hintParams);
                hintContainer.addView(hintView);

                // measure the hint's height to find how much more vertical space
                // we need to add to the drop down's height
                int widthSpec = MeasureSpec.makeMeasureSpec(getWidth(), MeasureSpec.AT_MOST);
                int heightSpec = MeasureSpec.UNSPECIFIED;
                hintView.measure(widthSpec, heightSpec);

                hintParams = (LinearLayout.LayoutParams) hintView.getLayoutParams();
                otherHeights = hintView.getMeasuredHeight() + hintParams.topMargin + hintParams.bottomMargin;

                dropDownView = hintContainer;
            }

            mPopup.setContentView(dropDownView);
        } else {
            dropDownView = (ViewGroup) mPopup.getContentView();
            final View view = dropDownView.findViewById(HINT_VIEW_ID);
            if (view != null) {
                LinearLayout.LayoutParams hintParams = (LinearLayout.LayoutParams) view.getLayoutParams();
                otherHeights = view.getMeasuredHeight() + hintParams.topMargin + hintParams.bottomMargin;
            }
        }

        // Max height available on the screen for a popup.
        boolean ignoreBottomDecorations = mPopup.getInputMethodMode() == PopupWindow.INPUT_METHOD_NOT_NEEDED;
        final int maxHeight = mPopup.getMaxAvailableHeightCompat(getDropDownAnchorView(), mDropDownVerticalOffset,
                ignoreBottomDecorations);

        // getMaxAvailableHeight() subtracts the padding, so we put it back,
        // to get the available height for the whole window
        int padding = 0;
        Drawable background = mPopup.getBackground();
        if (background != null) {
            background.getPadding(mTempRect);
            padding = mTempRect.top + mTempRect.bottom;
        }

        if (false || mDropDownHeight == ViewGroup.LayoutParams.MATCH_PARENT) {
            return maxHeight + padding;
        }

        final int listContent = mDropDownList.measureHeightOfChildrenCompat(MeasureSpec.UNSPECIFIED, 0,
                FroyoListView.NO_POSITION, maxHeight - otherHeights, 2);
        // add padding only if the list has items in it, that way we don't show
        // the popup if it is not needed
        if (listContent > 0)
            otherHeights += padding;

        return listContent + otherHeights;
    }

    protected void rebuildCombinedAdapter() {
        ListAdapter adapter = getCombinedAdapter(mUserAdapter);
        setCombinedAdapter(adapter);
    }

    // Zenlibs
    protected abstract ListAdapter getCombinedAdapter(ListAdapter userAdapter);

    private View getHintView(Context context) {
        if (mHintText != null && mHintText.length() > 0) {
            final TextView hintView = (TextView) LayoutInflater.from(context).inflate(mHintResource, null)
                    .findViewById(android.R.id.text1);
            hintView.setText(mHintText);
            hintView.setId(HINT_VIEW_ID);
            return hintView;
        } else {
            return null;
        }
    }

    /**
     * Sets the validator used to perform text validation.
     * 
     * @param validator
     *            The validator used to validate the text entered in this
     *            widget.
     * 
     * @see #getValidator()
     * @see #performValidation()
     */
    public void setValidator(Validator validator) {
        mValidator = validator;
    }

    /**
     * Returns the Validator set with {@link #setValidator}, or
     * <code>null</code> if it was not set.
     * 
     * @see #setValidator(AbsHistoryEditText.Validator)
     * @see #performValidation()
     */
    public Validator getValidator() {
        return mValidator;
    }

    /**
     * If a validator was set on this view and the current string is not valid,
     * ask the validator to fix it.
     * 
     * @see #getValidator()
     * @see #setValidator(AbsHistoryEditText.Validator)
     */
    public void performValidation() {
        if (mValidator == null)
            return;

        CharSequence text = getText();

        if (!TextUtils.isEmpty(text) && !mValidator.isValid(text)) {
            setText(mValidator.fixText(text));
        }
    }

    /**
     * Returns the Filter obtained from {@link Filterable#getFilter}, or
     * <code>null</code> if {@link #setAdapter} was not called with a
     * Filterable.
     */
    protected Filter getFilter() {
        return mFilter;
    }

    private class ListSelectorHider implements Runnable {
        public void run() {
            clearListSelection();
        }
    }

    private class ResizePopupRunnable implements Runnable {
        public void run() {
            mPopup.setInputMethodMode(PopupWindow.INPUT_METHOD_NOT_NEEDED);
            showDropDown();
        }
    }

    private class PopupTouchInterceptor implements OnTouchListener {
        public boolean onTouch(View v, MotionEvent event) {
            final int action = event.getAction();
            if (action == MotionEvent.ACTION_DOWN && mPopup != null && mPopup.isShowing()) {
                postDelayed(mResizePopupRunnable, EXPAND_LIST_TIMEOUT);
            } else if (action == MotionEvent.ACTION_UP) {
                removeCallbacks(mResizePopupRunnable);
            }
            return false;
        }
    }

    private class PopupScrollListener implements ListView.OnScrollListener {
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

        }

        public void onScrollStateChanged(AbsListView view, int scrollState) {
            if (scrollState == SCROLL_STATE_TOUCH_SCROLL && !isInputMethodNotNeeded()
                    && mPopup.getContentView() != null) {
                removeCallbacks(mResizePopupRunnable);
                mResizePopupRunnable.run();
            }
        }
    }

    private class DropDownItemClickListener implements AdapterView.OnItemClickListener {
        public void onItemClick(AdapterView parent, View v, int position, long id) {
            performCompletion(v, position, id);
        }
    }

    /**
     * <p>
     * Wrapper class for a ListView. This wrapper hijacks the focus to make sure
     * the list uses the appropriate drawables and states when displayed on
     * screen within a drop down. The focus is never actually passed to the drop
     * down; the list only looks focused.
     * </p>
     */
    private static class DropDownListView extends FroyoListView {
        /*
         * WARNING: This is a workaround for a touch mode issue.
         *
         * Touch mode is propagated lazily to windows. This causes problems in
         * the following scenario:
         * - Type something in the AutoCompleteTextView and get some results
         * - Move down with the d-pad to select an item in the list
         * - Move up with the d-pad until the selection disappears
         * - Type more text in the AutoCompleteTextView *using the soft keyboard*
         *   and get new results; you are now in touch mode
         * - The selection comes back on the first item in the list, even though
         *   the list is supposed to be in touch mode
         *
         * Using the soft keyboard triggers the touch mode change but that change
         * is propagated to our window only after the first list layout, therefore
         * after the list attempts to resurrect the selection.
         *
         * The trick to work around this issue is to pretend the list is in touch
         * mode when we know that the selection should not appear, that is when
         * we know the user moved the selection away from the list.
         *
         * This boolean is set to true whenever we explicitely hide the list's
         * selection and reset to false whenver we know the user moved the
         * selection back to the list.
         *
         * When this boolean is true, isInTouchMode() returns true, otherwise it
         * returns super.isInTouchMode().
         */
        private boolean mListSelectionHidden;

        /**
         * <p>
         * Creates a new list view wrapper.
         * </p>
         * 
         * @param context
         *            this view's context
         */
        public DropDownListView(Context context) {
            super(context, null, android.R.attr.dropDownListViewStyle);
        }

        /**
         * <p>
         * Avoids jarring scrolling effect by ensuring that list elements made
         * of a text view fit on a single line.
         * </p>
         * 
         * @param position
         *            the item index in the list to get a view for
         * @return the view for the specified item
         */
        @Override
        View obtainView(int position, boolean[] isScrap) {
            View view = super.obtainView(position, isScrap);

            if (view instanceof TextView) {
                ((TextView) view).setHorizontallyScrolling(true);
            }

            return view;
        }

        @Override
        public boolean isInTouchMode() {
            // WARNING: Please read the comment where mListSelectionHidden is declared
            return mListSelectionHidden || super.isInTouchMode();
        }

        /**
         * <p>
         * Returns the focus state in the drop down.
         * </p>
         * 
         * @return true always
         */
        @Override
        public boolean hasWindowFocus() {
            return true;
        }

        /**
         * <p>
         * Returns the focus state in the drop down.
         * </p>
         * 
         * @return true always
         */
        @Override
        public boolean isFocused() {
            return true;
        }

        /**
         * <p>
         * Returns the focus state in the drop down.
         * </p>
         * 
         * @return true always
         */
        @Override
        public boolean hasFocus() {
            return true;
        }

        protected int[] onCreateDrawableState(int extraSpace) {
            int[] res = super.onCreateDrawableState(extraSpace);
            //noinspection ConstantIfStatement
            if (false) {
                StringBuilder sb = new StringBuilder("Created drawable state: [");
                for (int i = 0; i < res.length; i++) {
                    if (i > 0)
                        sb.append(", ");
                    sb.append("0x");
                    sb.append(Integer.toHexString(res[i]));
                }
                sb.append("]");
                Log.i(TAG, sb.toString());
            }
            return res;
        }
    }

    /**
     * This interface is used to make sure that the text entered in this
     * TextView complies to a certain format. Since there is no foolproof way to
     * prevent the user from leaving this View with an incorrect value in it,
     * all we can do is try to fix it ourselves when this happens.
     */
    public interface Validator {
        /**
         * Validates the specified text.
         * 
         * @return true If the text currently in the text editor is valid.
         * 
         * @see #fixText(CharSequence)
         */
        boolean isValid(CharSequence text);

        /**
         * Corrects the specified text to make it valid.
         * 
         * @param invalidText
         *            A string that doesn't pass validation:
         *            isValid(invalidText) returns false
         * 
         * @return A string based on invalidText such as invoking isValid() on
         *         it returns true.
         * 
         * @see #isValid(CharSequence)
         */
        CharSequence fixText(CharSequence invalidText);
    }

    /**
     * Allows us a private hook into the on click event without preventing users
     * from setting their own click listener.
     */
    private class PassThroughClickListener implements OnClickListener {

        private View.OnClickListener mWrapped;

        /** {@inheritDoc} */
        public void onClick(View v) {
            onClickImpl();

            if (mWrapped != null)
                mWrapped.onClick(v);
        }
    }

    private class PopupDataSetObserver extends DataSetObserver {
        @Override
        public void onChanged() {
            if (isPopupShowing()) {
                // This will resize the popup to fit the new adapter's content
                showDropDown();
            } else if (mCombinedAdapter != null) {
                // If the popup is not showing already, showing it will cause
                // the list of data set observers attached to the adapter to
                // change. We can't do it from here, because we are in the middle
                // of iterating throught he list of observers.
                post(new Runnable() {
                    public void run() {
                        final ListAdapter adapter = mCombinedAdapter;
                        if (adapter != null) {
                            updateDropDownForFilter(adapter.getCount(), false);
                        }
                    }
                });
            }
        }

        @Override
        public void onInvalidated() {
            if (true) {
                // There's no data to display so make sure we're not showing
                // the drop down and its list
                dismissDropDown();
            }
        }
    }
}