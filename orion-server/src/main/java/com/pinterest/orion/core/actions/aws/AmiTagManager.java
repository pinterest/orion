/*******************************************************************************
 * Copyright 2024 Pinterest, Inc.
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
package com.pinterest.orion.core.actions.aws;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.function.UnaryOperator;

import com.pinterest.orion.server.api.Ami;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeImagesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeImagesResponse;
import software.amazon.awssdk.services.ec2.model.CreateTagsRequest;
import software.amazon.awssdk.services.ec2.model.CreateTagsResponse;
import software.amazon.awssdk.services.ec2.model.Filter;
import software.amazon.awssdk.services.ec2.model.Tag;

public class AmiTagManager {
  private static final Logger logger = Logger.getLogger(AmiTagManager.class.getCanonicalName());
  private Ec2Client ec2Client;
  public static String KEY_APPLICATION = "application";
  public static String KEY_RELEASE = "release";
  public static String KEY_CPU_ARCHITECTURE = "cpu_architecture";
  public static String KEY_APPLICATION_ENVIRONMENT = "application_environment";
  public static String VALUE_KAFKA = "kafka";
  UnaryOperator<String> tag = key -> "tag:" + key;

  public AmiTagManager() {
    ec2Client = Ec2Client.create();
  }

  public List<Ami> getAmiList(Map<String, String> filter) {
    List<Ami> amiList = new ArrayList<>();
    DescribeImagesRequest.Builder builder = DescribeImagesRequest.builder();
    builder = builder.filters(
      Filter.builder().name(tag.apply(KEY_APPLICATION)).values(VALUE_KAFKA).build()
    );
    if (filter.containsKey(KEY_RELEASE))
      builder = builder.filters(
        Filter.builder().name(tag.apply(KEY_RELEASE)).values(filter.get(KEY_RELEASE)).build()
      );
    if (filter.containsKey(KEY_CPU_ARCHITECTURE))
      builder = builder.filters(
        Filter.builder().name(tag.apply(KEY_CPU_ARCHITECTURE)).values(filter.get(KEY_CPU_ARCHITECTURE)).build()
      );
    builder = builder.filters(
      Filter.builder().name(tag.apply(KEY_APPLICATION_ENVIRONMENT)).values("*").build()
    );
    try {
      DescribeImagesResponse resp = ec2Client.describeImages(builder.build());
      if (resp.hasImages() && !resp.images().isEmpty()) {
        ZonedDateTime cutDate = ZonedDateTime.now().minusDays(180);
        resp.images().forEach(image -> {
          if (ZonedDateTime.parse(image.creationDate(), DateTimeFormatter.ISO_ZONED_DATE_TIME).isAfter(cutDate)) {
            Iterator<Tag> i = image.tags().iterator();
            Tag t;
            String appEnvTag = null;
            while (i.hasNext()) {
              t = i.next();
              if (t.key().equals(KEY_APPLICATION_ENVIRONMENT)) {
                appEnvTag = t.value();
                break;
              }
            }
            amiList.add(new Ami(
              image.imageId(),
              appEnvTag,
              image.creationDate()
            ));
          }
        });
        amiList.sort((a, b) -> - ZonedDateTime.parse(a.getCreationDate(), DateTimeFormatter.ISO_ZONED_DATE_TIME)
        .compareTo(ZonedDateTime.parse(b.getCreationDate(), DateTimeFormatter.ISO_ZONED_DATE_TIME)));
      }
    } catch (Exception e) {
      logger.log(Level.SEVERE, "AmiTagManager: could not retrieve AMI list", e);
      throw e;
    }
    return amiList;
  }

  public void updateAmiTag(String amiId, String applicationEnvironment) {
    CreateTagsRequest.Builder builder = CreateTagsRequest.builder();
    Tag newAppEnv = Tag.builder()
        .key(KEY_APPLICATION_ENVIRONMENT)
        .value(applicationEnvironment).build();
    CreateTagsRequest request = builder
        .resources(amiId)
        .tags(newAppEnv)
        .build();
    CreateTagsResponse resp;
    try {
      resp = ec2Client.createTags(request);
      if (!resp.sdkHttpResponse().isSuccessful())
        throw AwsServiceException.builder().message("Http code \" + resp.sdkHttpResponse().statusCode() + \" received").build();
    } catch (Exception e) {
      logger.severe("AmiTagManager: tag update failed for " + amiId + " and application_environment tag = " + applicationEnvironment + ", " + e);
      throw e;
    }
  }
}
