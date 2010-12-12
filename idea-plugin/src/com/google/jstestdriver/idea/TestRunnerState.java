/*
 * Copyright 2009 Google Inc.
 *
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
package com.google.jstestdriver.idea;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.jstestdriver.JsTestDriverServer;
import com.google.jstestdriver.idea.ui.ToolPanel;
import com.intellij.execution.DefaultExecutionResult;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.CommandLineState;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.testframework.TestConsoleProperties;
import com.intellij.execution.testframework.sm.runner.SMTRunnerConsoleProperties;
import com.intellij.execution.testframework.sm.runner.ui.SMTRunnerConsoleView;
import com.intellij.execution.testframework.ui.BaseTestsOutputConsoleView;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.concurrent.*;

import static com.intellij.execution.testframework.sm.SMTestRunnerConnectionUtil.attachRunner;
import static com.intellij.util.PathUtil.getJarPathForClass;
import static java.io.File.pathSeparator;
import static java.util.concurrent.Executors.newSingleThreadExecutor;

/**
 * Encapsulates the execution state of the test runner. The IDE will create and run an instance of this class when the
 * user requests to run the tests. We in turn launch a new Java process which will execute the tests.
 *
 * @author alexeagle@google.com (Alex Eagle)
 */
public class TestRunnerState extends CommandLineState {

  private final JSTestDriverConfiguration jsTestDriverConfiguration;
  protected final Project project;
  private final ExecutorService attachExecutor =
      newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat("testProcessLauncher-%d").build());
  private final ExecutorService testResultReceiverExecutor =
      newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat("remoteTestResultReceiver-%d").build());

  // TODO(alexeagle): needs to be configurable?
  private static final int testResultPort = 10998;

  public TestRunnerState(JSTestDriverConfiguration jsTestDriverConfiguration, Project project,
                         ExecutionEnvironment env) {
    super(env);
    this.jsTestDriverConfiguration = jsTestDriverConfiguration;
    this.project = project;
  }

  protected GeneralCommandLine createGeneralCommandLine() throws ExecutionException {
    final String serverURL = (jsTestDriverConfiguration.getServerType() == ServerType.INTERNAL ?
        "http://localhost:" + ToolPanel.serverPort :
        jsTestDriverConfiguration.getServerAddress());
    final File configFile = new File(jsTestDriverConfiguration.getSettingsFile());
    return new GeneralCommandLine() {{
      setWorkingDirectory(configFile.getParentFile());
      setExePath(System.getProperty("java.home") + File.separator + "bin" + File.separator + "java");
      // uncomment this if you want to debug jsTestDriver code in the test-runner process
      //addParameter("-Xdebug");
      //addParameter("-Xrunjdwp:transport=dt_socket,address=5000,server=y,suspend=y");
      
      addParameter("-cp");
      addParameter(getJarPathForClass(JsTestDriverServer.class) + pathSeparator + getJarPathForClass(TestRunner.class));
      addParameter(TestRunner.class.getName());
      addParameter(serverURL);
      addParameter(jsTestDriverConfiguration.getSettingsFile());
      addParameter(String.valueOf(testResultPort));
    }};
  }

  @Nullable
  public ExecutionResult execute(@NotNull Executor executor, @NotNull ProgramRunner runner) throws ExecutionException {
    final TestConsoleProperties testConsoleProperties =
        new SMTRunnerConsoleProperties(jsTestDriverConfiguration, "jsTestDriver", executor);
    final CountDownLatch receivingSocketOpen = new CountDownLatch(1);
    Future<ProcessData> data = attachExecutor.submit(new Callable<ProcessData>() {
      public ProcessData call() throws Exception {
        // Let the receiver start before we write anything to it.
        receivingSocketOpen.await();
        ProcessHandler processHandler = startProcess();
        BaseTestsOutputConsoleView consoleView =
            attachRunner(project.getName(), processHandler, testConsoleProperties, getRunnerSettings(), getConfigurationSettings());
        return new ProcessData((SMTRunnerConsoleView) consoleView, processHandler);
      }
    });
    TestListenerContext context = new TestListenerContext(data);
    final RemoteTestListener listener = new RemoteTestListener(context);
    testResultReceiverExecutor.submit(
        new RemoteTestResultReceiver(listener, testResultPort, receivingSocketOpen));
    return new DefaultExecutionResult(context.consoleView(), context.processHandler(),
        createActions(context.consoleView(), context.processHandler()));
  }

  @Override
  protected ProcessHandler startProcess() throws ExecutionException {
    return new OSProcessHandler(createGeneralCommandLine().createProcess(), "");
  }

  static class ProcessData {
    final SMTRunnerConsoleView consoleView;
    final ProcessHandler processHandler;

    public ProcessData(SMTRunnerConsoleView consoleView, ProcessHandler processHandler) {
      this.consoleView = consoleView;
      this.processHandler = processHandler;
    }
  }
}
