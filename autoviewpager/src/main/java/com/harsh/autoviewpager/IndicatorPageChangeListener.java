package com.harsh.autoviewpager;

public interface IndicatorPageChangeListener {
    void onIndicatorProgress(int selectingPosition, float progress);

    void onIndicatorPageChange(int newIndicatorPosition);
}