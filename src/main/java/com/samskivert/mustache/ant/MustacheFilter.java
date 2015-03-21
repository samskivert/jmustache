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

public class MustacheFilter extends ChainableReaderFilter {

	private Boolean projectProperties = true;
	private String prefix = null;
	private Boolean removePrefix = false;
	private Boolean supportLists = true;
	private String listIdName = "__id__";
	private String dataFile = null;
	private String listRegex = "(.+)\\.(\\d+)\\.(.+)";
	// other example of regex: (.+)\[(\d+)\]\.(.+)

	// JMustache settings
	private String defaultValue = null;
	private boolean strictSections = false;
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

	public String filter(String text) {
		getProject().log("Mustache DataModel: " + getDataModel().toString(),
				Project.MSG_DEBUG);
		Template tmpl = Mustache.compiler().defaultValue(defaultValue)
				.strictSections(strictSections).escapeHTML(escapeHTML)
				.compile(text);
		return tmpl.execute(getDataModel());
	}

	private Map<String, Object> _dataModel = null;

	private Map<String, Object> getDataModel() {
		if (_dataModel == null) {
			_dataModel = new HashMap<String, Object>();
			addProjectProperties();
			addSrcFile();
		}
		return _dataModel;
	}

	private void addProjectProperties() {
		if (projectProperties) {
			addProperties(getProject().getProperties(), prefix, removePrefix);
		}
	}

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

	private Map<String, Object> add(Map<String, Object> map, String key,
			Object value) {
		map.put(key, value);
		handleList(map, key, value);
		return map;
	}

	private void handleList(Map<String, Object> map, String key, Object value) {
		if (supportLists && getListRegex().matches(key)) {
			ListKeyParser parser = new ListKeyParser(key);
			addList(map, parser.rootKey, parser.id, parser.subKey, value);
		}
	}

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

	private Regexp getListRegex() {
		if (_listRegexp == null) {
			RegularExpression _listRegularExpression = new RegularExpression();
			_listRegularExpression.setPattern(listRegex);
			_listRegexp = _listRegularExpression.getRegexp(getProject());
		}
		return _listRegexp;
	}

	private class ListKeyParser {
		public String rootKey = null;
		public String id = null;
		public String subKey = null;

		public ListKeyParser(String key) {
			Vector<?> groups = getListRegex().getGroups(key);
			rootKey = (String) groups.get(1);
			id = (String) groups.get(2);
			subKey = (String) groups.get(3);
		}
	}

	private Object computeValue(String key, Object value) {
		if (key.endsWith("?") && "false".equals(value)) {
			return false;
		}
		return value;
	}
}
