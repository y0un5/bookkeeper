/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */

package org.apache.bookkeeper.test;

import java.io.File;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import static org.junit.Assert.*;

import org.apache.bookkeeper.client.BookKeeper.DigestType;
import java.util.HashSet;
import junit.framework.TestCase;
import org.apache.bookkeeper.proto.BookieServer;
import org.apache.bookkeeper.bookie.Bookie;

public class BookieZKExpireTest extends BaseTestCase {

    public BookieZKExpireTest(DigestType digestType) {
        super(0);
    }

    @Test
    public void runBookieServerZKExpireBehaviourTest() throws Exception {
        BookieServer server = null;
        try {
            File f = File.createTempFile("bookieserver", "test");
            f.delete();
            f.mkdir();
            
            HashSet<Thread> threadset = new HashSet<Thread>();
            int threadCount = Thread.activeCount();
            Thread threads[] = new Thread[threadCount*2];
            threadCount = Thread.enumerate(threads);
            for(int i = 0; i < threadCount; i++) {
                if (threads[i].getName().indexOf("SendThread") != -1) {
                    threadset.add(threads[i]);
                }
            }
            
            server = new BookieServer(initialPort + 1, HOSTPORT, f, new File[] { f });
            server.start();
            
            Thread.sleep(10);
            Thread sendthread = null;
            threadCount = Thread.activeCount();
            threads = new Thread[threadCount*2];
            threadCount = Thread.enumerate(threads);
            for(int i = 0; i < threadCount; i++) {
                if (threads[i].getName().indexOf("SendThread") != -1
                    && !threadset.contains(threads[i])) {
                    sendthread = threads[i];
                    break;
                }
            }
            assertNotNull("Send thread not found", sendthread);
            
            sendthread.suspend();
            Thread.sleep(2*10000);
            sendthread.resume();
            
            // allow watcher thread to run
            Thread.sleep(3000);
            assertFalse("Bookie should have shutdown on losing zk session", server.isBookieRunning());
            assertFalse("Nio Server should have shutdown on losing zk session", server.isNioServerRunning());
            assertFalse("Bookie Server should have shutdown on losing zk session", server.isRunning());
        } finally {
            server.shutdown();
        }
    }
}