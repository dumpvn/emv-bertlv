package io.github.binaryfoo.decoders;

import java.util.Arrays;
import java.util.List;

import io.github.binaryfoo.DecodedData;
import io.github.binaryfoo.Decoder;
import io.github.binaryfoo.tlv.ISOUtil;
import org.jetbrains.annotations.NotNull;

public class CVMResultsDecoder implements Decoder {

	private static final int FIELD_LENGTH = 6;

	@Override
	public List<DecodedData> decode(@NotNull String input, int startIndexInBytes, @NotNull DecodeSession decodeSession) {
		CVRule rule = new CVRule(input.substring(0, 4));
		String result = input.substring(4, 6);
		return Arrays.asList(
                DecodedData.primitive(input.substring(0, 2), rule.getVerificationMethodDescription(), startIndexInBytes, startIndexInBytes + 1),
                DecodedData.primitive(input.substring(2, 4), rule.getConditionCodeDescription(), startIndexInBytes + 1, startIndexInBytes + 2),
                DecodedData.primitive(result, decodeResult(result), startIndexInBytes + 2, startIndexInBytes + 3)
        );
	}

	private String decodeResult(String result) {
		return "01".equals(result) ? "Failed" :
			   "02".equals(result) ? "Sucessful" : "Unknown";
	}
	
	@Override
	public int getMaxLength() {
		return FIELD_LENGTH;
	}

	@Override
	public String validate(String bitString) {
		if (bitString == null || bitString.length() != FIELD_LENGTH) {
			return String.format("Value must be exactly %d characters", FIELD_LENGTH); 
		}
		if (!ISOUtil.isValidHexString(bitString)) {
			return "Value must contain only the characters 0-9 and A-F";
		}
		return null;
	}

}
