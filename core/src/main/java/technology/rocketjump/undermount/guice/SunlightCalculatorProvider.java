package technology.rocketjump.undermount.guice;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Provider;
import com.google.inject.ProvisionException;
import technology.rocketjump.undermount.assets.FloorTypeDictionary;
import technology.rocketjump.undermount.environment.SunlightCalculator;
import technology.rocketjump.undermount.environment.model.SunlightPhase;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;


public class SunlightCalculatorProvider implements Provider<SunlightCalculator> {
	@Override
	public SunlightCalculator get() {
		try {
			FileHandle sunlightJsonFile = Gdx.files.internal("assets/settings/sunlight.json");
			JSONArray jsonArray = JSON.parseArray(sunlightJsonFile.readString());
			List<SunlightPhase> sunlightPhaseList = new LinkedList<>();
			ObjectMapper objectMapper = new ObjectMapper();

			for (int index = 0; index < jsonArray.size(); index++) {
				SunlightPhase sunlightPhase = objectMapper.readValue(jsonArray.getJSONObject(index).toString(), SunlightPhase.class);
				sunlightPhaseList.add(sunlightPhase);
			}

			return new SunlightCalculator(sunlightPhaseList);
		} catch (IOException e) {
			throw new ProvisionException("Failed to createHumanoid " + FloorTypeDictionary.class.getSimpleName(), e);
		}
	}
}
