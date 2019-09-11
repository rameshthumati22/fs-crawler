package fr.pilato.elasticsearch.crawler.fs.tika.customparser;

import java.io.IOException;
import java.io.InputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.ResponseBody;
import com.squareup.okhttp.internal.Util;

import okio.BufferedSink;
import okio.Okio;
import okio.Source;

public class MicrosoftVisionClient {

	public static final MediaType MEDIA_TYPE = MediaType.parse("application/octet-stream");
	private final static Logger logger = LogManager.getLogger(MicrosoftVisionClient.class);

	/**
	 * Hitting Microsoft Batch Read file API - Computer Vision API - v2.0 to extract text from file (images and documents)
	 * Can handle hand-written, printed or mixed documents
	 * @param inputStream
	 * @param mocrosoftOcpApimSubscriptionKey
	 * @return
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	public static OCRResponseModel getResponse(InputStream inputStream, String mocrosoftOcpApimSubscriptionKey)
			throws IOException, InterruptedException {

		OCRResponseModel finalResponse = new OCRResponseModel();
		OkHttpClient client = new OkHttpClient();
		RequestBody body = createRequestBody(MEDIA_TYPE, inputStream);
		Request asyncBatchAnalyzeRequest = new Request.Builder()
				.url("https://westcentralus.api.cognitive.microsoft.com/vision/v2.0/read/core/asyncBatchAnalyze")
				.post(body).addHeader("ocp-apim-subscription-key", mocrosoftOcpApimSubscriptionKey)
				.addHeader("content-type", "application/octet-stream").build();
		Response asyncBatchAnalyzeResponse = client.newCall(asyncBatchAnalyzeRequest).execute();
		
		logger.trace("Batch Read File interface API response : " + asyncBatchAnalyzeResponse);
		
		/**
		 * Response 202 - Request accepted
		 * When you use the Batch Read File interface, the response contains a field called "Operation-Location". 
		 * The "Operation-Location" field contains the URL that you must use for your Get Read Operation Result operation to access OCR results.​
		 */
		if (asyncBatchAnalyzeResponse.code() == 202) {
			boolean isStillRunning = true;
			
			//processing time will depends on the size/capacity of the file. the loop will rotate until processing completes based on status
			while (isStillRunning) {
				//to read operation using Operation-Location URL present in the header of the previous response
				Request operationLocationRequest = new Request.Builder()
						.url(asyncBatchAnalyzeResponse.header("Operation-Location"))
						.addHeader("ocp-apim-subscription-key", mocrosoftOcpApimSubscriptionKey)
						.addHeader("content-type", "application/json").build();
				Response operationResponse = client.newCall(operationLocationRequest).execute();

				try(ResponseBody responseData = operationResponse.body()) {
					String res = responseData.string();
					finalResponse.setExtractedTextResponse(res);
					JSONObject obj = new JSONObject(res);
					//check still processing ? 
					isStillRunning = obj.get("status") != null && obj.get("status").equals("Running");
					if (!isStillRunning) {
						finalResponse.setResponseCode(operationResponse.code());
						break;
					}
				}
				//let the thread sleep for 50ms
				Thread.sleep(50);
			}
		} else {
			/**
			 * Custom OCR - Microsoft Computer Vision API - v2.0 - failure cases
			 * error codes- 400, 500, 503 -> BadArgument, InvalidImageURL, FailedToDownloadImage, InvalidImage, UnsupportedImageFormat
			 * InvalidImageSize, InvalidImageDimension 
			 */
			finalResponse.setResponseCode(asyncBatchAnalyzeResponse.code());
			//if custom OCR fails, redirect to default one
			finalResponse.setUseDefaultParser(true);
		}
		//clean up
		asyncBatchAnalyzeResponse.body().close();
		return finalResponse;
	}

	/**
	 * To create RequestBody for Microsoft - Batch Read File interface API
	 * @param mediaType
	 * @param inputStream
	 * @return
	 */
	public static RequestBody createRequestBody(final MediaType mediaType, final InputStream inputStream) {
		return new RequestBody() {

			@Override
			public MediaType contentType() {
				return mediaType;
			}

			@Override
			public long contentLength() {
				try {
					return inputStream.available();
				} catch (IOException e) {
					return 0;
				}
			}

			@Override
			public void writeTo(BufferedSink sink) throws IOException {
				Source source = null;
				try {
					source = Okio.source(inputStream);
					sink.writeAll(source);
				} finally {
					Util.closeQuietly(source);
				}
			}
		};
	}

}
