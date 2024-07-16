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
import java.util.function.Function;
import java.util.function.UnaryOperator;

import com.pinterest.orion.server.api.Ami;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.BlockDeviceMapping;
import software.amazon.awssdk.services.ec2.model.DescribeImagesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeImagesResponse;
import software.amazon.awssdk.services.ec2.model.CreateTagsRequest;
import software.amazon.awssdk.services.ec2.model.CreateTagsResponse;
import software.amazon.awssdk.services.ec2.model.Filter;
import software.amazon.awssdk.services.ec2.model.Tag;

/**
 * AmiTagManager interfaces APIs in
 * {@link com.pinterest.orion.server.api.ClusterManagerApi ClusterManagerApi}
 * with AWS for AMI tag management.
 */
public class AmiTagManager {
  private static final Logger logger = Logger.getLogger(AmiTagManager.class.getCanonicalName());
  private Ec2Client ec2Client;
  public static final String KEY_IS_PUBLIC = "is-public";
  public static final String KEY_AMI_ID = "ami_id";
  public static final String KEY_APPLICATION = "application";
  public static final String KEY_RELEASE = "release";
  public static final String KEY_ARCHITECTURE = "architecture";
  public static final String KEY_APPLICATION_ENVIRONMENT = "application_environment";
  public static final String VALUE_KAFKA = "kafka";
  public static UnaryOperator<String> tag = key -> "tag:" + key;
  public static final String ENV_TYPES_KEY = "envTypes";

  public AmiTagManager() {
    ec2Client = Ec2Client.create();
  }

  /**
   * retrieve AMI list from cloud provider
   *
   * @param filter - map of criteria fields
   * @return list of Ami objects
   */
  public List<Ami> getAmiList(Map<String, String> filter) {
    List<Ami> amiList = new ArrayList<>();
    DescribeImagesRequest.Builder builder = DescribeImagesRequest.builder();
    Filter.Builder filterBuilder = Filter.builder();
    List<Filter> filterList = new ArrayList<>();
    filterList.add(
      filterBuilder.name(tag.apply(KEY_APPLICATION))
        .values(VALUE_KAFKA)
        .build()
    );
    if (filter.containsKey(KEY_RELEASE))
      filterList.add(
        filterBuilder.name(tag.apply(KEY_RELEASE))
          .values(filter.get(KEY_RELEASE))
          .build()
      );
    if (filter.containsKey(KEY_ARCHITECTURE))
      filterList.add(
        filterBuilder.name(KEY_ARCHITECTURE)
          .values(filter.get(KEY_ARCHITECTURE))
          .build()
      );
    filterList.add(
      filterBuilder.name(tag.apply(KEY_APPLICATION_ENVIRONMENT))
        .values("*")
        .build()
    );
    builder = builder.filters(filterList);
    try {
      DescribeImagesResponse resp = ec2Client.describeImages(builder.build());
      if (resp.hasImages() && !resp.images().isEmpty()) {
        // The limitation of images newer than 180 days is temporarily suspended
        // ZonedDateTime cutDate = ZonedDateTime.now().minusDays(180);
        resp.images().forEach(image -> {
          /*if (ZonedDateTime.parse(image.creationDate(), DateTimeFormatter.ISO_ZONED_DATE_TIME).isAfter(cutDate)) {*/
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
          // }
        });
        Function<Ami, ZonedDateTime> parse = i -> ZonedDateTime.parse(i.getCreationDate(), DateTimeFormatter.ISO_ZONED_DATE_TIME);
        amiList.sort((a, b) -> - parse.apply(a).compareTo(parse.apply(b)));
      }
    } catch (Exception e) {
      logger.log(Level.SEVERE, "AmiTagManager: could not retrieve AMI list", e);
      throw e;
    }
    return amiList;
  }

  /**
   * update AMI tag 'application_environment'
   *
   * @param amiId - target AMI id
   * @param applicationEnvironment - new tag value
   */
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
