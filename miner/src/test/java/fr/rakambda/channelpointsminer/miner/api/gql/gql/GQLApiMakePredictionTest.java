package fr.rakambda.channelpointsminer.miner.api.gql.gql;

import fr.rakambda.channelpointsminer.miner.api.gql.gql.data.GQLResponse;
import fr.rakambda.channelpointsminer.miner.api.gql.gql.data.makeprediction.MakePredictionData;
import fr.rakambda.channelpointsminer.miner.api.gql.gql.data.types.MakePredictionError;
import fr.rakambda.channelpointsminer.miner.api.gql.gql.data.types.MakePredictionPayload;
import fr.rakambda.channelpointsminer.miner.tests.UnirestMockExtension;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@ExtendWith(UnirestMockExtension.class)
class GQLApiMakePredictionTest extends AbstractGQLTest{
	private static final String EVENT_ID = "event-id";
	private static final String OUTCOME_ID = "outcome-id";
	private static final int POINTS = 20;
	private static final String TRANSACTION_ID = "transaction-id";
	
	@Test
	void nominalMakePrediction(){
		var expected = GQLResponse.<MakePredictionData> builder()
				.extensions(Map.of(
						"durationMilliseconds", 127,
						"operationName", "MakePrediction",
						"requestID", "request-id"
				))
				.data(MakePredictionData.builder()
						.makePrediction(MakePredictionPayload.builder().build())
						.build())
				.build();
		
		expectValidRequestOkWithIntegrityOk("api/gql/gql/makePrediction_success.json");
		
		assertThat(tested.makePrediction(EVENT_ID, OUTCOME_ID, POINTS, TRANSACTION_ID)).contains(expected);
		
		verifyAll();
	}
	
	@Test
	void errorMakePrediction(){
		var expected = GQLResponse.<MakePredictionData> builder()
				.extensions(Map.of(
						"durationMilliseconds", 37,
						"operationName", "MakePrediction",
						"requestID", "request-id"
				))
				.data(MakePredictionData.builder()
						.makePrediction(MakePredictionPayload.builder()
								.error(MakePredictionError.builder()
										.code("NOT_ENOUGH_POINTS")
										.build())
								.build())
						.build())
				.build();
		
		expectValidRequestOkWithIntegrityOk("api/gql/gql/makePrediction_notEnoughPoints.json");
		
		assertThat(tested.makePrediction(EVENT_ID, OUTCOME_ID, POINTS, TRANSACTION_ID)).contains(expected);
		
		verifyAll();
	}
	
	@Override
	protected String getValidRequest(){
		return "{\"extensions\":{\"persistedQuery\":{\"sha256Hash\":\"b44682ecc88358817009f20e69d75081b1e58825bb40aa53d5dbadcc17c881d8\",\"version\":1}},\"operationName\":\"MakePrediction\",\"variables\":{\"input\":{\"eventID\":\"%s\",\"outcomeID\":\"%s\",\"points\":%d,\"transactionID\":\"%s\"}}}".formatted(EVENT_ID, OUTCOME_ID, POINTS, TRANSACTION_ID);
	}
}