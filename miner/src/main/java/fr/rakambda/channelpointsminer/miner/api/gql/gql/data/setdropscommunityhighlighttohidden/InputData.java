package fr.rakambda.channelpointsminer.miner.api.gql.gql.data.setdropscommunityhighlighttohidden;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode
@ToString
public class InputData{
	@JsonProperty("channelID")
	private String channelID;
	@JsonProperty("campaignID")
	private String campaignID;
}
