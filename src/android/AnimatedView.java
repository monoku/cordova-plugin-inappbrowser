package org.apache.cordova.inappbrowser;

import android.content.Context;
import android.view.View;
import android.view.animation.Animation;

import android.view.animation.Transformation;
import android.widget.LinearLayout;

/**
 * Created by davsket on 1/26/15.
 */
public class AnimatedView extends LinearLayout
{
    private Animation inAnimation;
    private Animation outAnimation;

    public AnimatedView(Context context)
    {
        super(context);
        final AnimatedView self = this;

        outAnimation = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                int newHeight = (int) (self.getMeasuredHeight() * (1 - interpolatedTime));
                self.getLayoutParams().height = newHeight;
                self.requestLayout();

                if (interpolatedTime == 1)
                    self.setVisibility(View.GONE);
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };
    }

    public void setInAnimation(Animation inAnimation)
    {
        this.inAnimation = inAnimation;
    }

    public void setOutAnimation(Animation outAnimation)
    {
        this.outAnimation = outAnimation;
    }

    @Override
    public void setVisibility(int visibility)
    {
        if (getVisibility() != visibility)
        {
            if (visibility == VISIBLE)
            {
                if (inAnimation != null) startAnimation(inAnimation);
            }
            else if ((visibility == INVISIBLE) || (visibility == GONE))
            {
                if (outAnimation != null) startAnimation(outAnimation);
            }
        }

        super.setVisibility(visibility);
    }
}