/*
 * Copyright (C) 2010.
 * All rights reserved.
 */
package ro.isdc.wro.extensions.processor.js;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ro.isdc.wro.WroRuntimeException;
import ro.isdc.wro.extensions.processor.algorithm.uglify.UglifyJs;
import ro.isdc.wro.model.resource.Resource;
import ro.isdc.wro.model.resource.ResourceType;
import ro.isdc.wro.model.resource.SupportedResourceType;
import ro.isdc.wro.model.resource.processor.ResourcePostProcessor;
import ro.isdc.wro.model.resource.processor.ResourcePreProcessor;


/**
 * Compress js using packer utility.
 *
 * @author Alex Objelean
 * @created 31 Jul 2010
 */
@SupportedResourceType(ResourceType.JS)
public class UglifyJsProcessor
  implements ResourcePreProcessor, ResourcePostProcessor {
  private static final Logger LOG = LoggerFactory.getLogger(UglifyJsProcessor.class);
  /**
   * Engine.
   */
  private UglifyJs engine;

  /**
   * {@inheritDoc}
   */
  public void process(final Resource resource, final Reader reader, final Writer writer)
    throws IOException {
    final String content = IOUtils.toString(reader);
    try {
      writer.write(getEngine().process(content));
    } catch (final WroRuntimeException e) {
      writer.write(content);
      LOG.warn("Exception while applying " + getClass().getSimpleName() + " processor on the resource, no processing applied...", e);
    } finally {
      reader.close();
      writer.close();
    }
  }


  /**
   * @return PackerJs engine.
   */
  private UglifyJs getEngine() {
    if (engine == null) {
      engine = new UglifyJs();
    }
    return engine;
  }


  /**
   * {@inheritDoc}
   */
  public void process(final Reader reader, final Writer writer)
    throws IOException {
    process(null, reader, writer);
  }

}
