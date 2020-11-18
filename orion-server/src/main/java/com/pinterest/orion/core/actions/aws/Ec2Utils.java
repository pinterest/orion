package com.pinterest.orion.core.actions.aws;

import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.core.retry.backoff.FullJitterBackoffStrategy;
import software.amazon.awssdk.core.retry.conditions.OrRetryCondition;
import software.amazon.awssdk.core.retry.conditions.RetryCondition;
import software.amazon.awssdk.core.retry.conditions.RetryOnExceptionsCondition;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeInstanceStatusRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstanceStatusResponse;
import software.amazon.awssdk.services.ec2.model.InstanceStateName;
import software.amazon.awssdk.services.route53.Route53Client;
import software.amazon.awssdk.services.route53.model.Change;
import software.amazon.awssdk.services.route53.model.ChangeAction;
import software.amazon.awssdk.services.route53.model.ChangeBatch;
import software.amazon.awssdk.services.route53.model.ChangeResourceRecordSetsRequest;
import software.amazon.awssdk.services.route53.model.RRType;
import software.amazon.awssdk.services.route53.model.ResourceRecord;
import software.amazon.awssdk.services.route53.model.ResourceRecordSet;
import software.amazon.awssdk.services.route53.model.Route53Exception;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Ec2Utils {

  public static Set<InstanceStateName> EC2_TERMIMATED_STATES = new HashSet<>(
      Arrays.asList(InstanceStateName.STOPPED, InstanceStateName.TERMINATED));
  public static Set<InstanceStateName> EC2_RUNNING_STATES = Collections
          .singleton(InstanceStateName.RUNNING);
  public static String CONF_ROUTE53_ZONE_ID = "zoneId";
  public static String CONF_ROUTE53_ZONE_NAME = "zoneName";

  public static void waitForInstanceStateChange(Ec2Client client,
                                                String instanceId,
                                                Set<InstanceStateName> targetStates,
                                                Logger logger) throws InterruptedException {
    DescribeInstanceStatusRequest instanceStatusRequest = DescribeInstanceStatusRequest.builder()
        .instanceIds(instanceId).includeAllInstances(true).build();
    int retries = 3;
    while (true) {
      Thread.sleep(5000);
      try {
        DescribeInstanceStatusResponse resp = client.describeInstanceStatus(instanceStatusRequest);
        if (resp.hasInstanceStatuses()) {
          InstanceStateName instanceState = resp.instanceStatuses().get(0).instanceState().name();
          if (targetStates.contains(instanceState)) {
            break;
          }
        }
      } catch (Exception e) {
        if (retries > 0) {
          logger.log(Level.WARNING, "Exception happened when waiting for instance status, retrying for " + retries + " times: ", e);
        } else {
          throw e;
        }
        retries--;
      }
    }
  }

  public static boolean upsertR53Record(String hostname, String privateIpAddress, String r53Name, String r53ZoneId, Logger logger) throws Exception {
    // update Route53 records to new instance
    // region needs to be AWS_GLOBAL based on
    // https://github.com/aws/aws-sdk-java-v2/issues/456
    // simple retry mechanism outside of SDK retry policy to be safe
    int retries = 3;
    try (Route53Client route53Client = Route53Client.builder().region(Region.AWS_GLOBAL)
        .overrideConfiguration(
            ClientOverrideConfiguration.builder()
                .retryPolicy(RetryPolicy.builder()
                    .numRetries(10)
                    .backoffStrategy(
                        FullJitterBackoffStrategy.builder()
                            .baseDelay(Duration.ofMillis(500))
                            .maxBackoffTime(Duration.ofMinutes(3))
                            .build()
                    )
                    .retryCondition(OrRetryCondition.create(
                        RetryOnExceptionsCondition.create(Route53Exception.class),
                        RetryCondition.defaultRetryCondition())
                    )
                    .build())
                .build()
        )
        .build()) {
      String mergedFQDN = mergeHostnameAndFQDN(hostname, r53Name);
      Change change = Change.builder().action(ChangeAction.UPSERT)
          .resourceRecordSet(ResourceRecordSet.builder()
              .name(mergedFQDN).type(RRType.A).ttl(600L)
              .resourceRecords(ResourceRecord.builder().value(privateIpAddress).build())
              .build())
          .build();
      ChangeResourceRecordSetsRequest
          changeResourceRecordSetsRequest =
          ChangeResourceRecordSetsRequest
              .builder().hostedZoneId(r53ZoneId)
              .changeBatch(ChangeBatch.builder().changes(change).build()).build();

      while (retries-- > 0) {
        try {
          route53Client.changeResourceRecordSets(changeResourceRecordSetsRequest);
          break;
        } catch (Exception e) {
          logger.log(Level.WARNING, "Could not upsert Route 53 Entry, retries left: " + retries, e);
          Thread.sleep(10_000); // wait for 10 seconds before retrying
        }
      }
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Could not upsert Route53 Entry after retrying", e);
      throw e;
    }
    return true;
  }


  // assuming hostname might be overlapping with zoneName,
  // and in this case zoneName has an extra period of the overlapping suffix in hostname
  protected static String mergeHostnameAndFQDN(String hostname, String zoneName) {
    if (hostname.endsWith(zoneName.substring(0, zoneName.length() - 1))) {
      return hostname + zoneName.substring(zoneName.length() - 1);
    }
    return String.join(".", hostname, zoneName);
  }
}
