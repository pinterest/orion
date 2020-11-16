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

import software.amazon.awssdk.regions.internal.util.EC2MetadataUtils;

import java.util.HashMap;
import java.util.Map;

import com.pinterest.orion.utils.OrionConstants;

public class MetadataUtils {

  public static Map<String, String> getEC2Metadata() {
    Map<String, String> ret = new HashMap<>();
    EC2MetadataUtils.InstanceInfo instanceInfo = EC2MetadataUtils.getInstanceInfo();
    if (instanceInfo != null) {
      ret.put(OrionConstants.INSTANCE_ID, instanceInfo.getInstanceId());
      ret.put(OrionConstants.INSTANCE_TYPE, instanceInfo.getInstanceType());
      ret.put(OrionConstants.IMAGE_ID, instanceInfo.getImageId());
      ret.put(OrionConstants.REGION, instanceInfo.getRegion());
      ret.put(OrionConstants.AVAILABILITY_ZONE, instanceInfo.getAvailabilityZone());
      ret.put(OrionConstants.USERDATA, EC2MetadataUtils.getUserData());
    }
    return ret;
  }
}
