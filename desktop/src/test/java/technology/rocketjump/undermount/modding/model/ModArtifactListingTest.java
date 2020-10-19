package technology.rocketjump.undermount.modding.model;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class ModArtifactListingTest {

	@Test
	public void printNames() {
		List<String> names = new ArrayList<>();
		for (ModArtifactDefinition artifactDefinition : new ModArtifactListing().getAll()) {
			names.add(artifactDefinition.getName());
		}
		Collections.sort(names);
		System.out.println(StringUtils.join(names, "\n"));
	}
}