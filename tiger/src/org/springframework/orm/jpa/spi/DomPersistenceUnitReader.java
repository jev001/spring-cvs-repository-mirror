/*
 * Copyright 2002-2006 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.orm.jpa.spi;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.springframework.util.xml.SimpleSaxErrorHandler;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

/**
 * DOM based PersistenceUnitReader.
 * 
 * @author Costin Leau
 * @since 2.0
 */
public class DomPersistenceUnitReader implements PersistenceUnitReader, ResourceLoaderAware {

	private static final Log logger = LogFactory.getLog(DomPersistenceUnitReader.class);

	private ResourceLoader resourceLoader = new DefaultResourceLoader();

	private JpaDataSourceLookup dataSourceLookup = new JndiDataSourceLookup();

	private static final String MAPPING_FILE_NAME = "mapping-file";

	private static final String JAR_FILE_URL = "jar-file";

	private static final String MANAGED_CLASS_NAME = "class";

	private static final String PROPERTIES = "properties";

	private static final String PROVIDER = "provider";

	private static final String EXCLUDE_UNLISTED_CLASSES = "exclude-unlisted-classes";

	private static final String NON_JTA_DATA_SOURCE = "non-jta-data-source";

	private static final String JTA_DATA_SOURCE = "jta-data-source";

	private static final String TRANSACTION_TYPE = "transaction-type";

	private static final String PERSISTENCE_UNIT = "persistence-unit";

	private static final String UNIT_NAME = "name";

	private static final String SCHEMA_NAME = "persistence_1_0.xsd";

	private static final String JAXP_SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";

	private static final String JAXP_SCHEMA_SOURCE = "http://java.sun.com/xml/jaxp/properties/schemaSource";

	private static final String XERCES_SCHEMA = "http://apache.org/xml/features/validation/schema";

	private static final String XERCES_DYNAMIC = "http://apache.org/xml/features/validation/dynamic";

	private static final String XERCES_NAMESPACE = "http://apache.org/xml/properties/schema/external-noNamespaceSchemaLocation";

	private static final String XERCES_SCHEMA_LOCATION = "http://apache.org/xml/properties/schema/external-schemaLocation";

	private static final String XML_VALIDATION = "http://xml.org/sax/features/validation";

	private static final String XML_SCHEMA_VALIDATION = "http://apache.org/xml/features/validation/schema";

	private boolean validation = true;

	/**
	 * Should we validate the persistence.xml?
	 * 
	 * @param validate
	 *            whether to validate persistence.xml
	 */
	public void setValidation(boolean validate) {
		this.validation = validate;
	}

	public boolean getValidation() {
		throw new UnsupportedOperationException();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.orm.jpa.PersistenceUnitReader#readPersistenceUnitInfo(org.springframework.core.io.Resource)
	 */
	public DefaultPersistenceUnitInfo[] readPersistenceUnitInfo(Resource resource) {
		InputStream stream = null;
		try {
			ErrorHandler handler = new SimpleSaxErrorHandler(logger);

			List<DefaultPersistenceUnitInfo> infos = new ArrayList<DefaultPersistenceUnitInfo>();

			stream = resource.getInputStream();
			Document document = validateResource(handler, stream);
			parseDocument(document, infos);

			return infos.toArray(new DefaultPersistenceUnitInfo[] {});
		}
		catch (IOException ex) {
			throw new IllegalArgumentException("Cannot parse persistence unit from resource " + resource, ex);
		}
		catch (SAXException ex) {
			throw new IllegalArgumentException("Invalid XML in persistence unit from resource " + resource, ex);
		}
		catch (ParserConfigurationException ex) {
			throw new IllegalArgumentException("Internal error parsing persistence unit from resource " + resource);
		}
		finally {
			if (stream != null) {
				try {
					stream.close();
				}
				catch (IOException ex) {
					throw new IllegalArgumentException(
							"Error trying to close stream from persistence unit from resource " + resource, ex);
				}
			}
		}
	}

	/**
	 * Parse the validated document and populates the given unit info list.
	 * 
	 * @param document
	 * @param infos
	 * @return
	 * @throws IOException
	 */
	protected List<DefaultPersistenceUnitInfo> parseDocument(Document document, List<DefaultPersistenceUnitInfo> infos)
			throws IOException {
		Element persistence = document.getDocumentElement();
		List<Element> units = (List<Element>) DomUtils.getChildElementsByTagName(persistence, PERSISTENCE_UNIT);
		for (Element unit : units) {
			infos.add(parsePersistenceUnitInfo(unit));
		}

		return infos;
	}

	/**
	 * Utility method that returns the first child element identified by its
	 * name.
	 * 
	 * @param rootElement
	 * @param childName
	 * @return
	 */
	protected Element getChildElementByName(Element rootElement, String childName) {
		NodeList nl = rootElement.getChildNodes();
		for (int i = 0; i < nl.getLength(); i++) {
			Node node = nl.item(i);
			if (node instanceof Element && DomUtils.nodeNameEquals(node, childName)) {
				return (Element) node;
			}
		}
		return null;
	}

	/**
	 * Utility method that returns the first child element value identified by
	 * its name.
	 * 
	 * @param rootElement
	 * @param childName
	 * @return
	 */
	protected String getChildElementValueByName(Element rootElement, String childName) {
		Element child = getChildElementByName(rootElement, childName);
		String value = null;
		if (child != null)
			value = DomUtils.getTextValue(child);

		return (StringUtils.hasText(value) ? value : null);
	}

	/**
	 * Parse the unit info DOM element.
	 * 
	 * @param persistenceUnit
	 * @return
	 * @throws IOException
	 */
	protected DefaultPersistenceUnitInfo parsePersistenceUnitInfo(Element persistenceUnit) throws IOException {
		DefaultPersistenceUnitInfo unitInfo = new DefaultPersistenceUnitInfo();
		// set name
		unitInfo.setPersistenceUnitName(persistenceUnit.getAttribute(UNIT_NAME));
		// set transaction type
		String txType = persistenceUnit.getAttribute(TRANSACTION_TYPE);

		if (StringUtils.hasText(txType))
			unitInfo.setTransactionType(PersistenceUnitTransactionType.valueOf(txType));

		// datasource
		String jtaDataSource = getChildElementValueByName(persistenceUnit, JTA_DATA_SOURCE);
		if (jtaDataSource != null) {
			unitInfo.setJtaDataSource(dataSourceLookup.lookupDataSource(jtaDataSource));
		}

		String nonJtaDataSource = getChildElementValueByName(persistenceUnit, NON_JTA_DATA_SOURCE);
		if (nonJtaDataSource != null) {
			unitInfo.setNonJtaDataSource(dataSourceLookup.lookupDataSource(nonJtaDataSource));
		}

		// provider
		String provider = getChildElementValueByName(persistenceUnit, PROVIDER);
		if (provider != null)
			unitInfo.setPersistenceProviderClassName(provider);

		// exclude unlisted classes
		Element excludeUnlistedClasses = getChildElementByName(persistenceUnit, EXCLUDE_UNLISTED_CLASSES);
		if (excludeUnlistedClasses != null)
			unitInfo.setExcludeUnlistedClasses(true);

		// mapping file
		parseMappingFiles(persistenceUnit, unitInfo);
		parseJarFiles(persistenceUnit, unitInfo);
		parseClass(persistenceUnit, unitInfo);
		parseProperty(persistenceUnit, unitInfo);
		return unitInfo;
	}

	/**
	 * Parse the <code>property</code> XML elements.
	 * 
	 * @param persistenceUnit
	 * @param unitInfo
	 */
	@SuppressWarnings("unchecked")
	protected void parseProperty(Element persistenceUnit, DefaultPersistenceUnitInfo unitInfo) {
		Element propRoot = getChildElementByName(persistenceUnit, PROPERTIES);
		if (propRoot == null)
			return;

		List<Element> properties = DomUtils.getChildElementsByTagName(propRoot, "property");
		for (Element property : properties) {
			String name = property.getAttribute("name");
			String value = property.getAttribute("value");
			unitInfo.addProperty(name, value);
		}
	}

	/**
	 * Parse the <code>class</code> XML elements.
	 * 
	 * @param persistenceUnit
	 * @param unitInfo
	 */
	@SuppressWarnings("unchecked")
	protected void parseClass(Element persistenceUnit, DefaultPersistenceUnitInfo unitInfo) {
		List<Element> classes = DomUtils.getChildElementsByTagName(persistenceUnit, MANAGED_CLASS_NAME);
		for (Element element : classes) {
			String value = DomUtils.getTextValue(element);
			if (StringUtils.hasText(value))
				unitInfo.addManagedClassName(value);
		}
	}

	/**
	 * Parse the <code>jar-file</code> XML elements.
	 * 
	 * @param persistenceUnit
	 * @param unitInfo
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	protected void parseJarFiles(Element persistenceUnit, DefaultPersistenceUnitInfo unitInfo) throws IOException {
		List<Element> jars = DomUtils.getChildElementsByTagName(persistenceUnit, JAR_FILE_URL);
		for (Element element : jars) {
			String value = DomUtils.getTextValue(element);
			if (StringUtils.hasText(value)) {
				Resource resource = resourceLoader.getResource(value);
				unitInfo.addJarFileUrl(resource.getURL());
			}
		}
	}

	/**
	 * Parse the <code>mapping-file</code> XML elements.
	 * 
	 * @param persistenceUnit
	 * @param unitInfo
	 */
	@SuppressWarnings("unchecked")
	protected void parseMappingFiles(Element persistenceUnit, DefaultPersistenceUnitInfo unitInfo) {
		List<Element> files = DomUtils.getChildElementsByTagName(persistenceUnit, MAPPING_FILE_NAME);
		for (Element element : files) {
			String value = DomUtils.getTextValue(element);
			if (StringUtils.hasText(value))
				unitInfo.addMappingFileName(value);
		}
	}

	/**
	 * Validate the given stream and return a valid DOM document for parsing.
	 * 
	 * @param handler
	 * @param stream
	 * @return
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
	protected Document validateResource(ErrorHandler handler, InputStream stream) throws ParserConfigurationException,
			SAXException, IOException {
		Resource schemaLocation = new ClassPathResource(SCHEMA_NAME);

		// InputSource source = new InputSource(stream);
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

		dbf.setValidating(validation);
		dbf.setNamespaceAware(true);

		dbf.setAttribute(JAXP_SCHEMA_LANGUAGE, XMLConstants.W3C_XML_SCHEMA_NS_URI);
		dbf.setAttribute(JAXP_SCHEMA_SOURCE, schemaLocation.getURL().toString());
		//dbf.setAttribute(XERCES_SCHEMA_LOCATION, schemaLocation.getURL().toString());
		/*
		 * see if these should be used on other jdks
		 * 
		 * dbf.setAttribute(JAXP_SCHEMA_SOURCE,
		 * schemaLocation.getURL().toString());
		 * dbf.setAttribute(XERCES_SCHEMA_LOCATION,
		 * schemaLocation.getURL().toString());
		 * 
		 * dbf.setAttribute(XML_SCHEMA_VALIDATION, Boolean.TRUE);
		 * dbf.setAttribute(XML_VALIDATION, Boolean.TRUE);
		 */

		DocumentBuilder parser = dbf.newDocumentBuilder();
		parser.setErrorHandler(handler);
		return parser.parse(stream);
	}

	/**
	 * @return Returns the dataSourceLookup.
	 */
	public JpaDataSourceLookup getDataSourceLookup() {
		return dataSourceLookup;
	}

	/**
	 * @param dataSourceLookup
	 *            The dataSourceLookup to set.
	 */
	public void setDataSourceLookup(JpaDataSourceLookup dataSourceLookup) {
		this.dataSourceLookup = dataSourceLookup;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.context.ResourceLoaderAware#setResourceLoader(org.springframework.core.io.ResourceLoader)
	 */
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

}
