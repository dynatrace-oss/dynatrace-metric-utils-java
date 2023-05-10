package com.dynatrace.metric.util.validation;

import com.dynatrace.metric.util.MetadataConstants;

public final class UnitValidator {

	private UnitValidator() {
	}

	/**
	 * Checks if the provided unit is valid according to the spec.
	 *
	 * @param unit The unit.
	 * @return True if the unit is valid, false otherwise.
	 */
	public static boolean isValidUnit(String unit) {
		final int length = unit.length();

		if (length > MetadataConstants.Limits.MAX_UNIT_LENGTH) {
			return false;
		}

		State state = State.START;

		for (int offset = 0; offset < length; ) {
			final int codePoint = unit.codePointAt(offset);
            // TODO: Validate code points to ensure they are allowed (don't normalize anything)
			switch (state) {
				case START:
					if (!isValidFirstSectionStartCharacter(codePoint)) {
						return false;
					}
					if (codePoint == CodePoints.CURLY_BRACKET_OPEN) {
						state = State.CURLY_BRACKET_STRING_INTERMEDIATE;
					} else {
						state = State.SECTION;
					}
					break;
				case CURLY_BRACKET_STRING_INTERMEDIATE:
					boolean isLastChar = offset + Character.charCount(codePoint) == length;
					if (isLastChar && codePoint != CodePoints.CURLY_BRACKET_CLOSE) {
						// if the last char is not a '}' the unit is invalid
						return false;
					}
					if (!isLastChar && isNotLetter(codePoint)) {
						return false;
					}
					break;
				case SECTION:
					if (isNotLetter(codePoint)) {
						return false;
					}
					break;
			}
			offset += Character.charCount(codePoint);
		}
		return true;
	}

	public enum State {
		START,
		CURLY_BRACKET_STRING_INTERMEDIATE,
		SECTION,
	}

	private static boolean isValidFirstSectionStartCharacter(int codePoint) {
		return (codePoint >= CodePoints.A_LOWERCASE && codePoint <= CodePoints.Z_LOWERCASE)
				|| (codePoint >= CodePoints.A_UPPERCASE && codePoint <= CodePoints.Z_UPPERCASE);
	}

	private static boolean isNotLetter(int codePoint) {
		return (codePoint < CodePoints.A_LOWERCASE || codePoint > CodePoints.Z_LOWERCASE)
				&& (codePoint < CodePoints.A_UPPERCASE || codePoint > CodePoints.Z_UPPERCASE);
	}
}
