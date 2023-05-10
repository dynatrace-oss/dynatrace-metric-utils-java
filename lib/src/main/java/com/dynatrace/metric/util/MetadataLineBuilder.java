package com.dynatrace.metric.util;

/**
 * A builder interface that allows constructing  metadata lines.
 */
public interface MetadataLineBuilder {

	interface MetricKeyStep {

		/**
		 * Sets the metric key on the metadata line. The key will be normalized according to the  spec.
		 *
		 * @param key The metric key.
		 * @return A {@link TypeStep} that can be used to set the unit and description.
		 */
		TypeStep metricKey(String key);
	}

	interface TypeStep {

		/**
		 * Sets the {@code gauge} payload type on the metadata line.
		 *
		 * @return A {@link DescriptionStep} that can be used to set the
		 * {@code dt.meta.description} dimension on the metadata line.
		 */
		DescriptionStep gauge();

		/**
		 * Sets the {@code count} payload type on the metadata line.
		 *
		 * @return A {@link DescriptionStep} that can be used to set the
		 * {@code dt.meta.description} dimension on the metadata line.
		 */
		DescriptionStep counter();
	}

	interface DescriptionStep {

		/**
		 * Sets the {@code dt.meta.description} dimension on the metadata line.
		 *
		 * @param description A short description of the metric.
		 * @return A {@link UnitStep} that can be used to build the  metadata line
		 * or set the {@code dt.meta.unit} dimension on the metadata line.
		 */
		UnitStep description(String description);

		/**
		 * Skips the {@code dt.meta.description} dimension.
		 *
		 * @return A {@link UnitStep} that can be used to build the  metadata line
		 * or set the {@code dt.meta.unit} dimension on the metadata line.
		 */
		UnitStep noDescription();
	}

	interface UnitStep extends BuildStep {

		/**
		 * Sets the {@code dt.meta.unit} dimension on the metadata line.
		 *
		 * @param unit The unit of the metric.
		 * @return A {@link BuildStep} that can be used to build the  metadata line.
		 */
		BuildStep unit(String unit);

		/**
		 * Skips the {@code dt.meta.unit} dimension.
		 *
		 * @return A {@link BuildStep} that can be used to build the  metadata line.
		 */
		BuildStep noUnit();
	}

	interface BuildStep {

		/**
		 * Gets the normalized  metadata line.
		 *
		 * @return A {@link String} or null in case the line is invalid.
		 */
    String build();
	}
}
