/*
 * Copyright (c) 2010. All rights reserved.
 */
package ro.isdc.wro.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Properties;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ro.isdc.wro.WroRuntimeException;
import ro.isdc.wro.config.Context;
import ro.isdc.wro.config.factory.FilterConfigWroConfigurationFactory;
import ro.isdc.wro.config.factory.PropertyWroConfigurationFactory;
import ro.isdc.wro.config.jmx.ConfigConstants;
import ro.isdc.wro.config.jmx.WroConfiguration;
import ro.isdc.wro.manager.WroManagerFactory;
import ro.isdc.wro.manager.factory.BaseWroManagerFactory;
import ro.isdc.wro.model.WroModel;
import ro.isdc.wro.model.factory.WroModelFactory;
import ro.isdc.wro.model.factory.XmlModelFactory;
import ro.isdc.wro.model.group.Group;
import ro.isdc.wro.model.group.InvalidGroupNameException;
import ro.isdc.wro.model.resource.processor.impl.css.CssUrlRewritingProcessor;
import ro.isdc.wro.util.ObjectFactory;


/**
 * Test for {@link WroFilter} class.
 *
 * @author Alex Objelean
 * @created Created on Jul 13, 2009
 */
public class TestWroFilter {
  private WroFilter filter;
  private FilterConfig config;


  @Before
  public void setUp()
    throws Exception {
    filter = new WroFilter();
    config = Mockito.mock(FilterConfig.class);
    final ServletContext servletContext = Mockito.mock(ServletContext.class);
    Mockito.when(config.getServletContext()).thenReturn(servletContext);
    filter.init(config);
  }

  /**
   * Initialize filter field with properly configured wro.xml.
   */
  private void initFilterWithValidConfig() {
    initFilterWithValidConfig(filter);
  }

  /**
   * Initialize filter field with properly configured wro.xml.
   */
  private void initFilterWithValidConfig(WroFilter filter) {
    filter = new WroFilter() {
      @Override
      protected WroManagerFactory getWroManagerFactory() {
        return new BaseWroManagerFactory().setModelFactory(new XmlModelFactory() {
          @Override
          protected InputStream getModelResourceAsStream() {
            return getClass().getResourceAsStream("wro.xml");
          }
        });
      }
    };
  }


  /**
   * Set filter init params with proper values and check they are the same in {@link WroConfiguration} object.
   */
  @Test(expected = WroRuntimeException.class)
  public void testFilterInitParamsAreWrong()
    throws Exception {
    Mockito.when(config.getInitParameter(ConfigConstants.cacheUpdatePeriod.name())).thenReturn("InvalidNumber");
    Mockito.when(config.getInitParameter(ConfigConstants.modelUpdatePeriod.name())).thenReturn("100");
    filter.init(config);
  }


  @Test(expected = WroRuntimeException.class)
  public void testInvalidAppFactoryClassNameIsSet()
    throws Exception {
    Mockito.when(config.getInitParameter(ConfigConstants.managerFactoryClassName.name())).thenReturn("Invalid value");
    filter.init(config);
  }


  /**
   * Test that in DEPLOYMENT mode if {@link InvalidGroupNameException} is thrown, the response redirect to 404.
   */
  @Test
  public void testInvalidGroupNameExceptionThrownInDEPLOYMENTMode()
    throws Exception {
    testChainContinueWhenSpecificExceptionThrown(new InvalidGroupNameException(""));
  }


  /**
   * Test that in DEPLOYMENT mode if {@link InvalidGroupNameException} is thrown, the response redirect to 404.
   */
  @Test
  public void testUnauthorizedRequestExceptionThrownInDEPLOYMENTMode()
    throws Exception {
    testChainContinueWhenSpecificExceptionThrown(new UnauthorizedRequestException(""));
  }


  /**
   * Test that in DEPLOYMENT mode if specified exception is thrown, the response redirect to 404.
   */
  public void testChainContinueWhenSpecificExceptionThrown(final Throwable e)
    throws Exception {
    final WroManagerFactory factory = Mockito.mock(WroManagerFactory.class);
    Mockito.when(factory.create()).thenThrow(e);
    filter = createTestFilter(factory, false);
    final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
    filter.init(config);

    final FilterChain chain = Mockito.mock(FilterChain.class);
    filter.doFilter(Mockito.mock(HttpServletRequest.class), response, chain);
    verifyChainIsCalled(chain);
  }


//  /**
//   * Test that in development mode, all runtime exception are not catched.
//   */
//  @Test(expected = WroRuntimeException.class)
//  public void testInvalidGroupNameExceptionThrownInDevelopmentMode()
//    throws Exception {
//    final WroManagerFactory factory = Mockito.mock(WroManagerFactory.class);
//    Mockito.when(factory.getInstance()).thenThrow(new InvalidGroupNameException(""));
//    filter = createTestFilter(factory, true);
//    final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
//    filter.init(config);
//    filter.doFilter(Mockito.mock(HttpServletRequest.class), response, Mockito.mock(FilterChain.class));
//  }


  /**
   * Creates a test filter with configured {@link WroManagerFactory} as a factory and with configuration mode set to
   * DEVELOPMENT if isDevelopment argument is true.
   *
   * @param factory used by the filter.
   * @param isDevelopment true if configuration mode is in DEVELOPMENT mode.
   * @return
   */
  private WroFilter createTestFilter(final WroManagerFactory factory, final boolean isDevelopment) {
    return new WroFilter() {
      @Override
      protected WroManagerFactory getWroManagerFactory() {
        return factory;
      }
    };
  }


  @Test
  public void testValidAppFactoryClassNameIsSet()
    throws Exception {
    Mockito.when(config.getInitParameter(ConfigConstants.managerFactoryClassName.name())).thenReturn(
      BaseWroManagerFactory.class.getName());
    filter.init(config);
  }


  @Test
  public void testJmxDisabled()
    throws Exception {
    Mockito.when(config.getInitParameter(ConfigConstants.jmxEnabled.name())).thenReturn("false");
    filter.init(config);
  }


  /**
   * Set filter init params with proper values and check they are the same in {@link WroConfiguration} object.
   */
  @Test
  public void testFilterInitParamsAreSetProperly()
    throws Exception {
    setConfigurationMode(FilterConfigWroConfigurationFactory.PARAM_VALUE_DEPLOYMENT);
    Mockito.when(config.getInitParameter(ConfigConstants.gzipResources.name())).thenReturn("false");
    Mockito.when(config.getInitParameter(ConfigConstants.cacheUpdatePeriod.name())).thenReturn("10");
    Mockito.when(config.getInitParameter(ConfigConstants.modelUpdatePeriod.name())).thenReturn("100");
    filter.init(config);
    final WroConfiguration config = filter.getWroConfiguration();
    Assert.assertEquals(false, config.isDebug());
    Assert.assertEquals(false, config.isGzipEnabled());
    Assert.assertEquals(10, config.getCacheUpdatePeriod());
    Assert.assertEquals(100, config.getModelUpdatePeriod());
  }


  @Test
  public void testValidHeaderParamIsSet()
    throws Exception {
    Mockito.when(config.getInitParameter(ConfigConstants.header.name())).thenReturn("ETag: 998989");
    filter.init(config);
  }


  @Test
  public void testValidHeaderParamsAreSet()
    throws Exception {
    Mockito.when(config.getInitParameter(ConfigConstants.header.name())).thenReturn(
      "ETag: 998989 | Expires: Thu, 15 Apr 2010 20:00:00 GMT");
    filter.init(config);
  }


  @Test(expected = WroRuntimeException.class)
  public void testInvalidHeaderParamIsSet()
    throws Exception {
    Mockito.when(config.getInitParameter(ConfigConstants.header.name())).thenReturn("ETag 998989 expires 1");
    filter.init(config);
  }


  /**
   * Set filter init params with proper values and check they are the same in {@link WroConfiguration} object.
   */
  @Test
  public void testConfigurationInitParam()
    throws Exception {
    Mockito.when(config.getInitParameter(FilterConfigWroConfigurationFactory.PARAM_CONFIGURATION)).thenReturn("anyOtherString");
    filter.init(config);
    Assert.assertEquals(true, filter.getWroConfiguration().isDebug());
  }

  @Test
  public void testDisableCacheInitParamInDeploymentMode()
    throws Exception {
    Mockito.when(config.getInitParameter(FilterConfigWroConfigurationFactory.PARAM_CONFIGURATION)).thenReturn(FilterConfigWroConfigurationFactory.PARAM_VALUE_DEPLOYMENT);
    Mockito.when(config.getInitParameter(ConfigConstants.disableCache.name())).thenReturn("true");
    filter.init(config);
    Assert.assertEquals(false, filter.getWroConfiguration().isDebug());
    Assert.assertEquals(false, filter.getWroConfiguration().isDisableCache());
  }

  @Test
  public void testDisableCacheInitParamInDevelopmentMode()
    throws Exception {
    Mockito.when(config.getInitParameter(ConfigConstants.disableCache.name())).thenReturn("true");
    filter.init(config);
    Assert.assertEquals(true, filter.getWroConfiguration().isDebug());
    Assert.assertEquals(true, filter.getWroConfiguration().isDisableCache());
  }

  /**
   * Check what happens when the request cannot be processed and assure that the we proceed with chain.
   *
   * @throws Exception
   */
  public void cannotProcessConfigResourceStream()
    throws Exception {
    final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
    final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
    final FilterChain chain = Mockito.mock(FilterChain.class);
    Mockito.when(request.getRequestURI()).thenReturn("");
    filter.doFilter(request, response, chain);
    verifyChainIsCalled(chain);
  }


  /**
   * Check if the chain call was performed.
   */
  private void verifyChainIsCalled(final FilterChain chain)
    throws IOException, ServletException {
    Mockito.verify(chain, Mockito.atLeastOnce()).doFilter(Mockito.any(HttpServletRequest.class),
      Mockito.any(HttpServletResponse.class));
  }

  /**
   * Check if the chain call was performed.
   */
  private void verifyChainIsNotCalled(final FilterChain chain)
    throws IOException, ServletException {
    Mockito.verify(chain, Mockito.never()).doFilter(Mockito.any(HttpServletRequest.class),
      Mockito.any(HttpServletResponse.class));
  }


  @Test
  public void cannotProcessInvalidUri()
    throws Exception {
    final FilterChain chain = Mockito.mock(FilterChain.class);
    requestGroupByUri("", chain);
    verifyChainIsCalled(chain);
  }


  @Test
  public void requestValidGroup()
    throws Exception {
    requestGroupByUri("/folder/g1.css");
  }


  @Test
  public void requestInvalidGroup()
    throws Exception {
    final FilterChain chain = Mockito.mock(FilterChain.class);
    requestGroupByUri("/folder/INVALID_GROUP.css", chain);
    verifyChainIsCalled(chain);
  }


  @Test
  public void cannotAccessUnauthorizedRequest()
    throws Exception {
    final FilterChain chain = Mockito.mock(FilterChain.class);
    final String resourcePath = "/g1.css";
    final String requestUri = CssUrlRewritingProcessor.PATH_RESOURCES + resourcePath;
    requestGroupByUri(requestUri, new RequestBuilder(requestUri) {
      @Override
      protected HttpServletRequest newRequest() {
        final HttpServletRequest request = super.newRequest();
        Mockito.when(request.getParameter(CssUrlRewritingProcessor.PARAM_RESOURCE_ID)).thenReturn(resourcePath);
        return request;
      }
    }, chain);
    verifyChainIsCalled(chain);
  }


  // TODO build model before performing the request
  // @Test
  public void requestUrlRewrittenResource()
    throws Exception {
    final String resourcePath = "classpath:ro/isdc/wro/http/2.css";
    final String requestUri = CssUrlRewritingProcessor.PATH_RESOURCES + "?id=" + resourcePath;
    requestGroupByUri(requestUri, new RequestBuilder(requestUri) {
      @Override
      protected HttpServletRequest newRequest() {
        final HttpServletRequest request = super.newRequest();
        Mockito.when(request.getParameter(CssUrlRewritingProcessor.PARAM_RESOURCE_ID)).thenReturn(resourcePath);
        return request;
      }
    });
  }


  private void requestGroupByUri(final String requestUri)
    throws IOException, ServletException {
    final FilterChain chain = Mockito.mock(FilterChain.class);
    requestGroupByUri(requestUri, new RequestBuilder(requestUri), chain);
  }


  private void requestGroupByUri(final String requestUri, final FilterChain chain)
    throws IOException, ServletException {
    requestGroupByUri(requestUri, new RequestBuilder(requestUri), chain);
  }


  @Test
  public void testDoFilterInDEPLOYMENTMode()
    throws Exception {
    initFilterWithValidConfig();
    final HttpServletRequest request = Mockito.mock(HttpServletRequest.class, Mockito.RETURNS_DEEP_STUBS);
    Mockito.when(request.getRequestURI()).thenReturn("/g2.js");
    final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
    final ServletOutputStream sos = Mockito.mock(ServletOutputStream.class);
    Mockito.when(response.getOutputStream()).thenReturn(sos);
    final FilterChain chain = Mockito.mock(FilterChain.class);
    setConfigurationMode(FilterConfigWroConfigurationFactory.PARAM_VALUE_DEPLOYMENT);
    filter.init(config);
    filter.doFilter(request, response, chain);
  }


  /**
   * Perform initialization and simulates a call to WroFilter with given requestUri.
   *
   * @param requestUri
   */
  private void requestGroupByUri(final String requestUri, final RequestBuilder requestBuilder, final FilterChain chain)
    throws IOException, ServletException {
    initFilterWithValidConfig();
    final HttpServletRequest request = requestBuilder.newRequest();
    final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
    final ServletOutputStream sos = Mockito.mock(ServletOutputStream.class);
    Mockito.when(response.getOutputStream()).thenReturn(sos);
    filter.init(config);
    filter.doFilter(request, response, chain);
  }


  private void requestGroupByUri(final String requestUri, final RequestBuilder requestBuilder)
    throws IOException, ServletException {
    final FilterChain chain = Mockito.mock(FilterChain.class);
    requestGroupByUri(requestUri, requestBuilder, chain);
  }

  /**
   * Tests that in DEPLOYMENT mode the API is not exposed.
   */
  @Test
  public void testApiCallInDEPLOYMENTMode()
    throws Exception {
    initFilterWithValidConfig();
    final HttpServletRequest request = Mockito.mock(HttpServletRequest.class, Mockito.RETURNS_DEEP_STUBS);
    Mockito.when(request.getRequestURI()).thenReturn(WroFilter.PATH_API + "/someMethod");
    final FilterChain chain = Mockito.mock(FilterChain.class);
    setConfigurationMode(FilterConfigWroConfigurationFactory.PARAM_VALUE_DEPLOYMENT);
    filter.init(config);
    filter.doFilter(request, Mockito.mock(HttpServletResponse.class), chain);
    //No api method exposed -> proceed with chain
    verifyChainIsCalled(chain);
  }

  /**
   * Tests that in DEPLOYMENT mode the API is not exposed.
   */
  @Test
  public void testApiCallInDEVELOPMENTModeAndInvalidApiCall()
    throws Exception {
    initFilterWithValidConfig();
    final HttpServletRequest request = Mockito.mock(HttpServletRequest.class, Mockito.RETURNS_DEEP_STUBS);
    Mockito.when(request.getRequestURI()).thenReturn(WroFilter.PATH_API + "/someMethod");
    final FilterChain chain = Mockito.mock(FilterChain.class);
    //by default configuration is development
    filter.init(config);
    filter.doFilter(request, Mockito.mock(HttpServletResponse.class), chain);
    //No api method exposed -> proceed with chain
    verifyChainIsCalled(chain);
  }


  /**
   * Tests that in DEVELOPMENT mode the API is exposed.
   */
  @Test
  public void testApiCallInDEVELOPMENTModeAndReloadCacheCall()
    throws Exception {

    initFilterWithValidConfig();
    final HttpServletRequest request = Mockito.mock(HttpServletRequest.class, Mockito.RETURNS_DEEP_STUBS);
    Mockito.when(request.getRequestURI()).thenReturn(WroFilter.API_RELOAD_CACHE);

    final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
    Mockito.when(response.getWriter()).thenReturn(new PrintWriter(System.out));
    final FilterChain chain = Mockito.mock(FilterChain.class);
    //by default configuration is development
    filter.init(config);

    filter.doFilter(request, response, chain);
    //api method exposed -> chain is not called
    verifyChainIsNotCalled(chain);
  }


  /**
   * Tests that in DEPLOYMENT mode the API is NOT exposed.
   */
  @Test
  public void apiCallInDeploymentMode()
    throws Exception {
    final Properties props = new Properties();
    //init WroConfig properties
    props.setProperty(ConfigConstants.debug.name(), Boolean.FALSE.toString());
    final WroFilter theFilter = new WroFilter() {
      @Override
      protected ObjectFactory<WroConfiguration> newWroConfigurationFactory() {
        final PropertyWroConfigurationFactory factory = new PropertyWroConfigurationFactory();
        factory.setProperties(props);
        return factory;
      }
    };
    initFilterWithValidConfig(theFilter);
    final HttpServletRequest request = Mockito.mock(HttpServletRequest.class, Mockito.RETURNS_DEEP_STUBS);
    Mockito.when(request.getRequestURI()).thenReturn(WroFilter.API_RELOAD_CACHE);

    final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
    Mockito.when(response.getWriter()).thenReturn(new PrintWriter(System.out));
    final FilterChain chain = Mockito.mock(FilterChain.class);
    //by default configuration is development
    theFilter.init(config);

    theFilter.doFilter(request, response, chain);
    //No api method exposed -> proceed with chain
    verifyChainIsCalled(chain);
  }


  /**
   * Proves that the model reload has effect.
   */
  @Test
  public void modelShouldBeReloadedWhenReloadIsTriggered() throws Exception {
    final WroManagerFactory wroManagerFactory = new BaseWroManagerFactory().setModelFactory(new WroModelFactory() {
      private boolean wasCreated = false;
      public WroModel create() {
        if (!wasCreated) {
          wasCreated = true;
          //return model with no groups defined
          return new WroModel();
        }
        //second time when created add one group
        return new WroModel().addGroup(new Group("g1"));
      }
      public void destroy() {
      }
    });
    Context.set(Context.standaloneContext());

    final WroFilter filter = new WroFilter() {
      protected WroManagerFactory getWroManagerFactory() {
        return wroManagerFactory;
      }
      @Override
      protected ObjectFactory<WroConfiguration> newWroConfigurationFactory() {
        return new ObjectFactory<WroConfiguration>() {
          public WroConfiguration create() {
            return Context.get().getConfig();
          }
        };
      }
    };
    filter.init(Mockito.mock(FilterConfig.class));
    final WroModelFactory modelFactory = wroManagerFactory.create().getModelFactory();

    Assert.assertTrue(modelFactory.create().getGroups().isEmpty());

    //reload model
    Context.get().getConfig().reloadModel();
    //the second time should have one group
    Assert.assertEquals(1, modelFactory.create().getGroups().size());
  }



  @Test
  public void testReloadCacheCall()
      throws Exception {
    final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
    Mockito.when(request.getRequestURI()).thenReturn(WroFilter.API_RELOAD_CACHE);

    final ThreadLocal<Integer> status = new ThreadLocal<Integer>();
    final HttpServletResponse response = new HttpServletResponseWrapper(Mockito.mock(HttpServletResponse.class,
        Mockito.RETURNS_DEEP_STUBS)) {
      @Override
      public void setStatus(final int sc) {
        status.set(sc);
      }
    };

    Context.set(Context.webContext(request, response, Mockito.mock(FilterConfig.class)));
    filter.doFilter(Context.get().getRequest(), Context.get().getResponse(), Mockito.mock(FilterChain.class));

    Assert.assertEquals(Integer.valueOf(HttpServletResponse.SC_OK), status.get());
  }

  @Test
  public void testReloadModelCall()
      throws Exception {
    final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
    Mockito.when(request.getRequestURI()).thenReturn(WroFilter.API_RELOAD_MODEL);

    final ThreadLocal<Integer> status = new ThreadLocal<Integer>();
    final HttpServletResponse response = new HttpServletResponseWrapper(Mockito.mock(HttpServletResponse.class, Mockito.RETURNS_DEEP_STUBS)) {
      @Override
      public void setStatus(final int sc) {
        status.set(sc);
      }
    };

    Context.set(Context.webContext(request, response,
        Mockito.mock(FilterConfig.class)));
    filter.doFilter(Context.get().getRequest(), Context.get().getResponse(), Mockito.mock(FilterChain.class));
    Assert.assertEquals(Integer.valueOf(HttpServletResponse.SC_OK), status.get());
  }

  /**
   * Mocks the WroFilter.PARAM_CONFIGURATION init param with passed value.
   */
  private void setConfigurationMode(final String value) {
    Mockito.when(config.getInitParameter(FilterConfigWroConfigurationFactory.PARAM_CONFIGURATION)).thenReturn(value);
  }

  class RequestBuilder {
    private final String requestUri;


    public RequestBuilder(final String requestUri) {
      this.requestUri = requestUri;
    }


    protected HttpServletRequest newRequest() {
      final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
      Mockito.when(request.getRequestURI()).thenReturn(requestUri);
      return request;
    }
  }

  @After
  public void tearDown() {
    if (filter != null) {
      filter.destroy();
    }
    Context.unset();
  }
}
