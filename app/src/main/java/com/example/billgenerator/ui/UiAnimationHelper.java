package com.example.billgenerator.ui;

import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.RecyclerView;

import com.example.billgenerator.R;
import com.google.android.material.button.MaterialButton;

/**
 * View-system helpers that mirror Compose AnimatedVisibility-style transitions.
 */
public final class UiAnimationHelper {

    private UiAnimationHelper() {
    }

    public static void setupRecyclerViewAnimations(RecyclerView recyclerView) {
        if (recyclerView == null) {
            return;
        }
        DefaultItemAnimator animator = new DefaultItemAnimator();
        animator.setAddDuration(260L);
        animator.setRemoveDuration(200L);
        animator.setMoveDuration(220L);
        animator.setChangeDuration(180L);
        recyclerView.setItemAnimator(animator);
        LayoutAnimationController layoutAnimation = AnimationUtils.loadLayoutAnimation(
                recyclerView.getContext(),
                R.anim.layout_fade_slide_up
        );
        if (layoutAnimation != null) {
            recyclerView.setLayoutAnimation(layoutAnimation);
        }
    }

    public static void applyDialogAnimations(@Nullable android.app.Dialog dialog) {
        if (dialog == null || dialog.getWindow() == null) {
            return;
        }
        dialog.getWindow().setWindowAnimations(R.style.Animation_Dialog);
    }

    public static void configureEmptyState(
            @Nullable View emptyStateRoot,
            @DrawableRes int iconRes,
            @Nullable CharSequence title,
            @Nullable CharSequence subtitle,
            @Nullable CharSequence actionText,
            @Nullable Runnable actionListener
    ) {
        if (emptyStateRoot == null) {
            return;
        }

        ImageView icon = emptyStateRoot.findViewById(R.id.empty_state_icon);
        TextView titleView = emptyStateRoot.findViewById(R.id.empty_state_title);
        TextView subtitleView = emptyStateRoot.findViewById(R.id.empty_state_subtitle);
        MaterialButton actionButton = emptyStateRoot.findViewById(R.id.empty_state_action);

        if (icon != null) {
            icon.setImageResource(iconRes);
        }
        if (titleView != null) {
            titleView.setText(title);
        }
        if (subtitleView != null) {
            subtitleView.setText(subtitle);
        }
        if (actionButton != null) {
            if (actionText == null || actionText.length() == 0) {
                actionButton.setVisibility(View.GONE);
                actionButton.setOnClickListener(null);
            } else {
                actionButton.setVisibility(View.VISIBLE);
                actionButton.setText(actionText);
                actionButton.setOnClickListener(v -> {
                    if (actionListener != null) {
                        actionListener.run();
                    }
                });
            }
        }
    }

    public static void setVisible(@Nullable View view, boolean visible) {
        setVisible(view, visible, true);
    }

    public static void setVisible(@Nullable View view, boolean visible, boolean animate) {
        if (view == null) {
            return;
        }

        if (!animate) {
            view.clearAnimation();
            view.setVisibility(visible ? View.VISIBLE : View.GONE);
            return;
        }

        if (visible) {
            if (view.getVisibility() == View.VISIBLE) {
                return;
            }
            view.setVisibility(View.VISIBLE);
            Animation animation = AnimationUtils.loadAnimation(view.getContext(), R.anim.slide_up_fade_in);
            if (animation != null) {
                view.startAnimation(animation);
            }
        } else {
            if (view.getVisibility() != View.VISIBLE) {
                view.setVisibility(View.GONE);
                return;
            }
            Animation animation = AnimationUtils.loadAnimation(view.getContext(), R.anim.fade_out);
            if (animation == null) {
                view.setVisibility(View.GONE);
                return;
            }
            animation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    view.setVisibility(View.GONE);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });
            view.startAnimation(animation);
        }
    }
}
