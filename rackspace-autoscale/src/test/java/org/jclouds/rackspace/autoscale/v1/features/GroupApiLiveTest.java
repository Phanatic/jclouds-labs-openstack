/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jclouds.rackspace.autoscale.v1.features;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.util.List;
import java.util.Map;

import org.jclouds.openstack.v2_0.domain.Link;
import org.jclouds.rackspace.autoscale.v1.domain.Group;
import org.jclouds.rackspace.autoscale.v1.domain.GroupConfiguration;
import org.jclouds.rackspace.autoscale.v1.domain.GroupState;
import org.jclouds.rackspace.autoscale.v1.domain.LaunchConfiguration;
import org.jclouds.rackspace.autoscale.v1.domain.LaunchConfiguration.LaunchConfigurationType;
import org.jclouds.rackspace.autoscale.v1.domain.LoadBalancer;
import org.jclouds.rackspace.autoscale.v1.domain.Personality;
import org.jclouds.rackspace.autoscale.v1.domain.CreateScalingPolicy;
import org.jclouds.rackspace.autoscale.v1.domain.CreateScalingPolicy.ScalingPolicyTargetType;
import org.jclouds.rackspace.autoscale.v1.domain.CreateScalingPolicy.ScalingPolicyType;
import org.jclouds.rackspace.autoscale.v1.internal.BaseAutoscaleApiLiveTest;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Group live test
 */
@Test(groups = "live", testName = "GroupApiLiveTest", singleThreaded = true)
public class GroupApiLiveTest extends BaseAutoscaleApiLiveTest {

   private static Map<String, List<Group>> created = Maps.newHashMap();

   @Override
   @BeforeClass(groups = { "integration", "live" })
   public void setup() {
      super.setup();
      for (String zone : api.getConfiguredZones()) {
         List<Group> createdGroupList = Lists.newArrayList();
         created.put(zone, createdGroupList);
         GroupApi groupApi = api.getGroupApiForZone(zone);

         GroupConfiguration groupConfiguration = GroupConfiguration.builder().maxEntities(10).cooldown(360)
               .name("testscalinggroup198547").minEntities(0)
               .metadata(ImmutableMap.of("gc_meta_key_2", "gc_meta_value_2", "gc_meta_key_1", "gc_meta_value_1"))
               .build();

         LaunchConfiguration launchConfiguration = LaunchConfiguration
               .builder()
               .loadBalancers(ImmutableList.of(LoadBalancer.builder().port(8080).id(9099).build()))
               .serverName("autoscale_server")
               .serverImageRef("57b8a366-ab2c-454b-939f-215303a4431f")
               .serverFlavorRef("2")
               .serverDiskConfig("AUTO")
               .serverMetadata(
                     ImmutableMap
                     .of("build_config", "core", "meta_key_1", "meta_value_1", "meta_key_2", "meta_value_2"))
                     .networks(
                           ImmutableList.of("11111111-1111-1111-1111-111111111111", "00000000-0000-0000-0000-000000000000"))
                           .personalities(
                                 ImmutableList.of(Personality.builder().path("testfile")
                                       .contents("VGhpcyBpcyBhIHRlc3QgZmlsZS4=").build()))
                                       .type(LaunchConfigurationType.LAUNCH_SERVER).build();

         List<CreateScalingPolicy> scalingPolicies = Lists.newArrayList();

         CreateScalingPolicy scalingPolicy = CreateScalingPolicy.builder().cooldown(1).type(ScalingPolicyType.WEBHOOK)
               .name("scale up by 1").targetType(ScalingPolicyTargetType.INCREMENTAL).target("1").build();
         scalingPolicies.add(scalingPolicy);

         Group g = groupApi.create(groupConfiguration, launchConfiguration, scalingPolicies);
         createdGroupList.add(g);

         assertNotNull(g);
         assertNotNull(g.getId());
         assertEquals(g.getLinks().size(), 1);
         assertEquals(g.getLinks().get(0).getHref().toString(),
               "https://" + zone.toLowerCase() + ".autoscale.api.rackspacecloud.com/v1.0/" + api.getCurrentTenantId().get().getId() + "/groups/" + g.getId() + "/");
         assertEquals(g.getLinks().get(0).getRelation(), Link.Relation.SELF);

         assertNotNull(g.getScalingPolicies().get(0).getId());
         assertEquals(g.getScalingPolicies().get(0).getLinks().size(), 1);
         assertEquals(
               g.getScalingPolicies().get(0).getLinks().get(0).getHref().toString(),
               "https://" + zone.toLowerCase() + ".autoscale.api.rackspacecloud.com/v1.0/" + api.getCurrentTenantId().get().getId() + "/groups/" + g.getId() + "/policies/" + g.getScalingPolicies().get(0).getId() + "/");
         assertEquals(g.getScalingPolicies().get(0).getLinks().get(0).getRelation(), Link.Relation.SELF);
         assertEquals(g.getScalingPolicies().get(0).getCooldown(), 1);
         assertEquals(g.getScalingPolicies().get(0).getTarget(), "1");
         assertEquals(g.getScalingPolicies().get(0).getTargetType(), ScalingPolicyTargetType.INCREMENTAL);
         assertEquals(g.getScalingPolicies().get(0).getType(), ScalingPolicyType.WEBHOOK);
         assertEquals(g.getScalingPolicies().get(0).getName(), "scale up by 1");

         assertEquals(g.getLaunchConfiguration().getLoadBalancers().size(), 1);
         assertEquals(g.getLaunchConfiguration().getLoadBalancers().get(0).getId(), 9099);
         assertEquals(g.getLaunchConfiguration().getLoadBalancers().get(0).getPort(), 8080);
         assertEquals(g.getLaunchConfiguration().getServerName(), "autoscale_server");
         assertNotNull(g.getLaunchConfiguration().getServerImageRef());
         assertEquals(g.getLaunchConfiguration().getServerFlavorRef(), "2");
         assertEquals(g.getLaunchConfiguration().getServerDiskConfig(), "AUTO");
         assertEquals(g.getLaunchConfiguration().getPersonalities().size(), 1);
         assertEquals(g.getLaunchConfiguration().getPersonalities().get(0).getPath(), "testfile");
         assertEquals(g.getLaunchConfiguration().getPersonalities().get(0).getContents(),
               "VGhpcyBpcyBhIHRlc3QgZmlsZS4=");
         assertEquals(g.getLaunchConfiguration().getNetworks().size(), 2);
         assertEquals(g.getLaunchConfiguration().getNetworks().get(0), "11111111-1111-1111-1111-111111111111");
         assertEquals(g.getLaunchConfiguration().getNetworks().get(1), "00000000-0000-0000-0000-000000000000");
         assertEquals(g.getLaunchConfiguration().getServerMetadata().size(), 3);
         assertTrue(g.getLaunchConfiguration().getServerMetadata().containsKey("build_config"));
         assertTrue(g.getLaunchConfiguration().getServerMetadata().containsValue("core"));
         assertEquals(g.getLaunchConfiguration().getType(), LaunchConfigurationType.LAUNCH_SERVER);

         assertEquals(g.getGroupConfiguration().getMaxEntities(), 10);
         assertEquals(g.getGroupConfiguration().getCooldown(), 360);
         assertEquals(g.getGroupConfiguration().getName(), "testscalinggroup198547");
         assertEquals(g.getGroupConfiguration().getMinEntities(), 0);
         assertEquals(g.getGroupConfiguration().getMetadata().size(), 2);
         assertTrue(g.getGroupConfiguration().getMetadata().containsKey("gc_meta_key_2"));
         assertTrue(g.getGroupConfiguration().getMetadata().containsValue("gc_meta_value_2"));
      }
   }

   @Test
   public void testGetGroup() {
      for (String zone : api.getConfiguredZones()) {
         GroupApi groupApi = api.getGroupApiForZone(zone);
         String groupId = created.get(zone).get(0).getId();
         Group testGroup = groupApi.get(groupId);
         assertEquals(testGroup.getId(), groupId);
         assertEquals(testGroup.getGroupConfiguration().getCooldown(), 360);
         assertEquals(testGroup.getLaunchConfiguration().getServerName(), "autoscale_server");
         assertEquals(testGroup.getScalingPolicies().get(0).getName(), "scale up by 1");
      }
   }

   @Test
   public void testGetState() {
      for (String zone : api.getConfiguredZones()) {
         GroupApi groupApi = api.getGroupApiForZone(zone);
         String groupId = created.get(zone).get(0).getId();
         GroupState testGroup = groupApi.getState(groupId);
         assertNull(testGroup.getId()); // The id recently changed to not be included when getting state.
      }
   }

   @Test
   public void testListGroups() {
      for (String zone : api.getConfiguredZones()) {
         GroupApi groupApi = api.getGroupApiForZone(zone);
         FluentIterable<GroupState> groupsList = groupApi.listGroupStates();
         String groupId = created.get(zone).get(0).getId();
         for (GroupState groupState : groupsList) {
            if (groupId.equals(groupState.getId())) {
               return;
            }
         }
         fail("Could not find known groupID " + groupId);
      }
   }

   /* TODO: uncomment when implemented
   @Test
   public void testPause() {
      for (String zone : api.getConfiguredZones()) {
         GroupApi groupApi = api.getGroupApiForZone(zone);
         String groupId = created.get(zone).get(0).getId();
         assertTrue(groupApi.pause(groupId));
      }
   }

   @Test
   public void testResume() {
      for (String zone : api.getConfiguredZones()) {
         GroupApi groupApi = api.getGroupApiForZone(zone);
         String groupId = created.get(zone).get(0).getId();
         assertTrue(groupApi.resume(groupId));
      }
   }
    */

   @Test
   public void testGetGroupConfiguration() {
      for (String zone : api.getConfiguredZones()) {
         GroupApi groupApi = api.getGroupApiForZone(zone);
         String groupId = created.get(zone).get(0).getId();
         GroupConfiguration testGroupConfiguration = groupApi.getGroupConfiguration(groupId);
         assertEquals(testGroupConfiguration.getCooldown(), 360);
         assertEquals(testGroupConfiguration.getMaxEntities(), 10);
         assertEquals(testGroupConfiguration.getMinEntities(), 0);
      }
   }

   @Test
   public void testGetGroupLaunchConfiguration() {
      for (String zone : api.getConfiguredZones()) {
         GroupApi groupApi = api.getGroupApiForZone(zone);
         String groupId = created.get(zone).get(0).getId();
         LaunchConfiguration testLaunchConfiguration = groupApi.getLaunchConfiguration(groupId);
         assertEquals(testLaunchConfiguration.getLoadBalancers().get(0).getPort(), 8080);
         assertEquals(testLaunchConfiguration.getType(), LaunchConfigurationType.LAUNCH_SERVER);
         assertEquals(testLaunchConfiguration.getServerFlavorRef(), "2");
      }
   }

   @Test
   public void testUpdateLaunchConfiguration() {
      for (String zone : api.getConfiguredZones()) {
         GroupApi groupApi = api.getGroupApiForZone(zone);
         String groupId = created.get(zone).get(0).getId();

         LaunchConfiguration launchConfiguration = LaunchConfiguration
               .builder()
               .loadBalancers(ImmutableList.of(LoadBalancer.builder().port(8080).id(9099).build()))
               .serverName("autoscale_server")
               .serverImageRef("57b8a366-ab2c-454b-939f-215303a4431f")
               .serverFlavorRef("2")
               .serverDiskConfig("AUTO")
               .serverMetadata(
                     ImmutableMap
                     .of("build_config", "core", "meta_key_1", "meta_value_1", "meta_key_2", "meta_value_2"))
                     .networks(
                           ImmutableList.of("11111111-1111-1111-1111-111111111111", "00000000-0000-0000-0000-000000000000"))
                           .personalities(
                                 ImmutableList.of(Personality.builder().path("testfile2")
                                       .contents("VGhpcyBpcyBhIHRlc3QgZmlsZS4=").build()))
                                       .type(LaunchConfigurationType.LAUNCH_SERVER).build();

         boolean result = groupApi.updateLaunchConfiguration(groupId, launchConfiguration);
         assertEquals(result, true);
      }
   }

   @Test
   public void testUpdateGroupConfiguration() {
      for (String zone : api.getConfiguredZones()) {
         GroupApi groupApi = api.getGroupApiForZone(zone);
         String groupId = created.get(zone).get(0).getId();

         GroupConfiguration groupConfiguration = GroupConfiguration.builder().maxEntities(10).cooldown(360)
               .name("testscalinggroup198547").minEntities(0)
               .metadata(ImmutableMap.of("gc_meta_key_2", "gc_meta_value_2", "gc_meta_key_1", "gc_meta_value_1"))
               .build();

         boolean result = groupApi.updateGroupConfiguration(groupId, groupConfiguration);
         assertEquals(result, true);
      }
   }

   @Override
   @AfterClass(groups = { "integration", "live" })
   public void tearDown() {
      for (String zone : api.getConfiguredZones()) {
         GroupApi groupApi = api.getGroupApiForZone(zone);
         for (Group group : created.get(zone)) {
            if (!groupApi.delete(group.getId()))
               throw new RuntimeException("Could not delete an autoscale group after tests!");
         }
      }
      super.tearDown();
   }
}
