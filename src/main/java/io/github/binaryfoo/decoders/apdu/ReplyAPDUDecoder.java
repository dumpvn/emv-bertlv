package io.github.binaryfoo.decoders.apdu;

import io.github.binaryfoo.DecodedData;
import io.github.binaryfoo.EmvTags;
import io.github.binaryfoo.decoders.DecodeSession;
import io.github.binaryfoo.decoders.TLVDecoder;
import io.github.binaryfoo.tlv.Tag;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ReplyAPDUDecoder {
    private TLVDecoder tlvDecoder;

    public ReplyAPDUDecoder(TLVDecoder tlvDecoder) {
        this.tlvDecoder = tlvDecoder;
    }

    public DecodedData decode(String input, int startIndexInBytes, DecodeSession session) {
        int statusBytesStart = input.length() - 4;
        int endIndex;
        List<DecodedData> children;
        String decodedData;
        if (input.length() == 4) {
            ResponseCode responseCode = ResponseCode.lookup(input.substring(statusBytesStart));
            decodedData = responseCode.getHex() + " " + responseCode.getDescription();
            children = Collections.emptyList();
            endIndex = startIndexInBytes + 2;
        } else {
            decodedData = input.substring(statusBytesStart);
            children = tlvDecoder.decode(input.substring(0, statusBytesStart), startIndexInBytes, session);
            addToSession(session, children, Arrays.asList(EmvTags.PDOL, EmvTags.CDOL_1, EmvTags.CDOL_2));
            DecodedData payload = children.get(0);
            endIndex = payload.getEndIndex() + 2;
        }
        return DecodedData.constructed("R-APDU", decodedData, startIndexInBytes, endIndex, children);
    }

    private void addToSession(DecodeSession session, List<DecodedData> children, List<Tag> tags) {
        for (DecodedData child : children) {
            if (tags.contains(child.getTag())) {
                session.put(child.getTag(), child.getFullDecodedData());
            } else if (child.isComposite()) {
                addToSession(session, child.getChildren(), tags);
            }
        }
    }
}
