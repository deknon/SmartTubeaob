package com.liskovsoft.smartyoutubetv2.common.app.models.playback.controllers;

import com.liskovsoft.mediaserviceinterfaces.data.HeatMarker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Picks the "Most Replayed" ranges to draw on the seek bar from YouTube's heatmap.
 */
final class MostReplayedHelper {
    private static final int MAX_RANGES = 6;
    // Weaker local maxima are noise on a flat heatmap, not peaks
    private static final float MIN_PEAK_INTENSITY = 0.25f;

    private MostReplayedHelper() {
    }

    /**
     * YouTube's own "Most replayed" ranges come first, then the strongest local maxima of the heatmap
     * fill up to {@link #MAX_RANGES}. The first bucket is never a candidate: everyone starts watching there.
     * Each peak spans one bucket either side, the same width YouTube uses for its own ranges.
     *
     * @return {startMs, endMs} pairs, empty when the video has no heatmap
     */
    static List<long[]> findRanges(List<HeatMarker> heatMarkers, List<HeatMarker> mostReplayedMarkers) {
        List<long[]> ranges = new ArrayList<>();

        if (heatMarkers == null || heatMarkers.isEmpty()) {
            return ranges;
        }

        if (mostReplayedMarkers != null) {
            for (HeatMarker marker : mostReplayedMarkers) {
                if (ranges.size() < MAX_RANGES) {
                    ranges.add(new long[] {marker.getStartMs(), marker.getStartMs() + marker.getDurationMs()});
                }
            }
        }

        List<HeatMarker> buckets = new ArrayList<>(heatMarkers);
        Collections.sort(buckets, (a, b) -> Long.compare(a.getStartMs(), b.getStartMs()));

        List<Integer> peakIndexes = new ArrayList<>();
        for (int i = 1; i < buckets.size(); i++) {
            float intensity = buckets.get(i).getIntensity();
            boolean isLocalMax = intensity >= buckets.get(i - 1).getIntensity()
                    && (i == buckets.size() - 1 || intensity >= buckets.get(i + 1).getIntensity());
            if (isLocalMax && intensity >= MIN_PEAK_INTENSITY) {
                peakIndexes.add(i);
            }
        }
        Collections.sort(peakIndexes, (a, b) -> Float.compare(buckets.get(b).getIntensity(), buckets.get(a).getIntensity()));

        for (int peakIndex : peakIndexes) {
            if (ranges.size() >= MAX_RANGES) {
                break;
            }

            int first = Math.max(peakIndex - 1, 0);
            int last = Math.min(peakIndex + 1, buckets.size() - 1);

            long[] range = {buckets.get(first).getStartMs(), buckets.get(last).getStartMs() + buckets.get(last).getDurationMs()};

            if (!touchesAny(range, ranges)) {
                ranges.add(range);
            }
        }

        return ranges;
    }

    /**
     * @return bucket intensities ordered by time, null when the video has no heatmap
     */
    static float[] toIntensities(List<HeatMarker> heatMarkers) {
        if (heatMarkers == null || heatMarkers.isEmpty()) {
            return null;
        }

        List<HeatMarker> buckets = new ArrayList<>(heatMarkers);
        Collections.sort(buckets, (a, b) -> Long.compare(a.getStartMs(), b.getStartMs()));

        float[] intensities = new float[buckets.size()];
        for (int i = 0; i < intensities.length; i++) {
            intensities[i] = buckets.get(i).getIntensity();
        }

        return intensities;
    }

    static long getTotalMs(List<HeatMarker> heatMarkers) {
        long totalMs = 0;

        for (HeatMarker marker : heatMarkers) {
            totalMs = Math.max(totalMs, marker.getStartMs() + marker.getDurationMs());
        }

        return totalMs;
    }

    private static boolean touchesAny(long[] range, List<long[]> ranges) {
        for (long[] other : ranges) {
            if (range[0] <= other[1] && other[0] <= range[1]) {
                return true;
            }
        }

        return false;
    }
}
