package ml.dukan.stores.CustomViews;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.widget.AppCompatTextView;
import android.util.AttributeSet;

import ml.dukan.stores.R;

/**
 * Created by khaled on 07/07/17.
 */

public class TextViewSquared extends AppCompatTextView {
    private boolean isSquare = false;

    public TextViewSquared(Context context) {
        super(context);
        init(null);
    }

    public TextViewSquared(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public TextViewSquared(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        if (attrs == null) {

        } else {
            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.TextAlphaSquareTextView);
            isSquare = a.getBoolean(R.styleable.TextAlphaSquareTextView_square_mode, false);
            a.recycle();
        }
        setText(getText());
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (isSquare) {
            int width = this.getMeasuredWidth();
            int height = this.getMeasuredHeight();
            int size = Math.max(width, height);
            int widthSpec = MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY);
            int heightSpec = MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY);
            super.onMeasure(widthSpec, heightSpec);
        }
    }
}