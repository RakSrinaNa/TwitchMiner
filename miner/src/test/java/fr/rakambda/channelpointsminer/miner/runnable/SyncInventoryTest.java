package fr.rakambda.channelpointsminer.miner.runnable;

import fr.rakambda.channelpointsminer.miner.api.gql.gql.GQLApi;
import fr.rakambda.channelpointsminer.miner.api.gql.gql.data.GQLResponse;
import fr.rakambda.channelpointsminer.miner.api.gql.gql.data.dropspageclaimdroprewards.DropsPageClaimDropRewardsData;
import fr.rakambda.channelpointsminer.miner.api.gql.gql.data.inventory.InventoryData;
import fr.rakambda.channelpointsminer.miner.api.gql.gql.data.types.DropCampaign;
import fr.rakambda.channelpointsminer.miner.api.gql.gql.data.types.Inventory;
import fr.rakambda.channelpointsminer.miner.api.gql.gql.data.types.TimeBasedDrop;
import fr.rakambda.channelpointsminer.miner.api.gql.gql.data.types.TimeBasedDropSelfEdge;
import fr.rakambda.channelpointsminer.miner.api.gql.gql.data.types.User;
import fr.rakambda.channelpointsminer.miner.event.impl.DropClaimEvent;
import fr.rakambda.channelpointsminer.miner.event.impl.DropClaimedEvent;
import fr.rakambda.channelpointsminer.miner.event.manager.IEventManager;
import fr.rakambda.channelpointsminer.miner.factory.TimeFactory;
import fr.rakambda.channelpointsminer.miner.miner.IMiner;
import fr.rakambda.channelpointsminer.miner.miner.MinerData;
import fr.rakambda.channelpointsminer.miner.streamer.Streamer;
import fr.rakambda.channelpointsminer.miner.tests.ParallelizableTest;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ParallelizableTest
@ExtendWith(MockitoExtension.class)
class SyncInventoryTest{
	private static final String DROP_ID = "drop-id";
	private static final Instant NOW = Instant.parse("2020-05-17T12:14:20.000Z");
	
	@InjectMocks
	private SyncInventory tested;
	
	@Mock
	private IMiner miner;
	@Mock
	private IEventManager eventManager;
	@Mock
	private MinerData minerData;
	@Mock
	private GQLApi gqlApi;
	@Mock
	private GQLResponse<InventoryData> inventoryDataGQLResponse;
	@Mock
	private GQLResponse<DropsPageClaimDropRewardsData> dropsPageClaimDropRewardsDataGQLResponse;
	@Mock
	private InventoryData inventoryData;
	@Mock
	private User user;
	@Mock
	private Inventory inventory;
	@Mock
	private DropCampaign dropCampaign;
	@Mock
	private TimeBasedDrop timeBasedDrop;
	@Mock
	private TimeBasedDropSelfEdge timeBasedDropSelfEdge;
	@Mock
	private Streamer streamer;
	
	@BeforeEach
	void setUp(){
		lenient().when(miner.getGqlApi()).thenReturn(gqlApi);
		lenient().when(miner.getStreamers()).thenReturn(List.of(streamer));
		lenient().when(miner.getMinerData()).thenReturn(minerData);
		
		lenient().when(inventoryDataGQLResponse.getData()).thenReturn(inventoryData);
		lenient().when(inventoryData.getCurrentUser()).thenReturn(user);
		lenient().when(user.getInventory()).thenReturn(inventory);
		lenient().when(inventory.getDropCampaignsInProgress()).thenReturn(List.of(dropCampaign));
		lenient().when(dropCampaign.getTimeBasedDrops()).thenReturn(List.of(timeBasedDrop));
		lenient().when(timeBasedDrop.getSelf()).thenReturn(timeBasedDropSelfEdge);
		
		lenient().when(dropsPageClaimDropRewardsDataGQLResponse.isError()).thenReturn(false);
		
		lenient().when(streamer.isParticipateCampaigns()).thenReturn(true);
		
		lenient().when(timeBasedDropSelfEdge.isClaimed()).thenReturn(false);
		lenient().when(timeBasedDropSelfEdge.getDropInstanceId()).thenReturn(DROP_ID);
		
		lenient().when(gqlApi.inventory()).thenReturn(Optional.of(inventoryDataGQLResponse));
		lenient().when(gqlApi.dropsPageClaimDropRewards(DROP_ID)).thenReturn(Optional.of(dropsPageClaimDropRewardsDataGQLResponse));
	}
	
	@Test
	void updateInventoryWithDropToClaim(){
		try(var timeFactory = mockStatic(TimeFactory.class)){
			timeFactory.when(TimeFactory::now).thenReturn(NOW);
			
			assertDoesNotThrow(() -> tested.run());
			
			verify(minerData).setInventory(inventoryData);
			verify(gqlApi).dropsPageClaimDropRewards(DROP_ID);
			verify(eventManager).onEvent(new DropClaimEvent(timeBasedDrop, NOW));
			verify(eventManager).onEvent(new DropClaimedEvent(timeBasedDrop, NOW));
		}
	}
	
	@Test
	void updateInventoryWithNoDropId(){
		when(timeBasedDropSelfEdge.getDropInstanceId()).thenReturn(null);
		
		assertDoesNotThrow(() -> tested.run());
		
		verify(minerData).setInventory(inventoryData);
		verify(gqlApi, never()).dropsPageClaimDropRewards(any());
		verify(eventManager, never()).onEvent(any());
	}
	
	@Test
	void updateInventoryWithAlreadyClaimed(){
		when(timeBasedDropSelfEdge.isClaimed()).thenReturn(true);
		
		assertDoesNotThrow(() -> tested.run());
		
		verify(minerData).setInventory(inventoryData);
		verify(gqlApi, never()).dropsPageClaimDropRewards(any());
		verify(eventManager, never()).onEvent(any());
	}
	
	@Test
	void updateInventoryWithDataNoSelfDrop(){
		when(timeBasedDrop.getSelf()).thenReturn(null);
		
		assertDoesNotThrow(() -> tested.run());
		
		verify(minerData).setInventory(inventoryData);
		verify(gqlApi, never()).dropsPageClaimDropRewards(any());
		verify(eventManager, never()).onEvent(any());
	}
	
	@Test
	void updateInventoryWithDataNoDrops(){
		when(dropCampaign.getTimeBasedDrops()).thenReturn(List.of());
		
		assertDoesNotThrow(() -> tested.run());
		
		verify(minerData).setInventory(inventoryData);
		verify(gqlApi, never()).dropsPageClaimDropRewards(any());
		verify(eventManager, never()).onEvent(any());
	}
	
	@Test
	void updateInventoryWithDataNoCampaignsInProgress(){
		when(inventory.getDropCampaignsInProgress()).thenReturn(List.of());
		
		assertDoesNotThrow(() -> tested.run());
		
		verify(minerData).setInventory(inventoryData);
		verify(gqlApi, never()).dropsPageClaimDropRewards(any());
		verify(eventManager, never()).onEvent(any());
	}
	
	@Test
	void updateInventoryWithDataNoInventory(){
		when(user.getInventory()).thenReturn(null);
		
		assertDoesNotThrow(() -> tested.run());
		
		verify(minerData).setInventory(inventoryData);
		verify(gqlApi, never()).dropsPageClaimDropRewards(any());
		verify(eventManager, never()).onEvent(any());
	}
	
	@Test
	void updateInventoryWithNoData(){
		when(gqlApi.inventory()).thenReturn(Optional.empty());
		
		assertDoesNotThrow(() -> tested.run());
		
		verify(minerData).setInventory(null);
		verify(gqlApi, never()).dropsPageClaimDropRewards(any());
		verify(eventManager, never()).onEvent(any());
	}
	
	@Test
	void updateInventoryWithException(){
		when(gqlApi.inventory()).thenThrow(new RuntimeException("For tests"));
		
		assertDoesNotThrow(() -> tested.run());
		
		verify(minerData, never()).setInventory(any());
		verify(gqlApi, never()).dropsPageClaimDropRewards(any());
		verify(eventManager, never()).onEvent(any());
	}
	
	@Test
	void updateInventoryNoStreamers(){
		when(miner.getStreamers()).thenReturn(List.of());
		
		assertDoesNotThrow(() -> tested.run());
		
		verify(minerData, never()).setInventory(any());
		verify(gqlApi, never()).dropsPageClaimDropRewards(any());
		verify(eventManager, never()).onEvent(any());
	}
	
	@Test
	void updateInventoryNoStreamersInCampaigns(){
		when(streamer.isParticipateCampaigns()).thenReturn(false);
		
		assertDoesNotThrow(() -> tested.run());
		
		verify(minerData, never()).setInventory(any());
		verify(gqlApi, never()).dropsPageClaimDropRewards(any());
		verify(eventManager, never()).onEvent(any());
	}
	
	@Test
	void updateInventoryWithClaimError(){
		try(var timeFactory = mockStatic(TimeFactory.class)){
			timeFactory.when(TimeFactory::now).thenReturn(NOW);
			
			when(dropsPageClaimDropRewardsDataGQLResponse.isError()).thenReturn(true);
			
			assertDoesNotThrow(() -> tested.run());
			
			verify(minerData).setInventory(inventoryData);
			verify(gqlApi).dropsPageClaimDropRewards(DROP_ID);
			verify(eventManager).onEvent(new DropClaimEvent(timeBasedDrop, NOW));
			verify(eventManager, never()).onEvent(any(DropClaimedEvent.class));
		}
	}
	
	@Test
	void updateInventoryWithResponseEmpty(){
		try(var timeFactory = mockStatic(TimeFactory.class)){
			timeFactory.when(TimeFactory::now).thenReturn(NOW);
			
			when(gqlApi.dropsPageClaimDropRewards(DROP_ID)).thenReturn(Optional.empty());
			
			assertDoesNotThrow(() -> tested.run());
			
			verify(minerData).setInventory(inventoryData);
			verify(gqlApi).dropsPageClaimDropRewards(DROP_ID);
			verify(eventManager).onEvent(new DropClaimEvent(timeBasedDrop, NOW));
			verify(eventManager, never()).onEvent(any(DropClaimedEvent.class));
		}
	}
}