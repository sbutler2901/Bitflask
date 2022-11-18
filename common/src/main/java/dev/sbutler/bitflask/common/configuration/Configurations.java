package dev.sbutler.bitflask.common.configuration;

/**
 * A grouping of runtime {@link Configuration}s.
 *
 * <p>Configurations can be set via command line flags or a property file. Otherwise, default
 * values will be set The priority order for defining the parameters is:
 * <ol>
 *   <li>command line flags</li>
 *   <li>property file</li>
 *   <li>hardcoded value</li>
 * </ol>
 *
 * <p>Note: an illegal parameter value will cause an
 * {@link dev.sbutler.bitflask.common.configuration.exceptions.IllegalConfigurationException} to be
 * thrown WITHOUT falling back to a lower priority parameter definition.
 */
public interface Configurations {

}
