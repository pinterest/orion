package com.pinterest.orion.core.actions.aws;

import java.util.Map;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public class S3Utils {

  public static S3Client getDefaultS3Client() {
    return S3Client.builder().build();
  }

  public static void putObject(
    S3Client s3, 
    String bucket, 
    String key, 
    byte[] object, 
    Map<String, String> metadata) throws S3Exception {
    PutObjectRequest putReq = PutObjectRequest.builder()
      .bucket(bucket)
      .key(key)
      .metadata(metadata)
      .build();

    s3.putObject(putReq, RequestBody.fromBytes(object));
  }
}
