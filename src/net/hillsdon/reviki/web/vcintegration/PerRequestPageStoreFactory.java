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
package net.hillsdon.reviki.web.vcintegration;

import net.hillsdon.reviki.search.SearchEngine;
import net.hillsdon.reviki.search.impl.SearchIndexPopulatingPageStore;
import net.hillsdon.reviki.vc.MimeIdentifier;
import net.hillsdon.reviki.vc.PageStore;
import net.hillsdon.reviki.vc.impl.AutoPropertiesApplier;
import net.hillsdon.reviki.vc.impl.BasicSVNOperations;
import net.hillsdon.reviki.vc.impl.DeletedRevisionTracker;
import net.hillsdon.reviki.vc.impl.PageListCachingPageStore;
import net.hillsdon.reviki.vc.impl.SVNPageStore;

import com.google.common.base.Supplier;

public class PerRequestPageStoreFactory implements Supplier<PageStore> {

  private final String _wiki;
  private final SearchEngine _indexer;
  private final DeletedRevisionTracker _tracker;
  private final BasicSVNOperations _operations;
  private final AutoPropertiesApplier _autoPropertiesApplier;
  private final MimeIdentifier _mimeIdentifier;

  public PerRequestPageStoreFactory(final String wiki, final SearchEngine indexer, final DeletedRevisionTracker tracker, final BasicSVNOperations operations, final AutoPropertiesApplier autoPropertiesApplier, final MimeIdentifier mimeIdentifier) {
    _wiki = wiki;
    _indexer = indexer;
    _tracker = tracker;
    _operations = operations;
    _autoPropertiesApplier = autoPropertiesApplier;
    _mimeIdentifier = mimeIdentifier;
  }

  public PageStore get() {
    return new SearchIndexPopulatingPageStore(_indexer, new PageListCachingPageStore(new SpecialPagePopulatingPageStore(new SVNPageStore(_wiki, _tracker, _operations, _autoPropertiesApplier, _mimeIdentifier))));
  }

}
