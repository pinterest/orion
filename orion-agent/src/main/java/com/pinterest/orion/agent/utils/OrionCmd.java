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

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Stream;

import com.google.common.io.Files;
import com.pinterest.orion.common.CmdResult;
import com.pinterest.orion.common.CmdResult.CmdState;

public class OrionCmd implements Future<CmdResult> {

  private static final Charset UTF8 = StandardCharsets.UTF_8;
  private Function<String, String> outputFunction;
  private Function<String, String> errorFunction;
  private String[] cmd;
  private File file;
  private CmdResult result;
  private Process proc;
  private File error;
  private File output;
  private int waitTimeout = Integer.MAX_VALUE;

  public OrionCmd(CmdResult result, String outputDir, String cmdId,
                   Function<String, String> outputFunction, Function<String, String> errorFunction,
                   String... cmd) {
    this.result = result;
    this.cmd = cmd;
    this.outputFunction = outputFunction;
    this.errorFunction = errorFunction;
    file = new File(outputDir + "/" + cmdId);
    file.mkdirs();
  }

  public OrionCmd(CmdResult result, String outputDir, String cmdId,
                   String... cmd) {
    this(result, outputDir, cmdId, null, null, cmd);
  }

  public String[] getCmd() {
    return cmd;
  }

  public OrionCmd exec() throws IOException, InterruptedException {
    if (cmd == null) {
      return null;
    }
    ProcessBuilder pb = new ProcessBuilder(cmd);
    error = new File(file, "error.txt");
    output = new File(file, "output.txt");
    pb.redirectError(error);
    pb.redirectOutput(output);
    proc = pb.start();
    result.setState(CmdState.RUNNING);
    return this;
  }

  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    if (proc == null) {
      cmd = null;
    } else if (mayInterruptIfRunning) {
      proc.destroy();
      return true;
    }
    return false;
  }

  @Override
  public boolean isCancelled() {
    return cmd == null;
  }

  @Override
  public boolean isDone() {
    return !proc.isAlive();
  }

  @Override
  public CmdResult get() throws InterruptedException, ExecutionException {
    proc.waitFor(waitTimeout, TimeUnit.SECONDS);
    try {
      updateResult();
    } catch (IOException e) {
      throw new ExecutionException(e);
    }
    return result;
  }

  @Override
  public CmdResult get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException,
                                                           TimeoutException {
    long endTime = System.currentTimeMillis() + unit.toMillis(timeout);
    long ts = System.currentTimeMillis();
    while (ts < endTime) {
      try {
        if (updateResult()) {
          return result;
        }
        Thread.sleep(1000);
        ts = System.currentTimeMillis();
      } catch (IOException e) {
        throw new ExecutionException(e);
      }
    }
    proc.destroyForcibly();
    throw new TimeoutException();
  }

  public CmdResult getResult() {
    return result;
  }
  
  protected boolean updateResult() throws IOException {
    if (outputFunction != null) {
      result.setOut(processResult(output, outputFunction));
    } else {
      result.setOut(Files.asCharSource(output, UTF8).read());
    }

    if (errorFunction != null) {
      result.setErr(processResult(error, errorFunction));
    } else {
      result.setErr(Files.asCharSource(error, UTF8).read());
    }

    if (!proc.isAlive()) {
      result.setExitCode((short) proc.exitValue());
      result.setState(CmdState.COMPLETED);
      return true;
    } else {
      return false;
    }
  }

  public static String processResult(File file, Function<String, String> function)
      throws IOException {
    try (Stream<String> stream = java.nio.file.Files.lines(file.toPath())) {
      Iterator<String> iterator = stream.iterator();
      while (iterator.hasNext()) {
        String line = iterator.next();
        String output = function.apply(line);
        if (output != null) {
          return output;
        }
      }
    }
    return null;
  }

  public void setWaitTimeout(int waitTimeout) {
    this.waitTimeout = waitTimeout;
  }
}
