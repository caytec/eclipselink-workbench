/*******************************************************************************
 * Copyright (c) 1998, 2010 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 and Eclipse Distribution License v. 1.0 
 * which accompanies this distribution. 
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at 
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *     Oracle - initial API and implementation from Oracle TopLink
******************************************************************************/
package org.eclipse.persistence.tools.workbench.mappingsplugin.ui.meta;

import javax.swing.JLabel;

import org.eclipse.persistence.tools.workbench.framework.context.WorkbenchContext;
import org.eclipse.persistence.tools.workbench.framework.context.WorkbenchContextHolder;
import org.eclipse.persistence.tools.workbench.framework.ui.chooser.ClassChooserDialog;
import org.eclipse.persistence.tools.workbench.framework.ui.chooser.ClassChooserPanel;
import org.eclipse.persistence.tools.workbench.framework.ui.chooser.ClassDescriptionAdapter;
import org.eclipse.persistence.tools.workbench.framework.ui.chooser.ClassDescriptionRepository;
import org.eclipse.persistence.tools.workbench.framework.ui.chooser.ClassDescriptionRepositoryFactory;
import org.eclipse.persistence.tools.workbench.mappingsmodel.meta.MWClass;
import org.eclipse.persistence.tools.workbench.mappingsmodel.meta.MWClassRepository;
import org.eclipse.persistence.tools.workbench.mappingsmodel.spi.meta.ClassDescription;
import org.eclipse.persistence.tools.workbench.uitools.app.PropertyValueModel;
import org.eclipse.persistence.tools.workbench.uitools.app.TransformationPropertyValueModel;
import org.eclipse.persistence.tools.workbench.utility.BidiTransformer;
import org.eclipse.persistence.tools.workbench.utility.ClassTools;
import org.eclipse.persistence.tools.workbench.utility.filters.ANDFilter;
import org.eclipse.persistence.tools.workbench.utility.filters.Filter;


public class ClassChooserTools {

	/**
	 * Prompt the user for a type, chosen from the specified
	 * repository's list of "combined" types. The list will be run
	 * through the specified filter before being displayed to the user.
	 */
	public static MWClass promptForType(
			MWClassRepository repository,
			Filter classNameFilter,
			WorkbenchContext context
	) {
		ClassDescriptionRepository cdr = new CombinedClassDescriptionRepository(repository, classNameFilter);
		ClassDescriptionAdapter cda = ClassDescriptionClassDescriptionAdapter.instance();
		String typeName = promptForType(cdr, cda, context);
		if (typeName == null) {
			return null;
		}
		return repository.typeNamed(typeName);
	}

	/**
	 * Prompt the user for a type, chosen from the specified
	 * repository. Return the type's name.
	 */
	public static String promptForType(
			ClassDescriptionRepository classDescriptionRepository,
			ClassDescriptionAdapter classDescriptionAdapter,
			WorkbenchContext context
	) {
		ClassChooserDialog dialog = ClassChooserDialog.createDialog(classDescriptionRepository, classDescriptionAdapter, context);
		dialog.show();
		if (dialog.wasCanceled()) {
			// try to force all the objects generated by the dialog to be garbage-collected
			dialog = null;
			ClassChooserDialog.gc();
			return null;
		}

		Object selection = dialog.selection();
		// try to force all the objects generated by the dialog to be garbage-collected
		dialog = null;
		ClassChooserDialog.gc();
		return (selection == null) ? null : classDescriptionAdapter.className(selection);
	}

	/**
	 * Build a class chooser panel for the specified settings:
	 * - the selection holder is a value model on the selected MWClass
	 * - the class repository holder allows the panel to be used by
	 *     multiple projects
	 * - the class name filter determines which classes show up in the
	 *     class chooser dialog
	 * - the context holder allows the panel to be used in multiple windows
	 */
	public static ClassChooserPanel buildPanel(
			PropertyValueModel selectionHolder,
			ClassRepositoryHolder classRepositoryHolder,
			Filter classNameFilter,
			WorkbenchContextHolder contextHolder
	) {
		PropertyValueModel selectionHolderWrapper = new TransformationPropertyValueModel(selectionHolder, new ClassDescriptionTransformer(classRepositoryHolder));
		ClassDescriptionRepositoryFactory cdrf = new CombinedClassDescriptionRepositoryFactory(classRepositoryHolder, classNameFilter);
		ClassDescriptionAdapter cda = ClassDescriptionClassDescriptionAdapter.instance();
		return new ClassChooserPanel(selectionHolderWrapper, cdrf, cda, contextHolder);
	}

	/**
	 * Build a class chooser panel for the specified settings:
	 * - the selection holder is a value model on the selected MWClass
	 * - the class repository holder allows the panel to be used by
	 *     multiple projects
	 * - the class name filter determines which classes show up in the
	 *     class chooser dialog
	 * - the context holder allows the panel to be used in multiple windows
	 */
	public static ClassChooserPanel buildPanel(
			PropertyValueModel selectionHolder,
			ClassRepositoryHolder classRepositoryHolder,
			Filter classNameFilter,
			JLabel label,
			WorkbenchContextHolder contextHolder
	) {
		PropertyValueModel selectionHolderWrapper = new TransformationPropertyValueModel(selectionHolder, new ClassDescriptionTransformer(classRepositoryHolder));
		ClassDescriptionRepositoryFactory cdrf = new CombinedClassDescriptionRepositoryFactory(classRepositoryHolder, classNameFilter);
		ClassDescriptionAdapter cda = ClassDescriptionClassDescriptionAdapter.instance();
		return new ClassChooserPanel(selectionHolderWrapper, cdrf, cda, label, contextHolder);
	}

	/**
	 * Build a class chooser panel for the specified settings:
	 * - the selection holder is a value model on the selected MWClass
	 * - the class repository holder allows the panel to be used by
	 *     multiple projects
	 * - the class name filter determines which classes show up in the
	 *     class chooser dialog
	 * - the context holder allows the panel to be used in multiple windows
	 * - the button key allows custom label for button, specifically different mnemonic
	 */
	public static ClassChooserPanel buildPanel(
			PropertyValueModel selectionHolder,
			ClassRepositoryHolder classRepositoryHolder,
			Filter classNameFilter,
			JLabel label,
			WorkbenchContextHolder contextHolder,
			String buttonKey
	) {
		PropertyValueModel selectionHolderWrapper = new TransformationPropertyValueModel(selectionHolder, new ClassDescriptionTransformer(classRepositoryHolder));
		ClassDescriptionRepositoryFactory cdrf = new CombinedClassDescriptionRepositoryFactory(classRepositoryHolder, classNameFilter);
		ClassDescriptionAdapter cda = ClassDescriptionClassDescriptionAdapter.instance();
		return new ClassChooserPanel(selectionHolderWrapper, cdrf, cda, label, contextHolder, buttonKey);
	}

	/**
	 * Filter out the "array", "local", "anonymous", primitive,
	 * and void classes from the original list.
	 */
	public static Filter buildDeclarableReferenceFilter() {
		Filter declarable = buildDeclarableFilter();
		Filter reference = buildReferenceFilter();
		return new ANDFilter(declarable, reference);
	}

	/**
	 * Filter out the "array", "local", "anonymous", and void
	 * classes from the original list (primitives are allowed).
	 */
	public static Filter buildDeclarableNonVoidFilter() {
		Filter declarable = buildDeclarableFilter();
		Filter nonVoid = buildNonVoidFilter();
		return new ANDFilter(declarable, nonVoid);
	}

	/**
	 * Filter out the "array", "local", and "anonymous" classes from the original list.
	 * (Hopefully there are no "array" classes....)
	 */
	public static Filter buildDeclarableFilter() {
		return new Filter() {
			public boolean accept(Object o) {
				return ClassTools.classNamedIsDeclarable((String) o);
			}
			public String toString() {
				return "declarable filter";
			}
		};
	}

	/**
	 * Filter out the void class and the primitive classes
	 * from the original list.
	 */
	public static Filter buildReferenceFilter() {
		return new Filter() {
			public boolean accept(Object o) {
				return ClassTools.classNamedIsReference((String) o);
			}
			public String toString() {
				return "reference filter";
			}
		};
	}

	/**
	 * Filter out the void class from the original list.
	 */
	public static Filter buildNonVoidFilter() {
		return new Filter() {
			public boolean accept(Object o) {
				return ! o.equals("void");
			}
			public String toString() {
				return "non-void filter";
			}
		};
	}


	// ********** helper classes **********

	/**
	 * Transform a MWClass to an ClassDescription and vice versa.
	 */
	public static class ClassDescriptionTransformer implements BidiTransformer {
		private ClassRepositoryHolder classRepositoryHolder;

		public ClassDescriptionTransformer(ClassRepositoryHolder classRepositoryHolder) {
			super();
			this.classRepositoryHolder = classRepositoryHolder;
		}

		/**
		 * The selection from the model is a MWClass and implements
		 * ClassDescription, so it does not need to be converted.
		 * @see org.eclipse.persistence.tools.workbench.utility.Transformer#transform(Object)
		 */
		public Object transform(Object o) {
			return o;
		}

		/**
		 * The selection from the dialog can be either a MWClass or an ExternalClassDescription -
		 * convert it to the appropriate MWClass.
		 * @see org.eclipse.persistence.tools.workbench.utility.BidiTransformer#reverseTransform(Object)
		 */
		public Object reverseTransform(Object o) {
			return (o == null) ? null : this.classRepositoryHolder.getClassRepository().typeNamed(((ClassDescription) o).getName());
		}

	}

}
