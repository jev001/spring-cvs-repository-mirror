/*
 * The Spring Framework is published under the terms of the Apache Software License.
 */

package org.springframework.metadata.commons;

import java.io.File;

import org.apache.commons.attributes.compiler.AttributeCompiler;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Javac;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Path;
import org.springframework.util.ControlFlowFactory;

/**
 * Programmatic support classes for compiling with Commons Attributes
 * so that tests can run within Eclipse.
 * @author Rod Johnson
 * @version $Id$
 */
public class CommonsAttributeCompilerUtils {

	public static final String SPRING_ROOT = "c:\\work\\spring";
	
	
	/**
	 * 
	 */
	public static void compileAttributesIfNecessary(String testWildcards) {
		if (inIde()) {
			ideAttributeCompile(testWildcards);
		}
	}

	public static boolean inIde() {
		return inEclipse();
	}

	public static boolean inEclipse() {
		// Use our AOP control flow functionality
		return ControlFlowFactory.getInstance().createControlFlow().underToken("eclipse.jdt");
	}

	public static void ideAttributeCompile(String testWildcards) {
		System.out.println("Compiling attributes under IDE");
		Project project = new Project();
		project.setBaseDir(new File(SPRING_ROOT));
		project.init();

		AttributeCompiler commonsAttributesCompiler = new AttributeCompiler();
		commonsAttributesCompiler.setProject(project);

		//commonsAttributesCompiler.setSourcepathref("test");
		String tempPath = "target/generated-commons-attributes-src";
		commonsAttributesCompiler.setDestdir(new File(tempPath));
		FileSet fileset = new FileSet();
		fileset.setDir(new File(SPRING_ROOT + "/test"));
		String attributeClasses = testWildcards;
		fileset.setIncludes(attributeClasses);
		commonsAttributesCompiler.addFileset(fileset);

		//project.setProperty("JAVA_HOME", "c:\\jdsdk1.4.1_02");

		commonsAttributesCompiler.execute();

		System.out.println("Compiling Java sources generated by Commons Attributes using Javac: requires tools.jar on Eclipse project classpath");
		// We now have the generated Java source: compile it.
		// This requires Javac on the source path
		Javac javac = new Javac();
		javac.setProject(project);
		//project.setCoreLoader(Thread.currentThread().getContextClassLoader());
		Path path = new Path(project, tempPath);
		javac.setSrcdir(path);

		// Couldn't get this to work: trying to use Eclipse
		//javac.setCompiler("org.eclipse.jdt.core.JDTCompilerAdapter");
		javac.setDestdir(new File(SPRING_ROOT + "/target/test-classes"));
		javac.setIncludes(attributeClasses);
		javac.execute();
	}

}