package org.grouplens.reflens;

import static org.grouplens.reflens.params.meta.Parameters.getDefaultClass;
import static org.grouplens.reflens.params.meta.Parameters.getDefaultDouble;
import static org.grouplens.reflens.params.meta.Parameters.getDefaultInt;
import static org.grouplens.reflens.params.meta.Parameters.hasDefaultClass;
import static org.grouplens.reflens.params.meta.Parameters.hasDefaultDouble;
import static org.grouplens.reflens.params.meta.Parameters.hasDefaultInt;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

import org.grouplens.reflens.params.MinRating;
import org.grouplens.reflens.params.meta.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;

/**
 * Base class for modules providing recommender providers.  It provides general
 * infrastructure and helpers to make writing recommender configuration modules
 * easier.
 * 
 * <p>RefLens recommender modules should be built compositionally.  To create
 * the base module for a new recommender, extend this module and create the
 * modules containing the various parameter sets you need as fields.  Install
 * them using {@link #install(com.google.inject.Module)} in your
 * {@link #configure()} implementation.
 *
 * <p>This class provides the feature of automatically setting default values for
 * members.  For every field, including private fields, annotated with a parameter
 * annotation which contains a public static variable <var>DEFAULT_VALUE</var>,
 * the field is set to contain the value of <var>DEFAULT_VALUE</var>.
 * Parameter annotations are annotations which are themselves annotated with
 * {@link Parameter}.  See {@link MinRating} for an example parameter annotation.
 * 
 * @author Michael Ekstrand <ekstrand@cs.umn.edu>
 */
public abstract class RecommenderModuleComponent extends AbstractModule {
	/**
	 * A logger, initialized for the class.  Provided here so subclasses don't
	 * have to include the boilerplate of creating their own loggers.
	 */
	private Logger logger;
	private String name;

	public RecommenderModuleComponent() {
		setupLogger(null);
		initializeDefaultValues(getClass());
	}

	protected void initializeDefaultValues(Class<?> clazz) {
		Class<?> parent = clazz.getSuperclass();
		if (parent != null)
			initializeDefaultValues(parent);
		
		for (Field f: clazz.getDeclaredFields()) {
			f.setAccessible(true);
			Class<?> fType = f.getType();
			for (Annotation a: f.getAnnotations()) {
				Class<? extends Annotation> aType = a.annotationType();
				if (aType.isAnnotationPresent(Parameter.class)) {
					try {
						if (fType.equals(double.class) && hasDefaultDouble(aType)) {
							f.set(this, getDefaultDouble(aType));
						} else if (fType.equals(int.class) && hasDefaultInt(aType)) {
							f.set(this, getDefaultInt(aType));
						} else if (fType.equals(Class.class) && hasDefaultClass(aType)) {
							f.set(this, getDefaultClass(aType));
						}
					} catch (IllegalAccessException e) {
						logger.warn("Cannot set default value for {}", f.getName(), e);
					}
				}
			}
		}
	}
	
	protected void setLogger(Logger logger) {
		this.logger = logger;
	}

	protected Logger getLogger() {
		return logger;
	}

	/**
	 * Get the name of the recommender configuration.
	 * @see #setName(String)
	 * @return The recommender configuration name.
	 */
	public String getName() {
		return name;
	}
	
	private void setupLogger(String name) {
		if (name == null)
			name = "<unnamed>";
		logger = LoggerFactory.getLogger(this.getClass().getName() + ":" + name);
	}

	/**
	 * Set the recommender name.  Subclasses must override this method to pass
	 * the new name on to all delegates.
	 * 
	 * <p>The name has no particular meaning; it's just used to identify the
	 * recommender and its configuration in logging and other output.
	 * 
	 * @param name The recommender network name.
	 */
	public void setName(String name) {
		this.name = name;
		setupLogger(name);
	}
}