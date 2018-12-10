/**
 * Copyright 2016 Bartosz Schiller
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.barteksc.pdfviewer;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.graphics.PointF;
import android.util.Log;
import android.view.animation.DecelerateInterpolator;
import android.widget.OverScroller;


/**
 * This manager is used by the PDFView to launch animations.
 * It uses the ValueAnimator appeared in API 11 to start
 * an animation, and call moveTo() on the PDFView as a result
 * of each animation update.
 */
class AnimationManager {
    private static final String TAG = "zuo_AnimationManager";

    private PDFView pdfView;

    private ValueAnimator animation;

    private OverScroller scroller;

    private boolean flinging = false;

    private final int DURATION = 200;

    private boolean pageFlinging = false;
    private FlingChangeCallback flingChangeCallback;

    public AnimationManager(PDFView pdfView) {
        this.pdfView = pdfView;
        scroller = new OverScroller(pdfView.getContext());
    }

    public void startXAnimation(float xFrom, float xTo) {
        Log.d(TAG, "startXAnimation() called with: xFrom = [" + xFrom + "], xTo = [" + xTo + "]");
        stopAll();
        animation = ValueAnimator.ofFloat(xFrom, xTo);
        XAnimation xAnimation = new XAnimation();
        animation.setInterpolator(new DecelerateInterpolator());
        animation.addUpdateListener(xAnimation);
        animation.addListener(xAnimation);
        animation.setDuration(DURATION);
        animation.start();
    }

    public void startYAnimation(float yFrom, float yTo) {
        Log.d(TAG, "startYAnimation() called with: yFrom = [" + yFrom + "], yTo = [" + yTo + "]");
        stopAll();
        animation = ValueAnimator.ofFloat(yFrom, yTo);
        YAnimation yAnimation = new YAnimation();
        animation.setInterpolator(new DecelerateInterpolator());
        animation.addUpdateListener(yAnimation);
        animation.addListener(yAnimation);
        animation.setDuration(DURATION);
        animation.start();
    }

    public void startZoomAnimation(float centerX, float centerY, float zoomFrom, float zoomTo) {
        Log.d(TAG, "startZoomAnimation() called with: centerX = [" + centerX + "], centerY = [" + centerY + "], zoomFrom = [" + zoomFrom + "], zoomTo = [" + zoomTo + "]");
        stopAll();
        animation = ValueAnimator.ofFloat(zoomFrom, zoomTo);
        animation.setInterpolator(new DecelerateInterpolator());
        ZoomAnimation zoomAnim = new ZoomAnimation(centerX, centerY);
        animation.addUpdateListener(zoomAnim);
        animation.addListener(zoomAnim);
        animation.setDuration(DURATION);
        animation.start();
    }

    public void startFlingAnimation(int startX, int startY, int velocityX, int velocityY, int minX, int maxX, int minY, int maxY) {
        if (!pdfView.isSwipeVertical() && pdfView.getZoom() == 1) {
            return;
        }
        Log.d(TAG, "startFlingAnimation() called with: startX = [" + startX + "], startY = [" + startY + "], velocityX = [" + velocityX + "], velocityY = [" + velocityY + "], minX = [" + minX + "], maxX = [" + maxX + "], minY = [" + minY + "], maxY = [" + maxY + "]");
        stopAll();
        flinging = true;
        scroller.fling(startX, startY, velocityX, velocityY, minX, maxX, minY, maxY);
        if (flingChangeCallback != null) {
            flingChangeCallback.onFlingChanged(true);
        }
    }

    public void startPageFlingAnimation(float targetOffset) {
        Log.d(TAG, "startPageFlingAnimation() called with: targetOffset = [" + targetOffset + "]");
        if (pdfView.isSwipeVertical()) {
            startYAnimation(pdfView.getCurrentYOffset(), targetOffset);
        } else {
            startXAnimation(pdfView.getCurrentXOffset(), targetOffset);
        }
        pageFlinging = true;
        if (flingChangeCallback != null) {
            flingChangeCallback.onFlingChanged(true);
        }
    }

    void computeFling() {
        boolean computeScrollOffset = scroller.computeScrollOffset();
        if (computeScrollOffset) {
            pdfView.moveTo(scroller.getCurrX(), scroller.getCurrY());
            pdfView.loadPageByOffset();
        } else if (flinging) { // fling finished
            flinging = false;
            pdfView.loadPages();
            hideHandle();
            pdfView.performPageSnap();
            if (flingChangeCallback != null) {
                flingChangeCallback.onFlingChanged(false);
            }
        }
    }

    public void stopAll() {
        Log.d(TAG, "stopAll() called");
        if (animation != null) {
            animation.cancel();
            animation = null;
        }
        stopFling();
    }

    public void stopFling() {
        Log.d(TAG, "stopFling() called");
        flinging = false;
        scroller.forceFinished(true);
    }

    public void setFlingChangeCallback(FlingChangeCallback callback) {
        flingChangeCallback = callback;
    }

    public boolean isFlinging() {
        return flinging || pageFlinging;
    }

    class XAnimation extends AnimatorListenerAdapter implements AnimatorUpdateListener {

        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            float offset = (Float) animation.getAnimatedValue();
            pdfView.moveTo(offset, pdfView.getCurrentYOffset());
            pdfView.loadPageByOffset();
        }

        @Override
        public void onAnimationCancel(Animator animation) {
            pdfView.loadPages();
            pageFlinging = false;
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            pdfView.loadPages();
            pageFlinging = false;
        }
    }

    class YAnimation extends AnimatorListenerAdapter implements AnimatorUpdateListener {

        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            float offset = (Float) animation.getAnimatedValue();
            pdfView.moveTo(pdfView.getCurrentXOffset(), offset);
            pdfView.loadPageByOffset();
        }

        @Override
        public void onAnimationCancel(Animator animation) {
            pdfView.loadPages();
            pageFlinging = false;
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            pdfView.loadPages();
            pageFlinging = false;
        }
    }

    interface FlingChangeCallback {
        void onFlingChanged(boolean isFling);
    }

    class ZoomAnimation implements AnimatorUpdateListener, AnimatorListener {

        private final float centerX;
        private final float centerY;

        public ZoomAnimation(float centerX, float centerY) {
            this.centerX = centerX;
            this.centerY = centerY;
        }

        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            float zoom = (Float) animation.getAnimatedValue();
            pdfView.zoomCenteredTo(zoom, new PointF(centerX, centerY));
        }

        @Override
        public void onAnimationCancel(Animator animation) {
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            pdfView.loadPages();
            hideHandle();
            pdfView.performPageSnap();
        }

        @Override
        public void onAnimationRepeat(Animator animation) {
        }

        @Override
        public void onAnimationStart(Animator animation) {
        }

    }

    private void hideHandle() {
        if (pdfView.getScrollHandle() != null) {
            pdfView.getScrollHandle().hideDelayed();
        }
    }

}
