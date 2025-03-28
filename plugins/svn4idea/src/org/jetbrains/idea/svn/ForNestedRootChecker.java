// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.idea.svn;

import com.intellij.openapi.vcs.impl.VcsRootIterator;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.svn.api.Revision;
import org.jetbrains.idea.svn.api.Url;
import org.jetbrains.idea.svn.commandLine.SvnBindException;
import org.jetbrains.idea.svn.info.Info;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import static com.intellij.openapi.progress.ProgressManager.checkCanceled;

public class ForNestedRootChecker {

  private final @NotNull SvnVcs myVcs;
  private final @NotNull VcsRootIterator myRootIterator;

  public ForNestedRootChecker(@NotNull SvnVcs vcs) {
    myVcs = vcs;
    myRootIterator = new VcsRootIterator(vcs.getProject(), vcs);
  }

  public @NotNull List<Node> getAllNestedWorkingCopies(@NotNull VirtualFile root) {
    LinkedList<Node> result = new LinkedList<>();
    LinkedList<VirtualFile> workItems = new LinkedList<>();

    workItems.add(root);
    while (!workItems.isEmpty()) {
      VirtualFile item = workItems.removeFirst();
      checkCanceled();

      final Node vcsElement = new VcsFileResolver(myVcs, item, root).resolve();
      if (vcsElement != null) {
        result.add(vcsElement);
      }
      else {
        for (VirtualFile child : item.getChildren()) {
          checkCanceled();

          if (child.isDirectory() && myRootIterator.acceptFolderUnderVcs(root, child)) {
            workItems.add(child);
          }
        }
      }
    }
    return result;
  }

  private static final class VcsFileResolver {

    private final @NotNull SvnVcs myVcs;
    private final @NotNull VirtualFile myFile;
    private final @NotNull File myIoFile;
    private final @NotNull VirtualFile myRoot;
    private @Nullable Info myInfo;
    private @Nullable SvnBindException myError;

    private VcsFileResolver(@NotNull SvnVcs vcs, @NotNull VirtualFile file, @NotNull VirtualFile root) {
      myVcs = vcs;
      myFile = file;
      myIoFile = VfsUtilCore.virtualToIoFile(file);
      myRoot = root;
    }

    public @Nullable Node resolve() {
      runInfo();

      return processInfo();
    }

    private void runInfo() {
      if (isRoot() || hasChildAdminDirectory()) {
        try {
          myInfo = myVcs.getFactory(myIoFile, false).createInfoClient().doInfo(myIoFile, Revision.UNDEFINED);
        }
        catch (SvnBindException e) {
          myError = e;
        }
      }
    }

    @SuppressWarnings("UseVirtualFileEquals")
    private boolean isRoot() {
      return myRoot == myFile;
    }

    private boolean hasChildAdminDirectory() {
      return myFile.findChild(SvnUtil.SVN_ADMIN_DIR_NAME) != null;
    }

    private @Nullable Node processInfo() {
      Node result = null;

      if (myError != null) {
        if (!SvnUtil.isUnversionedOrNotFound(myError)) {
          // error code does not indicate that myFile is unversioned or path is invalid => create result, but indicate error
          result = new Node(myFile, Url.EMPTY, Url.EMPTY, myError);
        }
      }
      else if (myInfo != null && myInfo.getRepositoryRootUrl() != null && myInfo.getUrl() != null) {
        result = new Node(myFile, myInfo.getUrl(), myInfo.getRepositoryRootUrl());
      }

      return result;
    }
  }
}
