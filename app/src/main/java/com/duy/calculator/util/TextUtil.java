package com.duy.calculator.util;

import android.widget.TextView;

import com.duy.math.Constants;
import com.duy.math.EquationFormatter;
import com.duy.math.Solver;

/**
 * For formatting text in the display
 */
public class TextUtil {
    public static String getCleanText(TextView textView, Solver solver) {
        return removeFormatting(solver, textView.getText().toString());
    }

    public static String formatText(String input, EquationFormatter equationFormatter, Solver solver) {
        if (solver != null) {
            // Add grouping, and then split on the selection handle
            // which is saved as a unique char
            input = equationFormatter.addComas(solver, input, -1);
        }

        return equationFormatter.insertSupScripts(input);
    }

    protected static String removeFormatting(Solver solver, String input) {
        input = input.replace(Constants.POWER_PLACEHOLDER, Constants.POWER);
        if (solver != null) {
            input = input.replace(String.valueOf(solver.getBaseModule().getSeparator()), "");
        }
        return input;
    }

    public static int countOccurrences(String haystack, char needle) {
        int count = 0;
        for (int i = 0; i < haystack.length(); i++) {
            if (haystack.charAt(i) == needle) {
                count++;
            }
        }
        return count;
    }
}
