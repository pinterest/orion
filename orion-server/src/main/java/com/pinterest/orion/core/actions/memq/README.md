# MemQ Orion Actions

### Introduction

To use the MemQ actions, you need to create a new action class that extends the abstract class.

In the new class, override the getEC2Helper() method in your action class to manage instance in your own way.

If the default time value does not suit your requirements, you can override the getTime methods to set your desired check interval or timeout values.

If you want to run multiple actions in parallel, you can override the MemqCluster* actions based on the document in the class.

### Instance Management

Pinterest recommends to use Teletraan to manage memq hosts in memq clusters.

Teletraan is Pinterest's deploy system. It deploys thousands of Pinterest internal services, supports tens of thousands hosts, and has been running in production for over many years. It empowers Pinterest Engineers to deliver their code to pinners fast and safe.

Project Link: https://github.com/pinterest/teletraan
