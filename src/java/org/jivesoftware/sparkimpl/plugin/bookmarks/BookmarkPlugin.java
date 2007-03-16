/**
 * $Revision: $
 * $Date: $
 *
 * Copyright (C) 2006 Jive Software. All rights reserved.
 *
 * This software is published under the terms of the GNU Lesser Public License (LGPL),
 * a copy of which is included in this distribution.
 */

package org.jivesoftware.sparkimpl.plugin.bookmarks;

import org.jivesoftware.resource.Res;
import org.jivesoftware.resource.SparkRes;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.PrivateDataManager;
import org.jivesoftware.smackx.bookmark.BookmarkedConference;
import org.jivesoftware.smackx.bookmark.BookmarkedURL;
import org.jivesoftware.smackx.bookmark.Bookmarks;
import org.jivesoftware.spark.SparkManager;
import org.jivesoftware.spark.plugin.Plugin;
import org.jivesoftware.spark.ui.conferences.ConferenceUtils;
import org.jivesoftware.spark.util.BrowserLauncher;
import org.jivesoftware.spark.util.SwingTimerTask;
import org.jivesoftware.spark.util.SwingWorker;
import org.jivesoftware.spark.util.TaskEngine;
import org.jivesoftware.spark.util.log.Log;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.TimerTask;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenu;

/**
 * Allows for adding and removal of Bookmarks within Spark.
 */
public class BookmarkPlugin implements Plugin {

    public void initialize() {
        final SwingWorker bookmarkThreadWorker = new SwingWorker() {
            public Object construct() {
                // Register own provider for simpler implementation.
                PrivateDataManager.addPrivateDataProvider("storage", "storage:bookmarks", new Bookmarks.Provider());
                PrivateDataManager manager = new PrivateDataManager(SparkManager.getConnection());
                Bookmarks bookmarks = null;
                try {
                    bookmarks = (Bookmarks)manager.getPrivateData("storage", "storage:bookmarks");
                }
                catch (XMPPException e) {
                    Log.error(e);
                }
                return bookmarks;

            }

            public void finished() {
                final Bookmarks bookmarks = (Bookmarks)get();

                final JMenu bookmarkMenu = new JMenu("Bookmarks");

                if (bookmarks != null) {
                    bookmarkMenu.setToolTipText(Res.getString("title.view.bookmarks"));
                    SparkManager.getWorkspace().getStatusBar().invalidate();
                    SparkManager.getWorkspace().getStatusBar().validate();
                    SparkManager.getWorkspace().getStatusBar().repaint();


                    Collection bookmarkedConferences = bookmarks.getBookmarkedConferences();
                    final Collection bookmarkedLinks = bookmarks.getBookmarkedURLS();

                    final Iterator bookmarkLinks = bookmarkedLinks.iterator();
                    while (bookmarkLinks.hasNext()) {
                        final BookmarkedURL link = (BookmarkedURL)bookmarkLinks.next();

                        Action urlAction = new AbstractAction() {
                            public void actionPerformed(ActionEvent actionEvent) {
                                try {
                                    BrowserLauncher.openURL(link.getURL());
                                }
                                catch (IOException e) {
                                    Log.error(e);
                                }
                            }
                        };

                        urlAction.putValue(Action.NAME, link.getName());
                        urlAction.putValue(Action.SMALL_ICON, SparkRes.getImageIcon(SparkRes.LINK_16x16));
                        bookmarkMenu.add(urlAction);
                    }


                    final Iterator bookmarkConferences = bookmarkedConferences.iterator();
                    while (bookmarkConferences.hasNext()) {
                        final BookmarkedConference conferences = (BookmarkedConference)bookmarkConferences.next();

                        Action conferenceAction = new AbstractAction() {
                            public void actionPerformed(ActionEvent actionEvent) {
                                final TimerTask task = new SwingTimerTask() {
                                    public void doRun() {
                                        ConferenceUtils.joinConferenceOnSeperateThread(conferences.getName(), conferences.getJid(), conferences.getPassword());
                                    }
                                };

                                TaskEngine.getInstance().schedule(task, 10);
                            }
                        };

                        conferenceAction.putValue(Action.NAME, conferences.getName());
                        conferenceAction.putValue(Action.SMALL_ICON, SparkRes.getImageIcon(SparkRes.CONFERENCE_IMAGE_16x16));
                        bookmarkMenu.add(conferenceAction);
                    }
                }

                if (bookmarkMenu.getMenuComponentCount() > 0) {
                    int menuCount = SparkManager.getMainWindow().getMenu().getMenuCount();
                    SparkManager.getMainWindow().getMenu().add(bookmarkMenu, menuCount - 1);
                }
            }
        };

        bookmarkThreadWorker.start();

    }

    public void shutdown() {
    }

    public boolean canShutDown() {
        return false;
    }

    public void uninstall() {
    }
}
