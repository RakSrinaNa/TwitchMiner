package fr.rakambda.channelpointsminer.miner.runnable;

import fr.rakambda.channelpointsminer.miner.api.gql.gql.GQLApi;
import fr.rakambda.channelpointsminer.miner.api.gql.gql.data.types.Game;
import fr.rakambda.channelpointsminer.miner.api.passport.TwitchLogin;
import fr.rakambda.channelpointsminer.miner.api.twitch.TwitchApi;
import fr.rakambda.channelpointsminer.miner.api.twitch.data.MinuteWatchedEvent;
import fr.rakambda.channelpointsminer.miner.api.twitch.data.MinuteWatchedProperties;
import fr.rakambda.channelpointsminer.miner.factory.TimeFactory;
import fr.rakambda.channelpointsminer.miner.miner.IMiner;
import fr.rakambda.channelpointsminer.miner.streamer.Streamer;
import fr.rakambda.channelpointsminer.miner.tests.ParallelizableTest;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ParallelizableTest
@ExtendWith(MockitoExtension.class)
class SendSpadeMinutesWatchedTest{
	private static final String STREAMER_ID = "streamer-id";
	private static final String STREAMER_NAME = "streamer-name";
	private static final String STREAM_ID = "stream-id";
	private static final String SITE_PLAYER = "site";
	private static final int USER_ID = 123456789;
	private static final String GAME_NAME = "game-name";
	private static final String GAME_ID = "game-id";
	private static final Instant NOW = Instant.parse("2021-03-25T18:12:36Z");
	private static final int INDEX = 5;
	
	@InjectMocks
	private SendSpadeMinutesWatched tested;
	
	@Mock
	private IMiner miner;
	@Mock
	private TwitchApi twitchApi;
	@Mock
	private GQLApi gqlApi;
	@Mock
	private Streamer streamer;
	@Mock
	private TwitchLogin twitchLogin;
	@Mock
	private Game game;
	
	private URL spadeUrl;
	
	@BeforeEach
	void setUp() throws MalformedURLException{
		spadeUrl = new URL("https://google.com/");
		
		lenient().when(miner.getTwitchApi()).thenReturn(twitchApi);
		lenient().when(miner.getStreamers()).thenReturn(List.of(streamer));
		lenient().when(miner.getTwitchLogin()).thenReturn(twitchLogin);
		lenient().when(miner.getGqlApi()).thenReturn(gqlApi);
		
		lenient().when(twitchLogin.getUserIdAsInt(gqlApi)).thenReturn(USER_ID);
		
		lenient().when(streamer.getId()).thenReturn(STREAMER_ID);
		lenient().when(streamer.getUsername()).thenReturn(STREAMER_NAME);
		lenient().when(streamer.getSpadeUrl()).thenReturn(spadeUrl);
		lenient().when(streamer.getStreamId()).thenReturn(Optional.of(STREAM_ID));
		lenient().when(streamer.isStreaming()).thenReturn(true);
		lenient().when(streamer.getIndex()).thenReturn(INDEX);
		
		lenient().when(game.getName()).thenReturn(GAME_NAME);
		lenient().when(game.getId()).thenReturn(GAME_ID);
	}
	
	@Test
	void sendingMinutesWatched(){
		when(streamer.getGame()).thenReturn(Optional.of(game));
		
		var expected = MinuteWatchedEvent.builder()
				.properties(MinuteWatchedProperties.builder()
						.channelId(STREAMER_ID)
						.channel(STREAMER_NAME)
						.broadcastId(STREAM_ID)
						.player(SITE_PLAYER)
						.userId(USER_ID)
						.gameId(GAME_ID)
						.game(GAME_NAME)
						.live(true)
						.build())
				.build();
		
		when(twitchApi.sendPlayerEvents(spadeUrl, expected)).thenReturn(true);
		
		assertDoesNotThrow(() -> tested.run());
		
		verify(streamer, never()).addWatchedDuration(any());
	}
	
	@Test
	void sendingMinutesWatchedUpdatesMinutesWatched(){
		try(var timeFactory = mockStatic(TimeFactory.class)){
			timeFactory.when(TimeFactory::now).thenReturn(NOW);
			
			when(streamer.getGame()).thenReturn(Optional.of(game));
			
			var expected = MinuteWatchedEvent.builder()
					.properties(MinuteWatchedProperties.builder()
							.channelId(STREAMER_ID)
							.channel(STREAMER_NAME)
							.broadcastId(STREAM_ID)
							.player(SITE_PLAYER)
							.userId(USER_ID)
							.game(GAME_NAME)
							.gameId(GAME_ID)
							.live(true)
							.build())
					.build();
			
			when(twitchApi.sendPlayerEvents(spadeUrl, expected)).thenReturn(true);
			
			assertDoesNotThrow(() -> tested.run());
			verify(streamer, never()).addWatchedDuration(any());
			
			var delta = Duration.ofSeconds(30);
			timeFactory.when(TimeFactory::now).thenReturn(NOW.plus(delta));
			assertDoesNotThrow(() -> tested.run());
			verify(streamer).addWatchedDuration(delta);
		}
	}
	
	@Test
	void sendingMinutesWatchedUpdatesMinutesWatchedResetIfNotPresentOnARound(){
		try(var timeFactory = mockStatic(TimeFactory.class)){
			timeFactory.when(TimeFactory::now).thenReturn(NOW);
			
			when(streamer.getGame()).thenReturn(Optional.of(game));
			
			var expected = MinuteWatchedEvent.builder()
					.properties(MinuteWatchedProperties.builder()
							.channelId(STREAMER_ID)
							.channel(STREAMER_NAME)
							.broadcastId(STREAM_ID)
							.player(SITE_PLAYER)
							.userId(USER_ID)
							.game(GAME_NAME)
							.gameId(GAME_ID)
							.live(true)
							.build())
					.build();
			
			when(miner.getStreamers()).thenReturn(List.of(streamer));
			when(twitchApi.sendPlayerEvents(spadeUrl, expected)).thenReturn(true);
			
			assertDoesNotThrow(() -> tested.run());
			verify(streamer, never()).addWatchedDuration(any());
			
			var delta = Duration.ofSeconds(30);
			timeFactory.when(TimeFactory::now).thenReturn(NOW.plus(delta));
			
			assertDoesNotThrow(() -> tested.run());
			verify(streamer).addWatchedDuration(delta);
			clearInvocations(streamer);
			
			timeFactory.when(TimeFactory::now).thenReturn(NOW);
			when(miner.getStreamers()).thenReturn(List.of());
			assertDoesNotThrow(() -> tested.run());
			verify(streamer, never()).addWatchedDuration(any());
			
			when(miner.getStreamers()).thenReturn(List.of(streamer));
			assertDoesNotThrow(() -> tested.run());
			verify(streamer, never()).addWatchedDuration(any());
			
			delta = Duration.ofSeconds(45);
			timeFactory.when(TimeFactory::now).thenReturn(NOW.plus(delta));
			when(miner.getStreamers()).thenReturn(List.of(streamer));
			assertDoesNotThrow(() -> tested.run());
			verify(streamer).addWatchedDuration(delta);
		}
	}
	
	@Test
	void sendingMinutesWatchedDoesNotUpdateMinutesWatchedIfCallFailed(){
		try(var timeFactory = mockStatic(TimeFactory.class)){
			timeFactory.when(TimeFactory::now).thenReturn(NOW);
			
			when(streamer.getGame()).thenReturn(Optional.of(game));
			
			var expected = MinuteWatchedEvent.builder()
					.properties(MinuteWatchedProperties.builder()
							.channelId(STREAMER_ID)
							.channel(STREAMER_NAME)
							.broadcastId(STREAM_ID)
							.player(SITE_PLAYER)
							.userId(USER_ID)
							.game(GAME_NAME)
							.gameId(GAME_ID)
							.live(true)
							.build())
					.build();
			
			when(twitchApi.sendPlayerEvents(spadeUrl, expected)).thenReturn(true);
			
			assertDoesNotThrow(() -> tested.run());
			verify(streamer, never()).addWatchedDuration(any());
			
			when(twitchApi.sendPlayerEvents(spadeUrl, expected)).thenReturn(false);
			
			var delta = Duration.ofSeconds(30);
			timeFactory.when(TimeFactory::now).thenReturn(NOW.plus(delta));
			assertDoesNotThrow(() -> tested.run());
			verify(streamer, never()).addWatchedDuration(any());
		}
	}
	
	@Test
	void sendingMinutesWatchedNoGameName(){
		when(streamer.getGame()).thenReturn(Optional.of(game));
		when(game.getName()).thenReturn(null);
		when(game.getId()).thenReturn(null);
		
		var expected = MinuteWatchedEvent.builder()
				.properties(MinuteWatchedProperties.builder()
						.channelId(STREAMER_ID)
						.channel(STREAMER_NAME)
						.broadcastId(STREAM_ID)
						.player(SITE_PLAYER)
						.userId(USER_ID)
						.live(true)
						.build())
				.build();
		
		when(twitchApi.sendPlayerEvents(spadeUrl, expected)).thenReturn(true);
		
		assertDoesNotThrow(() -> tested.run());
		
		verify(streamer, never()).addWatchedDuration(any());
	}
	
	@Test
	void sendingMinutesWatchedNoGame(){
		var expected = MinuteWatchedEvent.builder()
				.properties(MinuteWatchedProperties.builder()
						.channelId(STREAMER_ID)
						.channel(STREAMER_NAME)
						.broadcastId(STREAM_ID)
						.player(SITE_PLAYER)
						.userId(USER_ID)
						.live(true)
						.build())
				.build();
		
		when(twitchApi.sendPlayerEvents(spadeUrl, expected)).thenReturn(true);
		
		assertDoesNotThrow(() -> tested.run());
		
		verify(streamer, never()).addWatchedDuration(any());
	}
	
	@Test
	void sendingMinutesWatchedNotStreaming(){
		when(streamer.isStreaming()).thenReturn(false);
		
		assertDoesNotThrow(() -> tested.run());
		
		verify(twitchApi, never()).sendPlayerEvents(any(), any());
		verify(streamer, never()).addWatchedDuration(any());
	}
	
	@Test
	void sendingMinutesWatchedNoStreamId(){
		when(streamer.getStreamId()).thenReturn(Optional.empty());
		
		assertDoesNotThrow(() -> tested.run());
		
		verify(twitchApi, never()).sendPlayerEvents(any(), any());
		verify(streamer, never()).addWatchedDuration(any());
	}
	
	@Test
	void sendingMinutesWatchedNoSpadeUrl(){
		when(streamer.getSpadeUrl()).thenReturn(null);
		
		assertDoesNotThrow(() -> tested.run());
		
		verify(twitchApi, never()).sendPlayerEvents(any(), any());
		verify(streamer, never()).addWatchedDuration(any());
	}
	
	@Test
	void sendingMinutesWatchedChatBanned(){
		when(streamer.isChatBanned()).thenReturn(true);
		
		assertDoesNotThrow(() -> tested.run());
		
		verify(twitchApi, never()).sendPlayerEvents(any(), any());
		verify(streamer, never()).addWatchedDuration(any());
	}
	
	@Test
	void sendingMinutesWatchedSeveralStreamers() throws MalformedURLException{
		var spadeUrl2 = new URL("https://google.com/2");
		
		var streamer2 = mock(Streamer.class);
		when(streamer2.getId()).thenReturn("s2");
		when(streamer2.getUsername()).thenReturn("sn2");
		when(streamer2.getSpadeUrl()).thenReturn(spadeUrl2);
		when(streamer2.getStreamId()).thenReturn(Optional.of(STREAM_ID));
		when(streamer2.isStreaming()).thenReturn(true);
		
		when(twitchApi.sendPlayerEvents(any(), any())).thenReturn(true);
		
		when(miner.getStreamers()).thenReturn(List.of(streamer, streamer2));
		
		assertDoesNotThrow(() -> tested.run());
		
		verify(twitchApi, times(2)).sendPlayerEvents(any(), any());
		verify(streamer, never()).addWatchedDuration(any());
		verify(streamer2, never()).addWatchedDuration(any());
	}
	
	@Test
	void sendingMinutesWatchedMaxTwoStreamers(){
		when(miner.getStreamers()).thenReturn(List.of(streamer, streamer, streamer, streamer));
		
		when(twitchApi.sendPlayerEvents(any(), any())).thenReturn(true);
		
		assertDoesNotThrow(() -> tested.run());
		
		verify(twitchApi, times(2)).sendPlayerEvents(any(), any());
	}
	
	@Test
	void sendingMinutesWatchedException(){
		when(twitchApi.sendPlayerEvents(any(), any())).thenThrow(new RuntimeException("For tests"));
		
		assertDoesNotThrow(() -> tested.run());
	}
	
	@Test
	void sendingMinutesWatchedBestScores() throws MalformedURLException{
		var s1 = mock(Streamer.class);
		when(s1.getSpadeUrl()).thenReturn(new URL("https://spade1"));
		when(s1.isStreaming()).thenReturn(true);
		when(s1.getScore(miner)).thenReturn(10);
		
		var spade2 = new URL("https://spade2");
		var s2 = mock(Streamer.class);
		when(s2.getId()).thenReturn("s2");
		when(s2.getUsername()).thenReturn("sn2");
		when(s2.getSpadeUrl()).thenReturn(spade2);
		when(s2.getStreamId()).thenReturn(Optional.of("sid2"));
		when(s2.isStreaming()).thenReturn(true);
		when(s2.getScore(miner)).thenReturn(100);
		
		var s3 = mock(Streamer.class);
		when(s3.getSpadeUrl()).thenReturn(new URL("https://spade3"));
		when(s3.isStreaming()).thenReturn(true);
		when(s3.getScore(miner)).thenReturn(20);
		
		var spade4 = new URL("https://spade4");
		var s4 = mock(Streamer.class);
		when(s4.getId()).thenReturn("s4");
		when(s4.getUsername()).thenReturn("sn4");
		when(s4.getSpadeUrl()).thenReturn(spade4);
		when(s4.getStreamId()).thenReturn(Optional.of("sid4"));
		when(s4.isStreaming()).thenReturn(true);
		when(s4.getScore(miner)).thenReturn(50);
		
		when(miner.getStreamers()).thenReturn(List.of(s1, s2, s3, s4));
		when(twitchApi.sendPlayerEvents(any(), any())).thenReturn(true);
		
		assertDoesNotThrow(() -> tested.run());
		
		verify(twitchApi).sendPlayerEvents(eq(spade2), any());
		verify(twitchApi).sendPlayerEvents(eq(spade4), any());
	}
	
	@Test
	void sendingMinutesWatchedBestScoresEqualsPicksIndex() throws MalformedURLException{
		var s1 = mock(Streamer.class);
		when(s1.getSpadeUrl()).thenReturn(new URL("https://spade1"));
		when(s1.isStreaming()).thenReturn(true);
		when(s1.getScore(miner)).thenReturn(10);
		when(s1.getIndex()).thenReturn(1);
		
		var spade2 = new URL("https://spade2");
		var s2 = mock(Streamer.class);
		when(s2.getId()).thenReturn("s2");
		when(s2.getUsername()).thenReturn("sn2");
		when(s2.getSpadeUrl()).thenReturn(spade2);
		when(s2.getStreamId()).thenReturn(Optional.of("sid2"));
		when(s2.isStreaming()).thenReturn(true);
		when(s2.getIndex()).thenReturn(0);
		when(s2.getScore(miner)).thenReturn(10);
		
		var s3 = mock(Streamer.class);
		when(s3.getSpadeUrl()).thenReturn(new URL("https://spade3"));
		when(s3.isStreaming()).thenReturn(true);
		when(s3.getScore(miner)).thenReturn(10);
		when(s3.getIndex()).thenReturn(25);
		
		var spade4 = new URL("https://spade4");
		var s4 = mock(Streamer.class);
		when(s4.getId()).thenReturn("s4");
		when(s4.getUsername()).thenReturn("sn4");
		when(s4.getSpadeUrl()).thenReturn(spade4);
		when(s4.getStreamId()).thenReturn(Optional.of("sid4"));
		when(s4.isStreaming()).thenReturn(true);
		when(s4.getIndex()).thenReturn(-5);
		when(s4.getScore(miner)).thenReturn(10);
		
		when(miner.getStreamers()).thenReturn(List.of(s1, s2, s3, s4));
		when(twitchApi.sendPlayerEvents(any(), any())).thenReturn(true);
		
		assertDoesNotThrow(() -> tested.run());
		
		verify(twitchApi).sendPlayerEvents(eq(spade2), any());
		verify(twitchApi).sendPlayerEvents(eq(spade4), any());
	}
}