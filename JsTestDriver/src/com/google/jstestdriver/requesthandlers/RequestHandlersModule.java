// Copyright 2010 Google Inc. All Rights Reserved.
package com.google.jstestdriver.requesthandlers;

import com.google.common.collect.ImmutableList;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.servlet.RequestParameters;
import com.google.inject.servlet.RequestScoped;
import com.google.jstestdriver.annotations.RequestProtocol;
import com.google.jstestdriver.annotations.ResponseWriter;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * An abstract {@link Guice} module providing an EDSL for binding {@link RequestHandler}s
 * to ({@link HttpMethod}, {@link RequestMatcher}}) pairs.  Also, exposes the
 * {@link HttpServletRequest}, {@link HttpServletResponse} and interesting
 * properties of these in the request scope.
 *
 * @author rdionne@google.com (Robert Dionne)
 */
public abstract class RequestHandlersModule extends AbstractModule {
  
  private final ImmutableList.Builder<RequestMatcher> matchers;
  private final RequestScope requestScope;

  public RequestHandlersModule() {
    matchers = ImmutableList.builder();
    requestScope = new RequestScope();
  }

  /**
   * Override this method to specify ({@link HttpMethod}, {@link RequestMatcher})
   * pairs associated with {@link RequestHandler}s.  Also provide any further
   * bindings for your handlers here.
   *
   * The {@link RequestDispatcher} will dispatch requests to the {@link RequestHandler}
   * associated with the first matching {@link RequestMatcher}.
   *
   * Specify the order via:
   *
   * serve(GET, "/first/*", FirstHandler.class);
   * serve(GET, "/first/second", SecondHandler.class);
   * ...
   * etc.
   */
  protected abstract void configureHandlers();

  @Override
  protected void configure() {
    configureHandlers();

    bindScope(RequestScoped.class, requestScope);
    bind(RequestScope.class).toInstance(requestScope);

    bind(new Key<List<RequestMatcher>>() {}).toInstance(matchers.build());
    bind(Servlet.class).to(RequestHandlerServlet.class).in(Singleton.class);
  }

  @Provides @RequestScoped HttpServletRequest provideRequest() {
    return RequestHandlerServlet.getRequest();
  }

  @Provides @RequestScoped HttpMethod provideRequestMethod() {
    return HttpMethod.valueOf(RequestHandlerServlet.getRequest().getMethod());
  }

  @Provides @RequestScoped @RequestProtocol String provideRequestProtocol() {
    return RequestHandlerServlet.getRequest().getProtocol();
  }

  @Provides @RequestScoped @RequestParameters Map<String, String[]> provideRequestParameters() {
    return RequestHandlerServlet.getRequest().getParameterMap();
  }

  @Provides @RequestScoped HttpServletResponse provideResponse() {
    return RequestHandlerServlet.getResponse();
  }

  @Provides @RequestScoped @ResponseWriter PrintWriter provideResponseWriter() throws IOException {
    return RequestHandlerServlet.getResponse().getWriter();
  }

  protected void serve(HttpMethod method, String pattern, Class<? extends RequestHandler> withHttpHandler) {
    RequestMatcher matcher = new RequestMatcher(method, pattern);
    matchers.add(matcher);
    MapBinder.newMapBinder(binder(), RequestMatcher.class, RequestHandler.class)
        .addBinding(matcher).to(withHttpHandler).in(RequestScoped.class);
  }
}
