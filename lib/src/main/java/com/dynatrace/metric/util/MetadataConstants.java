package com.dynatrace.metric.util;

/**
 * Constants related to metadata line creation, serialization and normalization.
 */
public final class MetadataConstants {

	private MetadataConstants() {
	}

	/**
	 * Constants for metadata length limits according to .
	 */
	public static final class Limits {

		private Limits() {
		}

		public static final int MAX_DESCRIPTION_LENGTH = 65535;
		public static final int MAX_UNIT_LENGTH = 63;
	}

	/**
	 * Constants for payload types of a metadata line.
	 */
	public static final class Payload {

		private Payload() {
		}

		public static final String TYPE_COUNT = "count";
		public static final String TYPE_GAUGE = "gauge";
	}

	/**
	 * Constants for Dynatrace-reserved metadata dimension keys
	 */
	public static final class Dimensions {

		private Dimensions() {
		}

		public static final String DESCRIPTION_KEY = "dt.meta.description";
		public static final String UNIT_KEY = "dt.meta.unit";
	}

	/**
	 * Constants for Dynatrace and OpenTelemetry units
	 */
	public static final class Units {

		private Units() {
		}

		/**
		 * The OTel default dimensionless unit
		 */
		public static final String OTEL_UTILIZATION_DEFAULT_UNIT = "1";

		/**
		 * The OTel percent unit symbol
		 */
		public static final String OTEL_PERCENT_UNIT_SYMBOL = "%";

		/**
		 * The Dynatrace pre-defined unit id for Percent. See also DtUnits#EXP_UNIT_TO_UNIT
		 */
		public static final String DT_PERCENT = "Percent";
	}

	/**
	 * Constants for errors and warning messages encountered during processing.
	 * These messages are sent back to clients of the API.
	 */
	public static final class ValidationMessages {

		private ValidationMessages() {
		}

		// errors
		public static final String METADATA_DROPPED_MESSAGE = "Metadata for metric '%s' dropped";

		// warnings
		public static final String UNIT_DROPPED_MESSAGE = "Unit '%s' for metric '%s' dropped";
		public static final String DESCRIPTION_NORMALIZED_MESSAGE = "Description for metric '%s' normalized from '%s' to '%s'";
	}
}
