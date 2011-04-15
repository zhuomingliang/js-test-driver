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

import java.io.File;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Sets;
import com.google.inject.Module;
import com.google.jstestdriver.JsTestDriverServer;
import com.google.jstestdriver.idea.javascript.navigation.NavigationRegistry;
import com.google.jstestdriver.idea.javascript.navigation.NavigationRegistryBuilder;
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
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.util.concurrency.FutureResult;

import static com.google.common.collect.Lists.transform;
import static com.intellij.execution.testframework.sm.SMTestRunnerConnectionUtil.attachRunner;
import static com.intellij.util.PathUtil.getJarPathForClass;
import static java.io.File.pathSeparator;
import static java.io.File.separatorChar;
import static java.util.Arrays.asList;
import static java.util.concurrent.Executors.newSingleThreadExecutor;

/**
 * Encapsulates the execution state of the test runner. The IDE will create and run an instance of this class when the
 * user requests to run the tests. We in turn launch a new Java process which will execute the tests.
 *
 * @author alexeagle@google.com (Alex Eagle)
 */
public class TestRunnerState extends CommandLineState {

  private static final ExecutorService testResultReceiverExecutor =
      newSingleThreadExecutor(namedThreadFactory("remoteTestResultReceiver-%d"));
  private static final Logger logger = Logger.getInstance(TestRunnerState.class.getCanonicalName());

  // TODO(alexeagle): needs to be configurable?
  private static final int testResultPort = 10998;
  private final JSTestDriverConfiguration jsTestDriverConfiguration;
  protected final Project project;

  private static ThreadFactory namedThreadFactory(final String threadName) {
    return new ThreadFactory() {
      final AtomicInteger cnt = new AtomicInteger(0);
      @Override public Thread newThread(Runnable r) {
        int num = cnt.incrementAndGet();
        Thread thread = Executors.defaultThreadFactory().newThread(r);
        thread.setName(String.format(threadName, num));
        thread.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
          @Override
          public void uncaughtException(Thread t, Throwable e) {
            logger.error("Uncaught exception on " + t, e);
          }
        });
        return thread;
      }};
  }

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
      addParameter(buildClasspath());

      addParameter(TestRunner.class.getName());
      addParameter(serverURL);
      addParameter(jsTestDriverConfiguration.getSettingsFile());
      addParameter(String.valueOf(testResultPort));
    }};
  }

  private static final Function<File, String> getAbsolutePath = new Function<File, String>() {
    @Override public String apply(File file) {
      return file.getAbsolutePath();
    }};

  private String buildClasspath() {
    Set<String> classpath = Sets.newHashSet();

    String pathToJstd = getJarPathForClass(JsTestDriverServer.class);
    boolean isRunningInIde = !pathToJstd.endsWith(".jar");
    if (isRunningInIde) {
      // JSTD compiled code is in a classes/ folder
      classpath.add(pathToJstd);
      String pathToLibJar = getJarPathForClass(Module.class);
      File[] libs = new File(pathToLibJar.substring(0, pathToLibJar.lastIndexOf(separatorChar))).listFiles();

      classpath.addAll(transform(asList(libs), getAbsolutePath));
    } else {
      // JSTD is in a jar next to other libraries
      File[] libs = new File(pathToJstd.substring(0, pathToJstd.lastIndexOf(separatorChar))).listFiles();
      classpath.addAll(transform(asList(libs), getAbsolutePath));
    }

    return Joiner.on(pathSeparator).join(classpath);
  }

  @Nullable
  public ExecutionResult execute(@NotNull Executor executor, @NotNull ProgramRunner runner) throws ExecutionException {
    final TestConsoleProperties testConsoleProperties =
        new SMTRunnerConsoleProperties(jsTestDriverConfiguration, "jsTestDriver", executor);

    FutureResult<ProcessData> processDataFuture = new FutureResult<ProcessData>();
    TestListenerContext context = new TestListenerContext(processDataFuture);
    RemoteTestListener listener = createRemoteTestListener(context);

    CountDownLatch receivingSocketOpen = new CountDownLatch(1);
    Future<?> testResultReceiverFuture = testResultReceiverExecutor.submit(
        new RemoteTestResultReceiver(listener, testResultPort, receivingSocketOpen));
    // TODO (ssimonchik) try get test results from process output stream without sockets
    try {
      receivingSocketOpen.await(5, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      throw new ExecutionException("Thread has been interrupted unexpectedly", e);
    }
    if (testResultReceiverFuture.isDone()) {
      throw new ExecutionException("RemoteTestResultReceiver exits unexpectedly. See log for details");
    }

    ProcessHandler processHandler = startProcess();
    BaseTestsOutputConsoleView consoleView =
        attachRunner(project.getName(), processHandler, testConsoleProperties, getRunnerSettings(), getConfigurationSettings());
    processDataFuture.set(new ProcessData((SMTRunnerConsoleView) consoleView, processHandler));

    return new DefaultExecutionResult(context.consoleView(), context.processHandler(),
        createActions(context.consoleView(), context.processHandler()));
  }

  private RemoteTestListener createRemoteTestListener(TestListenerContext context) {
    File configFile = new File(jsTestDriverConfiguration.getSettingsFile());
    NavigationRegistryBuilder builder = NavigationRegistryBuilder.getInstance();
    NavigationRegistry registry = builder.buildNavigationRegistry(project, configFile, configFile.getParentFile());
    return new RemoteTestListener(registry, context);
  }

  @Override
  protected ProcessHandler startProcess() throws ExecutionException {
    GeneralCommandLine commandLine = createGeneralCommandLine();
    logger.info("Running JSTestDriver: " + commandLine.getCommandLineString());
    return new OSProcessHandler(commandLine.createProcess(), "");
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
