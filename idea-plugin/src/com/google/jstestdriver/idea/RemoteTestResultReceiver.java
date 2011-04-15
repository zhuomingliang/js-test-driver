package com.google.jstestdriver.idea;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import javax.swing.SwingUtilities;

/**
 * The IDE end of the socket communication from the TestRunner. Should be run in a background thread. When data is
 * available on the socket, it will be read and trigger an event on the RemoteTestListener (on the AWT event thread),
 * passing the deserialized test result.
 *
 * @author alexeagle@google.com (Alex Eagle)
 */
public class RemoteTestResultReceiver implements Runnable {
  private final RemoteTestListener listener;
  private final int port;
  private final CountDownLatch serverStarted;
  private volatile ServerSocket socket;

  public RemoteTestResultReceiver(RemoteTestListener listener, int port, CountDownLatch serverStarted) {
    this.listener = listener;
    this.port = port;
    this.serverStarted = serverStarted;
  }

  /**
   * Create a socket, and read all the TestResultProtocolMessage objects which the TestRunner process has written to us.
   * When we reach the end of the communication, notify the listener that it may shutdown.
   */
  @Override public void run() {
    Socket client = null;
    Exception savedException = null;
    try {
      socket = new ServerSocket(port);
      serverStarted.countDown();
      client = socket.accept();
      ObjectInputStream in = new ObjectInputStream(client.getInputStream());
      readTestResults(in);
    } catch (Exception e) {
      savedException = e;
    } finally {
      if (client != null) {
        try {
          // also closes socket input stream
          client.close();
        } catch (Exception e) {
          if (savedException != null) {
            savedException = e;
          }
        }
      }
      if (socket != null) {
        try {
          socket.close();
        } catch (Exception e) {
          if (savedException != null) {
            savedException = e;
          }
        }
      }
    }
    if (savedException != null) {
      savedException.printStackTrace();
      throw new RuntimeException(savedException);
    }
  }


  private void readTestResults(ObjectInputStream in) throws IOException {
    while (true) {
      try {
        final TestResultProtocolMessage message = (TestResultProtocolMessage) in.readObject();
        SwingUtilities.invokeLater(new Runnable() {
          @Override
          public void run() {
            if (message.isDryRun()) {
              listener.onTestStarted(message);
            } else {
              listener.onTestFinished(message);
            }
          }
        });
      } catch (EOFException e) {
        break;
      } catch (Exception e) {
        throw new RuntimeException("Problem in communication with TestRunner process", e);
      }
    }
  }
}
