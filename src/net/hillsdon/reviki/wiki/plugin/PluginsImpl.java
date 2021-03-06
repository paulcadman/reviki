/**
 * Copyright 2008 Matthew Hillsdon
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.hillsdon.reviki.wiki.plugin;

import static net.hillsdon.reviki.web.vcintegration.BuiltInPageReferences.CONFIG_PLUGINS;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import net.hillsdon.reviki.vc.ChangeInfo;
import net.hillsdon.reviki.vc.ContentTypedSink;
import net.hillsdon.reviki.vc.PageStore;
import net.hillsdon.reviki.vc.PageStoreException;
import net.hillsdon.reviki.vc.StoreKind;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.picocontainer.DefaultPicoContainer;

import com.google.common.collect.Lists;

/**
 * An aggregate of all the current plugins.
 *
 * @author mth
 */
public class PluginsImpl implements Plugins {

  private static final Log LOG = LogFactory.getLog(PluginsImpl.class);

  private final DefaultPicoContainer _context;
  private final ConcurrentMap<String, PluginAtRevision> _plugin = new ConcurrentHashMap<String, PluginAtRevision>();
  private final PageStore _store;
  private long _lastSyncedRevision;

  /**
   */
  public PluginsImpl(final PageStore store) {
    _store = store;
    _context = new DefaultPicoContainer();
    _lastSyncedRevision = 0;
  }

  public <T> List<T> getImplementations(final Class<T> clazz) {
    List<T> implementations = new ArrayList<T>();
    for (PluginAtRevision plugin : _plugin.values()) {
      implementations.addAll(plugin.getImplementations(clazz));
    }
    return implementations;
  }

  public void handleChanges(final long upto, final List<ChangeInfo> chronological) {
    // We want to do the most recent first to prevent repeated work.
    try {
      for (ChangeInfo change : Lists.reverse(chronological)) {
        if (change.getKind() == StoreKind.ATTACHMENT && change.getPage().equals(CONFIG_PLUGINS.getPath())) {
          PluginAtRevision plugin = _plugin.get(change.getName());
          if (plugin == null || plugin.getRevision() < change.getRevision()) {
            updatePlugin(change);
          }
        }
      }
    }
    catch (Exception ex) {
      // If we propagate this then issues with plugins can disable a wiki.
      // Ideally we'd report this on ConfigPlugins only.
      LOG.error("Error encountered updating plugins", ex);
    }
    _lastSyncedRevision = upto;
  }

  private void updatePlugin(final ChangeInfo change) throws PageStoreException, IOException {
    final String name = change.getName();
    final long revision = change.getRevision();
    if (change.isDeletion()) {
      LOG.info("Removing " + name + " due to revision " + revision);
      _plugin.put(name, new PluginAtRevision(null, revision));
    }
    else {
      LOG.info("Updating " + name + " to revision " + revision);
      final File jar = File.createTempFile("cached-", name);
      jar.deleteOnExit();
      final OutputStream stream = new FileOutputStream(jar);
      try {
        _store.attachment(CONFIG_PLUGINS, name, revision, new ContentTypedSink() {
          public void setContentType(final String contentType) {
          }
          public void setFileName(final String attachment) {
          }
          public OutputStream stream() throws IOException {
            return stream;
          }
        });
      }
      finally {
        IOUtils.closeQuietly(stream);
      }
      URL url = jar.toURI().toURL();
      try {
        _plugin.put(name, new PluginAtRevision(new Plugin(name, url, _context), revision));
      }
      catch (InvalidPluginException ex) {
        // Some way to indicate the error to the user would be nice...
        _plugin.remove(name);
        LOG.error("Invalid plugin uploaded.", ex);
      }
    }
  }

  public void addPluginAccessibleComponent(final Object component) {
    _context.addComponent(component);
  }

  public long getHighestSyncedRevision() throws IOException {
    return _lastSyncedRevision;
  }

}
