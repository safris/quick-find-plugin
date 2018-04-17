/* Copyright (c) 2018 Seva Safris
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * You should have received a copy of The MIT License (MIT) along with this
 * program. If not, see <http://opensource.org/licenses/MIT/>.
 */

package org.safris.intellij.plugin.quickfind;

import com.intellij.codeInsight.highlighting.HighlightManager;
import com.intellij.codeInsight.highlighting.HighlightManagerImpl;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.editor.event.SelectionEvent;
import com.intellij.openapi.editor.event.SelectionListener;
import com.intellij.openapi.project.Project;

abstract class QuickFindAction extends AnAction {
  private static void invokeAction(final AnActionEvent e, final String navigationActionId) {
    e.getActionManager().getAction(navigationActionId).actionPerformed(e);
  }

  private final String navigationActionId;
  private SelectionListener selectionListener;
  private int start;
  private int end;

  public QuickFindAction(final String navigationActionId) {
    this.navigationActionId = navigationActionId;
  }

  @Override
  public void actionPerformed(final AnActionEvent e) {
    final Editor editor = e.getData(PlatformDataKeys.EDITOR_EVEN_IF_INACTIVE);
    if (editor == null)
      return;

    final SelectionModel selectionModel = editor.getSelectionModel();
    if (selectionModel.getSelectionStart() == selectionModel.getSelectionEnd())
      return;

    if (selectionListener == null) {
      selectionModel.addSelectionListener(selectionListener = new SelectionListener() {
        @Override
        public void selectionChanged(final SelectionEvent se) {
          if (se.getOldRange().getStartOffset() == start && se.getOldRange().getEndOffset() == end && se.getNewRange().getLength() == 0 && (se.getOldRange().getStartOffset() == se.getNewRange().getStartOffset() || se.getOldRange().getEndOffset() == se.getNewRange().getEndOffset())) {
            selectionModel.setSelection(start, end);
            start = -1;
            end = -1;
          }
        }
      });
    }

    final Project project = e.getProject();
    if (project == null)
      return;

    start = selectionModel.getSelectionStart();
    end = selectionModel.getSelectionEnd();

    ((HighlightManagerImpl)HighlightManager.getInstance(project)).hideHighlights(editor, HighlightManager.HIDE_BY_ESCAPE | HighlightManager.HIDE_BY_ANY_KEY);
    invokeAction(e, IdeActions.ACTION_EDITOR_ESCAPE);
    invokeAction(e, IdeActions.ACTION_HIGHLIGHT_USAGES_IN_FILE);
    invokeAction(e, navigationActionId);
  }
}