/*
 * $Id$ $Revision:
 * 1.5 $ $Date$
 * 
 * ==============================================================================
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package wicket;

import java.util.Iterator;
import java.util.Locale;
import java.util.MissingResourceException;

import wicket.model.IModel;
import wicket.resource.IStringResourceLoader;
import wicket.util.string.interpolator.OgnlVariableInterpolator;

/**
 * Utility class that encapsulates all of the localization related functionality
 * in a way that is can be accessed by all areas of the framework in a
 * consistent way. A singleton instance of this class is available via the
 * <code>Application</code> object.
 * 
 * @author Chris Turner
 * @see Application#getLocalizer()
 */
public class Localizer
{
	/** The application and its settings to use to control the utils. */
	private Application application;

	/**
	 * Create the utils instance class backed by the configuration information
	 * contained within the supplied settings object.
	 * 
	 * @param application
	 *            The application to localize for
	 */
	public Localizer(final Application application)
	{
		this.application = application;
	}

	/**
	 * @param key
	 *            The key to obtain the resource for
	 * @param component
	 *            The component to get the resource for (optional)
	 * @return The string resource
	 * @throws MissingResourceException
	 *             If resource not found and configuration dictates that
	 *             exception should be thrown
	 * @see #getString(String, Component, String)
	 */
	public String getString(final String key, final Component component)
			throws MissingResourceException
	{
		return getString(key, component, null, component.getLocale(), component.getStyle(), null);
	}

	/**
	 * @param key
	 *            The key to obtain the resource for
	 * @param component
	 *            The component to get the resource for (optional)
	 * @param model
	 *            The model to use for OGNL substitutions in the strings
	 *            (optional)
	 * @return The string resource
	 * @throws MissingResourceException
	 *             If resource not found and configuration dictates that
	 *             exception should be thrown
	 * @see #getString(String, Component, IModel, String)
	 */
	public String getString(final String key, final Component component, final IModel model)
			throws MissingResourceException
	{
		return getString(key, component, model, component.getLocale(), component.getStyle(), null);
	}

	/**
	 * Get the localized string using all of the supplied parameters. This
	 * method is left public to allow developers full control over string
	 * resource loading. However, it is recommended that one of the other
	 * convenience methods in the class are used as they handle all of the work
	 * related to obtaining the current user locale and style information.
	 * 
	 * @param key
	 *            The key to obtain the resource for
	 * @param component
	 *            The component to get the resource for (optional)
	 * @param model
	 *            The model to use for OGNL substitutions in the strings
	 *            (optional)
	 * @param locale
	 *            The locale to get the resource for (optional)
	 * @param style
	 *            The style to get the resource for (optional) (see {@link wicket.Session})
	 * @param defaultValue
	 *            The default value (optional)
	 * @return The string resource
	 * @throws MissingResourceException
	 *             If resource not found and configuration dictates that
	 *             exception should be thrown
	 */
	public String getString(final String key, final Component component, final IModel model,
			final Locale locale, final String style, final String defaultValue)
			throws MissingResourceException
	{
		// The string to return
		String string = null;

		// Get application settings
		final ApplicationSettings settings = application.getSettings();

		// Search each loader in turn and return the string if it is found
		for (final Iterator iterator = settings.getStringResourceLoaders().iterator(); iterator
				.hasNext();)
		{
			IStringResourceLoader loader = (IStringResourceLoader)iterator.next();
			string = loader.loadStringResource(component, key, locale, style);
			if (string != null)
			{
				return substituteOgnl(component, string, model);
			}
		}

		// Resource not found, so handle missing resources based on application
		// configuration
		if (settings.getUseDefaultOnMissingResource() && defaultValue != null)
		{
			return defaultValue;
		}

		if (settings.getThrowExceptionOnMissingResource())
		{
			throw new MissingResourceException("Unable to find resource: " + key, getClass()
					.getName(), key);
		}
		else
		{
			return "[Warning: String resource for '" + key + "' not found]";
		}
	}

	/**
	 * Get the localized string for the given component. The component may be
	 * null in which case the component string resource loader will not be used.
	 * It the component is not null then it must be a component that has already
	 * been added to a page, either directly or via a parent container. The
	 * locale and style are obtained from the current user session. If the model
	 * is not null then OGNL substitution will be carried out on the string,
	 * using the object contained within the model.
	 * 
	 * @param key
	 *            The key to obtain the resource for
	 * @param component
	 *            The component to get the resource for (optional)
	 * @param model
	 *            The model to use for OGNL substitutions in the strings
	 *            (optional)
	 * @param defaultValue
	 *            The default value (optional)
	 * @return The string resource
	 * @throws MissingResourceException
	 *             If resource not found and configuration dictates that
	 *             exception should be thrown
	 */
	public String getString(final String key, final Component component, final IModel model,
			final String defaultValue) throws MissingResourceException
	{
		return getString(key, component, model, component.getLocale(), component.getStyle(),
				defaultValue);
	}

	/**
	 * Get the localized string for the given component. The component may be
	 * null in which case the component string resource loader will not be used.
	 * It the component is not null then it must be a component that has already
	 * been added to a page, either directly or via a parent container. The
	 * locale and style are obtained from the current user session.
	 * 
	 * @param key
	 *            The key to obtain the resource for
	 * @param component
	 *            The component to get the resource for (optional)
	 * @param defaultValue
	 *            The default value (optional)
	 * @return The string resource
	 * @throws MissingResourceException
	 *             If resource not found and configuration dictates that
	 *             exception should be thrown
	 */
	public String getString(final String key, final Component component, final String defaultValue)
			throws MissingResourceException
	{
		return getString(key, component, null, component.getLocale(), component.getStyle(),
				defaultValue);
	}

	/**
	 * Helper method to handle OGNL variable substituion in strings.
	 * 
	 * @param component
	 *            The component requesting a model value
	 * @param string
	 *            The string to substitute into
	 * @param model
	 *            The model
	 * @return The resulting string
	 */
	private String substituteOgnl(final Component component, final String string, final IModel model)
	{
		if (string != null && model != null)
		{
			return OgnlVariableInterpolator.interpolate(string, model.getObject(component));
		}
		return string;
	}
}