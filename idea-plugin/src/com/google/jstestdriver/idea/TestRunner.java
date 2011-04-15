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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.LogManager;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Module;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.google.jstestdriver.ActionRunner;
import com.google.jstestdriver.BrowserInfo;
import com.google.jstestdriver.DryRunInfo;
import com.google.jstestdriver.FailureParser;
import com.google.jstestdriver.FlagsImpl;
import com.google.jstestdriver.PathResolver;
import com.google.jstestdriver.PluginLoader;
import com.google.jstestdriver.Response;
import com.google.jstestdriver.ResponseStream;
import com.google.jstestdriver.ResponseStreamFactory;
import com.google.jstestdriver.TestResult;
import com.google.jstestdriver.TestResultGenerator;
import com.google.jstestdriver.config.Configuration;
import com.google.jstestdriver.config.ConfigurationSource;
import com.google.jstestdriver.config.UserConfigurationSource;
import com.google.jstestdriver.config.YamlParser;
import com.google.jstestdriver.hooks.FileParsePostProcessor;
import com.google.jstestdriver.model.NullPathPrefix;
import com.google.jstestdriver.output.DefaultListener;
import com.google.jstestdriver.output.MultiTestResultListener;
import com.google.jstestdriver.output.TestResultHolder;
import com.google.jstestdriver.output.TestResultListener;
import com.google.jstestdriver.runner.RunnerMode;
import com.google.jstestdriver.util.DisplayPathSanitizer;

import static com.google.inject.multibindings.Multibinder.newSetBinder;

/**
 * Run JSTD in its own process, and stream messages via a socket to a server that lives in the IDEA process,
 * which will update the UI with our results.
 *
 * @author alexeagle@google.com (Alex Eagle)
 */
public class TestRunner {

  private final String serverURL;
  private final File settingsFile;
  private final File baseDirectory;
  private final ObjectOutput testResultProtocolMessageOutput;

  public TestRunner(String serverURL, String settingsFile, File baseDirectory,
                    ObjectOutput testResultProtocolMessageOutput) {
    this.serverURL = serverURL;
    this.settingsFile = new File(settingsFile);
    this.baseDirectory = baseDirectory;
    this.testResultProtocolMessageOutput = testResultProtocolMessageOutput;
  }

  public void execute() throws InterruptedException {
    final ActionRunner dryRunRunner =
        makeActionBuilder().dryRunFor(Arrays.asList("all")).build();
    final ActionRunner testRunner =
        makeActionBuilder().addAllTests().build();
    //TODO(alexeagle): support client-side reset action
    final ActionRunner resetRunner =
        makeActionBuilder().resetBrowsers().build();

    dryRunRunner.runActions();
    testRunner.runActions();
  }

  /**
   * Informs IDE about test's status changes.
   */
  public static class TestRunnerResponseStreamFactory implements ResponseStreamFactory {

    private final ObjectOutput testResultProtocolMessageOutput;
    private final TestResultListener testResultListener;

    @Inject
    public TestRunnerResponseStreamFactory(
        @Named("testResultProtocolMessageOutput") ObjectOutput testResultProtocolMessageOutput,
        TestResultListener testResultListener) {
      this.testResultProtocolMessageOutput = testResultProtocolMessageOutput;
      this.testResultListener = testResultListener;
    }

    public ResponseStream getRunTestsActionResponseStream(String s) {
      return new ResponseStream() {
        public void stream(Response response) {
          TestResultGenerator testResultGenerator = new TestResultGenerator(new FailureParser(new NullPathPrefix()));
          for (TestResult testResult : testResultGenerator.getTestResults(response)) {
            if (response.getResponseType() == Response.ResponseType.TEST_RESULT) {
              testResultListener.onTestComplete(testResult);
            }
            try {
              synchronized (testResultProtocolMessageOutput) {
                testResultProtocolMessageOutput.writeObject(TestResultProtocolMessage.fromTestResult(testResult));
              }
            } catch (IOException e) {
              throw new RuntimeException(e);
            }
          }
        }

        public void finish() {
        }
      };
    }

    public ResponseStream getDryRunActionResponseStream() {
      return new ResponseStream() {
        public void stream(Response response) {
          if (response.getResponseType() == Response.ResponseType.FILE_LOAD_RESULT) {
            // TODO process it?
//              new Gson().fromJson(response.getResponse(), LoadedFiles.class);
            return; // for now, don't send back to IDEA
          }
          BrowserInfo browser = response.getBrowser();
          DryRunInfo dryRunInfo = DryRunInfo.fromJson(response);
          for (String testName : dryRunInfo.getTestNames()) {
            try {
              synchronized (testResultProtocolMessageOutput) {
                testResultProtocolMessageOutput.writeObject(TestResultProtocolMessage.fromDryRun(testName, browser));
              }
            } catch (IOException e) {
              throw new RuntimeException(e);
            }
          }
        }

        public void finish() {
        }
      };
    }

    public ResponseStream getEvalActionResponseStream() {
      return null;
    }

    public ResponseStream getResetActionResponseStream() {
      return null;
    }
  }

  private IDEPluginActionBuilder makeActionBuilder() {
    FlagsImpl flags = new FlagsImpl();
    flags.setServer(serverURL);
    Configuration configuration = resolveConfiguration(flags);
    IDEPluginActionBuilder builder =
        new IDEPluginActionBuilder(configuration, flags);
    List<Module> modules = new PluginLoader().load(configuration.getPlugins());
    for (Module module : modules) {
      builder.install(module);
    }
    builder.install(createTestResultPrintingModule());
    return builder;
  }

  private Configuration resolveConfiguration(FlagsImpl flags) {
    try {
      ConfigurationSource confSrc = new UserConfigurationSource(settingsFile);
      Configuration parsedConf = confSrc.parse(baseDirectory, new YamlParser());
      PathResolver pathResolver = new PathResolver(
          baseDirectory,
          Collections.<FileParsePostProcessor>emptySet(),
          new DisplayPathSanitizer(baseDirectory)
      );
      return parsedConf.resolvePaths(pathResolver, flags);
    } catch (FileNotFoundException e) {
      throw new RuntimeException("Failed to read settings file " + settingsFile, e);
    }
  }

  private Module createTestResultPrintingModule() {
    return new AbstractModule() {
      @Override
      protected void configure() {
        bind(ObjectOutput.class).annotatedWith(Names.named("testResultProtocolMessageOutput"))
            .toInstance(testResultProtocolMessageOutput);
        Multibinder<TestResultListener> testResultListeners =
            newSetBinder(binder(), TestResultListener.class);

        testResultListeners.addBinding().to(TestResultHolder.class);
        testResultListeners.addBinding().to(DefaultListener.class).in(Singleton.class);

        bind(TestResultListener.class).to(MultiTestResultListener.class);
        newSetBinder(binder(),
            ResponseStreamFactory.class).addBinding().to(TestRunnerResponseStreamFactory.class);
      }
    };
  }

  public static void main(String[] args) throws Exception {
    LogManager.getLogManager().readConfiguration(RunnerMode.QUIET.getLogConfig());

    final String serverURL = args[0];
    final String settingsFile = args[1];
    final int port = Integer.parseInt(args[2]);

    ObjectOutput testResultProtocolMessageOutput = fetchSocketObjectOutput(port);
    try {
      new TestRunner(serverURL, settingsFile, new File(System.getProperty("user.dir")), testResultProtocolMessageOutput).execute();
    } catch (Exception ex) {
      if (ex instanceof ConnectException) {
        System.err.println("\nCould not connect to a JSTD server running at " + serverURL + "\n" +
            "Check that the server is running.");
      } else {
        System.err.println("JSTestDriver crashed!");
        throw ex;
      }
    } finally {
      try {
        testResultProtocolMessageOutput.close();
      } catch (Exception e) {
        System.err.println("Exception occurred while closing testResultProtocolMessageOutput");
        e.printStackTrace();
      }
    }
  }

  private static ObjectOutput fetchSocketObjectOutput(int port) {
    try {
      SocketAddress endpoint = new InetSocketAddress(InetAddress.getByName(null), port);
      final Socket socket = connectToServer(endpoint, 2 * 1000, 5);
      try {
        return new ObjectOutputStream(socket.getOutputStream()) {
          @Override
          public void close() throws IOException {
            socket.close(); // socket's input and output streams are closed too
          }
        };
      } catch (IOException inner) {
        closeSocketSilently(socket);
        throw inner;
      }
    } catch (IOException e) {
      throw new RuntimeException("Could not connect to IDE, address: " +
          "'localhost:" + port + "'", e);
    }
  }

  private static Socket connectToServer(SocketAddress endpoint, int connectTimeoutMillis,
                                        int retries) throws IOException {
    IOException saved = null;
    for (int i = 0; i < retries; i++) {
      Socket socket = new Socket();
      try {
        socket.connect(endpoint, connectTimeoutMillis);
        return socket;
      } catch (IOException e) {
        closeSocketSilently(socket);
        saved = e;
      }
    }
    throw saved;
  }

  private static void closeSocketSilently(Socket socket) {
    try {
      socket.close();
    } catch (Exception e) {
      // swallow exception
    }
  }
}
