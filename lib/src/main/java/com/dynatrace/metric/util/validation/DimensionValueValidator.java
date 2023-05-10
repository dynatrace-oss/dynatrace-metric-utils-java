package com.dynatrace.metric.util.validation;

/**
 * Offers validation methods for Dimension Values according to the specification.
 */
public final class DimensionValueValidator {

	private DimensionValueValidator() {

	}

	/**
	 * Checks if the codepoint of a dimension value should be escaped according to spec.
	 *
	 * @param codePoint The codepoint.
	 * @return True if the codepoint falls into the list of chars to escape according to spec, false otherwise.
	 */
	public static boolean shouldEscapeString(int codePoint) {
		return codePoint == CodePoints.COMMA || codePoint == CodePoints.EQUALS ||
				codePoint == CodePoints.BLANK || codePoint == CodePoints.BACKSLASH || codePoint == CodePoints.QUOTE;
	}

	/**
	 * Checks if the codepoint of a quoted dimension value should be escaped according to spec.
	 *
	 * @param codePoint The codepoint.
	 * @return True if the codepoint falls into the list of chars to escape according to spec, false otherwise.
	 */
	public static boolean shouldEscapeQuotedString(int codePoint) {
		return codePoint == CodePoints.QUOTE || codePoint == CodePoints.BACKSLASH;
	}

	/**
	 * Checks if the string is composed of only empty quotes.
	 *
	 * @param value The string value.
	 * @return True if the string value is two quotes (""""), false otherwise.
	 */
	public static boolean isEmptyQuoted(String value) {
		return "\"\"".equals(value);
	}

	/**
	 * Checks if the codepoint is an invalid codepoint (control chars)
	 *
	 * @param codePoint The codepoint.
	 * @return True if the codepoint falls into the list of invalid chars for dimension values, false otherwise.
	 */
	public static boolean isInvalidCodePoint(int codePoint) {
		switch (Character.getType(codePoint)) {
			case Character.UNASSIGNED:
				// support all emojis of unicode range "Supplemental Symbols and Pictographs"
				// see https://www.unicode.org/charts/PDF/Unicode-14.0/U140-1F900.pdf
				// See rationale in APM-360016
				if (codePoint >= CodePoints.UC_SUPPLEMENTAL_SYMBOLS_AND_PICTOGRAPHS_START
						&& codePoint <= CodePoints.UC_SUPPLEMENTAL_SYMBOLS_AND_PICTOGRAPHS_END) {
					return false;
				}
				// else fallthrough
			case Character.CONTROL:
			case Character.FORMAT:
			case Character.PRIVATE_USE:
			case Character.SURROGATE:
			case Character.LINE_SEPARATOR:
			case Character.PARAGRAPH_SEPARATOR:
				return true;
			default:
				return false;
		}
	}

	/**
	 * Calculates if by adding the next character, the dimension value length is still less or equal than the limits
	 * described by the spec. It considers the next character + any necessary escaping character or final quote in
	 * case of quoted dimension values.
	 *
	 * @param currentValueLength      The current length of the dimension value.
	 * @param codePoint               The next character to be added to the dimension value StringBuilder.
	 * @param isQuoted                Indicates whether the dimension value is a quoted value. This causes the calculation to consider  the
	 *                                length of the quote character.
	 * @param includeEscapeChar       Indicates whether the char to be added should be escaped. This causes the calculation to consider the
	 *                                length of the backslash character.
	 * @param maxDimensionValueLength The maximum value to compare too. E.g. The maximum value for dimensions
	 * @return True if the next character fits and can be safely added to the StringBuilder and False if not.
	 */
	public static boolean canAppendToValue(
			int currentValueLength,
			int codePoint,
			boolean isQuoted,
			boolean includeEscapeChar,
			int maxDimensionValueLength) {
		int sizeToAdd = currentValueLength + Character.charCount(codePoint);

		if (isQuoted) {
			sizeToAdd += Character.charCount(CodePoints.QUOTE);
		}

		if (includeEscapeChar) {
			sizeToAdd += Character.charCount(CodePoints.BACKSLASH);
		}
		return sizeToAdd <= maxDimensionValueLength;
	}

	/**
	 * Iterates through the string dimension value once to find out if it needs to be normalized.
	 *
	 * @param value                   The dimension string value
	 * @param maxDimensionValueLength The maximum value to compare too. E.g. The maximum value for dimensions
	 * @return True if it needs to be normalized (truncated, replaced or escaped) according to spec, false otherwise.
	 */
	public static boolean normalizationRequiredStringValue(String value, int maxDimensionValueLength) {
		final int length = value.length();

		if (length > maxDimensionValueLength) {
			return true;
		}

		for (int offset = 0; offset < length; ) {
			final int codePoint = value.codePointAt(offset);

			if (isInvalidCodePoint(codePoint) || shouldEscapeString(codePoint)) {
				return true;
			}

			offset += Character.charCount(codePoint);
		}
		return false;
	}

	/**
	 * Iterates through the quoted string dimension value once to find out if it needs to be normalized.
	 *
	 * @param value                   The quoted dimension string value
	 * @param maxDimensionValueLength The maximum value to compare too. E.g. The maximum value for dimensions
	 * @return True if it needs to be normalized (truncated, replaced or escaped) according to spec, false otherwise.
	 */
	public static boolean normalizationRequiredQuotedStringValue(String value, int maxDimensionValueLength) {
		final int length = value.length();

		if (length > maxDimensionValueLength) {
			return true;
		}

		final int quoteSize = Character.charCount(CodePoints.QUOTE);
		final int contentLength = length - quoteSize;

		// start from the first quote (thus skipping it) and stops before the last one
		for (int offset = quoteSize; offset < contentLength; ) {
			final int codePoint = value.codePointAt(offset);

			if (isInvalidCodePoint(codePoint) || shouldEscapeQuotedString(codePoint)) {
				return true;
			}

			offset += Character.charCount(codePoint);
		}
		return false;
	}

	public enum State {
		START,
		QUOTED_STRING_INTERMEDIATE,
		QUOTED_STRING,
	}
}
