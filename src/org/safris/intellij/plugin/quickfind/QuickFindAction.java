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

import com.intellij.find.FindManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.editor.event.CaretEvent;
import com.intellij.openapi.editor.event.CaretListener;
import com.intellij.openapi.editor.event.SelectionEvent;
import com.intellij.openapi.editor.event.SelectionListener;
import com.intellij.openapi.project.Project;

abstract class QuickFindAction extends AnAction {
  private static void invokeAction(final AnActionEvent e, final String navigationActionId) {
    e.getActionManager().getAction(navigationActionId).actionPerformed(e);
  }

  private static void setSelectionToCaret(final SelectionModel selectionModel, final Caret caret) {
    selectionModel.setSelection(caret.getOffset() - currentSelection.length(), caret.getOffset());
  }

  private static volatile AnAction currentAction;
  private static volatile String currentSelection;
  private static SelectionListener selectionListener;

  private final String actionId;
  private CaretListener caretListener;
  private static boolean inCaret = false;

  QuickFindAction(final String actionId) {
    this.actionId = actionId;
  }

  @Override
  public void actionPerformed(final AnActionEvent e) {
    final Editor editor = e.getData(PlatformDataKeys.EDITOR_EVEN_IF_INACTIVE);
    if (editor == null)
      return;

    final SelectionModel selectionModel = editor.getSelectionModel();
    if (selectionModel.getSelectionStart() == selectionModel.getSelectionEnd()) {
      invokeAction(e, actionId);
      return;
    }

    final Project project = e.getProject();
    if (project == null)
      return;

    if (caretListener == null) {
      editor.getCaretModel().addCaretListener(caretListener = new CaretListener() {
        @Override
        public void caretPositionChanged(final CaretEvent event) {
//          System.err.println("caretPositionChanged(from: " + event.getOldPosition() + ", to: " + event.getNewPosition() + ", selectionStart: " + selectionModel.getSelectionStart() + ")");
          if (currentAction != QuickFindAction.this)
            return;

          inCaret = true;
          setSelectionToCaret(selectionModel, event.getCaret());
          inCaret = false;
        }
      });
    }

    if (selectionListener == null) {
      selectionModel.addSelectionListener(selectionListener = new SelectionListener() {
        @Override
        public void selectionChanged(final SelectionEvent se) {
          if (currentAction == null || !inCaret)
            return;

          final CaretModel caretModel = editor.getCaretModel();
//          System.err.println("Selection offset: " + selectionModel.getLeadSelectionOffset() + ", Caret offset: " + caretModel.getOffset());
          currentAction = null;
          if (selectionModel.hasSelection() && caretModel.isUpToDate() && caretModel.getOffset() != selectionModel.getSelectionStart() && caretModel.getOffset() != selectionModel.getSelectionEnd())
            setSelectionToCaret(selectionModel, caretModel.getCurrentCaret());
        }
      });
    }

    // This is necessary to allow for the first action to actually move the cursor+selection
    FindManager.getInstance(project).setFindWasPerformed();

    final String selection = selectionModel.getSelectedText();
//    System.err.println("actionPerformed(" + selectionModel.getSelectedText() + ", " + selectionModel.getSelectionEnd() + "): \"" + selection + "\"");
    if (currentSelection == null || !currentSelection.equals(selection)) {
      currentSelection = selection;

      invokeAction(e, IdeActions.ACTION_FIND);
      invokeAction(e, IdeActions.ACTION_HIGHLIGHT_USAGES_IN_FILE);
    }

    currentAction = this;
    invokeAction(e, actionId);
  }
}