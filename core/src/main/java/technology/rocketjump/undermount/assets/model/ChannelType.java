package technology.rocketjump.undermount.assets.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import technology.rocketjump.undermount.misc.Name;
import technology.rocketjump.undermount.misc.SequentialId;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChannelType {

	@Name
	private final String channelTypeName;
	@SequentialId
	private long channelTypeId;

	@JsonCreator
	public ChannelType(@JsonProperty("channelTypeName") String channelTypeName,
					   @JsonProperty("channelTypeId") long channelTypeId) {
		this.channelTypeName = channelTypeName;
		this.channelTypeId = channelTypeId;
	}

	public String getChannelTypeName() {
		return channelTypeName;
	}

	public long getChannelTypeId() {
		return channelTypeId;
	}

	@Override
	public String toString() {
		return channelTypeName;
	}

}
