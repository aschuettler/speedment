package com.speedment.codegen;

import com.speedment.codegen.model.CodeModel;
import com.speedment.codegen.view.CodeView;
import com.speedment.codegen.view.CodeViewBuilder;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.Stack;

/**
 *
 * @author Duncan
 */
public class CodeGenerator {
	private final Map<CodeModel.Type, Generator> generators = new EnumMap<>(CodeModel.Type.class);
	
	/**
	 * Sets up all the generators used to create views from models.
	 * @param vb The <code>CodeViewBuilder</code> to use when creating views
	 * from models.
	 */
	public CodeGenerator(CodeViewBuilder vb) {
		Arrays.asList(CodeModel.Type.values()).forEach(
			(t) -> generators.put(t, new Generator(this, vb.getView(t)))
		);
	}
	
	/**
	 * Generates a textual representation of the model based on the installed
	 * view fo that kind of model.
	 * @param model The model to view.
	 * @return A text representation of that (often code).
	 */
	public CharSequence on(CodeModel model) {
		return generators.get(model.getType()).on(model);
	}
	
	/**
	 * Returns the last model of the type specified traversed using the
	 * <code>on(CodeModel)</code> method. This can be used to access parent
	 * components from within the views.
	 * @param type The type of the model to return.
	 * @return The last model traversed of that type.
	 */
	public CodeModel last(CodeModel.Type type) {
		return generators.get(type).peek();
	}
	
	/**
	 * Returns the number of generators installed in the code generator. After
	 * the constructor has finished this should always be the same as the number
	 * of <code>CodeModel.Type</code>s there are.
	 * @return The number of installed generators.
	 */
	public int generatorsCount() {
		return generators.size();
	}
	
	/**
	 * A small helper class used to call views.
	 */
	private class Generator {
		private final Stack<CodeModel> invocationStack = new Stack<>();
		private final CodeGenerator generator;
		private final CodeView<CodeModel> view;
		
		public Generator(CodeGenerator generator, CodeView<CodeModel> view) {
			this.generator = generator;
			this.view = view;
		}
		
		public CodeModel peek() {
			return invocationStack.peek();
		}

		public CharSequence on(CodeModel model) {
			invocationStack.add(model);
			CharSequence result = view.render(generator, model);
			invocationStack.pop();
			return result;
		}
	}
}