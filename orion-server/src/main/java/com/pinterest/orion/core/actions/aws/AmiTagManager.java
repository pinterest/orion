package com.pinterest.orion.core.actions.aws;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.pinterest.orion.server.api.AMI;

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

  public AmiTagManager() {
    ec2Client = Ec2Client.create();
  }

  public List<AMI> getAmiList(Map<String, String> filter) {
    List<AMI> amiList = new ArrayList<>();
    DescribeImagesRequest.Builder builder = DescribeImagesRequest.builder();
    builder = builder.filters(
      Filter.builder().name("tag:application").values("kafka").build()
    );
    if (filter.containsKey("os"))
      builder = builder.filters(
        Filter.builder().name("tag:release").values(filter.get("os")).build()
      );
    if (filter.containsKey("arch"))
      builder = builder.filters(
        Filter.builder().name("tag:cpu_architecture").values(filter.get("arch")).build()
      );
    builder = builder.filters(
      Filter.builder().name("tag:application_environment").values("*").build()
    );
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
            if (t.key().equals("application_environment")) {
              appEnvTag = t.value();
              break;
            }
          }
          amiList.add(new AMI(
            image.imageId(),
            appEnvTag,
            image.creationDate()
          ));
        }
      });
      amiList.sort((a, b) -> - ZonedDateTime.parse(a.getCreationDate(), DateTimeFormatter.ISO_ZONED_DATE_TIME)
      .compareTo(ZonedDateTime.parse(b.getCreationDate(), DateTimeFormatter.ISO_ZONED_DATE_TIME)));
    }
    return amiList;
  }

  public void updateAmiTag(String amiId, String applicationEnvironment) {
    CreateTagsRequest.Builder builder = CreateTagsRequest.builder();
    Tag newAppEnv = Tag.builder()
        .key("application_environment")
        .value(applicationEnvironment).build();
    CreateTagsRequest request = builder
        .resources(amiId)
        .tags(newAppEnv)
        .build();
    CreateTagsResponse resp = ec2Client.createTags(request);
    if (!resp.sdkHttpResponse().isSuccessful())
      logger.severe("Tag update failed for " + amiId + " and application_environment tag = " + applicationEnvironment);
  }
}
