package fr.raksrinana.channelpointsminer.miner.config.login;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import java.nio.file.Path;
import java.nio.file.Paths;

@JsonTypeName("http")
@Getter
@EqualsAndHashCode
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonClassDescription("Login though Twitch's Passport API.")
public class HttpLoginMethod implements ILoginMethod{
	@NotNull
	@JsonProperty(value = "password", required = true)
	@JsonPropertyDescription("Password of your Twitch account.")
	@ToString.Exclude
	private String password;
	@JsonProperty("use2FA")
	@JsonPropertyDescription("If this account uses 2FA set this to true to directly ask for it. Default: false")
	@Builder.Default
	private boolean use2Fa = false;
	@NotNull
	@JsonProperty("authenticationFolder")
	@JsonPropertyDescription(value = "Path to a folder that contains authentication files used to log back in after a restart. Default: ./authentication")
	@Builder.Default
	private Path authenticationFolder = Paths.get("authentication");
}
