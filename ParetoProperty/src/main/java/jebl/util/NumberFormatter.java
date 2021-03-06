/*
 * NumberFormatter.java
 *
 * (c) 2002-2006 JEBL Development Core Team
 *
 * This package may be distributed under the
 * Lesser Gnu Public Licence (LGPL)
 */

package jebl.util;

import java.text.DecimalFormat;

/**
 * An interface for a numerical column in a log.
 *
 * @version $Id: NumberFormatter.java 1003 2009-06-05 04:18:57Z stevensh $
 *
 * @author Andrew Rambaut
 * @author Alexei Drummond
 */

public class NumberFormatter {

	private int sf = -1;
	private int dp = -1;

	private double upperCutoff;
	private double[] cutoffTable;
	private DecimalFormat decimalFormat = new DecimalFormat();
	private DecimalFormat scientificFormat = null;

	public NumberFormatter(int sf) {
		setSignificantFigures(sf);
	}

	/**
	 * Set the number of significant figures to display when formatted.
	 * Setting this overrides the decimal places option.
	 */
	public void setSignificantFigures(int sf) {
		this.sf = sf;
		this.dp = -1;

		upperCutoff = Math.pow(10,sf-1);
		cutoffTable = new double[sf];
		long num = 10;
		for (int i = 0; i < cutoffTable.length; i++) {
			cutoffTable[i] = (double)num;
			num *= 10;
		}
		decimalFormat.setGroupingUsed(false);
		decimalFormat.setMinimumIntegerDigits(1);
		decimalFormat.setMaximumFractionDigits(sf-1);
		decimalFormat.setMinimumFractionDigits(sf-1);
		scientificFormat = new DecimalFormat(getPattern(sf));
	}

	/**
	 * Get the number of significant figures to display when formatted.
	 * Returns -1 if maximum s.f. are to be used.
	 */
	public int getSignificantFigures() { return sf; }

	/**
	 * Set the number of decimal places to display when formatted.
	 * Setting this overrides the significant figures option.
	 */
	public void setDecimalPlaces(int dp) {
		this.dp = dp;
		this.sf = -1;
	}

	/**
	 * Get the number of decimal places to display when formatted.
	 * Returns -1 if maximum d.p. are to be used.
	 */
	public int getDecimalPlaces() { return dp; }


	/**
	 * Returns a string containing the current value for this column with
	 * appropriate formatting.
	 *
	 * @return the formatted string.
	 */
	public String getFormattedValue(double value) {

		if (dp < 0 && sf < 0) {
			// return it at full precision
			return Double.toString(value);
		}

		int numFractionDigits = 0;

		if (dp < 0) {

			double absValue = Math.abs(value);

			if ((absValue > upperCutoff) || (absValue < 0.001)) {

				return scientificFormat.format(value);

			} else {

				numFractionDigits = getNumFractionDigits(value);
			}

		} else {

			numFractionDigits = dp;
		}

		decimalFormat.setMaximumFractionDigits(numFractionDigits);
		decimalFormat.setMinimumFractionDigits(numFractionDigits);
        String labelString = decimalFormat.format(value);
        //remove trailing zeros (we can't trust the formatter to do this)
        while(labelString.contains(".") && labelString.charAt(labelString.length()-1) == '0'){
            labelString = labelString.substring(0, labelString.length()-1);
        }
        if (labelString.endsWith(".")) {
            labelString = labelString.substring(0, labelString.length()-1);
        }

        return labelString;
	}

	private int getNumFractionDigits(double value) {
        value = Math.abs(value);
        if(value <1)
            return sf;
        for (int i = 0; i < cutoffTable.length; i++) {
			if (value < cutoffTable[i]) return sf-i-1;
		}
		return sf - 1;
	}

	private String getPattern(int sf) {
		String pattern = "0.";
		 for (int i =0; i < sf-1; i++) {
		 	pattern += "#";
		 }
		 pattern += "E0";
		return pattern;
	}

}
