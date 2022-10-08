package fr.raksrinana.channelpointsminer.miner.priority;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonTypeName;
import fr.raksrinana.channelpointsminer.miner.miner.IMiner;
import fr.raksrinana.channelpointsminer.miner.streamer.Streamer;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.jetbrains.annotations.NotNull;

@JsonTypeName("watchStreak")
@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@JsonClassDescription("Return a score if the streamer has a potential watch streak to claim.")
public class WatchStreakPriority extends IStreamerPriority{
	@Override
	public int getScore(@NotNull IMiner miner, @NotNull Streamer streamer){
		return streamer.mayClaimStreak() ? getScore() : 0;
	}
}
