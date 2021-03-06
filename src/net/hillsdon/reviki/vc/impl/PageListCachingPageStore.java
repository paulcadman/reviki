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
package net.hillsdon.reviki.vc.impl;

import java.util.Set;

import net.hillsdon.reviki.vc.PageReference;
import net.hillsdon.reviki.vc.PageStore;
import net.hillsdon.reviki.vc.PageStoreException;


/**
 * Caches the set of pages forever.  This makes it only
 * suitable for a single request!
 * 
 * This lets us answer questions on page existence efficiently.
 * 
 * @author mth
 */
public class PageListCachingPageStore extends SimpleDelegatingPageStore {

  private Set<PageReference> _cached = null;
  
  public PageListCachingPageStore(final PageStore delegate) {
    super(delegate);
  }

  @Override
  public Set<PageReference> list() throws PageStoreException {
    if (_cached == null) {
      _cached = super.list();
    }
    return _cached;
  }

}
