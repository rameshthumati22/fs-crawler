package fr.pilato.elasticsearch.crawler.fs.tika.customparser;

import java.util.Map;

import org.apache.commons.collections4.map.HashedMap;

public class OCRResponseModel {
	private String extractedTextResponse;
	private int responseCode;
	private boolean useDefaultParser = false;
	private Map<String, Object> others = new HashedMap<>();

	public String getExtractedTextResponse() {
		return extractedTextResponse;
	}

	public void setExtractedTextResponse(String extractedTextResponse) {
		this.extractedTextResponse = extractedTextResponse;
	}

	public int getResponseCode() {
		return responseCode;
	}

	public void setResponseCode(int responseCode) {
		this.responseCode = responseCode;
	}

	public boolean isUseDefaultParser() {
		return useDefaultParser;
	}

	public void setUseDefaultParser(boolean useDefaultParser) {
		this.useDefaultParser = useDefaultParser;
	}

	public Map<String, Object> getOthers() {
		return others;
	}

	public void setOthers(String key, Object value) {
		others.put(key, value);
	}

}
