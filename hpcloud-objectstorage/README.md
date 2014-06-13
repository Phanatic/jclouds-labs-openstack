HP Cloud Object Storage
==========================

The new HP Cloud Object Storage multi-region based service API.

This new "hpcloud-objectstorage" API supersedes the jclouds "objectstorage" API, which will eventually be deprecated.

With this multi-region support, each BlobStore can be isolated to a specific region:

     RegionScopedBlobStoreContext ctx =
     	contextBuilder.buildView(RegionScopedBlobStoreContext.class);

     Set<String> regionIds = ctx.configuredRegions();

     // isolated to a specific region
     BlobStore dfwBlobStore = ctx.blobStoreInRegion("DFW");
     BlobStore iadBlobStore = ctx.blobStoreInRegion("IAD");

Production ready?
Beta

This API is new to jclouds and hence is in Beta. That means we need people to use it and give us feedback. Based on that feedback, minor changes to the interfaces may happen. This code will replace org.jclouds.openstack.swift.SwiftClient in jclouds 2.0 and it is recommended you adopt it sooner than later.
