package org.scalastuff.esbt;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.MultiRule;

public class Processor extends Job {

	// The plug-in ID
	public static final String PLUGIN_ID = "scalastuff.eclipse.sbt"; //$NON-NLS-1$

	private Console console;
	private List<ProjectInfo> projects;
	private String command;
	
	public Processor() throws CoreException, IOException {
		super("SBT Project Update");
	  setPriority(Job.LONG);
	  List<ISchedulingRule> rules = new ArrayList<ISchedulingRule>();
	  for (ProjectInfo prg : WorkspaceInfo.getAllProjects()) {
	  	rules.add(prg.getSbtFile());
	  	rules.add(prg.getClassPathFile());
	  }
		setRule(new MultiRule(rules.toArray(new ISchedulingRule[0])));
	}
	
	public void setProjects(List<ProjectInfo> projects) {
		this.projects = projects;
	}
	
	public void setCommand(String command) {
		this.command = command;
	}
	
	private Collection<ProjectInfo> getModifiedProjects() throws CoreException, IOException {
		return projects != null ? 
				projects :
			WorkspaceInfo.getModifiedProjects();
	}
	
	@Override
	protected IStatus run(IProgressMonitor monitor) {
		try {
			try {
				Collection<ProjectInfo> modifiedProjects = getModifiedProjects();
				if (modifiedProjects.isEmpty()) return Status.OK_STATUS;
				console = new Console();
				for (ProjectInfo modifiedSbtProject : modifiedProjects) {
					process(modifiedSbtProject);
				}
				return Status.OK_STATUS;
			} catch (Throwable t) {
				return new Status(Status.ERROR, PLUGIN_ID, t.getMessage(), t);
			}
		} catch (Throwable t) {
			return new Status(Status.ERROR, PLUGIN_ID, t.getMessage(), t);
		} finally {
			try {
				if (console != null) {
					console.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void process(ProjectInfo project) throws IOException, CoreException {
		if (project.isUnderSbtControl()) {
			console.activate();
			console.println("*** Processing Project: " + project.getName() + " ***");
			InvokeSbt sbt = new InvokeSbt(project, console);		
			if (command != null) {
				sbt.setCommand(command);
				sbt.setProjectDir(project.getProjectDir());
				sbt.invokeSbt();
			} else {
				SbtPluginCreator.createSbtPlugin();
				sbt.setProjectDir(cloneProjectDir(project));
				sbt.invokeSbt();
				project.update(sbt);
			}
		}
	}

	private File cloneProjectDir(ProjectInfo project) throws FileNotFoundException, IOException {

		// create build.sbt
		File dir = new File(WorkspaceInfo.getMetaDataDir(), "tmpprj");
		dir.mkdirs();
		List<String> sbtFile = project.getSbtFileWithoutProjectDependencies();
		Utils.write(new FileOutputStream(new File(dir, "build.sbt")), sbtFile);
		
		// copy Build.scala
		File srcBuildScala = new File(dir, "project/Build.scala");
		File destBuildScala = new File(project.getProjectDir(), "project/Build.scala");
		if (srcBuildScala.exists()) {
			Utils.copyStream(new FileInputStream(srcBuildScala), new FileOutputStream(destBuildScala));
		} else {
			destBuildScala.delete();
		}
		return dir;
	}
}
