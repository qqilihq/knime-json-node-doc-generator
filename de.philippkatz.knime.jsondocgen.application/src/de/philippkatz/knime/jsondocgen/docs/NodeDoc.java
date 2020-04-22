package de.philippkatz.knime.jsondocgen.docs;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Intermediate object used for constructing the JSON and for testing. Use the
 * {@link NodeDocBuilder} for construction.
 */
public final class NodeDoc extends AbstractDoc {
	
	public static final class NodeDocBuilder extends AbstractDocBuilder {
		private String intro;
		private List<OptionTab> optionTabs;
		private List<Option> options;
		private List<Port> inPorts;
		private List<Port> outPorts;
		private List<View> views;
		private String type;
		private boolean deprecated;
		private boolean hidden;
		private InteractiveView interactiveView;
		private boolean streamable;
		/** @since 1.11 -- added with KNIME 4.2 */
		private List<DynamicPortGroup> dynamicInPorts;
		/** @since 1.11 -- added with KNIME 4.2 */
		private List<DynamicPortGroup> dynamicOutPorts;
		public NodeDocBuilder setIntro(String intro) {
			this.intro = intro;
			return this;
		}
		public NodeDocBuilder setOptionTabs(List<OptionTab> optionTabs) {
			this.optionTabs = optionTabs;
			return this;
		}
		public NodeDocBuilder setOptions(List<Option> options) {
			this.options = options;
			return this;
		}
		public NodeDocBuilder setInPorts(List<Port> inPorts) {
			this.inPorts = inPorts;
			return this;
		}
		public NodeDocBuilder setOutPorts(List<Port> outPorts) {
			this.outPorts = outPorts;
			return this;
		}
		public NodeDocBuilder setViews(List<View> views) {
			this.views = views;
			return this;
		}
		public NodeDocBuilder setType(String type) {
			this.type = type;
			return this;
		}
		public NodeDocBuilder setDeprecated(boolean deprecated) {
			this.deprecated = deprecated;
			return this;
		}
		public NodeDocBuilder setHidden(boolean hidden) {
			this.hidden = hidden;
			return this;
		}
		public NodeDocBuilder setInteractiveView(InteractiveView interactiveView) {
			this.interactiveView = interactiveView;
			return this;
		}
		public NodeDocBuilder addOptionTab(OptionTab optionTab) {
			if (optionTabs == null) {
				optionTabs = new ArrayList<>();
			}
			optionTabs.add(optionTab);
			return this;
		}
		public NodeDocBuilder addView(View view) {
			if (views == null) {
				views = new ArrayList<>();
			}
			views.add(view);
			return this;
		}
		public NodeDocBuilder setStreamable(boolean streamable) {
			this.streamable = streamable;
			return this;
		}
		public NodeDoc build() {
			return new NodeDoc(this);
		}
		public NodeDocBuilder setDynamicInPorts(List<DynamicPortGroup> dynamicInPorts) {
			this.dynamicInPorts = dynamicInPorts;
			return this;
		}
		public NodeDocBuilder setDynamicOutPorts(List<DynamicPortGroup> dynamicOutPorts) {
			this.dynamicOutPorts = dynamicOutPorts;
			return this;
		}
	}
	
	public static final class OptionTab {
		public final String name;
		public final String description; 
		public final List<Option> options;
		public OptionTab(String name, String description, List<Option> options) {
			this.name = name;
			this.description = description;
			this.options = copyOrNull(options);
		}
		public OptionTab(String name, List<Option> options) {
			this(name, null, options);
		}
	}
	public static final class Option {
		public final String type;
		public final String name;
		public final String description;
		public final boolean optional;
		public Option(String type, String name, String description, boolean optional) {
			this.type = type;
			this.name = name;
			this.description = description;
			this.optional = optional;
		}
	}
	public static final class Port {
		public final int index;
		public final String portObjectClass;
		public final String name;
		public final String description;
		/** null in case of an outputPort. */
		final Boolean optional;
		public Port(int index, String portObjectClass, String name, String description, Boolean optional) {
			this.index = index;
			this.portObjectClass = portObjectClass;
			this.name = name;
			this.description = description;
			this.optional = optional;
		}
	}
	public static final class View {
		public final int index;
		public final String name;
		public final String description;
		public View(int index, String name, String description) {
			this.index = index;
			this.name = name;
			this.description = description;
		}
	}
	public static final class InteractiveView {
		public final String name;
		public final String description;
		public InteractiveView(String name, String description) {
			this.name = name;
			this.description = description;
		}
	}
	public static final class DynamicPortGroup {
		// TODO
	}

	public final String intro;
	public final List<OptionTab> optionTabs;
	public final List<Option> options;
	public final List<Port> inPorts;
	public final List<Port> outPorts;
	/** @deprecated JSON backwards compatibility, get this from {@link #inPorts}. */
	public final List<String> inPortObjectClasses;
	/** @deprecated JSON backwards compatibility, get this from {@link #outPorts}. */
	public final List<String> outPortObjectClasses;
	public final List<View> views;
	public final String type;
	public final boolean deprecated;
	public final boolean hidden;
	public final InteractiveView interactiveView;
	public final boolean streamable;
	/** @since 1.11 -- added with KNIME 4.2 */
	public final List<DynamicPortGroup> dynamicInPorts;
	/** @since 1.11 -- added with KNIME 4.2 */
	public final List<DynamicPortGroup> dynamicOutPorts;

	private NodeDoc(NodeDocBuilder builder) {
		super(builder);
		intro = builder.intro;
		optionTabs = copyOrNull(builder.optionTabs);
		options = copyOrNull(builder.options);
		inPorts = copyOrNull(builder.inPorts);
		outPorts = copyOrNull(builder.outPorts);
		inPortObjectClasses = convert(builder.inPorts);
		outPortObjectClasses = convert(builder.outPorts);
		views = copyOrNull(builder.views);
		type = builder.type;
		deprecated = builder.deprecated;
		hidden = builder.hidden;
		interactiveView = builder.interactiveView;
		streamable = builder.streamable;
		dynamicInPorts = builder.dynamicInPorts;
		dynamicOutPorts = builder.dynamicOutPorts;
	}

	private static List<String> convert(List<Port> ports) {
		if (ports != null) {
			return ports.stream().map(p -> p.portObjectClass).collect(Collectors.toList());
		} else {
			return null;
		}
	}

}
