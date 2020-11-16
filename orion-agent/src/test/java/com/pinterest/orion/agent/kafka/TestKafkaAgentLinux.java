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
package com.pinterest.orion.agent.kafka;

import org.apache.commons.lang3.SystemUtils;
import org.junit.Assume;
import org.junit.Before;

public class TestKafkaAgentLinux {

  @Before
  public void isLinux() {
    Assume.assumeTrue(SystemUtils.IS_OS_LINUX);
  }


}
