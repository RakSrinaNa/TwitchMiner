package fr.rakambda.channelpointsminer.miner.runnable;

import fr.rakambda.channelpointsminer.miner.api.gql.gql.data.types.Game;
import fr.rakambda.channelpointsminer.miner.api.twitch.data.MinuteWatchedEvent;
import fr.rakambda.channelpointsminer.miner.api.twitch.data.MinuteWatchedProperties;
import fr.rakambda.channelpointsminer.miner.factory.TimeFactory;
import fr.rakambda.channelpointsminer.miner.log.LogContext;
import fr.rakambda.channelpointsminer.miner.miner.IMiner;
import fr.rakambda.channelpointsminer.miner.streamer.Streamer;
import fr.rakambda.channelpointsminer.miner.util.CommonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Log4j2
@RequiredArgsConstructor
public abstract class SendMinutesWatched implements Runnable{
	@NotNull
	protected final IMiner miner;
	private final Map<String, Instant> lastSend = new ConcurrentHashMap<>();
	
	protected abstract String getType();
	
	protected abstract boolean checkStreamer(Streamer streamer);
	
	protected abstract boolean send(Streamer streamer);
	
	@Override
	public void run(){
		log.debug("Starting sending {} minutes watched", getType());
		
		try(var ignored = LogContext.with(miner)){
			var toSendMinutesWatched = miner.getStreamers().stream()
					.filter(Streamer::isStreaming)
					.filter(streamer -> !streamer.isChatBanned())
					.filter(this::checkStreamer)
					.map(streamer -> Map.entry(streamer, streamer.getScore(miner)))
					.sorted(this::compare)
					.limit(2)
					.map(Map.Entry::getKey)
					.toList();
			
			for(var streamer : toSendMinutesWatched){
				try(var ignored2 = LogContext.empty().withStreamer(streamer)){
					log.debug("Sending {} minutes watched", getType());
					if(send(streamer)){
						updateWatchedMinutes(streamer);
					}
					CommonUtils.randomSleep(100, 50);
				}
			}
			
			removeLastSend(toSendMinutesWatched);
			
			log.debug("Done sending all {} minutes watched", getType());
		}
		catch(Exception e){
			log.error("Failed to send all {} minutes watched", getType(), e);
		}
	}
	
	private int compare(@NotNull Map.Entry<Streamer, Integer> e1, @NotNull Map.Entry<Streamer, Integer> e2){
		var compareScore = Integer.compare(e2.getValue(), e1.getValue());
		if(compareScore != 0){
			return compareScore;
		}
		return Integer.compare(e1.getKey().getIndex(), e2.getKey().getIndex());
	}
	
	private boolean sendSpade(Streamer streamer){
		var streamId = streamer.getStreamId();
		if(streamId.isEmpty()){
			return false;
		}
		
		var request = MinuteWatchedEvent.builder()
				.properties(MinuteWatchedProperties.builder()
						.channelId(streamer.getId())
						.channel(streamer.getUsername())
						.broadcastId(streamId.get())
						.player("site")
						.userId(miner.getTwitchLogin().getUserIdAsInt(miner.getGqlApi()))
						.gameId(streamer.getGame().map(Game::getId).orElse(null))
						.game(streamer.getGame().map(Game::getName).orElse(null))
						.live(true)
						.build())
				.build();
		
		return miner.getTwitchApi().sendPlayerEvents(streamer.getSpadeUrl(), request);
	}
	
	private boolean sendM3u8(@NotNull Streamer streamer){
		return miner.getTwitchApi().openM3u8LastChunk(streamer.getM3u8Url());
	}
	
	private void updateWatchedMinutes(@NotNull Streamer streamer){
		var now = TimeFactory.now();
		var previousUpdate = lastSend.get(streamer.getId());
		if(Objects.nonNull(previousUpdate)){
			var duration = Duration.between(previousUpdate, now);
			streamer.addWatchedDuration(duration);
		}
		lastSend.put(streamer.getId(), now);
	}
	
	private void removeLastSend(@NotNull List<Streamer> currentStreamers){
		var currentIds = currentStreamers.stream().map(Streamer::getId).toList();
		var keysToRemove = lastSend.keySet().stream().filter(id -> !currentIds.contains(id)).toList();
		keysToRemove.forEach(lastSend::remove);
	}
}
