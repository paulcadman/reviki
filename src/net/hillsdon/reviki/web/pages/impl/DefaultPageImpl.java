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
package net.hillsdon.reviki.web.pages.impl;

import static net.hillsdon.reviki.web.common.RequestParameterReaders.getLong;
import static net.hillsdon.reviki.web.common.RequestParameterReaders.getRequiredString;
import static net.hillsdon.reviki.web.common.RequestParameterReaders.getString;
import static net.hillsdon.reviki.web.common.ViewTypeConstants.CTYPE_ATOM;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.hillsdon.fij.text.Strings;
import net.hillsdon.reviki.configuration.WikiConfiguration;
import net.hillsdon.reviki.search.QuerySyntaxException;
import net.hillsdon.reviki.vc.AttachmentHistory;
import net.hillsdon.reviki.vc.ChangeInfo;
import net.hillsdon.reviki.vc.ConflictException;
import net.hillsdon.reviki.vc.ContentTypedSink;
import net.hillsdon.reviki.vc.LostLockException;
import net.hillsdon.reviki.vc.NotFoundException;
import net.hillsdon.reviki.vc.PageInfo;
import net.hillsdon.reviki.vc.PageReference;
import net.hillsdon.reviki.vc.PageStoreAuthenticationException;
import net.hillsdon.reviki.vc.PageStoreException;
import net.hillsdon.reviki.vc.RenameException;
import net.hillsdon.reviki.vc.VersionedPageInfo;
import net.hillsdon.reviki.vc.impl.CachingPageStore;
import net.hillsdon.reviki.vc.impl.PageInfoImpl;
import net.hillsdon.reviki.vc.impl.PageReferenceImpl;
import net.hillsdon.reviki.vc.impl.PageRevisionReference;
import net.hillsdon.reviki.web.common.ConsumedPath;
import net.hillsdon.reviki.web.common.InvalidInputException;
import net.hillsdon.reviki.web.common.JspView;
import net.hillsdon.reviki.web.common.RequestAttributes;
import net.hillsdon.reviki.web.common.RequestParameterReaders;
import net.hillsdon.reviki.web.common.View;
import net.hillsdon.reviki.web.common.ViewTypeConstants;
import net.hillsdon.reviki.web.handlers.RawPageView;
import net.hillsdon.reviki.web.pages.DefaultPage;
import net.hillsdon.reviki.web.pages.DiffGenerator;
import net.hillsdon.reviki.web.redirect.RedirectToPageView;
import net.hillsdon.reviki.web.urls.WikiUrls;
import net.hillsdon.reviki.web.urls.impl.ResponseSessionURLOutputFilter;
import net.hillsdon.reviki.wiki.MarkupRenderer;
import net.hillsdon.reviki.wiki.feeds.FeedWriter;
import net.hillsdon.reviki.wiki.graph.WikiGraph;
import net.hillsdon.reviki.wiki.renderer.result.ResultNode;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import com.google.common.base.Predicate;
import com.google.common.collect.Maps;

public class DefaultPageImpl implements DefaultPage {

  public static final String ATTR_PREVIEW = "preview";

  public static final String SUBMIT_DELETE = "delete";

  public static final String SUBMIT_SAVE = "save";

  public static final String SUBMIT_COPY = "copy";

  public static final String SUBMIT_RENAME = "rename";

  public static final String SUBMIT_UNLOCK = "unlock";

  public static final String SUBMIT_PREVIEW = ATTR_PREVIEW;

  public static final String PARAM_TO_PAGE = "toPage";

  public static final String PARAM_FROM_PAGE = "fromPage";

  public static final String PARAM_CONTENT = "content";

  public static final String PARAM_ATTRIBUTES = "attributes";

  public static final String PARAM_ORIGINAL_ATTRIBUTES = "originalAttrs";

  public static final String PARAM_BASE_REVISION = "baseRevision";

  public static final String PARAM_LOCK_TOKEN = "lockToken";

  public static final String PARAM_COMMIT_MESSAGE = "description";

  public static final String PARAM_MINOR_EDIT = "minorEdit";

  public static final String PARAM_DIFF_REVISION = "diff";

  public static final String PARAM_REVISION = "revision";

  public static final String PARAM_ATTACHMENT_NAME = "attachmentName";

  public static final String PARAM_ATTACHMENT_MESSAGE = "attachmentMessage";

  public static final String PARAM_SESSION_ID = "sessionId";

  public static final String ATTR_PAGE_INFO = "pageInfo";

  public static final String ATTR_ORIGINAL_ATTRIBUTES = "originalAttributes";

  public static final String ATTR_BACKLINKS = "backlinks";

  public static final String ATTR_BACKLINKS_LIMITED = "backlinksLimited";

  public static final String ATTR_RENDERED_CONTENTS = "renderedContents";

  public static final String ATTR_MARKED_UP_DIFF = "markedUpDiff";

  public static final String ATTR_DIFF_START_REV = "diffStartRev";

  public static final String ATTR_DIFF_END_REV = "diffEndRev";

  /**
   * Because the diff view may be looking at renamed pages.
   */
  public static final String ATTR_DIFF_TITLE = "diffTitle";

  public static final String ATTR_SHOW_REV = "showHeadRev";

  public static final String ATTR_SESSION_ID = "sessionId";

  public static final String ERROR_NO_FILE = "Please browse to a non-empty file to upload.";

  public static final String ERROR_SESSION_EXPIRED = "Your session has expired. Please try again.";

  public static final int MAX_NUMBER_OF_BACKLINKS_TO_DISPLAY = 15;

  private final CachingPageStore _store;

  private final MarkupRenderer _renderer;

  private final WikiGraph _graph;

  private final DiffGenerator _diffGenerator;

  private final WikiUrls _wikiUrls;

  private final FeedWriter _feedWriter;

  private final WikiConfiguration _configuration;

  public DefaultPageImpl(final WikiConfiguration configuration, final CachingPageStore store, final MarkupRenderer renderer, final WikiGraph graph, final DiffGenerator diffGenerator, final WikiUrls wikiUrls, final FeedWriter feedWriter) {
    _configuration = configuration;
    _store = store;
    _renderer = renderer;
    _graph = graph;
    _diffGenerator = diffGenerator;
    _wikiUrls = wikiUrls;
    _feedWriter = feedWriter;
  }

  public View attach(final PageReference page, final ConsumedPath path, final HttpServletRequest request, final HttpServletResponse response) throws Exception {
    if (!ServletFileUpload.isMultipartContent(request)) {
      throw new InvalidInputException("multipart request expected.");
    }
    List<FileItem> items = getFileItems(request);
    try {
      if (items.size() > 4) {
        throw new InvalidInputException("One file at a time.");
      }
      String attachmentName = null;
      Long baseRevision = null;
      String attachmentMessage = null;
      FileItem file = null;
      for (FileItem item : items) {
        if (PARAM_ATTACHMENT_NAME.equals(item.getFieldName())) {
          attachmentName = item.getString().trim();
        }
        if (PARAM_BASE_REVISION.equals(item.getFieldName())) {
          baseRevision = RequestParameterReaders.getLong(item.getString().trim(), PARAM_BASE_REVISION);
        }
        if (PARAM_ATTACHMENT_MESSAGE.equals(item.getFieldName())) {
          attachmentMessage = item.getString().trim();
          if (attachmentMessage.length() == 0) {
            attachmentMessage = null;
          }
        }
        if (!item.isFormField()) {
          file = item;
        }
      }
      if (baseRevision == null) {
        baseRevision = VersionedPageInfo.UNCOMMITTED;
      }

      if (file == null || file.getSize() == 0) {
        request.setAttribute("flash", ERROR_NO_FILE);
        return attachments(page, path, request, response);
      }
      else {
        InputStream in = file.getInputStream();
        try {
          // get the page from store - needed to get content of the special pages which were not saved yet
          PageInfo pageInfo = _store.get(page, -1);
          // IE sends the full file path.
          storeAttachment(pageInfo, attachmentName, baseRevision, attachmentMessage, FilenameUtils.getName(file.getName()), in);
          _store.expire(pageInfo);
          return new RedirectToPageView(_wikiUrls, page, "/attachments/");
        }
        finally {
          IOUtils.closeQuietly(in);
        }
      }
    }
    finally {
      for (FileItem item : items) {
        item.delete();
      }
    }
  }

  @SuppressWarnings("unchecked")
  // commons-upload...
  private List<FileItem> getFileItems(final HttpServletRequest request) throws FileUploadException {
    FileItemFactory factory = new DiskFileItemFactory();
    ServletFileUpload upload = new ServletFileUpload(factory);
    List<FileItem> items = upload.parseRequest(request);
    return items;
  }

  private void storeAttachment(final PageInfo page, final String attachmentName, final long baseRevision, final String attachmentMessage, final String fileName, final InputStream in) throws PageStoreException {
    String storeName = attachmentName;
    if (storeName == null || storeName.length() == 0) {
      storeName = fileName;
    }
    else if (storeName.indexOf('.') == -1) {
      final int indexOfDotInFileName = fileName.indexOf('.');
      if(indexOfDotInFileName!=-1) {
        storeName += fileName.substring(indexOfDotInFileName);
      }
    }
    String operation = baseRevision < 0 ? "Added" : "Updated";
    final String message = attachmentMessage == null ? operation + " attachment " + storeName : attachmentMessage;
    _store.attach(page, storeName, baseRevision, in, message);
  }

  public View deleteAttachment(final PageReference page, final ConsumedPath path, final HttpServletRequest request, final HttpServletResponse response) throws PageStoreAuthenticationException, PageStoreException {
    final String attachmentName = path.next();
    final long baseRevision = -1;
    _store.deleteAttachment(page, attachmentName, baseRevision, "Deleted attachment");
    return new RedirectToPageView(_wikiUrls, page, "/attachments/");
  }

  public View attachment(final PageReference page, final ConsumedPath path, final HttpServletRequest request, final HttpServletResponse response) throws Exception {
    final String attachmentName = path.next();
    return new View() {
      public void render(final HttpServletRequest request, final HttpServletResponse response) throws IOException, ServletException, NotFoundException, PageStoreException, InvalidInputException {
        _store.attachment(page, attachmentName, getRevision(request), new ContentTypedSink() {
          public void setContentType(final String contentType) {
            response.setContentType(contentType);
          }

          public void setFileName(final String name) {
            final String quoteEscapedAttachmentName = attachmentName.replace("\\", "\\\\").replace("\"", "\\\"");
            response.setHeader("Content-Disposition", "inline; filename=\"" + quoteEscapedAttachmentName + "\"");
          }

          public OutputStream stream() throws IOException {
            return response.getOutputStream();
          }
        });
      }
    };
  }

  public View attachments(final PageReference page, final ConsumedPath path, final HttpServletRequest request, final HttpServletResponse response) throws Exception {
    Collection<AttachmentHistory> allAttachments = _store.attachments(page);
    Collection<AttachmentHistory> currentAttachments = new LinkedList<AttachmentHistory>();
    Collection<AttachmentHistory> deletedAttachments = new LinkedList<AttachmentHistory>();
    for (AttachmentHistory history: allAttachments) {
      if (history.isAttachmentDeleted()) {
        deletedAttachments.add(history);
      }
      else {
        currentAttachments.add(history);
      }
    }
    request.setAttribute("currentAttachments", currentAttachments);
    request.setAttribute("deletedAttachments", deletedAttachments);
    return new JspView("Attachments");
  }

  private boolean isLockTokenValid(final VersionedPageInfo pageInfo, final HttpServletRequest request, final boolean preview) throws InvalidInputException {
    final String username = (String) request.getAttribute(RequestAttributes.USERNAME);
    return pageInfo.isNewOrLockedByUser(username) && (!preview || pageInfo.isNewPage() || lockTokenMatches(pageInfo, request));
  }

  private boolean lockTokenMatches(final VersionedPageInfo pageInfo, final HttpServletRequest request) throws InvalidInputException {
    return pageInfo.getLockToken().equals(getRequiredString(request, PARAM_LOCK_TOKEN));
  }

  private boolean isSessionIdValid(final HttpServletRequest request) {
    final String postedSessionId = request.getParameter(PARAM_SESSION_ID);
    final String requestedSessionId = request.getRequestedSessionId();
    return requestedSessionId != null && postedSessionId != null && postedSessionId.equals(requestedSessionId) && request.isRequestedSessionIdValid();
  }

  public View editor(final PageReference page, final ConsumedPath path, final HttpServletRequest request, final HttpServletResponse response) throws Exception {
    final boolean preview = request.getParameter(SUBMIT_PREVIEW) != null;
    VersionedPageInfo pageInfo = _store.getUnderlying().tryToLock(page);
    request.setAttribute(ATTR_PAGE_INFO, pageInfo);
    request.setAttribute(ATTR_ORIGINAL_ATTRIBUTES, pageInfo.getAttributes());
    copySessionIdAsAttribute(request);
    if (!isLockTokenValid(pageInfo, request, preview)) {
      if (preview) {
        return diffEditorView(page, null, request);
      }
      else {
        request.setAttribute("flash", "Could not lock the page.");
        return new JspView("ViewPage");
      }
    }
    else {
      if (preview) {
        if (!isSessionIdValid(request)) {
          return diffEditorView(page, ERROR_SESSION_EXPIRED, request);
        }
        else {
          final String oldContent = pageInfo.getContent();
          final String newContent = getContent(request);
          pageInfo = pageInfo.withAlternativeContent(newContent);
          pageInfo = pageInfo.withAlternativeAttributes(Maps.filterValues(getAttributes(request), new Predicate<String>() {
            public boolean apply(final String value) {
              if (value == null) {
                return false;
              }
              return true;
            }
          }));
          request.setAttribute(ATTR_PAGE_INFO, pageInfo);
          ResultNode rendered = _renderer.render(pageInfo, new ResponseSessionURLOutputFilter(request, response));
          request.setAttribute(ATTR_PREVIEW, rendered.toXHTML());
          request.setAttribute(ATTR_MARKED_UP_DIFF, _diffGenerator.getDiffMarkup(oldContent, newContent));
        }
      }
      return new JspView("EditPage");
    }
  }

  private void copySessionIdAsAttribute(final HttpServletRequest request) {
    request.setAttribute(ATTR_SESSION_ID, request.getSession().getId());
  }

  public View get(final PageReference page, final ConsumedPath path, final HttpServletRequest request, final HttpServletResponse response) throws Exception {
    final String revisionParam = request.getParameter(PARAM_REVISION);
    final String diffParam = request.getParameter(PARAM_DIFF_REVISION);
    addBacklinksInformation(request, page);
    final VersionedPageInfo main = pageInfoFromRevisionParam(page, revisionParam, PARAM_REVISION);
    request.setAttribute(ATTR_PAGE_INFO, main);
    request.setAttribute(ATTR_SHOW_REV, revisionParam != null);
    if (diffParam != null) {
      request.setAttribute(ATTR_DIFF_TITLE, page.getPath());
      final VersionedPageInfo compare = pageInfoFromRevisionParam(page, diffParam, PARAM_DIFF_REVISION);
      request.setAttribute(ATTR_DIFF_END_REV, revisionText(main.getRevision()));
      request.setAttribute(ATTR_DIFF_START_REV, revisionText(compare.getRevision()));
      request.setAttribute(ATTR_MARKED_UP_DIFF, _diffGenerator.getDiffMarkup(compare.getContent(), main.getContent()));
      if (compare.getRevision() > main.getRevision() && main.getRevision() > -1) {
        request.setAttribute("flash", "Note this diff is reversed.");
      }
      return new JspView("ViewDiff");
    }
    else if (ViewTypeConstants.is(request, ViewTypeConstants.CTYPE_RAW)) {
      return new RawPageView(main);
    }
    else {
      ResultNode rendered = _renderer.render(main, new ResponseSessionURLOutputFilter(request, response));
      request.setAttribute(ATTR_RENDERED_CONTENTS, rendered.toXHTML());
      return new JspView("ViewPage");
    }
  }

  private String revisionText(final long revision) {
    if (revision == -1) {
      return "latest";
    }
    else if (revision == -2) {
      return "uncommitted";
    }
    else if (revision == -3) {
      return "deleted";
    }
    else {
      return "r" + revision;
    }
  }

  private void addBacklinksInformation(final HttpServletRequest request, final PageReference page) throws IOException, QuerySyntaxException, PageStoreException {
    List<String> pageNames = new ArrayList<String>(_graph.incomingLinks(page.getPath()));
    Collections.sort(pageNames);
    if (pageNames.size() > MAX_NUMBER_OF_BACKLINKS_TO_DISPLAY) {
      pageNames = pageNames.subList(0, MAX_NUMBER_OF_BACKLINKS_TO_DISPLAY);
      request.setAttribute(ATTR_BACKLINKS_LIMITED, true);
    }
    request.setAttribute(ATTR_BACKLINKS, pageNames);
  }

  public View history(final PageReference page, final ConsumedPath path, final HttpServletRequest request, final HttpServletResponse response) throws Exception {
    List<ChangeInfo> changes = _store.history(page);
    if (ViewTypeConstants.is(request, CTYPE_ATOM)) {
      final String feedUrl = _wikiUrls.page(null, page.getName()) + "?history&ctype=atom";
      return new FeedView(_configuration, _feedWriter, changes, feedUrl);
    }
    request.setAttribute("changes", changes);
    return new JspView("History");
  }

  public View set(final PageReference page, final ConsumedPath path, final HttpServletRequest request, final HttpServletResponse response) throws Exception {
    final boolean hasSaveParam = request.getParameter(SUBMIT_SAVE) != null;
    final boolean hasCopyParam = request.getParameter(SUBMIT_COPY) != null;
    final boolean hasRenameParam = request.getParameter(SUBMIT_RENAME) != null;
    final boolean hasUnlockParam = request.getParameter(SUBMIT_UNLOCK) != null;
    if (!(hasSaveParam ^ hasCopyParam ^ hasRenameParam ^ hasUnlockParam)) {
      throw new InvalidInputException("Exactly one action must be specified.");
    }

    if (hasSaveParam) {
      copySessionIdAsAttribute(request);
      if (!isSessionIdValid(request)) {
        return diffEditorView(page, ERROR_SESSION_EXPIRED, request);
      }
      String lockToken = getRequiredString(request, PARAM_LOCK_TOKEN);
      if ("".equals(lockToken)) {
        lockToken = null;
      }
      String content = getContent(request);
      Map<String, String> attributes = getAttributes(request);
      try {
        _store.set(new PageInfoImpl(_store.getWiki(), page.getPath(), content, attributes), lockToken, getBaseRevision(request), createLinkingCommitMessage(page, request));
      }
      catch (LostLockException e) {
        return diffEditorView(page, null, request);
      }
      catch (ConflictException e) {
        return diffEditorView(page, "Page has been updated since you started. Please do not save unless you feel that you can merge the changes.", request);
      }
    }
    else if (hasCopyParam) {
      final String fromPage = getString(request, PARAM_FROM_PAGE);
      final String toPage = getString(request, PARAM_TO_PAGE);
      if (!(fromPage == null ^ toPage == null)) {
        throw new InvalidInputException("'copy' requires one of toPage, fromPage");
      }
      final PageReference toRef;
      final PageReference fromRef;
      if (fromPage != null) {
        fromRef = new PageReferenceImpl(fromPage);
        toRef = page;
      }
      else {
        fromRef = page;
        toRef = new PageReferenceImpl(toPage);
      }
      _store.copy(fromRef, -1, toRef, createLinkingCommitMessage(toRef, request));
      return new RedirectToPageView(_wikiUrls, toRef);
    }
    else if (hasRenameParam) {
      final PageReference toPage = new PageReferenceImpl(getRequiredString(request, PARAM_TO_PAGE));
      try {
        _store.rename(page, toPage, -1, createLinkingCommitMessage(toPage, request));
      }
      catch (RenameException ex) {
        VersionedPageInfo pageInfo = _store.getUnderlying().tryToLock(page);
        String message;
        if (pageInfo.isLocked()) {
          message = "Cannot rename while page is locked. Page was locked by ";
          if (pageInfo.isNewOrLockedByUser((String) request.getAttribute(RequestAttributes.USERNAME))) {
            message += "you ";
          }
          else {
            message += pageInfo.getLockedBy();
          }
          message += " on " + pageInfo.getLockedSince() + ".";
        }
        else {
          message = "Rename failed";
        }
        pageInfo = pageInfo.withoutLockToken();
        request.setAttribute(ATTR_PAGE_INFO, pageInfo);
        request.setAttribute("flash", message);
        return new JspView("Rename");
      }
      return new RedirectToPageView(_wikiUrls, toPage);
    }
    else if (hasUnlockParam) {
      String lockToken = request.getParameter(PARAM_LOCK_TOKEN);
      // New pages don't have a lock.
      if (lockToken != null && lockToken.length() > 0) {
        _store.unlock(page, lockToken);
      }
    }
    else {
      throw new InvalidInputException("No action specified.");
    }
    return new RedirectToPageView(_wikiUrls, page);
  }

  private View diffEditorView(final PageReference page, final String customMessage, final HttpServletRequest request) throws PageStoreException, InvalidInputException {
    VersionedPageInfo pageInfo = _store.getUnderlying().tryToLock(page);
    String message;
    final String content = getContent(request);
    final String newContent = pageInfo.getContent();
    pageInfo = pageInfo.withAlternativeContent(content);
    request.setAttribute(ATTR_MARKED_UP_DIFF, _diffGenerator.getDiffMarkup(newContent, content));
    if (pageInfo.isLocked() && !pageInfo.isNewOrLockedByUser((String) request.getAttribute(RequestAttributes.USERNAME))) {
      message = "Page was locked by " + pageInfo.getLockedBy() + " on " + pageInfo.getLockedSince() + ".";
      pageInfo = pageInfo.withoutLockToken();
    }
    else {
      message = "Lock was lost!";
    }
    if (customMessage != null) {
      message = customMessage;
    }
    request.setAttribute(ATTR_ORIGINAL_ATTRIBUTES, pageInfo.getAttributes());
    pageInfo = pageInfo.withAlternativeAttributes(Maps.filterValues(getAttributes(request), new Predicate<String>() {
      public boolean apply(final String value) {
        if (value == null) {
          return false;
        }
        return true;
      }
    }));
    request.setAttribute(ATTR_PAGE_INFO, pageInfo);
    request.setAttribute("flash", message);
    return new JspView("EditPage");
  }

  private String getContent(final HttpServletRequest request) throws InvalidInputException {
    String content = getRequiredString(request, PARAM_CONTENT);
    content = content + Strings.CRLF;
    return content;
  }

  private Map<String, String> getAttributes(final HttpServletRequest request) throws InvalidInputException {
    String attributesString = getRequiredString(request, PARAM_ATTRIBUTES).trim();
    String[] lines = attributesString.split("\n");
    String regexp = "(\\\")?([^\\\"]+)(\\\")?(\\s)*(=)(\\s)*(\\\")?([^\\\"]+)(\\\")?";
    Pattern pattern = Pattern.compile(regexp);
    Matcher matcher = null;
    String attrPart = null;
    String valuePart = null;
    Map<String, String> attributes = new LinkedHashMap<String, String>();
    for(String line: lines){
      line = line.trim();
      matcher = pattern.matcher(line);
      if (matcher.find()) {
        attrPart = matcher.group(2);
        valuePart = matcher.group(8);
        attributes.put(attrPart.trim(), valuePart.trim());
      }
    }
    String originalAttributesString = getRequiredString(request, PARAM_ORIGINAL_ATTRIBUTES).trim();
    String[] originalAttrs = originalAttributesString.split("\n");
    for(String attr: originalAttrs) {
      attr = attr.trim();
      if (attr.length() > 0 && !attributes.containsKey(attr)) {
        attributes.put(attr, null);
      }
    }
    return attributes;
  }

  private Long getBaseRevision(final HttpServletRequest request) throws InvalidInputException {
    return getLong(getRequiredString(request, PARAM_BASE_REVISION), PARAM_BASE_REVISION);
  }

  String createLinkingCommitMessage(final PageReference page, final HttpServletRequest request) {
    boolean minorEdit = request.getParameter(PARAM_MINOR_EDIT) != null;
    String commitMessage = request.getParameter(PARAM_COMMIT_MESSAGE);
    if (commitMessage == null || commitMessage.trim().length() == 0) {
      commitMessage = ChangeInfo.NO_COMMENT_MESSAGE_TAG;
    }
    return (minorEdit ? ChangeInfo.MINOR_EDIT_MESSAGE_TAG : "") + commitMessage + "\n" + _wikiUrls.page(null, page.getName());
  }

  private VersionedPageInfo pageInfoFromRevisionParam(final PageReference defaultPage, final String revisionText, final String paramName) throws InvalidInputException, PageStoreException {
    final PageRevisionReference reference = getPageRevisionReference(defaultPage, revisionText, paramName);
    return _store.get(reference.getPage(), reference.getRevision());
  }

  /**
   * Gets the PageInfo from a diff or rev parameter. I.e. "1234" or "PageName.1234"
   * @param defaultPage The page (used when the parameter does not override the page).
   * @param revisionText The parameter value.
   * @param paramName The name of the parameter for error messages.
   * @return The PageInfo.
   * @throws InvalidInputException When the parameter value is not of the correct form.
   * @throws PageStoreException
   */
  PageRevisionReference getPageRevisionReference(final PageReference defaultPage, final String revisionText, final String paramName) throws InvalidInputException {
    if (revisionText == null) {
      return new PageRevisionReference(defaultPage, -1);
    }

    final int dotIndex = revisionText.lastIndexOf('.');
    if (dotIndex != -1) {
      final Long revision = getLong(revisionText.substring(dotIndex + 1), paramName);
      final String pageName = revisionText.substring(0, dotIndex);
      final PageReference pageReference = pageName.length() > 0 ? new PageReferenceImpl(pageName) : defaultPage;
      return new PageRevisionReference(pageReference, revision);
    }
    return new PageRevisionReference(defaultPage, getLong(revisionText, paramName));
  }

  private static long getRevision(final HttpServletRequest request) throws InvalidInputException {
    Long givenRevision = getLong(request.getParameter(PARAM_REVISION), PARAM_REVISION);
    return givenRevision == null ? -1 : givenRevision;
  }

}
