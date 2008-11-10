/*
 *  Copyright 2007, Plutext Pty Ltd.
 *   
 *  This file is part of Docx4all.

    Docx4all is free software: you can redistribute it and/or modify
    it under the terms of version 3 of the GNU General Public License 
    as published by the Free Software Foundation.

    Docx4all is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License   
    along with Docx4all.  If not, see <http://www.gnu.org/licenses/>.
    
 */
package org.docx4all.swing.text;

import java.io.Serializable;

import javax.swing.SizeRequirements;

/**
 * This file is based on javax.swing.text.html.CSS class downloaded from 
 * <a href="http://download.java.net/openjdk/jdk7/">OpenJDK Source Releases</a>
 */
public class CSS implements Serializable {
    /**
     * Calculate the requirements needed to tile the requirements
     * given by the iterator that would be tiled.  The calculation
     * takes into consideration margin and border spacing.
     */
    static SizeRequirements calculateTiledRequirements(LayoutIterator iter, SizeRequirements r) {
        long minimum = 0;
        long maximum = 0;
        long preferred = 0;
        int lastMargin = 0;
        int totalSpacing = 0;
        int n = iter.getCount();
        for (int i = 0; i < n; i++) {
            iter.setIndex(i);
            int margin0 = lastMargin;
            int margin1 = (int) iter.getLeadingCollapseSpan();
            totalSpacing += Math.max(margin0, margin1);
            preferred += (int) iter.getPreferredSpan(0);
            minimum += iter.getMinimumSpan(0);
            maximum += iter.getMaximumSpan(0);

            lastMargin = (int) iter.getTrailingCollapseSpan();
        }
        totalSpacing += lastMargin;
        totalSpacing += 2 * iter.getBorderWidth();

        // adjust for the spacing area
        minimum += totalSpacing;
        preferred += totalSpacing;
        maximum += totalSpacing;

        // set return value
        if (r == null) {
            r = new SizeRequirements();
        }
        r.minimum = (minimum > Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int)minimum;
        r.preferred = (preferred > Integer.MAX_VALUE) ? Integer.MAX_VALUE :(int) preferred;
        r.maximum = (maximum > Integer.MAX_VALUE) ? Integer.MAX_VALUE :(int) maximum;
        return r;
    }

    /**
     * Calculate a tiled layout for the given iterator.
     * This should be done collapsing the neighboring
     * margins to be a total of the maximum of the two
     * neighboring margin areas as described in the CSS spec.
     */
    static void calculateTiledLayout(LayoutIterator iter, int targetSpan) {

        /*
         * first pass, calculate the preferred sizes, adjustments needed because
         * of margin collapsing, and the flexibility to adjust the sizes.
         */
        long preferred = 0;
        long currentPreferred;
        int lastMargin = 0;
        int totalSpacing = 0;
        int n = iter.getCount();
        int adjustmentWeightsCount = LayoutIterator.WorstAdjustmentWeight + 1;
        //max gain we can get adjusting elements with adjustmentWeight <= i
        long gain[] = new long[adjustmentWeightsCount];
        //max loss we can get adjusting elements with adjustmentWeight <= i
        long loss[] = new long[adjustmentWeightsCount];

        for (int i = 0; i < adjustmentWeightsCount; i++) {
            gain[i] = loss[i] = 0;
        }
        for (int i = 0; i < n; i++) {
            iter.setIndex(i);
            int margin0 = lastMargin;
            int margin1 = (int) iter.getLeadingCollapseSpan();

            iter.setOffset(Math.max(margin0, margin1));
            totalSpacing += iter.getOffset();

            currentPreferred = (long)iter.getPreferredSpan(targetSpan);
            iter.setSpan((int) currentPreferred);
            preferred += currentPreferred;
            gain[iter.getAdjustmentWeight()] +=
                (long)iter.getMaximumSpan(targetSpan) - currentPreferred;
            loss[iter.getAdjustmentWeight()] +=
                currentPreferred - (long)iter.getMinimumSpan(targetSpan);
            lastMargin = (int) iter.getTrailingCollapseSpan();
        }
        totalSpacing += lastMargin;
        totalSpacing += 2 * iter.getBorderWidth();

        for (int i = 1; i < adjustmentWeightsCount; i++) {
            gain[i] += gain[i - 1];
            loss[i] += loss[i - 1];
        }

        /*
         * Second pass, expand or contract by as much as possible to reach
         * the target span.  This takes the margin collapsing into account
         * prior to adjusting the span.
         */

        // determine the adjustment to be made
        int allocated = targetSpan - totalSpacing;
        long desiredAdjustment = allocated - preferred;
        long adjustmentsArray[] = (desiredAdjustment > 0) ? gain : loss;
        desiredAdjustment = Math.abs(desiredAdjustment);
        int adjustmentLevel = 0;
        for (;adjustmentLevel <= LayoutIterator.WorstAdjustmentWeight;
             adjustmentLevel++) {
            // adjustmentsArray[] is sorted. I do not bother about
            // binary search though
            if (adjustmentsArray[adjustmentLevel] >= desiredAdjustment) {
                break;
            }
        }
        float adjustmentFactor = 0.0f;
        if (adjustmentLevel <= LayoutIterator.WorstAdjustmentWeight) {
            desiredAdjustment -= (adjustmentLevel > 0) ?
                adjustmentsArray[adjustmentLevel - 1] : 0;
            if (desiredAdjustment != 0) {
                float maximumAdjustment =
                    adjustmentsArray[adjustmentLevel] -
                    ((adjustmentLevel > 0) ?
                     adjustmentsArray[adjustmentLevel - 1] : 0
                     );
                adjustmentFactor = desiredAdjustment / maximumAdjustment;
            }
        }
        // make the adjustments
        int totalOffset = (int)iter.getBorderWidth();
        for (int i = 0; i < n; i++) {
            iter.setIndex(i);
            iter.setOffset( iter.getOffset() + totalOffset);
            if (iter.getAdjustmentWeight() < adjustmentLevel) {
                iter.setSpan((int)
                             ((allocated > preferred) ?
                              Math.floor(iter.getMaximumSpan(targetSpan)) :
                              Math.ceil(iter.getMinimumSpan(targetSpan))
                              )
                             );
            } else if (iter.getAdjustmentWeight() == adjustmentLevel) {
                int availableSpan = (allocated > preferred) ?
                    (int) iter.getMaximumSpan(targetSpan) - iter.getSpan() :
                    iter.getSpan() - (int) iter.getMinimumSpan(targetSpan);
                int adj = (int)Math.floor(adjustmentFactor * availableSpan);
                iter.setSpan(iter.getSpan() +
                             ((allocated > preferred) ? adj : -adj));
            }
            totalOffset = (int) Math.min((long) iter.getOffset() +
                                         (long) iter.getSpan(),
                                         Integer.MAX_VALUE);
        }

        // while rounding we could lose several pixels.
        int roundError = targetSpan - totalOffset -
            (int)iter.getTrailingCollapseSpan() -
            (int)iter.getBorderWidth();
        int adj = (roundError > 0) ? 1 : -1;
        roundError *= adj;

        boolean canAdjust = true;
        while (roundError > 0 && canAdjust) {
            // check for infinite loop
            canAdjust = false;
            int offsetAdjust = 0;
            // try to distribute roundError. one pixel per cell
            for (int i = 0; i < n; i++) {
                iter.setIndex(i);
                iter.setOffset(iter.getOffset() + offsetAdjust);
                int curSpan = iter.getSpan();
                if (roundError > 0) {
                    int boundGap = (adj > 0) ?
                        (int)Math.floor(iter.getMaximumSpan(targetSpan)) - curSpan :
                        curSpan - (int)Math.ceil(iter.getMinimumSpan(targetSpan));
                    if (boundGap >= 1) {
                        canAdjust = true;
                        iter.setSpan(curSpan + adj);
                        offsetAdjust += adj;
                        roundError--;
                    }
                }
            }
        }
    }

    /**
     * An iterator to express the requirements to use when computing
     * layout.
     */
    interface LayoutIterator {

        void setOffset(int offs);

        int getOffset();

        void setSpan(int span);

        int getSpan();

        int getCount();

        void setIndex(int i);

        float getMinimumSpan(float parentSpan);

        float getPreferredSpan(float parentSpan);

        float getMaximumSpan(float parentSpan);

        int getAdjustmentWeight(); //0 is the best weight WorstAdjustmentWeight is a worst one

        //float getAlignment();

        float getBorderWidth();

        float getLeadingCollapseSpan();

        float getTrailingCollapseSpan();
        public static final int WorstAdjustmentWeight = 2;
    }

}
