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

import com.google.jstestdriver.JsTestDriverServer;
import com.google.jstestdriver.idea.ui.ToolPanel;
import com.intellij.execution.DefaultExecutionResult;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.CommandLineState;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessListener;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.testframework.TestConsoleProperties;
import com.intellij.execution.testframework.sm.SMTestRunnerConnectionUtil;
import com.intellij.execution.testframework.sm.runner.SMTRunnerConsoleProperties;
import com.intellij.execution.testframework.sm.runner.ui.SMTRunnerConsoleView;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.concurrent.*;

import static com.intellij.util.PathUtil.getJarPathForClass;
import static java.io.File.pathSeparator;

/**
 * Encapsulates the execution state of the test runner.
 *
 * @author alexeagle@google.com (Alex Eagle)
 */
public class TestRunnerState extends CommandLineState {

  private final JSTestDriverConfiguration jsTestDriverConfiguration;
  protected final Project project;
  private final ExecutorService attachExecutor = Executors.newSingleThreadExecutor();

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
      addParameter("-cp");
      addParameter(getJarPathForClass(JsTestDriverServer.class) + pathSeparator + getJarPathForClass(TestRunner.class));
      addParameter(TestRunner.class.getName());
      addParameter(serverURL);
      addParameter(jsTestDriverConfiguration.getSettingsFile());
      addParameter(String.valueOf(testResultPort));
      // uncomment this thing if you want to debug jsTestDriver code in the test-runner process
      // addParameter("-Xdebug");
      // addParameter("-Xrunjdwp:transport=dt_socket,address=5000,server=y");
    }};
  }

  @Nullable
  public ExecutionResult execute(@NotNull Executor executor, @NotNull ProgramRunner runner) throws ExecutionException {
    TestConsoleProperties testConsoleProperties = new SMTRunnerConsoleProperties(jsTestDriverConfiguration, "jsTestDriver", executor);
    TestListenerContext ctx = startAndAttach(testConsoleProperties);
    final RemoteTestListener listener = new RemoteTestListener(ctx);
    listener.listen(testResultPort);
    ctx.processHandler().addProcessListener(new ProcessListener() {
      public void startNotified(ProcessEvent event) {}

      public void processTerminated(ProcessEvent event) {
        listener.shutdown();
      }

      public void processWillTerminate(ProcessEvent event, boolean willBeDestroyed) {}
      public void onTextAvailable(ProcessEvent event, Key outputType) {}
    });
    return new DefaultExecutionResult(ctx.consoleView(), ctx.processHandler(), createActions(ctx.consoleView(), ctx.processHandler()));
  }

  @Override
  protected ProcessHandler startProcess() throws ExecutionException {
    return new OSProcessHandler(createGeneralCommandLine().createProcess(), "");
  }

  private TestListenerContext startAndAttach(final TestConsoleProperties tcp) throws ExecutionException {
    final CountDownLatch gate = new CountDownLatch(1);
    Future<ProcessData> data = attachExecutor.submit(new Callable<ProcessData>() {
      public ProcessData call() throws Exception {
        gate.await();
        ProcessHandler ph = startProcess();
        SMTRunnerConsoleView cv = (SMTRunnerConsoleView) SMTestRunnerConnectionUtil.attachRunner(project.getName(), ph, tcp, getRunnerSettings(), getConfigurationSettings());
        return new ProcessData(cv, ph);
      }
    });
    return new TestListenerContext(data, gate);
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
