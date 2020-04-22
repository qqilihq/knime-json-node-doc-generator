/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   Oct 10, 2013 (hornm): created
 */
package de.philippkatz.knime.jsondocgen;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.xml.transform.TransformerException;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.swt.widgets.Display;
import org.knime.core.node.ConfigurableNodeFactory;
import org.knime.core.node.Node;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeModel;
import org.knime.core.node.context.ModifiableNodeCreationConfiguration;
import org.knime.core.node.context.NodeCreationConfiguration;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.PortTypeRegistry;
import org.knime.core.node.streamable.PartitionInfo;
import org.knime.workbench.repository.model.Category;
import org.knime.workbench.repository.model.IContainerObject;
import org.knime.workbench.repository.model.IRepositoryObject;
import org.knime.workbench.repository.model.NodeTemplate;
import org.knime.workbench.repository.model.Root;
import org.w3c.dom.Element;

import de.philippkatz.knime.jsondocgen.docs.CategoryDoc;
import de.philippkatz.knime.jsondocgen.docs.CategoryDoc.CategoryDocBuilder;
import de.philippkatz.knime.jsondocgen.docs.NodeDoc.DynamicPortGroup;
import de.philippkatz.knime.jsondocgen.docs.NodeDoc.NodeDocBuilder;
import de.philippkatz.knime.jsondocgen.docs.NodeDoc.Port;
import de.philippkatz.knime.jsondocgen.docs.PortTypeDoc;
import de.philippkatz.knime.jsondocgen.docs.PortTypeDoc.PortTypeDocBuilder;
import de.philippkatz.knime.jsondocgen.docs.SplashIconDoc;

/**
 * Creates a summary of the node descriptions of a all available KNIME nodes in
 * JSON format.
 *
 * This class is based on org.knime.workbench.repository.util.NodeDocuGenerator
 *
 * @author Martin Horn, University of Konstanz
 * @author Philipp Katz, seleniumnodes.com
 */
public class JsonNodeDocuGenerator implements IApplication {

	private static final Logger LOGGER = Logger.getLogger(JsonNodeDocuGenerator.class);

	private static final String DESTINATION_ARG = "-destination";

	private static final String CATEGORY_ARG = "-category";

	private static final String PLUGIN_ARG = "-plugin";

	private static final String INCLUDE_DEPRECATED_ARG = "-includeDeprecated";

	private static final String INCLUDE_HIDDEN_ARG = "-includeHidden";

	private static final String SKIP_NODE_DOCUMENTATION = "-skipNodeDocumentation";

	private static final String SKIP_PORT_DOCUMENTATION = "-skipPortDocumentation";

	private static final String SKIP_SPLASH_ICONS = "-skipSplashIcons";

	/** Return code in case an error occurs during execution. */
	private static final Integer EXIT_EXECUTION_ERROR = Integer.valueOf(1);

	private static void printUsage() {
		System.err.println("Usage: NodeDocuGenerator options");
		System.err.println("Allowed options are:");
		System.err.println("\t" + DESTINATION_ARG
				+ " dir : directory where the result should be written to (directory must exist)");
		System.err.println("\t" + PLUGIN_ARG
				+ " plugin-id : Only nodes of the specified plugin will be considered (specify multiple plugins by repeating this option). If not all available plugins will be processed.");
		System.err.println("\t" + CATEGORY_ARG
				+ " category-path (e.g. /community) : Only nodes within the specified category path will be considered. If not specified '/' is used.");
		System.err.println(
				"\t" + INCLUDE_DEPRECATED_ARG + " : Include nodes marked as 'deprecated' in the extension point.");
		System.err.println("\t" + INCLUDE_HIDDEN_ARG
				+ " : Include nodes marked as 'hidden' in the extension point (new since KNIME 4.2).");
		System.err.println("\t" + SKIP_NODE_DOCUMENTATION + " : Skip generating node documentation");
		System.err.println("\t" + SKIP_PORT_DOCUMENTATION + " : Skip generating port documentation");
		System.err.println("\t" + SKIP_SPLASH_ICONS + " : Skip extracting splash screen icons");

	}

	/* target directory */
	private File m_directory;

	private final Set<String> m_pluginIds = new HashSet<>();

	private String m_catPath = "/";

	private boolean m_includeDeprecated = false;

	private boolean m_includeHidden = false;

	private boolean m_skipNodeDocumentation = false;

	private boolean m_skipPortDocumentation = false;

	private boolean m_skipSplashIcons = false;

	private CategoryDocBuilder rootCategoryDoc;

	@Override
	public Object start(final IApplicationContext context) throws Exception {
		Object o = context.getArguments().get(IApplicationContext.APPLICATION_ARGS);
		Display.getDefault();
		if (o != null && o instanceof String[]) {
			String[] args = (String[]) o;
			for (int i = 0; i < args.length; i++) {
				if (args[i].equals(DESTINATION_ARG)) {
					m_directory = new File(args[i + 1]);
				} else if (args[i].equals(CATEGORY_ARG)) {
					m_catPath = args[i + 1];
				} else if (args[i].equals(PLUGIN_ARG)) {
					m_pluginIds.add(args[i + 1]);
				} else if (args[i].equals(INCLUDE_DEPRECATED_ARG)) {
					m_includeDeprecated = true;
				} else if (args[i].equals(INCLUDE_HIDDEN_ARG)) {
					m_includeHidden = true;
				} else if (args[i].equals(SKIP_NODE_DOCUMENTATION)) {
					m_skipNodeDocumentation = true;
				} else if (args[i].equals(SKIP_PORT_DOCUMENTATION)) {
					m_skipPortDocumentation = true;
				} else if (args[i].equals(SKIP_SPLASH_ICONS)) {
					m_skipSplashIcons = true;
				} else if (args[i].equals("-help")) {
					printUsage();
					return EXIT_OK;
				}
			}
		}

		if (m_directory == null) {
			System.err.println("No output directory specified");
			printUsage();
			return EXIT_EXECUTION_ERROR;
		} else if (!m_directory.exists() && !m_directory.mkdirs()) {
			System.err.println("Could not create output directory '" + m_directory.getAbsolutePath() + "'.");
			return EXIT_EXECUTION_ERROR;
		}

		try {
			generate();
		} catch (Throwable t) {
			// important: catch all throwables here and do not throw them out of this
			// method; this is due to the fact, that execution errors are being shown in a
			// GUI dialog, and when running headless (with Xvfb), we cannot access this
			// dialog, the application will just remain running, and we will never know why
			// we're hanging. Happy debugging! (nb: catching Throwable on purpose, Exception
			// is not sufficient; we had a java.lang.NoClassDefFoundError)
			LOGGER.error("Encountered error", t);
			return EXIT_EXECUTION_ERROR;
		}

		return EXIT_OK;
	}

	@Override
	public void stop() {

	}

	/**
	 * Starts generating the node reference documents.
	 *
	 * @throws Exception
	 */
	private void generate() throws Exception {

		if (!m_skipNodeDocumentation) {

			LOGGER.info("Reading node repository");

			IRepositoryObject root = RepositoryManager.INSTANCE.getRoot();
			rootCategoryDoc = new CategoryDocBuilder();
			rootCategoryDoc.setId(root.getID());
			rootCategoryDoc.setName(root.getName());
			rootCategoryDoc.setContributingPlugin(root.getContributingPlugin());

			// replace '/' with points and remove leading '/'
			if (m_catPath.startsWith("/")) {
				m_catPath = m_catPath.substring(1);
			}
			m_catPath = m_catPath.replaceAll("/", ".");

			// recursively generate the node reference and the node description
			// pages
			generate(m_directory, root, null, rootCategoryDoc);

			CategoryDoc rootCategory = rootCategoryDoc.build();
			String resultJson = rootCategory.toJson();
			File resultFile = new File(m_directory, "nodeDocumentation.json");
			LOGGER.info("Writing nodes to " + resultFile);
			IOUtils.write(resultJson, new FileOutputStream(resultFile), StandardCharsets.UTF_8);

		}

		if (!m_skipPortDocumentation) {

			LOGGER.info("Generating port documentation");

			Map<Class<? extends PortObject>, PortTypeDocBuilder> builders = new HashMap<>();

			// all registered port types indexed by the PortObject class; read this only
			// once from the registry and cache it, b/c the registry creates new PortTypes
			// dynamically when requesting an unknown type
			Map<Class<? extends PortObject>, PortType> portTypes = PortTypeRegistry.getInstance().availablePortTypes()
					.stream().collect(Collectors.toMap(PortType::getPortObjectClass, Function.identity()));

			LOGGER.debug(String.format("Found %s ports to process", portTypes.size()));

			processPorts(portTypes.keySet(), portTypes, builders);

			// get the root element (all PortObjects inherit from this interface).
			PortTypeDoc rootElement = builders.get(PortObject.class).build();

			File portTypeResultFile = new File(m_directory, "portDocumentation.json");
			LOGGER.info("Writing port types to " + portTypeResultFile);
			IOUtils.write(Utils.toJson(rootElement), new FileOutputStream(portTypeResultFile), StandardCharsets.UTF_8);

		}

		if (!m_skipSplashIcons) {

			LOGGER.debug("Generating splash icons");

			List<SplashIconDoc> splashIcons = SplashIconReader.readSplashIcons();
			LOGGER.debug(String.format("Found %s splash icons", splashIcons.size()));

			File splashIconsResultFile = new File(m_directory, "splashIcons.json");
			LOGGER.info("Writing splash icons to " + splashIconsResultFile);
			IOUtils.write(Utils.toJson(splashIcons), new FileOutputStream(splashIconsResultFile),
					StandardCharsets.UTF_8);
		}
	}

	/**
	 * Process port type information (and recursively build the hierarchical
	 * documentation structure).
	 * 
	 * @param portObjectClasses
	 *            The {@link PortObject} classes to process.
	 * @param registeredPortTypes
	 *            All *registered* port types.
	 * @param builders
	 *            Map with builders for appending the children.
	 */
	private static void processPorts(Collection<Class<? extends PortObject>> portObjectClasses,
			Map<Class<? extends PortObject>, PortType> registeredPortTypes,
			Map<Class<? extends PortObject>, PortTypeDocBuilder> builders) {

		portObjectClasses.forEach(portObjectClass -> {

			LOGGER.debug(String.format("Processing %s", portObjectClass.getName()));

			PortTypeDoc.PortTypeDocBuilder builder = builders.get(portObjectClass);

			List<Class<? extends PortObject>> parentPortObjectClasses = getParentPortObjectClasses(portObjectClass);

			processPorts(parentPortObjectClasses, registeredPortTypes, builders);

			if (builder == null) { // haven't processed this type yet
				PortType parent = registeredPortTypes.get(portObjectClass);
				if (parent != null) {
					// parent port type is registered via extension point
					builder = PortTypeDoc.builderForObjectClass(parent.getPortObjectClass().getName());
					builder.setName(parent.getName());
					builder.setSpecClass(parent.getPortObjectSpecClass().getName());
					builder.setColor(makeHexColor(parent.getColor()));
					builder.setHidden(parent.isHidden());
					builder.setRegistered(true);
				} else {
					// not registered -- only create dummy intermediate; this is e.g. the case for
					// org.knime.core.node.port.AbstractPortObject which only serve as
					// implementation helper and are not supposed to be used directly
					builder = PortTypeDoc.builderForObjectClass(portObjectClass.getName());
					builder.setHidden(true);
					builder.setRegistered(false);
				}
				builders.put(portObjectClass, builder);
			}

			for (Class<? extends PortObject> parent : parentPortObjectClasses) {
				builders.get(parent).addChild(builder);
			}
		});
	}

	/* package */ static String makeHexColor(int color) {
		// ensure that hex color is padded with zeros
		// (TODO probably ensure that the hex string is no longer than six characters)
		String hex = Integer.toHexString(color);
		return "000000".concat(hex).substring(hex.length());
	}

	@SuppressWarnings("unchecked")
	/* package */ static List<Class<? extends PortObject>> getParentPortObjectClasses(
			Class<? extends PortObject> portObjectClass) {
		List<Class<? extends PortObject>> result = new ArrayList<>();
		Class<?> superClass = portObjectClass.getSuperclass();
		if (superClass != null && superClass != Object.class && PortObject.class.isAssignableFrom(superClass)) {
			result.add((Class<? extends PortObject>) superClass);
		}
		Class<?>[] interfaces = portObjectClass.getInterfaces();
		for (Class<?> interFace : interfaces) {
			if (PortObject.class.isAssignableFrom(interFace)) {
				result.add((Class<? extends PortObject>) interFace);
			}
		}
		return result;
	}

	/**
	 * Recursively generates the nodes description documents and the menu entries.
	 *
	 * @param directory
	 * @param current
	 * @param parent
	 *            parent repository object as some nodes pointing to "frequently
	 *            used"-repository object as a parent
	 * @param parentCategory
	 *            The parent category where to insert the JSON entry.
	 * @throws Exception
	 * @throws TransformerException
	 *
	 * @return true, if the element was added to the documentation, false if it has
	 *         been skipped
	 */
	private boolean generate(final File directory, final IRepositoryObject current, final IRepositoryObject parent,
			CategoryDocBuilder parentCategory) throws TransformerException, Exception {

		if (current instanceof NodeTemplate) {

			// skip node if not part of the specified plugin
			if (!m_pluginIds.isEmpty() && !m_pluginIds.contains(current.getContributingPlugin())) {
				return false;
			}

			// skip if not in a sub-category of the category specified
			// as argument
			if (m_catPath.length() > 0) {
				String catIdentifier = getCategoryIdentifier(parent);
				if (!catIdentifier.startsWith(m_catPath)) {
					return false;
				}
			}

			NodeTemplate nodeTemplate = (NodeTemplate) current;
			NodeFactory<? extends NodeModel> factory = nodeTemplate.createFactoryInstance();

			NodeDocBuilder builder = new NodeDocBuilder();
			builder.setId(current.getID());
			builder.setName(current.getName());
			
			// get additional information from the node XML description
			Element xmlDescription = factory.getXMLDescription();
			if (xmlDescription != null) {
				NodeDocJsonParser.parse(xmlDescription, builder);
			}
			
			builder.setContributingPlugin(current.getContributingPlugin());
			if (nodeTemplate.getIcon() != null) {
				builder.setIconBase64(Utils.getImageBase64(nodeTemplate.getIcon()));
			}
			builder.setAfterId(Utils.stringOrNull(nodeTemplate.getAfterID()));
			boolean deprecated = RepositoryManager.INSTANCE.isDeprecated(current.getID());
			boolean hidden = RepositoryManager.INSTANCE.isHidden(current.getID());
			builder.setHidden(hidden);
			// port type information -- extract this information separately and do not merge
			// with the node description's port information, because the documentation and
			// the actual implementation might be inconsistent.
			try {
				NodeModel nodeModel = createNodeModel(factory);
				PortType[] outPorts = getPorts(nodeModel, false);
				// TODO need to consider dynamic ports here?!
				builder.setOutPorts(mergePortInfo(builder.build().outPorts, outPorts, current.getID()));
				PortType[] inPorts = getPorts(nodeModel, true);
				// TODO need to consider dynamic ports here?!
				builder.setInPorts(mergePortInfo(builder.build().inPorts, inPorts, current.getID()));
				builder.setStreamable(isStreamable(nodeModel));
				// TODO merge this “dynamic port” shit here
				// https://github.com/knime/knime-workbench/blob/9b42402105c8f8ebb3a74ae8fa869522d263bf68/org.knime.workbench.repository/src/eclipse/org/knime/workbench/repository/nodalizer/NodeInfo.java
				builder.setDynamicInPorts(getDynamicPorts(factory, PortDirection.In));
				builder.setDynamicOutPorts(getDynamicPorts(factory, PortDirection.Out));
			} catch (Throwable t) {
				LOGGER.warn(String.format("Could not create NodeModel for %s", factory.getClass().getName()), t);
			}

			if (deprecated) {
				// there are two locations, where nodes can be set to deprecated:
				// so, do not overwrite with false, if already set to true
				builder.setDeprecated(true);
			}
			if ((!deprecated || m_includeDeprecated) && (!hidden || m_includeHidden)) {
				parentCategory.addNode(builder.build());
			}

			return true;
		} else if (current instanceof Category || current instanceof Root) {
			LOGGER.info("Processing category " + getPath(current));
			IRepositoryObject[] repoObjs = ((IContainerObject) current).getChildren();

			CategoryDocBuilder newCategory = parentCategory;

			if (current instanceof Category) {
				Category category = (Category) current;
				CategoryDocBuilder builder = new CategoryDocBuilder();
				builder.setId(category.getID());
				builder.setName(category.getName());
				builder.setDescription(category.getDescription());
				builder.setContributingPlugin(category.getContributingPlugin());
				if (category.getIcon() != null) {
					builder.setIconBase64(Utils.getImageBase64(category.getIcon()));
				}
				builder.setAfterId(Utils.stringOrNull(category.getAfterID()));
				newCategory = builder;
			}

			boolean hasChildren = false;
			for (IRepositoryObject repoObj : repoObjs) {
				hasChildren = hasChildren | generate(directory, repoObj, current, newCategory);
			}

			if (hasChildren && current instanceof Category) {
				parentCategory.addChild(newCategory.build());
			}

			return hasChildren;

		} else {
			// if the repository object is neither a node nor a category (hence, most likely
			// a metanode), we just ignore them for now
			return false;
		}

	}

	/**
	 * Create the {@link NodeModel} for the given {@link NodeFactory}. Apply
	 * workaround for {@link ConfigurableNodeFactory}; see here:
	 * https://github.com/qqilihq/knime-json-node-doc-generator/issues/17
	 * 
	 * @param factory
	 * @return
	 * @throws Exception
	 */
	/* package */ static NodeModel createNodeModel(NodeFactory<? extends NodeModel> factory) throws Exception {
		if (factory instanceof ConfigurableNodeFactory) {
			ConfigurableNodeFactory<?> configurableFactory = (ConfigurableNodeFactory<?>) factory;
			ModifiableNodeCreationConfiguration config = configurableFactory.createNodeCreationConfig();
			// org.knime.core.node.NodeFactory.createNodeModel(NodeCreationConfiguration)
			Method method = NodeFactory.class.getDeclaredMethod("createNodeModel", NodeCreationConfiguration.class);
			method.setAccessible(true);
			return (NodeModel) method.invoke(configurableFactory, config);
		}
		return factory.createNodeModel();
	}

	/**
	 * Merge port information which is defined (a) in the node's documentation, (b)
	 * via the {@link NodeModel}'s implementation.
	 * 
	 * @param ports
	 *            The port info from the documentation.
	 * @param portTypes
	 *            The port type info from the implementation.
	 * @param nodeId
	 *            The ID of the currently processed node (for outputting the debug
	 *            log)
	 * @return the merged port information.
	 */
	private static List<Port> mergePortInfo(List<Port> ports, PortType[] portTypes, String nodeId) {
		List<Port> result = new ArrayList<>();
		int numDocPorts = ports.size();
		int numImplPorts = portTypes.length;
		if (numDocPorts != numImplPorts) {
			LOGGER.warn(String.format("%s: Documentation does not match implementation: %s vs. %s ports", nodeId,
					numDocPorts, numImplPorts));
		}
		for (int index = 0; index < numImplPorts; index++) {
			PortType portType = portTypes[index];
			String name = null;
			String description = null;
			if (ports.size() > index) {
				Port portInfo = ports.get(index);
				name = portInfo.name;
				description = portInfo.description;
			}
			result.add(
					new Port(index, portType.getPortObjectClass().getName(), name, description, portType.isOptional()));
		}
		return result;
	}

	/**
	 * Get runtime port type information via reflection (this information cannot be
	 * accessed via public API).
	 * 
	 * @param nodeModel
	 *            The node model instance.
	 * @param inPort
	 *            <code>true</code> for input port, <code>false</code> for output
	 *            port.
	 * @return The port type information.
	 * @throws Exception
	 *             In case anything goes wrong.
	 */
	/* package */ static PortType[] getPorts(NodeModel nodeModel, boolean inPort) throws Exception {
		// TODO I see that we could probably get this through the org.knime.core.node.Node class

		Method getPortType = NodeModel.class.getDeclaredMethod(inPort ? "getInPortType" : "getOutPortType", int.class);
		getPortType.setAccessible(true);

		Method getNrPorts = NodeModel.class.getDeclaredMethod(inPort ? "getNrInPorts" : "getNrOutPorts");
		getNrPorts.setAccessible(true);
		int nrPorts = (int) getNrPorts.invoke(nodeModel);

		PortType[] portTypes = new PortType[nrPorts];

		for (int index = 0; index < nrPorts; index++) {
			portTypes[index] = (PortType) getPortType.invoke(nodeModel, index);
		}

		return portTypes;
	}
	
	private static List<DynamicPortGroup> getDynamicPorts(NodeFactory<? extends NodeModel> factory, PortDirection portDirection) {
		if (factory instanceof ConfigurableNodeFactory) {
			@SuppressWarnings("unchecked")
			Node node = new Node((NodeFactory<NodeModel>) factory);
			node.getCopyOfCreationConfig().ifPresent(nodeCreationConfig -> {
				nodeCreationConfig.getPortConfig().ifPresent(portsConfig -> {
					// TODO implement this; look at this mess:
					// https://github.com/knime/knime-workbench/commit/508b59c8f475277df5c095567c8f441eda6808cd
					// https://github.com/knime/knime-workbench/blob/master/org.knime.workbench.repository/src/eclipse/org/knime/workbench/repository/nodalizer/Nodalizer.java
				});
			});
		}
		return Collections.emptyList();
	}

	/**
	 * This code is taken from
	 * org.knime.workbench.repository.view.AbstractRepositoryView.enrichWithAdditionalInfo(IRepositoryObject,
	 * IProgressMonitor, boolean)
	 */
	private static boolean isStreamable(NodeModel nodeModel) {
		try {
			// check whether the current node model overrides the
			// #createStreamableOperator-method
			Method m = nodeModel.getClass().getMethod("createStreamableOperator", PartitionInfo.class,
					PortObjectSpec[].class);
			if (m.getDeclaringClass() != NodeModel.class) {
				// method has been overriden -> node is probably streamable or distributable
				return true;
			}
		} catch (NoSuchMethodException e) {
			// this should never happen, as the method is implemented by the NodeModel class
			LOGGER.error(String.format("No createStreamableOperator method in %s", nodeModel.getClass().getName()), e);
		}
		return false;
	}

	/*
	 * Helper to compose the category names/identifier of the super-categories and
	 * the current one
	 */
	private static String getCategoryIdentifier(final IRepositoryObject cat) {
		IContainerObject parent = cat.getParent();
		String identifier = cat.getID();
		while (parent != null && !(parent instanceof Root)) {
			identifier = parent.getID() + "." + identifier;
			parent = parent.getParent();
		}
		return identifier;
	}

	private static String getPath(final IRepositoryObject object) {
		if (object.getParent() != null) {
			return getPath(object.getParent()) + "/" + object.getName();
		} else {
			return "";
		}
	}
	
	private static enum PortDirection {
		In, Out
	}
}
