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
package net.hillsdon.reviki.web.handlers.impl;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.hillsdon.reviki.vc.PageReference;
import net.hillsdon.reviki.vc.PageStore;
import net.hillsdon.reviki.web.common.ConsumedPath;
import net.hillsdon.reviki.web.common.View;
import net.hillsdon.reviki.web.handlers.Attachments;
import net.hillsdon.reviki.web.handlers.Page;

public class AttachmentsImpl implements Attachments {

  private final Page _list;
  private final Page _upload;
  private final Page _get;

  public AttachmentsImpl(final PageStore pageStore) {
    _list = new ListAttachmentsImpl(pageStore);
    _upload = new UploadAttachmentImpl(pageStore, _list);
    _get = new GetAttachmentImpl(pageStore);
  }

  public View handlePage(final ConsumedPath path, final HttpServletRequest request, final HttpServletResponse response, final PageReference page) throws Exception {
    if (path.hasNext()) {
      return _get.handlePage(path, request, response, page);
    }
    else {
      if (request.getMethod().equals("POST")) {
        return _upload.handlePage(path, request, response, page);
      }
      else {
        return _list.handlePage(path, request, response, page);
      }
    }
  }

}