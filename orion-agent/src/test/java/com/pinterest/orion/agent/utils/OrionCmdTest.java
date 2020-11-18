/*******************************************************************************
 * Copyright 2020 Pinterest, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.pinterest.orion.agent.utils;

import static org.junit.Assert.*;

import org.junit.Test;

import com.pinterest.orion.agent.utils.OrionCmd;
import com.pinterest.orion.common.CmdResult;

import java.io.IOException;
import java.util.Date;

public class OrionCmdTest {

  private static String[] EXCEPTION_CMD = new String[]{"Test_cmd_that_will_return_IOException"};
  private static String[] NONZERO_CMD = new String[]{"false"};
  private static String[] GOOD_CMD = new String[]{"printf", "1"};

  @Test
  public void getCmd() {
    CmdResult result = new CmdResult();
    OrionCmd cmd = new OrionCmd(result, "target", "exception", EXCEPTION_CMD);
    assertArrayEquals(EXCEPTION_CMD, cmd.getCmd());
  }

  @Test
  public void exec() {
    CmdResult result = new CmdResult();
    OrionCmd exceptionCmd = new OrionCmd(result, "target", "exception1", EXCEPTION_CMD);
    try {
      exceptionCmd.exec().get();
      fail("Should throw IOException");
    } catch (IOException e) {

    } catch (Exception e) {
      fail("Should throw IOException");
    }
    result = new CmdResult();
    OrionCmd badCmd = new OrionCmd(result, "target", "nonzero", NONZERO_CMD);
    try {
      CmdResult r = badCmd.exec().get();
      assertNotEquals(0, r.getExitCode());
    } catch (Exception e) {
      e.printStackTrace();
      fail("Should not throw an exception but return nonzero status here");
    }
    result = new CmdResult();
    OrionCmd goodCmd = new OrionCmd(result, "target", "good", GOOD_CMD);
    try {
      CmdResult r = goodCmd.exec().get();
      assertEquals(0, r.getExitCode());
      assertEquals(GOOD_CMD[1], r.getOut());
    } catch (Exception e) {
      fail("Should not throw an exception but return nonzero status here");
    }
  }

  @Test
  public void processResult() throws Exception {
    CmdResult result = new CmdResult();
    OrionCmd cmd1 =
        new OrionCmd(result, "target", "testParsing", s -> s.startsWith("5") ? s : null, null,
            "echo", "1 2 3 4 \n5 6 7 8");
    cmd1.exec();
    assertEquals("5 6 7 8", cmd1.get().getOut());
  }
}