package com.samskivert.mustache.ant;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.filters.TokenFilter.ChainableReaderFilter;
import org.apache.tools.ant.types.RegularExpression;
import org.apache.tools.ant.util.regexp.Regexp;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;

/**
 * Provides <a href="http://mustache.github.com/">Mustache</a> templating
 * services for Ant.
 *
 * <p>
 * See README.md for the basic usage within an Ant build script.
 */

public class MustacheFilter extends ChainableReaderFilter {

	/**
	 * Whether to use Ant project properties in the data model
	 */
	private Boolean projectProperties = true;

	/**
	 * Prefix that project properties should have in order to be included in the
	 * data model. By default, no prefix is set, so all project properties may
	 * be included.
	 */
	private String prefix = null;

	/**
	 * If prefix is not null, removePrefix defines if this prefix should be
	 * removed when inserting keys in the data model. Project properties names
	 * remain unchanged
	 */
	private Boolean removePrefix = false;

	/**
	 * Whether to support list parsing in property names
	 */
	private Boolean supportLists = true;

	/**
	 * when list parsing is enabled, this defines the name of the id to be given
	 * to each element of the list
	 */
	private String listIdName = "__id__";

	/**
	 * the regular expression pattern used to parse list property names. This
	 * pattern should include three groups. The first group is the root key to
	 * access the list. The second group is the id of this item in the list. The
	 * third group is the sub-key to assign the value to.
	 */
	private String listRegex = "(.+)\\.(\\d+)\\.(.+)";
	// other example of regex: (.+)\[(\d+)\]\.(.+)

	/**
	 * A file name from which data model properties should be loaded from.
	 * Disabled by default.
	 */
	private String dataFile = null;

	// JMustache settings

	/**
	 * Default value to give to undefined and null keys. By default, undefined
	 * keys cause a failure. See
	 * {@link com.samskivert.mustache.Mustache.Compiler#defaultValue}.
	 */
	private String defaultValue = null;

	/**
	 * Whether sections should be strict. Default is false. See
	 * {@link com.samskivert.mustache.Mustache.Compiler#strictSections}.
	 */
	private boolean strictSections = false;

	/**
	 * Whether HTML output should be escaped. Default is false. See
	 * {@link com.samskivert.mustache.Mustache.Compiler#escapeHTML}.
	 */
	private boolean escapeHTML = false;

	public Boolean getProjectProperties() {
		return projectProperties;
	}

	public void setProjectProperties(Boolean projectProperties) {
		this.projectProperties = projectProperties;
	}

	public String getPrefix() {
		return prefix;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	public Boolean getRemovePrefix() {
		return removePrefix;
	}

	public void setRemovePrefix(Boolean removePrefix) {
		this.removePrefix = removePrefix;
	}

	public Boolean getSupportLists() {
		return supportLists;
	}

	public void setSupportLists(Boolean supportLists) {
		this.supportLists = supportLists;
	}

	public String getListIdName() {
		return listIdName;
	}

	public void setListIdName(String listIdName) {
		this.listIdName = listIdName;
	}

	public String getDataFile() {
		return dataFile;
	}

	public void setDataFile(String dataFile) {
		this.dataFile = dataFile;
	}

	public String getDefaultValue() {
		return defaultValue;
	}

	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	public boolean isStrictSections() {
		return strictSections;
	}

	public void setStrictSections(boolean strictSections) {
		this.strictSections = strictSections;
	}

	public boolean isEscapeHTML() {
		return escapeHTML;
	}

	public void setEscapeHTML(boolean escapeHTML) {
		this.escapeHTML = escapeHTML;
	}

	public void setListRegex(String listRegex) {
		this.listRegex = listRegex;
	}

	/**
	 * The main method to implement the filter. Compiles the input text and
	 * returns the output according to the defined data model
	 */
	public String filter(String text) {
		getProject().log("Mustache DataModel: " + getDataModel().toString(),
				Project.MSG_DEBUG);
		Template tmpl = Mustache.compiler().defaultValue(defaultValue)
				.strictSections(strictSections).escapeHTML(escapeHTML)
				.compile(text);
		return tmpl.execute(getDataModel());
	}

	private Map<String, Object> _dataModel = null;

	/**
	 * gets the data model, building it from project properties and/or data file
	 * if not already done
	 * 
	 * @return the data model Map
	 */
	private Map<String, Object> getDataModel() {
		if (_dataModel == null) {
			_dataModel = new HashMap<String, Object>();
			addProjectProperties();
			addSrcFile();
		}
		return _dataModel;
	}

	/**
	 * Add project properties in the data model
	 */
	private void addProjectProperties() {
		if (projectProperties) {
			addProperties(getProject().getProperties(), prefix, removePrefix);
		}
	}

	/**
	 * Add property file content to the data model.
	 */
	private void addSrcFile() {
		if (dataFile != null) {
			Properties props = new Properties();
			try {
				props.load(new FileInputStream(dataFile));
			} catch (IOException e) {
				throw new BuildException(e);
			}
			addProperties(props, null, false);
		}
	}

	/**
	 * Adds a set of properties to the datamodel
	 * 
	 * @param props
	 *            the properties to add
	 * @param prefix
	 *            the prefix that properties should have in order to be
	 *            considered. If null, all properties will be considered.
	 * @param removePrefix
	 *            whether the prefix should be removed in the data model key
	 *            name
	 */
	private void addProperties(Hashtable<?, ?> props, String prefix,
			Boolean removePrefix) {
		Iterator<?> it = props.keySet().iterator();
		while (it.hasNext()) {
			String key = (String) it.next();
			if (prefix == null || key.startsWith(prefix)) {
				Object value = computeValue(key, props.get(key));
				if (removePrefix && prefix != null) {
					key = key.substring(prefix.length());
				}
				add(_dataModel, key, value);
			}
		}
	}

	/**
	 * Adds a single key/value pair inside the specified Map. This map could be
	 * the datamodel itself or a sub-map in case of lists. The key will be
	 * parsed for a list item
	 * 
	 * @param map
	 *            the map to insert into
	 * @param key
	 *            the key to insert
	 * @param value
	 *            the value to insert
	 * @return the modified map
	 */
	private Map<String, Object> add(Map<String, Object> map, String key,
			Object value) {
		map.put(key, value);
		handleList(map, key, value);
		return map;
	}

	/**
	 * Verifies if the provided key should be considered as a list item and
	 * creates the corresponding list objects in the map if needed
	 * 
	 * @param map
	 *            the map to insert the list into
	 * @param key
	 *            the key to be parsed as a list
	 * @param value
	 *            the value ton insert
	 */
	private void handleList(Map<String, Object> map, String key, Object value) {
		if (supportLists && getListRegex().matches(key)) {
			ListKeyParser parser = new ListKeyParser(key);
			addList(map, parser.rootKey, parser.id, parser.subKey, value);
		}
	}

	/**
	 * Adds or updates a list into the specified Map, creating the necessary
	 * List and Map objects
	 * 
	 * @param map
	 *            the map to insert or update the list into
	 * @param rootKey
	 *            the list name
	 * @param id
	 *            the id of the element being inserted in the list
	 * @param subKey
	 *            the key of the value to put in the list element
	 * @param value
	 *            the value to put in the list element
	 */
	private void addList(Map<String, Object> map, String rootKey, String id,
			String subKey, Object value) {
		List<Map<String, Object>> listContext = (List<Map<String, Object>>) map
				.get(rootKey);
		if (listContext == null) {
			listContext = new ArrayList<Map<String, Object>>();
			map.put(rootKey, listContext);
		}

		Map<String, Object> foundMap = null;
		for (Map<String, Object> subMap : listContext) {
			if (id.equals(subMap.get(listIdName))) {
				foundMap = subMap;
				break;
			}
		}
		if (foundMap == null) {
			foundMap = new HashMap<String, Object>();
			foundMap.put(listIdName, id);
			listContext.add(foundMap);
			Collections.sort(listContext,
					new Comparator<Map<String, Object>>() {
						public int compare(Map<String, Object> m1,
								Map<String, Object> m2) {
							return ((String) m1.get(listIdName))
									.compareTo((String) m2.get(listIdName));
						}
					});
		}
		add(foundMap, subKey, value);
	}

	private Regexp _listRegexp = null;

	/**
	 * get an Ant regexp from the given standard regex pattern
	 * 
	 * @return the ant regexp
	 */
	private Regexp getListRegex() {
		if (_listRegexp == null) {
			RegularExpression _listRegularExpression = new RegularExpression();
			_listRegularExpression.setPattern(listRegex);
			_listRegexp = _listRegularExpression.getRegexp(getProject());
		}
		return _listRegexp;
	}

	/**
	 * This class splits the key string provided to its constructor into the
	 * root key, the id and the sub key of a list item
	 * 
	 * @author Patrick
	 *
	 */
	private class ListKeyParser {
		/**
		 * the list name
		 */
		public String rootKey = null;

		/**
		 * the id of the list element
		 */
		public String id = null;

		/**
		 * the key to put into the list element
		 */
		public String subKey = null;

		/**
		 * constructor: parses the key into root key, id and sub-key
		 * 
		 * @param key
		 *            the key to parse
		 */
		public ListKeyParser(String key) {
			Vector<?> groups = getListRegex().getGroups(key);
			rootKey = (String) groups.get(1);
			id = (String) groups.get(2);
			subKey = (String) groups.get(3);
		}
	}

	/**
	 * computes the value into a Boolean if needed
	 * 
	 * @param key
	 *            the key to evaluate, which should match the boolean pattern in
	 *            order to be treated as a boolean
	 * @param value
	 *            the value to translate into a boolean if needed
	 * @return either a corresponding boolean or the value itself
	 */
	private Object computeValue(String key, Object value) {
		if (key.endsWith("?") && "false".equals(value)) {
			return false;
		}
		return value;
	}
}
